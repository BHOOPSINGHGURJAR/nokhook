import type { VercelRequest, VercelResponse } from '@vercel/node';
import { applyCors } from '../../lib/cors';
import { getAdminAllowlist, isAdminEmail, requireAuthenticatedUser } from '../../lib/firebaseAdmin';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (applyCors(req, res)) return;

  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const user = await requireAuthenticatedUser(req, res);
  if (!user) return;

  const email = (user.email ?? '').trim().toLowerCase();
  const isAdmin = user.email_verified === true && isAdminEmail(email);

  return res.status(200).json({
    uid: user.uid,
    email,
    isAdmin,
    allowedAdmins: isAdmin ? Array.from(getAdminAllowlist()) : undefined,
  });
}
