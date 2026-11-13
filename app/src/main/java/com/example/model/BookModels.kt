package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.TierLightRead
import com.example.ui.theme.TierMustRead
import com.example.ui.theme.TierSkip
import com.example.ui.theme.TierWorthReading

enum class RatingTier(
    val key: String,
    val label: String,
    val color: Color,
    val shortHint: String
) {
    MUST_READ("must_read", "Must Read", TierMustRead, "Essential shelf pick"),
    WORTH_READING("worth_reading", "Worth Reading", TierWorthReading, "Memorable & rewarding"),
    LIGHT_READ("light_read", "Light Read", TierLightRead, "Easy breezy page-turner"),
    SKIP("skip", "Skip", TierSkip, "Not worth the hours");

    companion object {
        fun fromKey(key: String?): RatingTier? = entries.firstOrNull { it.key == key }
    }
}

enum class ReadingStatus(val key: String, val label: String) {
    NONE("none", "None"),
    READ("read", "Marked Read"),
    TBR("tbr", "Read Later (TBR)"),
    INTERESTED("interested", "Interested");

    companion object {
        fun fromKey(key: String?): ReadingStatus = entries.firstOrNull { it.key == key } ?: NONE
    }
}

enum class TimeFilter(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_QUARTER("This Quarter"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}

enum class ExploreFilterType(
    val id: String,
    val title: String,
    val subtitle: String
) {
    FOLLOWING("following", "Following Activity", "What friends are reading right now"),
    TODAY_TRENDING("today_trending", "Today Trending", "Fastest-rising books on nokhook today"),
    TOP_100("top_100", "Monthly Ranking & Top 100", "Highest rated across nokhook"),
    GENRE("genre", "Category & Genre", "Fiction, Thriller, Romance, Business..."),
    LANGUAGE_COUNTRY("language", "Language & Country", "Hindi, English, Marathi, Japanese..."),
    AWARDS("awards", "Award Winners", "Booker, Pulitzer, Sahitya Akademi"),
    SERIES_MANGA("series_manga", "Book Series & Manga", "Multi-book sagas & graphic novels"),
    BEGINNER_YA("beginner_ya", "Beginner Friendly & YA", "Approachable pacing & young adult")
}

data class VibeSlice(
    val name: String,
    val percentage: Int,
    val colorHex: Long
)

data class RatingMeterStats(
    val mustReadVotes: Int,
    val worthReadingVotes: Int,
    val lightReadVotes: Int,
    val skipVotes: Int
) {
    val totalVotes: Int get() = mustReadVotes + worthReadingVotes + lightReadVotes + skipVotes

    fun withUserVote(tier: RatingTier?): RatingMeterStats {
        if (tier == null) return this
        return when (tier) {
            RatingTier.MUST_READ -> copy(mustReadVotes = mustReadVotes + 1)
            RatingTier.WORTH_READING -> copy(worthReadingVotes = worthReadingVotes + 1)
            RatingTier.LIGHT_READ -> copy(lightReadVotes = lightReadVotes + 1)
            RatingTier.SKIP -> copy(skipVotes = skipVotes + 1)
        }
    }

    fun percentageFor(tier: RatingTier): Int {
        if (totalVotes == 0) return 0
        val count = when (tier) {
            RatingTier.MUST_READ -> mustReadVotes
            RatingTier.WORTH_READING -> worthReadingVotes
            RatingTier.LIGHT_READ -> lightReadVotes
            RatingTier.SKIP -> skipVotes
        }
        return ((count * 100f) / totalVotes).toInt()
    }

    fun dominantTier(): RatingTier {
        val pairs = listOf(
            RatingTier.MUST_READ to mustReadVotes,
            RatingTier.WORTH_READING to worthReadingVotes,
            RatingTier.LIGHT_READ to lightReadVotes,
            RatingTier.SKIP to skipVotes
        )
        return pairs.maxByOrNull { it.second }?.first ?: RatingTier.WORTH_READING
    }
}

data class ExternalPlatformLink(
    val platformName: String,
    val actionLabel: String,
    val url: String
)

data class AuthorProfile(
    val id: String,
    val name: String,
    val country: String,
    val bio: String,
    val followersLabel: String,
    val accentColorHex: Long,
    @DrawableRes val avatarRes: Int = 0
)

data class RecommendationPerson(
    val id: String,
    val name: String,
    val profession: String,
    @DrawableRes val imageRes: Int,
    val quote: String = "",
    val username: String = ""
)

data class PublisherProfile(
    val id: String,
    val name: String,
    val headquarters: String,
    val description: String,
    val accentColorHex: Long
)

data class Book(
    val id: String,
    val title: String,
    val subtitle: String,
    val authorId: String,
    val authorName: String,
    val publisherId: String,
    val publisherName: String,
    val year: Int,
    val pages: Int,
    val estimatedHours: Int,
    val language: String,
    val country: String,
    val ageRating: String,
    val genre: String,
    val subGenre: String,
    val awards: List<String>,
    val overview: String,
    val vibes: List<VibeSlice>,
    val ratingStats: RatingMeterStats,
    @DrawableRes val coverRes: Int,
    val accentColorHex: Long,
    val isAppSelect: Boolean = false,
    val isTalkOfTown: Boolean = false,
    val isEditorsPick: Boolean = false,
    val isUpcoming: Boolean = false,
    val releaseDateLabel: String = "",
    val interestedByFilter: Map<TimeFilter, Int> = emptyMap(),
    val isSeriesOrManga: Boolean = false,
    val seriesLabel: String = "",
    val isBeginnerFriendly: Boolean = false,
    val monthlyRank: Int = 99,
    val buyLinks: List<ExternalPlatformLink> = emptyList(),
    val recommendations: List<RecommendationPerson> = emptyList(),
    val coverImageUrl: String = ""
)

data class BookNewsItem(
    val id: String,
    val headline: String,
    val summary: String,
    val source: String,
    val timeAgo: String,
    val topicTag: String,
    val relatedBookId: String? = null
)

data class BookSummaryItem(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val authorName: String,
    val durationLabel: String,
    val formatLabel: String,
    val topicTag: String,
    val keyTakeaway: String,
    val youtubeUrl: String,
    @DrawableRes val coverRes: Int
)

data class WhatsNewItem(
    val id: String,
    val versionTag: String,
    val title: String,
    val description: String,
    val dateLabel: String
)

data class CuratedCollectionSeed(
    val id: String,
    val title: String,
    val curatorName: String,
    val curatorUsername: String,
    val description: String,
    val topicTag: String,
    val bookIds: List<String>,
    val followersCount: Int
)

data class SeedReview(
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val userId: String,
    val authorName: String,
    val authorUsername: String,
    val timeAgo: String,
    val ratingTier: RatingTier,
    val topicTag: String,
    val body: String,
    @DrawableRes val coverRes: Int
)

data class SeedDiscussion(
    val id: String,
    val userId: String,
    val authorName: String,
    val authorUsername: String,
    val topicTag: String,
    val bookId: String,
    val title: String,
    val body: String,
    val timeAgo: String,
    val upvotes: Int,
    val replies: List<SeedReply>
)

data class SeedReply(
    val id: String,
    val authorName: String,
    val authorUsername: String,
    val timeAgo: String,
    val body: String,
    val parentReplyId: String = ""
)

data class FollowedActivityItem(
    val id: String,
    val userName: String,
    val username: String,
    val actionLabel: String,
    val bookId: String,
    val bookTitle: String,
    val timeAgo: String,
    val ratingTier: RatingTier?
)

data class SeedReaderProfile(
    val username: String,
    val displayName: String,
    val bio: String,
    val followersCountLabel: String,
    val joinedLabel: String,
    val isVerified: Boolean = true,
    val accentColorHex: Long = 0xFF475569,
    val reviewsCount: Int = 24,
    val publicCollectionsCount: Int = 3,
    val interestedBookIds: List<String> = emptyList(),
    val socialUrlHint: String = ""
)
