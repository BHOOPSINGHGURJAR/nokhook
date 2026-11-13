import { v2 as cloudinary } from 'cloudinary';

export interface CloudinaryCredentials {
  cloudName: string;
  apiKey: string;
  apiSecret: string;
}

function parseCloudinaryUrl(rawUrl?: string): CloudinaryCredentials | null {
  if (!rawUrl) return null;
  const trimmed = rawUrl.trim();
  const match = trimmed.match(/^cloudinary:\/\/([^:]+):([^@]+)@([^/?\s]+)$/i);
  if (!match) return null;

  const stripBrackets = (val: string) => val.replace(/^<|>$/g, '').trim();
  const apiKey = stripBrackets(match[1]);
  const apiSecret = stripBrackets(match[2]);
  const cloudName = stripBrackets(match[3]);

  if (!cloudName || !apiKey || !apiSecret) return null;
  return { cloudName, apiKey, apiSecret };
}

export function resolveCloudinaryCredentials(): CloudinaryCredentials | null {
  const fromUrl = parseCloudinaryUrl(process.env.CLOUDINARY_URL);
  if (fromUrl) return fromUrl;

  const cloudName = process.env.CLOUDINARY_CLOUD_NAME?.trim();
  const apiKey = process.env.CLOUDINARY_API_KEY?.trim();
  const apiSecret = process.env.CLOUDINARY_API_SECRET?.trim();

  if (cloudName && apiKey && apiSecret) {
    return { cloudName, apiKey, apiSecret };
  }
  return null;
}

export function isCloudinaryConfigured(): boolean {
  return resolveCloudinaryCredentials() !== null;
}

function ensureCloudinaryConfigured(): CloudinaryCredentials {
  const creds = resolveCloudinaryCredentials();
  if (!creds) {
    throw new Error(
      'Cloudinary credentials missing. Set CLOUDINARY_URL or CLOUDINARY_CLOUD_NAME + CLOUDINARY_API_KEY + CLOUDINARY_API_SECRET.'
    );
  }

  cloudinary.config({
    cloud_name: creds.cloudName,
    api_key: creds.apiKey,
    api_secret: creds.apiSecret,
    secure: true,
  });

  return creds;
}

export function createSignedUploadParams(folder = 'nokhook/covers') {
  const { cloudName, apiKey, apiSecret } = ensureCloudinaryConfigured();
  const timestamp = Math.round(Date.now() / 1000);
  const signature = cloudinary.utils.api_sign_request(
    { timestamp, folder },
    apiSecret
  );

  return {
    cloudName,
    apiKey,
    timestamp,
    folder,
    signature,
  };
}
