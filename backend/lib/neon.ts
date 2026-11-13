import { neon } from '@neondatabase/serverless';

export function getSql() {
  const raw = process.env.DATABASE_URL?.trim() ?? '';
  const url = raw.replace(/^['"(]+|['")]+$/g, '').trim();
  if (!url) {
    throw new Error('DATABASE_URL environment variable is not configured for NeonDB.');
  }
  return neon(url);
}

let schemaInitialized = false;
let schemaInitPromise: Promise<void> | null = null;

/**
 * Ensures all required NeonDB PostgreSQL tables exist and seeds flagship catalog rows if empty.
 * Caches completion per warm Vercel serverless instance.
 */
export async function ensureNeonSchema() {
  if (schemaInitialized) return;
  if (!schemaInitPromise) {
    schemaInitPromise = (async () => {
      const sql = getSql();
      await sql`
        CREATE TABLE IF NOT EXISTS app_settings (
          id TEXT PRIMARY KEY DEFAULT 'global',
          app_name TEXT NOT NULL DEFAULT 'nokhook',
          tagline TEXT NOT NULL DEFAULT 'Discover books by vibe, mood & honest 4-tier ratings',
          announcement_banner TEXT NOT NULL DEFAULT '',
          spotlight_title TEXT NOT NULL DEFAULT 'The Glass Monolith',
          spotlight_subtitle TEXT NOT NULL DEFAULT 'Meera Deshmukh • Thriller • Explore the Vibe Chart',
          spotlight_image_url TEXT NOT NULL DEFAULT '',
          require_login_for_reviews BOOLEAN NOT NULL DEFAULT TRUE,
          allow_contributions BOOLEAN NOT NULL DEFAULT TRUE,
          maintenance_mode BOOLEAN NOT NULL DEFAULT FALSE,
          updated_by TEXT NOT NULL DEFAULT 'system',
          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )
      `;
      await sql`INSERT INTO app_settings (id) VALUES ('global') ON CONFLICT (id) DO NOTHING`;
      await sql`
        CREATE TABLE IF NOT EXISTS books (
          id TEXT PRIMARY KEY,
          title TEXT NOT NULL,
          subtitle TEXT NOT NULL DEFAULT '',
          author_name TEXT NOT NULL,
          publisher_name TEXT NOT NULL DEFAULT 'Independent',
          year INT NOT NULL DEFAULT 2026,
          pages INT NOT NULL DEFAULT 320,
          language TEXT NOT NULL DEFAULT 'English',
          country TEXT NOT NULL DEFAULT 'India',
          genre TEXT NOT NULL DEFAULT 'Fiction',
          sub_genre TEXT NOT NULL DEFAULT '',
          overview TEXT NOT NULL DEFAULT '',
          cover_url TEXT NOT NULL DEFAULT '',
          cloudinary_public_id TEXT NOT NULL DEFAULT '',
          vibes JSONB NOT NULL DEFAULT '[]'::jsonb,
          rating_stats JSONB NOT NULL DEFAULT '{"mustReadVotes":10,"worthReadingVotes":5,"lightReadVotes":2,"skipVotes":1}'::jsonb,
          is_app_select BOOLEAN NOT NULL DEFAULT FALSE,
          is_talk_of_town BOOLEAN NOT NULL DEFAULT FALSE,
          is_editors_pick BOOLEAN NOT NULL DEFAULT FALSE,
          is_upcoming BOOLEAN NOT NULL DEFAULT FALSE,
          release_date_label TEXT NOT NULL DEFAULT '',
          monthly_rank INT NOT NULL DEFAULT 99,
          updated_by TEXT NOT NULL DEFAULT 'seed',
          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )
      `;
      await sql`
        CREATE TABLE IF NOT EXISTS user_ratings (
          user_uid TEXT NOT NULL,
          book_id TEXT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
          status TEXT NOT NULL DEFAULT 'none',
          rating_tier TEXT NOT NULL DEFAULT '',
          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
          PRIMARY KEY (user_uid, book_id)
        )
      `;
      await sql`
        CREATE TABLE IF NOT EXISTS reviews (
          id TEXT PRIMARY KEY,
          user_uid TEXT NOT NULL,
          user_email TEXT NOT NULL DEFAULT '',
          author_name TEXT NOT NULL,
          author_username TEXT NOT NULL,
          book_id TEXT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
          book_title TEXT NOT NULL,
          rating_tier TEXT NOT NULL,
          topic_tag TEXT NOT NULL DEFAULT 'Fiction',
          body TEXT NOT NULL,
          is_public BOOLEAN NOT NULL DEFAULT TRUE,
          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )
      `;
      await sql`
        CREATE TABLE IF NOT EXISTS contributions (
          id TEXT PRIMARY KEY,
          user_uid TEXT NOT NULL,
          user_email TEXT NOT NULL DEFAULT '',
          book_title TEXT NOT NULL,
          author_name TEXT NOT NULL,
          publisher TEXT NOT NULL DEFAULT '',
          language TEXT NOT NULL DEFAULT 'English',
          genre TEXT NOT NULL DEFAULT 'Fiction',
          notes TEXT NOT NULL DEFAULT '',
          status TEXT NOT NULL DEFAULT 'pending',
          reviewed_by TEXT NOT NULL DEFAULT '',
          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )
      `;
      await sql`
        CREATE TABLE IF NOT EXISTS admin_audit_logs (
          id BIGSERIAL PRIMARY KEY,
          admin_email TEXT NOT NULL,
          action TEXT NOT NULL,
          target_type TEXT NOT NULL,
          target_id TEXT NOT NULL DEFAULT '',
          payload JSONB NOT NULL DEFAULT '{}'::jsonb,
          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        )
      `;

      const [existingCount] = await sql`SELECT COUNT(*)::int AS total FROM books`;
      if ((existingCount?.total ?? 0) === 0) {
        await sql`
          INSERT INTO books (
            id, title, subtitle, author_name, publisher_name, year, pages, language, country,
            genre, sub_genre, overview, vibes, rating_stats, is_app_select, is_talk_of_town,
            is_editors_pick, is_upcoming, release_date_label, monthly_rank, updated_by
          ) VALUES
          (
            'book_glass_monolith',
            'The Glass Monolith',
            'A locked-observatory psychological thriller',
            'Meera Deshmukh',
            'Harper Horizon',
            2027, 384, 'English', 'India', 'Thriller', 'Psychological Mystery',
            'When five glaciologists wake up inside a mirrored Himalayan research dome with forty-eight hours of missing memory, every instrument points to an impossible sixth survivor.',
            ${JSON.stringify([
              { name: 'Suspense', percentage: 45, colorHex: '#4F46E5' },
              { name: 'Psychology', percentage: 35, colorHex: '#E05D44' },
              { name: 'Mystery', percentage: 20, colorHex: '#0D9488' },
            ])}::jsonb,
            ${JSON.stringify({ mustReadVotes: 420, worthReadingVotes: 210, lightReadVotes: 45, skipVotes: 18 })}::jsonb,
            TRUE, TRUE, TRUE, TRUE, 'Jan 18, 2027', 1, 'seed'
          ),
          (
            'book_monsoon_malgudi',
            'Monsoon Over Malgudi',
            'Interlocking tales of rain, memory, and letters',
            'Clara Vance',
            'Penguin Literary',
            2026, 296, 'English', 'UK', 'Fiction', 'Literary Drama',
            'A postmaster discovers an undelivered trunk of 1974 postcards in a crumbling veranda, sparking a tender journey across three generations of dreamers.',
            ${JSON.stringify([
              { name: 'Drama', percentage: 50, colorHex: '#E05D44' },
              { name: 'Nostalgia', percentage: 30, colorHex: '#D97706' },
              { name: 'Romance', percentage: 20, colorHex: '#9333EA' },
            ])}::jsonb,
            ${JSON.stringify({ mustReadVotes: 310, worthReadingVotes: 190, lightReadVotes: 62, skipVotes: 12 })}::jsonb,
            TRUE, FALSE, TRUE, TRUE, 'Dec 04, 2026', 2, 'seed'
          ),
          (
            'book_zero_quiet',
            'Zero to Quiet',
            'Deep work, calm ambition & creative clarity',
            'Arjun Malhotra',
            'Harper Horizon',
            2025, 264, 'English', 'India', 'Business', 'Strategy & Focus',
            'An actionable blueprint for builders, writers, and leaders escaping notification fatigue to produce enduring work.',
            ${JSON.stringify([
              { name: 'Business', percentage: 45, colorHex: '#10B981' },
              { name: 'Clarity', percentage: 35, colorHex: '#F59E0B' },
              { name: 'Psychology', percentage: 20, colorHex: '#6366F1' },
            ])}::jsonb,
            ${JSON.stringify({ mustReadVotes: 390, worthReadingVotes: 245, lightReadVotes: 80, skipVotes: 22 })}::jsonb,
            TRUE, TRUE, FALSE, FALSE, 'Published', 3, 'seed'
          )
          ON CONFLICT (id) DO NOTHING
        `;
      }

      schemaInitialized = true;
    })().catch((err) => {
      schemaInitPromise = null;
      throw err;
    });
  }
  await schemaInitPromise;
}
