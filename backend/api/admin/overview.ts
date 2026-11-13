import type { VercelRequest, VercelResponse } from '@vercel/node';
import { applyCors } from '../../lib/cors';
import { requireAdminUser } from '../../lib/firebaseAdmin';
import { getSql } from '../../lib/neon';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (applyCors(req, res)) return;

  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const admin = await requireAdminUser(req, res);
  if (!admin) return;

  try {
    const sql = getSql();
    const [counts] = await sql`
      SELECT
        (SELECT COUNT(*)::int FROM books) AS "totalBooks",
        (SELECT COUNT(*)::int FROM reviews) AS "totalReviews",
        (SELECT COUNT(*)::int FROM user_ratings) AS "totalRatings",
        (SELECT COUNT(*)::int FROM contributions WHERE status = 'pending') AS "pendingContributions"
    `;
    const recentLogs = await sql`
      SELECT id, admin_email AS "adminEmail", action, target_type AS "targetType", target_id AS "targetId", payload, created_at AS "createdAt"
      FROM admin_audit_logs
      ORDER BY created_at DESC
      LIMIT 25
    `;

    return res.status(200).json({
      adminEmail: admin.email,
      stats: counts,
      recentLogs,
    });
  } catch (error) {
    return res.status(500).json({
      error: 'Failed to load admin overview',
      details: error instanceof Error ? error.message : 'Unknown error',
    });
  }
}
