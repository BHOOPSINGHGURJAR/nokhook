import type { VercelRequest, VercelResponse } from '@vercel/node';
import { z } from 'zod';
import { applyCors } from '../lib/cors';
import { requireAdminUser } from '../lib/firebaseAdmin';
import { ensureNeonSchema, getSql } from '../lib/neon';

const vibeSliceSchema = z.object({
  name: z.string().min(1).max(40),
  percentage: z.number().int().min(0).max(100),
  colorHex: z.string().default('#7C3AED'),
});

const ratingStatsSchema = z.object({
  mustReadVotes: z.number().int().min(0),
  worthReadingVotes: z.number().int().min(0),
  lightReadVotes: z.number().int().min(0),
  skipVotes: z.number().int().min(0),
});

const bookInputSchema = z.object({
  id: z.string().min(2).max(120),
  title: z.string().min(1).max(180),
  subtitle: z.string().max(240).default(''),
  authorName: z.string().min(1).max(120),
  publisherName: z.string().max(120).default('Independent'),
  year: z.number().int().min(1400).max(2100).default(2026),
  pages: z.number().int().min(1).max(5000).default(320),
  language: z.string().min(1).max(40).default('English'),
  country: z.string().min(1).max(60).default('India'),
  genre: z.string().min(1).max(60).default('Fiction'),
  subGenre: z.string().max(60).default(''),
  overview: z.string().max(4000).default(''),
  coverUrl: z.string().max(1000).default(''),
  cloudinaryPublicId: z.string().max(240).default(''),
  vibes: z.array(vibeSliceSchema).default([]),
  ratingStats: ratingStatsSchema.default({
    mustReadVotes: 10,
    worthReadingVotes: 5,
    lightReadVotes: 2,
    skipVotes: 1,
  }),
  isAppSelect: z.boolean().default(false),
  isTalkOfTown: z.boolean().default(false),
  isEditorsPick: z.boolean().default(false),
  isUpcoming: z.boolean().default(false),
  releaseDateLabel: z.string().max(60).default(''),
  monthlyRank: z.number().int().min(1).max(999).default(99),
});

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (applyCors(req, res)) return;

  try {
    await ensureNeonSchema();
    const sql = getSql();

    if (req.method === 'GET') {
      const rows = await sql`
        SELECT
          id, title, subtitle, author_name AS "authorName",
          publisher_name AS "publisherName", year, pages, language, country,
          genre, sub_genre AS "subGenre", overview, cover_url AS "coverUrl",
          cloudinary_public_id AS "cloudinaryPublicId", vibes,
          rating_stats AS "ratingStats", is_app_select AS "isAppSelect",
          is_talk_of_town AS "isTalkOfTown", is_editors_pick AS "isEditorsPick",
          is_upcoming AS "isUpcoming", release_date_label AS "releaseDateLabel",
          monthly_rank AS "monthlyRank", updated_by AS "updatedBy",
          updated_at AS "updatedAt"
        FROM books
        ORDER BY monthly_rank ASC, updated_at DESC
      `;
      return res.status(200).json({ books: rows });
    }

    if (req.method === 'POST' || req.method === 'PUT') {
      const admin = await requireAdminUser(req, res);
      if (!admin) return;

      const parsed = bookInputSchema.safeParse(req.body);
      if (!parsed.success) {
        return res.status(400).json({ error: 'Invalid book payload', details: parsed.error.flatten() });
      }

      const b = parsed.data;
      const adminEmail = admin.email!;

      await sql`
        INSERT INTO books (
          id, title, subtitle, author_name, publisher_name, year, pages,
          language, country, genre, sub_genre, overview, cover_url,
          cloudinary_public_id, vibes, rating_stats, is_app_select,
          is_talk_of_town, is_editors_pick, is_upcoming, release_date_label,
          monthly_rank, updated_by, updated_at
        ) VALUES (
          ${b.id}, ${b.title}, ${b.subtitle}, ${b.authorName}, ${b.publisherName},
          ${b.year}, ${b.pages}, ${b.language}, ${b.country}, ${b.genre},
          ${b.subGenre}, ${b.overview}, ${b.coverUrl}, ${b.cloudinaryPublicId},
          ${JSON.stringify(b.vibes)}::jsonb, ${JSON.stringify(b.ratingStats)}::jsonb,
          ${b.isAppSelect}, ${b.isTalkOfTown}, ${b.isEditorsPick}, ${b.isUpcoming},
          ${b.releaseDateLabel}, ${b.monthlyRank}, ${adminEmail}, NOW()
        )
        ON CONFLICT (id) DO UPDATE SET
          title = EXCLUDED.title,
          subtitle = EXCLUDED.subtitle,
          author_name = EXCLUDED.author_name,
          publisher_name = EXCLUDED.publisher_name,
          year = EXCLUDED.year,
          pages = EXCLUDED.pages,
          language = EXCLUDED.language,
          country = EXCLUDED.country,
          genre = EXCLUDED.genre,
          sub_genre = EXCLUDED.sub_genre,
          overview = EXCLUDED.overview,
          cover_url = EXCLUDED.cover_url,
          cloudinary_public_id = EXCLUDED.cloudinary_public_id,
          vibes = EXCLUDED.vibes,
          rating_stats = EXCLUDED.rating_stats,
          is_app_select = EXCLUDED.is_app_select,
          is_talk_of_town = EXCLUDED.is_talk_of_town,
          is_editors_pick = EXCLUDED.is_editors_pick,
          is_upcoming = EXCLUDED.is_upcoming,
          release_date_label = EXCLUDED.release_date_label,
          monthly_rank = EXCLUDED.monthly_rank,
          updated_by = EXCLUDED.updated_by,
          updated_at = NOW()
      `;

      await sql`
        INSERT INTO admin_audit_logs (admin_email, action, target_type, target_id, payload)
        VALUES (${adminEmail}, ${req.method === 'POST' ? 'UPSERT_BOOK' : 'UPDATE_BOOK'}, 'book', ${b.id}, ${JSON.stringify({ title: b.title })}::jsonb)
      `;

      return res.status(200).json({ ok: true, id: b.id });
    }

    if (req.method === 'DELETE') {
      const admin = await requireAdminUser(req, res);
      if (!admin) return;

      const bookId = typeof req.query.id === 'string' ? req.query.id : '';
      if (!bookId) {
        return res.status(400).json({ error: 'Query param id is required.' });
      }

      await sql`DELETE FROM books WHERE id = ${bookId}`;
      await sql`
        INSERT INTO admin_audit_logs (admin_email, action, target_type, target_id, payload)
        VALUES (${admin.email!}, 'DELETE_BOOK', 'book', ${bookId}, '{}'::jsonb)
      `;
      return res.status(200).json({ ok: true, deletedId: bookId });
    }

    return res.status(405).json({ error: 'Method not allowed' });
  } catch (error) {
    return res.status(500).json({
      error: 'Database error while processing books',
      details: error instanceof Error ? error.message : 'Unknown error',
    });
  }
}
