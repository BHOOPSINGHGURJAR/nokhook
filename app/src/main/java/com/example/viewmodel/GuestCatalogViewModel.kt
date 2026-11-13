package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.CatalogSeed
import com.example.model.Book
import com.example.model.ExploreFilterType
import com.example.model.TimeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PendingRegistrationDraft(
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val countryCode: String = "+91",
    val phoneNumber: String = "",
    val phoneVerified: Boolean = false,
    val email: String = "",
    val verifiedMethod: String = "phone"
)

class GuestCatalogViewModel : ViewModel() {

    private val _selectedTimeFilter = MutableStateFlow(TimeFilter.THIS_WEEK)
    val selectedTimeFilter: StateFlow<TimeFilter> = _selectedTimeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTopic = MutableStateFlow("All Topics")
    val selectedTopic: StateFlow<String> = _selectedTopic.asStateFlow()

    private val _activeExploreFilter = MutableStateFlow<ExploreFilterType?>(null)
    val activeExploreFilter: StateFlow<ExploreFilterType?> = _activeExploreFilter.asStateFlow()

    private val _activeExploreSubFilter = MutableStateFlow("All")
    val activeExploreSubFilter: StateFlow<String> = _activeExploreSubFilter.asStateFlow()

    private val _pendingDraft = MutableStateFlow<PendingRegistrationDraft?>(null)
    val pendingDraft: StateFlow<PendingRegistrationDraft?> = _pendingDraft.asStateFlow()

    fun setTimeFilter(filter: TimeFilter) {
        _selectedTimeFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTopic(topic: String) {
        _selectedTopic.value = topic
    }

    fun selectExploreFilter(filter: ExploreFilterType?, subFilter: String = "All") {
        _activeExploreFilter.value = filter
        _activeExploreSubFilter.value = subFilter
    }

    fun savePendingDraft(draft: PendingRegistrationDraft?) {
        _pendingDraft.value = draft
    }

    fun getUpcomingLeaderboard(filter: TimeFilter): List<Pair<Book, Int>> {
        return CatalogSeed.books
            .filter { it.isUpcoming }
            .map { book ->
                val count = book.interestedByFilter[filter] ?: 0
                book to count
            }
            .sortedByDescending { it.second }
    }

    fun getTodayTrending(): List<Pair<Book, String>> {
        return CatalogSeed.books
            .sortedByDescending { book ->
                val votesScore = book.ratingStats.totalVotes
                val talkBonus = if (book.isTalkOfTown) 1600 else 0
                val selectBonus = if (book.isAppSelect) 600 else 0
                val rankBonus = (100 - book.monthlyRank.coerceAtMost(99)) * 25
                val weeklyBuzz = (book.interestedByFilter[TimeFilter.THIS_WEEK] ?: 0) / 8
                votesScore + talkBonus + selectBonus + rankBonus + weeklyBuzz
            }
            .take(8)
            .mapIndexed { idx, book ->
                val badge = when (idx) {
                    0 -> "+42% today"
                    1 -> "+36% today"
                    2 -> "+29% today"
                    3 -> "+24% today"
                    else -> "+${(18 - idx * 2).coerceAtLeast(8)}% today"
                }
                book to badge
            }
    }

    fun getTalkOfTheTown(): List<Book> = CatalogSeed.books.filter { it.isTalkOfTown }

    fun getEditorsPicks(): List<Book> = CatalogSeed.books.filter { it.isEditorsPick }

    fun getAppSelectBooks(): List<Book> = CatalogSeed.books.filter { it.isAppSelect }

    fun getFilteredExploreBooks(
        filterType: ExploreFilterType?,
        subFilter: String,
        query: String
    ): List<Book> {
        val base = when (filterType) {
            ExploreFilterType.TODAY_TRENDING -> {
                val trendingList = getTodayTrending().map { it.first }
                when (subFilter) {
                    "Books" -> trendingList.filter { !it.isSeriesOrManga }
                    "Upcoming" -> trendingList.filter { it.isUpcoming }
                    "Series & Manga" -> trendingList.filter { it.isSeriesOrManga }
                    else -> trendingList
                }
            }
            ExploreFilterType.TOP_100 -> CatalogSeed.books.filter { !it.isUpcoming }.sortedBy { it.monthlyRank }
            ExploreFilterType.GENRE -> {
                if (subFilter == "All") CatalogSeed.books
                else CatalogSeed.books.filter { it.genre.equals(subFilter, ignoreCase = true) }
            }
            ExploreFilterType.LANGUAGE_COUNTRY -> {
                if (subFilter == "All") CatalogSeed.books
                else CatalogSeed.books.filter {
                    it.language.equals(subFilter, ignoreCase = true) ||
                        it.country.equals(subFilter, ignoreCase = true)
                }
            }
            ExploreFilterType.AWARDS -> CatalogSeed.books.filter { it.awards.isNotEmpty() }
            ExploreFilterType.SERIES_MANGA -> CatalogSeed.books.filter { it.isSeriesOrManga }
            ExploreFilterType.BEGINNER_YA -> CatalogSeed.books.filter { it.isBeginnerFriendly || it.ageRating == "13+" }
            ExploreFilterType.FOLLOWING, null -> CatalogSeed.books
        }
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) base
        else base.filter {
            it.title.contains(trimmed, ignoreCase = true) ||
                it.authorName.contains(trimmed, ignoreCase = true) ||
                it.genre.contains(trimmed, ignoreCase = true) ||
                it.language.contains(trimmed, ignoreCase = true) ||
                it.country.contains(trimmed, ignoreCase = true)
        }
    }
}
