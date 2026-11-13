import React, { useEffect, useState } from 'react';
import { onAuthStateChanged, type User } from 'firebase/auth';
import {
  BookMarked,
  Compass,
  LogIn,
  LogOut,
  Settings,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';
import { AdminDashboard } from './components/AdminDashboard';
import { VibeChartRecharts } from './components/VibeChartRecharts';
import { DEFAULT_APP_SETTINGS, SEED_BOOKS } from './data/seedBooks';
import { nokhookApi } from './lib/api';
import { auth, isAdminEmail, signInWithGoogle, signOutUser } from './lib/firebase';
import type { AppSettings, Book, RatingTierKey } from './types';

type ViewMode = 'discover' | 'settings' | 'admin';

export function App() {
  const [user, setUser] = useState<User | null>(null);
  const [serverAdminVerified, setServerAdminVerified] = useState(false);
  const [view, setView] = useState<ViewMode>('discover');
  const [books, setBooks] = useState<Book[]>(SEED_BOOKS);
  const [selectedBookId, setSelectedBookId] = useState<string>(SEED_BOOKS[0].id);
  const [settings, setSettings] = useState<AppSettings>(DEFAULT_APP_SETTINGS);
  const [userTier, setUserTier] = useState<RatingTierKey | null>(null);
  const [authError, setAuthError] = useState<string | null>(null);

  const isAdmin = isAdminEmail(user?.email) && (nokhookApi.isConfigured() ? serverAdminVerified : true);

  useEffect(() => {
    if (!auth) return;
    return onAuthStateChanged(auth, async (nextUser) => {
      setUser(nextUser);
      setServerAdminVerified(false);
      if (nextUser && nokhookApi.isConfigured()) {
        try {
          const me = await nokhookApi.verifyAdminMe();
          setServerAdminVerified(Boolean(me.isAdmin));
        } catch {
          setServerAdminVerified(false);
        }
      }
    });
  }, []);

  useEffect(() => {
    if (!nokhookApi.isConfigured()) return;
    nokhookApi.getBooks().then((r) => {
      if (r.books.length > 0) {
        setBooks(r.books);
        setSelectedBookId(r.books[0].id);
      }
    }).catch(() => {});
    nokhookApi.getSettings().then((r) => {
      if (r.settings) setSettings(r.settings);
    }).catch(() => {});
  }, []);

  const activeBook = books.find((b) => b.id === selectedBookId) || books[0];

  const handleSaveBook = async (book: Book) => {
    setBooks((prev) => {
      const exists = prev.some((item) => item.id === book.id);
      return exists ? prev.map((item) => (item.id === book.id ? book : item)) : [book, ...prev];
    });
    if (nokhookApi.isConfigured()) {
      await nokhookApi.upsertBook(book);
    }
  };

  const handleDeleteBook = async (id: string) => {
    setBooks((prev) => prev.filter((b) => b.id !== id));
    if (nokhookApi.isConfigured()) {
      await nokhookApi.deleteBook(id);
    }
  };

  const handleSaveSettings = async (next: AppSettings) => {
    setSettings(next);
    if (nokhookApi.isConfigured()) {
      await nokhookApi.updateSettings(next);
    }
  };

  return (
    <div className="min-h-screen bg-[#09090D] text-[#F8FAFC]">
      <header className="sticky top-0 z-20 border-b border-[#262634] bg-[#09090D]/90 backdrop-blur-md">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between gap-4">
          <button
            type="button"
            onClick={() => setView('discover')}
            className="flex items-center gap-2.5 text-left"
          >
            <div className="w-9 h-9 rounded-xl bg-violet-600/20 border border-violet-500/40 flex items-center justify-center">
              <BookMarked className="w-5 h-5 text-violet-400" />
            </div>
            <div>
              <span className="text-base font-extrabold tracking-wider uppercase">{settings.appName}</span>
              <p className="text-[11px] text-slate-400 hidden sm:block">{settings.tagline}</p>
            </div>
          </button>

          <nav className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setView('discover')}
              className={`px-3 py-2 rounded-xl text-xs font-semibold inline-flex items-center gap-1.5 ${
                view === 'discover' ? 'bg-violet-600 text-white' : 'text-slate-300 hover:bg-[#181822]'
              }`}
            >
              <Compass className="w-4 h-4" /> Discover
            </button>
            <button
              type="button"
              onClick={() => setView('settings')}
              className={`px-3 py-2 rounded-xl text-xs font-semibold inline-flex items-center gap-1.5 ${
                view === 'settings' || view === 'admin'
                  ? 'bg-violet-600 text-white'
                  : 'text-slate-300 hover:bg-[#181822]'
              }`}
            >
              <Settings className="w-4 h-4" /> Settings
            </button>

            {user ? (
              <button
                type="button"
                onClick={() => {
                  signOutUser();
                  if (view === 'admin') setView('discover');
                }}
                className="px-3 py-2 rounded-xl border border-[#262634] bg-[#111117] text-xs text-slate-200 inline-flex items-center gap-1.5"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">{user.email}</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={async () => {
                  setAuthError(null);
                  try {
                    await signInWithGoogle();
                  } catch (err) {
                    setAuthError(err instanceof Error ? err.message : 'Sign-in failed');
                  }
                }}
                className="px-3.5 py-2 rounded-xl bg-violet-600 hover:bg-violet-500 text-xs font-semibold text-white inline-flex items-center gap-1.5"
              >
                <LogIn className="w-3.5 h-3.5" /> Sign in with Google
              </button>
            )}
          </nav>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-6 space-y-6">
        {settings.announcementBanner && (
          <div className="rounded-xl border border-violet-500/30 bg-violet-500/10 px-4 py-2.5 text-xs text-violet-200 flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-violet-400 shrink-0" />
            <span>{settings.announcementBanner}</span>
          </div>
        )}

        {authError && (
          <div className="rounded-xl border border-rose-500/40 bg-rose-500/10 px-4 py-2.5 text-xs text-rose-200">
            {authError}
          </div>
        )}

        {view === 'discover' && activeBook && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="space-y-3">
              <div className="rounded-2xl border border-[#262634] bg-gradient-to-br from-[#1D1030] to-[#111117] p-5 space-y-1">
                <span className="text-[10px] font-bold uppercase tracking-wider text-violet-300">Spotlight Pick</span>
                <h1 className="text-xl font-bold text-white">{settings.spotlightTitle}</h1>
                <p className="text-xs text-slate-300">{settings.spotlightSubtitle}</p>
              </div>

              <div className="rounded-2xl border border-[#262634] bg-[#111117] p-3 space-y-2">
                <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-400 px-2">Catalog</h2>
                {books.map((book) => (
                  <button
                    key={book.id}
                    type="button"
                    onClick={() => {
                      setSelectedBookId(book.id);
                      setUserTier(null);
                    }}
                    className={`w-full text-left p-3 rounded-xl border transition ${
                      activeBook.id === book.id
                        ? 'border-violet-500 bg-violet-500/10'
                        : 'border-[#262634] bg-[#09090D] hover:border-slate-600'
                    }`}
                  >
                    <p className="text-sm font-semibold text-white">{book.title}</p>
                    <p className="text-xs text-slate-400">
                      {book.authorName} · {book.genre}
                    </p>
                  </button>
                ))}
              </div>
            </div>

            <div className="lg:col-span-2 space-y-5">
              <div className="rounded-2xl border border-[#262634] bg-[#111117] p-6 space-y-4">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="space-y-1">
                    <span className="inline-block px-2.5 py-0.5 rounded-full text-xs bg-[#181822] border border-[#262634] text-violet-300">
                      {activeBook.genre} · {activeBook.language}
                    </span>
                    <h2 className="text-2xl font-bold text-white">{activeBook.title}</h2>
                    <p className="text-sm text-slate-400">
                      by {activeBook.authorName} · {activeBook.year} · {activeBook.pages} pages
                    </p>
                  </div>
                  {activeBook.coverUrl && (
                    <img
                      src={activeBook.coverUrl}
                      alt={activeBook.title}
                      className="w-20 h-28 object-cover rounded-xl border border-[#262634]"
                    />
                  )}
                </div>
                <p className="text-sm text-slate-300 leading-relaxed">{activeBook.overview}</p>

                <div className="pt-2 space-y-2">
                  <p className="text-xs font-semibold text-slate-400">Vote on 4-Tier Rating Meter (live-updates Vibe Chart)</p>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                    {(
                      [
                        ['must_read', 'Must Read', '#9333EA'],
                        ['worth_reading', 'Worth Reading', '#10B981'],
                        ['light_read', 'Light Read', '#F59E0B'],
                        ['skip', 'Skip', '#EF4444'],
                      ] as const
                    ).map(([tierKey, label, color]) => (
                      <button
                        key={tierKey}
                        type="button"
                        onClick={async () => {
                          setUserTier(tierKey);
                          if (user && nokhookApi.isConfigured()) {
                            await nokhookApi.submitRatingVote(activeBook.id, tierKey, 'read').catch(() => {});
                          }
                        }}
                        className={`px-3 py-2 rounded-xl border text-xs font-semibold transition ${
                          userTier === tierKey ? 'text-white' : 'text-slate-300 border-[#262634] bg-[#09090D]'
                        }`}
                        style={userTier === tierKey ? { borderColor: color, backgroundColor: `${color}26` } : undefined}
                      >
                        {label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              <VibeChartRecharts
                baseVibes={activeBook.vibes}
                ratingStats={activeBook.ratingStats}
                userSelectedTier={userTier}
              />
            </div>
          </div>
        )}

        {view === 'settings' && (
          <div className="max-w-2xl mx-auto space-y-4">
            <div className="rounded-2xl border border-[#262634] bg-[#111117] p-6 space-y-4">
              <h2 className="text-xl font-bold text-white">Settings</h2>
              <div className="rounded-xl border border-[#262634] bg-[#09090D] p-4 flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-white">Signed-In Account</p>
                  <p className="text-xs text-slate-400">
                    {user?.email ? user.email : 'Guest mode — sign in with Firebase Google Auth'}
                  </p>
                </div>
                {isAdmin && (
                  <span className="px-2.5 py-1 rounded-full bg-violet-500/15 text-violet-300 text-xs font-semibold">
                    Admin
                  </span>
                )}
              </div>

              {isAdmin && user?.email && (
                <button
                  type="button"
                  onClick={() => setView('admin')}
                  className="w-full rounded-xl border border-violet-500/50 bg-gradient-to-r from-violet-600/20 to-indigo-600/10 p-4 flex items-center justify-between text-left hover:border-violet-400 transition"
                >
                  <div className="flex items-center gap-3">
                    <ShieldCheck className="w-5 h-5 text-violet-400" />
                    <div>
                      <p className="text-sm font-semibold text-white">Admin Panel & Editorial Studio</p>
                      <p className="text-xs text-slate-300">
                        Edit books, Vibe Charts, Cloudinary covers, homepage banners & moderation
                      </p>
                    </div>
                  </div>
                  <span className="text-xs font-semibold text-violet-300">Open →</span>
                </button>
              )}
            </div>
          </div>
        )}

        {view === 'admin' && isAdmin && user?.email && (
          <AdminDashboard
            adminEmail={user.email}
            books={books}
            settings={settings}
            onSaveBook={handleSaveBook}
            onDeleteBook={handleDeleteBook}
            onSaveSettings={handleSaveSettings}
          />
        )}
      </main>
    </div>
  );
}
export default App;
