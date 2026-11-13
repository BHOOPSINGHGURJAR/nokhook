import type { VercelRequest, VercelResponse } from '@vercel/node';
import { cert, getApps, initializeApp } from 'firebase-admin/app';
import { getAuth, type DecodedIdToken } from 'firebase-admin/auth';

const DEFAULT_ADMIN_EMAILS = [
  'bhoopsinghgurjar001@gmail.com',
  'bs9396684@gmail.com',
];

export function getAdminAllowlist(): Set<string> {
  const raw = process.env.ADMIN_EMAILS;
  const list = raw
    ? raw.split(',').map((item) => item.trim().toLowerCase()).filter(Boolean)
    : DEFAULT_ADMIN_EMAILS;
  return new Set(list);
}

export function isAdminEmail(email?: string | null): boolean {
  if (!email) return false;
  return getAdminAllowlist().has(email.trim().toLowerCase());
}

function ensureFirebaseAdmin() {
  if (getApps().length > 0) return;

  const projectId = process.env.FIREBASE_PROJECT_ID;
  const clientEmail = process.env.FIREBASE_CLIENT_EMAIL;
  const privateKey = process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n');

  if (!projectId || !clientEmail || !privateKey) {
    throw new Error(
      'Firebase Admin credentials (FIREBASE_PROJECT_ID, FIREBASE_CLIENT_EMAIL, FIREBASE_PRIVATE_KEY) are missing.'
    );
  }

  initializeApp({
    credential: cert({
      projectId,
      clientEmail,
      privateKey,
    }),
  });
}

export async function verifyFirebaseToken(req: VercelRequest): Promise<DecodedIdToken | null> {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return null;
  }
  const token = authHeader.slice('Bearer '.length).trim();
  if (!token) return null;

  ensureFirebaseAdmin();
  return getAuth().verifyIdToken(token);
}

export async function requireAuthenticatedUser(
  req: VercelRequest,
  res: VercelResponse
): Promise<DecodedIdToken | null> {
  try {
    const decoded = await verifyFirebaseToken(req);
    if (!decoded) {
      res.status(401).json({ error: 'Authentication required. Provide a valid Firebase Bearer token.' });
      return null;
    }
    return decoded;
  } catch (error) {
    res.status(401).json({
      error: 'Invalid or expired Firebase ID token.',
      details: error instanceof Error ? error.message : 'Unauthorized',
    });
    return null;
  }
}

export async function requireAdminUser(
  req: VercelRequest,
  res: VercelResponse
): Promise<DecodedIdToken | null> {
  const decoded = await requireAuthenticatedUser(req, res);
  if (!decoded) return null;

  if (!decoded.email || decoded.email_verified !== true || !isAdminEmail(decoded.email)) {
    res.status(403).json({
      error: 'Forbidden: Only authorized nokhook editors can access the Admin Panel or edit app data.',
    });
    return null;
  }

  return decoded;
}
