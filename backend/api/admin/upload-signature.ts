import type { VercelRequest, VercelResponse } from '@vercel/node';
import { applyCors } from '../../lib/cors';
import { createSignedUploadParams } from '../../lib/cloudinary';
import { requireAdminUser } from '../../lib/firebaseAdmin';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (applyCors(req, res)) return;

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const admin = await requireAdminUser(req, res);
  if (!admin) return;

  try {
    const folder = typeof req.body?.folder === 'string' && req.body.folder.startsWith('nokhook/')
      ? req.body.folder
      : 'nokhook/covers';
    const params = createSignedUploadParams(folder);
    return res.status(200).json(params);
  } catch (error) {
    return res.status(500).json({
      error: 'Failed to sign Cloudinary upload',
      details: error instanceof Error ? error.message : 'Unknown error',
    });
  }
}
