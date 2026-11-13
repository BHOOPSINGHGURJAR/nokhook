-- nokhook PostgreSQL Schema for NeonDB

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
);

INSERT INTO app_settings (id) VALUES ('global')
ON CONFLICT (id) DO NOTHING;

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
);

CREATE TABLE IF NOT EXISTS user_ratings (
    user_uid TEXT NOT NULL,
    book_id TEXT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'none',
    rating_tier TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_uid, book_id)
);

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
);

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
);

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_email TEXT NOT NULL,
    action TEXT NOT NULL,
    target_type TEXT NOT NULL,
    target_id TEXT NOT NULL DEFAULT '',
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
