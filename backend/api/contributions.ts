import type { VercelRequest, VercelResponse } from '@vercel/node';
import { z } from 'zod';
import { applyCors } from '../lib/cors';
import { requireAdminUser, requireAuthenticatedUser } from '../lib/firebaseAdmin';
import { getSql } from '../lib/neon';

const submitSchema = z.object({
  bookTitle: z.string().min(1).max(150),
  authorName: z.string().min(1).max(100),
  publisher: z.string().max(100).default(''),
  language: z.string().min(1).max(40).default('English'),
  genre: z.string().min(1).max(50).default('Fiction'),
  notes: z.string().max(1000).default(''),
});

const moderateSchema = z.object({
  id: z.string().min(1),
  status: z.enum(['pending', 'approved', 'rejected']),
});

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (applyCors(req, res)) return;

  try {
    const sql = getSql();

    if (req.method === 'GET') {
      const admin = await requireAdminUser(req, res);
      if (!admin) return;

      const rows = await sql`
        SELECT
          id, user_uid AS "userUid", user_email AS "userEmail",
          book_title AS "bookTitle", author_name AS "authorName",
          publisher, language, genre, notes, status,
          reviewed_by AS "reviewedBy", created_at AS "createdAt"
        FROM contributions
        ORDER BY created_at DESC
      `;
      return res.status(200).json({ contributions: rows });
    }

    if (req.method === 'POST') {
      const user = await requireAuthenticatedUser(req, res);
      if (!user) return;

      const parsed = submitSchema.safeParse(req.body);
      if (!parsed.success) {
        return res.status(400).json({ error: 'Invalid contribution payload', details: parsed.error.flatten() });
      }

      const c = parsed.data;
      const id = `contrib_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;

      await sql`
        INSERT INTO contributions (
          id, user_uid, user_email, book_title, author_name,
          publisher, language, genre, notes, status
        ) VALUES (
          ${id}, ${user.uid}, ${user.email ?? ''}, ${c.bookTitle}, ${c.authorName},
          ${c.publisher}, ${c.language}, ${c.genre}, ${c.notes}, 'pending'
        )
      `;

      return res.status(201).json({ ok: true, id });
    }

    if (req.method === 'PATCH') {
      const admin = await requireAdminUser(req, res);
      if (!admin) return;

      const parsed = moderateSchema.safeParse(req.body);
      if (!parsed.success) {
        return res.status(400).json({ error: 'Invalid moderation payload', details: parsed.error.flatten() });
      }

      const { id, status } = parsed.data;
      await sql`
        UPDATE contributions
        SET status = ${status}, reviewed_by = ${admin.email!}, updated_at = NOW()
        WHERE id = ${id}
      `;

      await sql`
        INSERT INTO admin_audit_logs (admin_email, action, target_type, target_id, payload)
        VALUES (${admin.email!}, 'MODERATE_CONTRIBUTION', 'contribution', ${id}, ${JSON.stringify({ status })}::jsonb)
      `;

      return res.status(200).json({ ok: true, id, status });
    }

    return res.status(405).json({ error: 'Method not allowed' });
  } catch (error) {
    return res.status(500).json({
      error: 'Failed to process contributions',
      details: error instanceof Error ? error.message : 'Unknown error',
    });
  }
}
