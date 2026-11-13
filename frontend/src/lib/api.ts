import { auth } from './firebase';
import type { AdminOverviewResponse, AppSettings, Book, ContributionItem, RatingTierKey } from '../types';

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '');

async function getAuthHeaders(): Promise<Record<string, string>> {
  const user = auth?.currentUser;
  if (!user) return { 'Content-Type': 'application/json' };
  const token = await user.getIdToken();
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };
}

async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  if (!API_BASE) {
    throw new Error('VITE_API_BASE_URL is not configured yet.');
  }
  const headers = await getAuthHeaders();
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      ...headers,
      ...(init?.headers || {}),
    },
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.error || `Request failed (${res.status})`);
  }
  return data as T;
}

export const nokhookApi = {
  isConfigured: () => Boolean(API_BASE),
  getHealth: () => apiFetch<{ status: string; integrations: Record<string, string> }>('/api/health'),
  getBooks: () => apiFetch<{ books: Book[] }>('/api/books'),
  upsertBook: (book: Book) =>
    apiFetch<{ ok: boolean; id: string }>('/api/books', {
      method: 'POST',
      body: JSON.stringify(book),
    }),
  deleteBook: (id: string) =>
    apiFetch<{ ok: boolean }>(`/api/books?id=${encodeURIComponent(id)}`, {
      method: 'DELETE',
    }),
  getSettings: () => apiFetch<{ settings: AppSettings | null }>('/api/settings'),
  updateSettings: (settings: AppSettings) =>
    apiFetch<{ ok: boolean }>('/api/settings', {
      method: 'PUT',
      body: JSON.stringify(settings),
    }),
  submitRatingVote: (bookId: string, ratingTier: RatingTierKey, status: string) =>
    apiFetch<{ ok: boolean }>('/api/ratings', {
      method: 'POST',
      body: JSON.stringify({ bookId, ratingTier, status }),
    }),
  verifyAdminMe: () => apiFetch<{ email: string; isAdmin: boolean }>('/api/admin/me'),
  getAdminOverview: () => apiFetch<AdminOverviewResponse>('/api/admin/overview'),
  getContributions: () => apiFetch<{ contributions: ContributionItem[] }>('/api/contributions'),
  moderateContribution: (id: string, status: 'pending' | 'approved' | 'rejected') =>
    apiFetch<{ ok: boolean }>('/api/contributions', {
      method: 'PATCH',
      body: JSON.stringify({ id, status }),
    }),
  async uploadImageToCloudinary(file: File, folder = 'nokhook/covers'): Promise<{ url: string; publicId: string }> {
    const signed = await apiFetch<{
      cloudName: string;
      apiKey: string;
      timestamp: number;
      folder: string;
      signature: string;
    }>('/api/admin/upload-signature', {
      method: 'POST',
      body: JSON.stringify({ folder }),
    });

    const formData = new FormData();
    formData.append('file', file);
    formData.append('api_key', signed.apiKey);
    formData.append('timestamp', String(signed.timestamp));
    formData.append('signature', signed.signature);
    formData.append('folder', signed.folder);

    const uploadRes = await fetch(`https://api.cloudinary.com/v1_1/${signed.cloudName}/image/upload`, {
      method: 'POST',
      body: formData,
    });
    const uploaded = await uploadRes.json();
    if (!uploadRes.ok) {
      throw new Error(uploaded.error?.message || 'Cloudinary upload failed');
    }
    return {
      url: uploaded.secure_url as string,
      publicId: uploaded.public_id as string,
    };
  },
};
