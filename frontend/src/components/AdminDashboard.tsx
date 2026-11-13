import React, { useEffect, useMemo, useState } from 'react';
import {
  BarChart,
  Bar,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import {
  BookOpen,
  CheckCircle2,
  CloudUpload,
  Database,
  LayoutDashboard,
  Plus,
  Save,
  Settings2,
  ShieldCheck,
  Trash2,
  XCircle,
} from 'lucide-react';
import { nokhookApi } from '../lib/api';
import { ADMIN_EMAILS } from '../lib/firebase';
import type { AdminOverviewResponse, AppSettings, Book, ContributionItem } from '../types';

interface AdminDashboardProps {
  adminEmail: string;
  books: Book[];
  settings: AppSettings;
  onSaveBook: (book: Book) => Promise<void>;
  onDeleteBook: (id: string) => Promise<void>;
  onSaveSettings: (settings: AppSettings) => Promise<void>;
}

type AdminTab = 'overview' | 'books' | 'settings' | 'moderation';

export const AdminDashboard: React.FC<AdminDashboardProps> = ({
  adminEmail,
  books,
  settings,
  onSaveBook,
  onDeleteBook,
  onSaveSettings,
}) => {
  const [activeTab, setActiveTab] = useState<AdminTab>('overview');
  const [overview, setOverview] = useState<AdminOverviewResponse | null>(null);
  const [contributions, setContributions] = useState<ContributionItem[]>([]);
  const [statusBanner, setStatusBanner] = useState<string | null>(null);
  const [uploadingCover, setUploadingCover] = useState(false);

  const [editingBook, setEditingBook] = useState<Book>(() => books[0]);
  const [draftSettings, setDraftSettings] = useState<AppSettings>(settings);

  useEffect(() => {
    setDraftSettings(settings);
  }, [settings]);

  useEffect(() => {
    if (!nokhookApi.isConfigured()) return;
    nokhookApi.getAdminOverview().then(setOverview).catch(() => {});
    nokhookApi.getContributions().then((r) => setContributions(r.contributions)).catch(() => {});
  }, []);

  const ratingChartData = useMemo(
    () =>
      books.slice(0, 6).map((b) => ({
        title: b.title.length > 14 ? `${b.title.slice(0, 14)}…` : b.title,
        mustRead: b.ratingStats.mustReadVotes,
        worthReading: b.ratingStats.worthReadingVotes,
        lightRead: b.ratingStats.lightReadVotes,
      })),
    [books]
  );

  const totalVotes = useMemo(
    () =>
      books.reduce(
        (sum, b) =>
          sum +
          b.ratingStats.mustReadVotes +
          b.ratingStats.worthReadingVotes +
          b.ratingStats.lightReadVotes +
          b.ratingStats.skipVotes,
        0
      ),
    [books]
  );

  const handleCloudinaryUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadingCover(true);
    setStatusBanner(null);
    try {
      const uploaded = await nokhookApi.uploadImageToCloudinary(file, 'nokhook/covers');
      setEditingBook((prev) => ({
        ...prev,
        coverUrl: uploaded.url,
        cloudinaryPublicId: uploaded.publicId,
      }));
      setStatusBanner('Cover uploaded to Cloudinary and attached.');
    } catch (err) {
      setStatusBanner(err instanceof Error ? err.message : 'Upload failed');
    } finally {
      setUploadingCover(false);
    }
  };

  const handleCreateNewBook = () => {
    const id = `book_${Date.now()}`;
    setEditingBook({
      id,
      title: 'New Curated Book',
      subtitle: '',
      authorName: '',
      publisherName: 'Independent',
      year: 2026,
      pages: 320,
      language: 'English',
      country: 'India',
      genre: 'Fiction',
      subGenre: '',
      overview: '',
      coverUrl: '',
      vibes: [
        { name: 'Drama', percentage: 50, colorHex: '#F59E0B' },
        { name: 'Psychology', percentage: 30, colorHex: '#6366F1' },
        { name: 'Suspense', percentage: 20, colorHex: '#EF4444' },
      ],
      ratingStats: { mustReadVotes: 25, worthReadingVotes: 15, lightReadVotes: 8, skipVotes: 2 },
      isAppSelect: false,
      isTalkOfTown: false,
      isEditorsPick: false,
      isUpcoming: false,
      releaseDateLabel: '2026',
      monthlyRank: books.length + 1,
    });
    setActiveTab('books');
  };

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-violet-500/30 bg-gradient-to-r from-[#1D1030] via-[#111117] to-[#111117] p-5 flex flex-wrap items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-violet-500/15 border border-violet-500/30 text-violet-300 text-xs font-semibold">
            <ShieldCheck className="w-3.5 h-3.5" /> Authorized Editorial Admin
          </div>
          <h2 className="text-2xl font-bold text-white">nokhook Admin Studio</h2>
          <p className="text-xs text-slate-400">
            Signed in as <span className="text-slate-200 font-medium">{adminEmail}</span> · Allowlisted editors:{' '}
            {ADMIN_EMAILS.join(', ')}
          </p>
        </div>

        <div className="flex flex-wrap gap-2 text-xs">
          <span className="px-3 py-1.5 rounded-xl bg-[#181822] border border-[#262634] text-emerald-400 inline-flex items-center gap-1.5">
            <Database className="w-3.5 h-3.5" /> NeonDB + Vercel APIs
          </span>
          <span className="px-3 py-1.5 rounded-xl bg-[#181822] border border-[#262634] text-sky-400 inline-flex items-center gap-1.5">
            <CloudUpload className="w-3.5 h-3.5" /> Cloudinary Media
          </span>
        </div>
      </div>

      {statusBanner && (
        <div className="rounded-xl border border-emerald-500/40 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-200">
          {statusBanner}
        </div>
      )}

      <div className="flex flex-wrap gap-2 border-b border-[#262634] pb-3">
        {(
          [
            ['overview', 'Overview & Analytics', LayoutDashboard],
            ['books', 'Books & Vibes Editor', BookOpen],
            ['settings', 'App Banners & Config', Settings2],
            ['moderation', 'Contributions Queue', CheckCircle2],
          ] as const
        ).map(([key, label, IconComp]) => (
          <button
            key={key}
            type="button"
            onClick={() => setActiveTab(key)}
            className={`px-4 py-2 rounded-xl text-sm font-medium inline-flex items-center gap-2 transition ${
              activeTab === key
                ? 'bg-violet-600 text-white'
                : 'bg-[#111117] text-slate-300 border border-[#262634] hover:border-violet-500/50'
            }`}
          >
            <IconComp className="w-4 h-4" />
            {label}
          </button>
        ))}
      </div>

      {activeTab === 'overview' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {[
              { label: 'Catalog Books', value: overview?.stats.totalBooks ?? books.length, accent: 'text-violet-400' },
              { label: 'Community Votes', value: (overview?.stats.totalRatings ?? totalVotes).toLocaleString(), accent: 'text-emerald-400' },
              { label: 'Published Reviews', value: overview?.stats.totalReviews ?? 18, accent: 'text-amber-400' },
              { label: 'Pending Submissions', value: overview?.stats.pendingContributions ?? contributions.filter((c) => c.status === 'pending').length, accent: 'text-rose-400' },
            ].map((kpi) => (
              <div key={kpi.label} className="rounded-2xl border border-[#262634] bg-[#111117] p-4">
                <p className="text-xs text-slate-400">{kpi.label}</p>
                <p className={`text-2xl font-bold mt-1 ${kpi.accent}`}>{kpi.value}</p>
              </div>
            ))}
          </div>

          <div className="rounded-2xl border border-[#262634] bg-[#111117] p-5 space-y-3">
            <h3 className="text-base font-semibold text-white">Rating Tier Distribution by Book</h3>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={ratingChartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#262634" vertical={false} />
                  <XAxis dataKey="title" stroke="#9CA3AF" fontSize={11} />
                  <YAxis stroke="#9CA3AF" fontSize={11} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#181822',
                      borderColor: '#262634',
                      borderRadius: '12px',
                    }}
                  />
                  <Bar dataKey="mustRead" name="Must Read" fill="#9333EA" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="worthReading" name="Worth Reading" fill="#10B981" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="lightRead" name="Light Read" fill="#F59E0B" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'books' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="rounded-2xl border border-[#262634] bg-[#111117] p-4 space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold text-white">Catalog ({books.length})</h3>
              <button
                type="button"
                onClick={handleCreateNewBook}
                className="px-2.5 py-1.5 rounded-lg bg-violet-600 text-white text-xs font-semibold inline-flex items-center gap-1"
              >
                <Plus className="w-3.5 h-3.5" /> Add Book
              </button>
            </div>
            <div className="space-y-2 max-h-[460px] overflow-y-auto">
              {books.map((b) => (
                <button
                  key={b.id}
                  type="button"
                  onClick={() => setEditingBook(b)}
                  className={`w-full text-left p-3 rounded-xl border transition ${
                    editingBook.id === b.id
                      ? 'border-violet-500 bg-violet-500/10'
                      : 'border-[#262634] bg-[#09090D] hover:border-slate-600'
                  }`}
                >
                  <p className="text-sm font-semibold text-white truncate">{b.title}</p>
                  <p className="text-xs text-slate-400 truncate">
                    {b.authorName} · {b.genre} · #{b.monthlyRank}
                  </p>
                </button>
              ))}
            </div>
          </div>

          <form
            className="lg:col-span-2 rounded-2xl border border-[#262634] bg-[#111117] p-5 space-y-4"
            onSubmit={async (e) => {
              e.preventDefault();
              await onSaveBook(editingBook);
              setStatusBanner(`Saved "${editingBook.title}" to catalog.`);
            }}
          >
            <div className="flex items-center justify-between">
              <h3 className="text-base font-semibold text-white">Edit Book & Vibe Weights</h3>
              <button
                type="button"
                onClick={async () => {
                  await onDeleteBook(editingBook.id);
                  setStatusBanner(`Removed "${editingBook.title}".`);
                }}
                className="px-3 py-1.5 rounded-lg border border-rose-500/40 text-rose-300 text-xs inline-flex items-center gap-1 hover:bg-rose-500/10"
              >
                <Trash2 className="w-3.5 h-3.5" /> Delete
              </button>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <label className="text-xs text-slate-300 space-y-1">
                <span>Title</span>
                <input
                  value={editingBook.title}
                  onChange={(e) => setEditingBook({ ...editingBook, title: e.target.value })}
                  className="w-full rounded-xl border border-[#262634] bg-[#09090D] px-3 py-2 text-sm text-white"
                  required
                />
              </label>
              <label className="text-xs text-slate-300 space-y-1">
                <span>Author</span>
                <input
                  value={editingBook.authorName}
                  onChange={(e) => setEditingBook({ ...editingBook, authorName: e.target.value })}
                  className="w-full rounded-xl border border-[#262634] bg-[#09090D] px-3 py-2 text-sm text-white"
                  required
                />
              </label>
              <label className="text-xs text-slate-300 space-y-1">
                <span>Genre</span>
                <input
                  value={editingBook.genre}
                  onChange={(e) => setEditingBook({ ...editingBook, genre: e.target.value })}
                  className="w-full rounded-xl border border-[#262634] bg-[#09090D] px-3 py-2 text-sm text-white"
                />
              </label>
              <label className="text-xs text-slate-300 space-y-1">
                <span>Language</span>
                <input
                  value={editingBook.language}
                  onChange={(e) => setEditingBook({ ...editingBook, language: e.target.value })}
                  className="w-full rounded-xl border border-[#262634] bg-[#09090D] px-3 py-2 text-sm text-white"
                />
              </label>
            </div>

            <label className="block text-xs text-slate-300 space-y-1">
              <span>Overview</span>
              <textarea
                rows={3}
                value={editingBook.overview}
                onChange={(e) => setEditingBook({ ...editingBook, overview: e.target.value })}
                className="w-full rounded-xl border border-[#262634] bg-[#09090D] px-3 py-2 text-sm text-white"
              />
            </label>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 items-end">
              <label className="text-xs text-slate-300 space-y-1">
                <span>Cloudinary Cover Image URL</span>
                <input
                  value={editingBook.coverUrl}
                  onChange={(e) => setEditingBook({ ...editingBook, coverUrl: e.target.value })}
                  placeholder="https://res.cloudinary.com/..."
                  className="w-full rounded-xl border border-[#262634] bg-[#09090D] px-3 py-2 text-sm text-white"
                />
              </label>
              <label className="cursor-pointer rounded-xl border border-dashed border-violet-500/50 bg-violet-500/10 px-4 py-2 text-xs text-violet-200 inline-flex items-center justify-center gap-2">
                <CloudUpload className="w-4 h-4" />
                {uploadingCover ? 'Uploading to Cloudinary…' : 'Upload Cover to Cloudinary'}
                <input type="file" accept="image/*" className="hidden" onChange={handleCloudinaryUpload} />
              </label>
            </div>

            <div className="flex flex-wrap gap-4 pt-1 text-xs text-slate-200">
              {(
                [
                  ['isAppSelect', 'nokhook Select'],
                  ['isEditorsPick', "Editor's Pick"],
                  ['isTalkOfTown', 'Talk Of The Town'],
                  ['isUpcoming', 'Upcoming Leaderboard'],
                ] as const
              ).map(([field, label]) => (
                <label key={field} className="inline-flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={Boolean(editingBook[field])}
                    onChange={(e) => setEditingBook({ ...editingBook, [field]: e.target.checked })}
                    className="accent-violet-600"
                  />
                  {label}
                </label>
              ))}
            </div>

            <button
              type="submit"
              className="px-5 py-2.5 rounded-xl bg-violet-600 hover:bg-violet-500 text-white text-sm font-semibold inline-flex items-center gap-2"
            >
              <Save className="w-4 h-4" /> Save Book Changes
            </button>
          </form>
        </div>
      )}

      {activeTab === 'settings' && (
        <form
          className="rounded-2xl border border-[#262634] bg-[#111117] p-5 space-y-4"
          onSubmit={async (e) => {
            e.preventDefault();
            await onSaveSettings(draftSettings);
            setStatusBanner('App settings & homepage banners saved.');
          }}
        >
          <h3 className="text-base font-semibold text-white">Editable App Configuration & Banners</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <label className="text-xs text-slate-300 space-y-1">
              <span>App Name</span>
              <input
                value={draftSettings.appName}
                onChange={(e) => setDraftSettings({ ...draftSettings, appName: e.target.value })}
                className="w-full rounded-xl border border-[#262634] bg-[#09090D] px-3 py-2 text-sm text-white"
              />
            </label>
            <label className="text-xs text-slate-300 space-y-1">
              <span>Tagline</span>
              <input
                value={draftSettings.tagline}
                onChange={(e) => setDraftSettings({ ...draftSettings, tagline: e.target.value })}
                className="w-full rounded-xl border border-[#262634] bg-[#09090D] px-3 py-2 text-sm text-white"
              />
            </label>
            <label className="text-xs text-slate-300 space-y-1 sm:col-span-2">
              <span>Announcement Ticker</span>
              <input
                value={draftSettings.announcementBanner}
                onChange={(e) => setDraftSettings({ ...draftSettings, announcementBanner: e.target.value })}
                className="w-full rounded-xl border border-[#262634] bg-[#09090D] px-3 py-2 text-sm text-white"
              />
            </label>
            <label className="text-xs text-slate-300 space-y-1">
              <span>Spotlight Hero Title</span>
              <input
                value={draftSettings.spotlightTitle}
                onChange={(e) => setDraftSettings({ ...draftSettings, spotlightTitle: e.target.value })}
                className="w-full rounded-xl border border-[#262634] bg-[#09090D] px-3 py-2 text-sm text-white"
              />
            </label>
            <label className="text-xs text-slate-300 space-y-1">
              <span>Spotlight Hero Subtitle</span>
              <input
                value={draftSettings.spotlightSubtitle}
                onChange={(e) => setDraftSettings({ ...draftSettings, spotlightSubtitle: e.target.value })}
                className="w-full rounded-xl border border-[#262634] bg-[#09090D] px-3 py-2 text-sm text-white"
              />
            </label>
          </div>

          <div className="flex flex-wrap gap-5 pt-2 text-xs text-slate-200">
            <label className="inline-flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={draftSettings.requireLoginForReviews}
                onChange={(e) => setDraftSettings({ ...draftSettings, requireLoginForReviews: e.target.checked })}
                className="accent-violet-600"
              />
              Gate User Reviews Behind Login
            </label>
            <label className="inline-flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={draftSettings.allowContributions}
                onChange={(e) => setDraftSettings({ ...draftSettings, allowContributions: e.target.checked })}
                className="accent-violet-600"
              />
              Allow Reader Book Contributions
            </label>
            <label className="inline-flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={draftSettings.maintenanceMode}
                onChange={(e) => setDraftSettings({ ...draftSettings, maintenanceMode: e.target.checked })}
                className="accent-rose-500"
              />
              Maintenance Banner Mode
            </label>
          </div>

          <button
            type="submit"
            className="px-5 py-2.5 rounded-xl bg-violet-600 hover:bg-violet-500 text-white text-sm font-semibold inline-flex items-center gap-2"
          >
            <Save className="w-4 h-4" /> Save App Configuration
          </button>
        </form>
      )}

      {activeTab === 'moderation' && (
        <div className="rounded-2xl border border-[#262634] bg-[#111117] p-5 space-y-4">
          <h3 className="text-base font-semibold text-white">Crowdsourced Book Submissions</h3>
          {contributions.length === 0 ? (
            <p className="text-sm text-slate-400">
              No pending reader submissions in NeonDB yet. Connect VITE_API_BASE_URL to view live submissions.
            </p>
          ) : (
            <div className="space-y-3">
              {contributions.map((item) => (
                <div
                  key={item.id}
                  className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-[#262634] bg-[#09090D] p-4"
                >
                  <div>
                    <p className="text-sm font-semibold text-white">{item.bookTitle}</p>
                    <p className="text-xs text-slate-400">
                      {item.authorName} · {item.language} · {item.genre} · submitted by {item.userEmail}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-1 rounded-full text-xs bg-[#181822] text-slate-300 capitalize">
                      {item.status}
                    </span>
                    <button
                      type="button"
                      onClick={async () => {
                        await nokhookApi.moderateContribution(item.id, 'approved');
                        setContributions((prev) =>
                          prev.map((c) => (c.id === item.id ? { ...c, status: 'approved' } : c))
                        );
                      }}
                      className="p-2 rounded-lg bg-emerald-500/15 text-emerald-300 hover:bg-emerald-500/25"
                      title="Approve"
                    >
                      <CheckCircle2 className="w-4 h-4" />
                    </button>
                    <button
                      type="button"
                      onClick={async () => {
                        await nokhookApi.moderateContribution(item.id, 'rejected');
                        setContributions((prev) =>
                          prev.map((c) => (c.id === item.id ? { ...c, status: 'rejected' } : c))
                        );
                      }}
                      className="p-2 rounded-lg bg-rose-500/15 text-rose-300 hover:bg-rose-500/25"
                      title="Reject"
                    >
                      <XCircle className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
