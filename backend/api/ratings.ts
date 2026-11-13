import type { VercelRequest, VercelResponse } from '@vercel/node';
import { z } from 'zod';
import { applyCors } from '../lib/cors';
import { requireAuthenticatedUser } from '../lib/firebaseAdmin';
import { getSql } from '../lib/neon';

const voteSchema = z.object({
  bookId: z.string().min(1).max(120),
  ratingTier: z.enum(['must_read', 'worth_reading', 'light_read', 'skip']),
  status: z.enum(['none', 'read', 'tbr', 'interested']).default('read'),
});

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (applyCors(req, res)) return;

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const user = await requireAuthenticatedUser(req, res);
  if (!user) return;

  const parsed = voteSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ error: 'Invalid rating vote payload', details: parsed.error.flatten() });
  }

  try {
    const sql = getSql();
    const { bookId, ratingTier, status } = parsed.data;

    await sql`
      INSERT INTO user_ratings (user_uid, book_id, status, rating_tier, updated_at)
      VALUES (${user.uid}, ${bookId}, ${status}, ${ratingTier}, NOW())
      ON CONFLICT (user_uid, book_id) DO UPDATE SET
        status = EXCLUDED.status,
        rating_tier = EXCLUDED.rating_tier,
        updated_at = NOW()
    `;

    return res.status(200).json({ ok: true, bookId, ratingTier, status });
  } catch (error) {
    return res.status(500).json({
      error: 'Failed to record rating vote',
      details: error instanceof Error ? error.message : 'Unknown error',
    });
  }
}
