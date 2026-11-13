export type RatingTierKey = 'must_read' | 'worth_reading' | 'light_read' | 'skip';

export interface VibeSlice {
  name: string;
  percentage: number;
  colorHex: string;
}

export interface RatingStats {
  mustReadVotes: number;
  worthReadingVotes: number;
  lightReadVotes: number;
  skipVotes: number;
}

export interface Book {
  id: string;
  title: string;
  subtitle: string;
  authorName: string;
  publisherName: string;
  year: number;
  pages: number;
  language: string;
  country: string;
  genre: string;
  subGenre: string;
  overview: string;
  coverUrl: string;
  cloudinaryPublicId?: string;
  vibes: VibeSlice[];
  ratingStats: RatingStats;
  isAppSelect: boolean;
  isTalkOfTown: boolean;
  isEditorsPick: boolean;
  isUpcoming: boolean;
  releaseDateLabel: string;
  monthlyRank: number;
}

export interface AppSettings {
  appName: string;
  tagline: string;
  announcementBanner: string;
  spotlightTitle: string;
  spotlightSubtitle: string;
  spotlightImageUrl: string;
  requireLoginForReviews: boolean;
  allowContributions: boolean;
  maintenanceMode: boolean;
  updatedBy?: string;
}

export interface ContributionItem {
  id: string;
  userUid: string;
  userEmail: string;
  bookTitle: string;
  authorName: string;
  publisher: string;
  language: string;
  genre: string;
  notes: string;
  status: 'pending' | 'approved' | 'rejected';
  reviewedBy?: string;
  createdAt: string;
}

export interface AdminOverviewResponse {
  adminEmail: string;
  stats: {
    totalBooks: number;
    totalReviews: number;
    totalRatings: number;
    pendingContributions: number;
  };
  recentLogs: Array<{
    id: number;
    adminEmail: string;
    action: string;
    targetType: string;
    targetId: string;
    createdAt: string;
  }>;
}
