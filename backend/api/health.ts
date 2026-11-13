import type { VercelRequest, VercelResponse } from '@vercel/node';
import { isCloudinaryConfigured } from '../lib/cloudinary';
import { applyCors } from '../lib/cors';
import { getSql } from '../lib/neon';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (applyCors(req, res)) return;

  let neonStatus: 'connected' | 'unconfigured' | 'error' = 'unconfigured';
  if (process.env.DATABASE_URL) {
    try {
      const sql = getSql();
      await sql`SELECT 1`;
      neonStatus = 'connected';
    } catch {
      neonStatus = 'error';
    }
  }

  const firebaseConfigured = Boolean(
    process.env.FIREBASE_PROJECT_ID &&
      process.env.FIREBASE_CLIENT_EMAIL &&
      process.env.FIREBASE_PRIVATE_KEY
  );

  const cloudinaryConfigured = isCloudinaryConfigured();

  res.status(200).json({
    service: 'nokhook-vercel-api',
    status: 'ok',
    timestamp: new Date().toISOString(),
    integrations: {
      neondb: neonStatus,
      firebaseAuth: firebaseConfigured ? 'configured' : 'unconfigured',
      cloudinary: cloudinaryConfigured ? 'configured' : 'unconfigured',
    },
  });
}
