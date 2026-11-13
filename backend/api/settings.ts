import type { VercelRequest, VercelResponse } from '@vercel/node';
import { z } from 'zod';
import { applyCors } from '../lib/cors';
import { requireAdminUser } from '../lib/firebaseAdmin';
import { ensureNeonSchema, getSql } from '../lib/neon';

const settingsSchema = z.object({
  appName: z.string().min(1).max(40),
  tagline: z.string().min(1).max(200),
  announcementBanner: z.string().max(240).default(''),
  spotlightTitle: z.string().min(1).max(160),
  spotlightSubtitle: z.string().max(240).default(''),
  spotlightImageUrl: z.string().max(1000).default(''),
  requireLoginForReviews: z.boolean().default(true),
  allowContributions: z.boolean().default(true),
  maintenanceMode: z.boolean().default(false),
});

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (applyCors(req, res)) return;

  try {
    await ensureNeonSchema();
    const sql = getSql();

    if (req.method === 'GET') {
      const rows = await sql`
        SELECT
          app_name AS "appName",
          tagline,
          announcement_banner AS "announcementBanner",
          spotlight_title AS "spotlightTitle",
          spotlight_subtitle AS "spotlightSubtitle",
          spotlight_image_url AS "spotlightImageUrl",
          require_login_for_reviews AS "requireLoginForReviews",
          allow_contributions AS "allowContributions",
          maintenance_mode AS "maintenanceMode",
          updated_by AS "updatedBy",
          updated_at AS "updatedAt"
        FROM app_settings
        WHERE id = 'global'
        LIMIT 1
      `;
      return res.status(200).json({ settings: rows[0] ?? null });
    }

    if (req.method === 'PUT') {
      const admin = await requireAdminUser(req, res);
      if (!admin) return;

      const parsed = settingsSchema.safeParse(req.body);
      if (!parsed.success) {
        return res.status(400).json({ error: 'Invalid settings payload', details: parsed.error.flatten() });
      }

      const s = parsed.data;
      const adminEmail = admin.email!;

      await sql`
        INSERT INTO app_settings (
          id, app_name, tagline, announcement_banner, spotlight_title,
          spotlight_subtitle, spotlight_image_url, require_login_for_reviews,
          allow_contributions, maintenance_mode, updated_by, updated_at
        ) VALUES (
          'global', ${s.appName}, ${s.tagline}, ${s.announcementBanner}, ${s.spotlightTitle},
          ${s.spotlightSubtitle}, ${s.spotlightImageUrl}, ${s.requireLoginForReviews},
          ${s.allowContributions}, ${s.maintenanceMode}, ${adminEmail}, NOW()
        )
        ON CONFLICT (id) DO UPDATE SET
          app_name = EXCLUDED.app_name,
          tagline = EXCLUDED.tagline,
          announcement_banner = EXCLUDED.announcement_banner,
          spotlight_title = EXCLUDED.spotlight_title,
          spotlight_subtitle = EXCLUDED.spotlight_subtitle,
          spotlight_image_url = EXCLUDED.spotlight_image_url,
          require_login_for_reviews = EXCLUDED.require_login_for_reviews,
          allow_contributions = EXCLUDED.allow_contributions,
          maintenance_mode = EXCLUDED.maintenance_mode,
          updated_by = EXCLUDED.updated_by,
          updated_at = NOW()
      `;

      await sql`
        INSERT INTO admin_audit_logs (admin_email, action, target_type, target_id, payload)
        VALUES (${adminEmail}, 'UPDATE_APP_SETTINGS', 'settings', 'global', ${JSON.stringify(s)}::jsonb)
      `;

      return res.status(200).json({ ok: true });
    }

    return res.status(405).json({ error: 'Method not allowed' });
  } catch (error) {
    return res.status(500).json({
      error: 'Failed to process app settings',
      details: error instanceof Error ? error.message : 'Unknown error',
    });
  }
}
