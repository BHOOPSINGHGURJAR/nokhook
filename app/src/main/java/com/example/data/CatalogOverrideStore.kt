package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.model.Book
import com.example.model.BookContribution
import com.example.model.RatingMeterStats
import com.example.model.VibeSlice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

data class EditableAppSettings(
    val appName: String = "nokhook",
    val tagline: String = "Read, Rate & Discuss",
    val announcementBanner: String = "",
    val spotlightTitle: String = "",
    val spotlightSubtitle: String = "",
    val requireLoginForReviews: Boolean = true,
    val allowContributions: Boolean = true,
    val maintenanceMode: Boolean = false
)

/**
 * Runtime + SharedPreferences store for Admin Studio edits (catalog books, vibe profiles,
 * rating stats, banners, app settings, and contribution moderation queue).
 * Safe to invoke cleanly in JVM unit tests without an Android Context.
 */
object CatalogOverrideStore {
    private const val PREFS_NAME = "nokhook_admin_overrides_v1"
    private const val KEY_SETTINGS = "app_settings_json"
    private const val KEY_BOOK_OVERRIDES = "book_overrides_json"
    private const val KEY_MODERATION_QUEUE = "moderation_queue_json"

    private var prefs: SharedPreferences? = null

    private val _appSettings = MutableStateFlow(EditableAppSettings())
    val appSettings: StateFlow<EditableAppSettings> = _appSettings.asStateFlow()

    private val _bookOverrides = MutableStateFlow<Map<String, Book>>(emptyMap())
    val bookOverrides: StateFlow<Map<String, Book>> = _bookOverrides.asStateFlow()

    private val _customBooks = MutableStateFlow<List<Book>>(emptyList())
    val customBooks: StateFlow<List<Book>> = _customBooks.asStateFlow()

    private val _deletedBookIds = MutableStateFlow<Set<String>>(emptySet())
    val deletedBookIds: StateFlow<Set<String>> = _deletedBookIds.asStateFlow()

    private val _catalogVersion = MutableStateFlow(0)
    val catalogVersion: StateFlow<Int> = _catalogVersion.asStateFlow()

    private val _moderationQueue = MutableStateFlow(defaultModerationSeeds())
    val moderationQueue: StateFlow<List<BookContribution>> = _moderationQueue.asStateFlow()

    fun initialize(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        loadFromPrefs(sp)
    }

    fun getEffectiveBooks(seed: List<Book> = CatalogSeed.seedBooks): List<Book> {
        val overrides = _bookOverrides.value
        val deleted = _deletedBookIds.value
        val updatedSeed = seed
            .filterNot { it.id in deleted }
            .map { base -> overrides[base.id] ?: base }
        val custom = _customBooks.value.filterNot { it.id in deleted }
        return custom + updatedSeed
    }

    fun getBookById(id: String): Book? = getEffectiveBooks().firstOrNull { it.id == id }

    fun isSeedBook(id: String): Boolean = CatalogSeed.seedBooks.any { it.id == id }

    fun updateAppSettings(settings: EditableAppSettings) {
        _appSettings.value = settings
        persistSettings()
    }

    fun resetAppSettings() {
        _appSettings.value = EditableAppSettings()
        persistSettings()
    }

    fun upsertBook(book: Book) {
        if (isSeedBook(book.id)) {
            _bookOverrides.update { it + (book.id to book) }
            _deletedBookIds.update { it - book.id }
        } else {
            _customBooks.update { list ->
                val without = list.filterNot { it.id == book.id }
                listOf(book) + without
            }
            _deletedBookIds.update { it - book.id }
        }
        _catalogVersion.update { it + 1 }
        persistBookEdits()
    }

    fun resetSeedBookOverride(bookId: String) {
        _bookOverrides.update { it - bookId }
        _deletedBookIds.update { it - bookId }
        _catalogVersion.update { it + 1 }
        persistBookEdits()
    }

    fun deleteBook(bookId: String) {
        if (isSeedBook(bookId)) {
            _bookOverrides.update { it - bookId }
            _deletedBookIds.update { it + bookId }
        } else {
            _customBooks.update { list -> list.filterNot { it.id == bookId } }
        }
        _catalogVersion.update { it + 1 }
        persistBookEdits()
    }

    fun recordSubmittedContribution(contribution: BookContribution) {
        _moderationQueue.update { existing ->
            val withoutDup = existing.filterNot { it.id == contribution.id }
            listOf(contribution) + withoutDup
        }
        persistModerationQueue()
    }

    fun moderateContribution(contributionId: String, newStatus: String) {
        var approvedItem: BookContribution? = null
        _moderationQueue.update { list ->
            list.map { item ->
                if (item.id == contributionId) {
                    val updated = item.copy(status = newStatus)
                    if (newStatus == "approved") approvedItem = updated
                    updated
                } else {
                    item
                }
            }
        }
        persistModerationQueue()

        approvedItem?.let { item ->
            val slug = "contrib_${item.bookTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { item.id }}"
            if (getBookById(slug) == null) {
                val created = Book(
                    id = slug,
                    title = item.bookTitle,
                    subtitle = if (item.publisher.isNotBlank()) "Published by ${item.publisher}" else "Community Approved Addition",
                    authorId = "auth_community",
                    authorName = item.authorName,
                    publisherId = "pub_community",
                    publisherName = item.publisher.ifBlank { "Independent Press" },
                    year = 2026,
                    pages = 280,
                    estimatedHours = 4,
                    language = item.language.ifBlank { "English" },
                    country = "India",
                    ageRating = "13+",
                    genre = item.genre.ifBlank { "Fiction" },
                    subGenre = "Editorial Addition",
                    awards = emptyList(),
                    overview = item.notes.ifBlank { "${item.bookTitle} by ${item.authorName} — approved by nokhook editorial admins." },
                    vibes = listOf(
                        VibeSlice("Inspirational", 35, 0xFF00C288),
                        VibeSlice("Dark", 20, 0xFFE53935),
                        VibeSlice("Fast-paced", 25, 0xFF7B61FF),
                        VibeSlice("Educational", 20, 0xFFF5A623)
                    ),
                    ratingStats = RatingMeterStats(mustReadVotes = 18, worthReadingVotes = 12, lightReadVotes = 5, skipVotes = 1),
                    coverRes = R.drawable.img_book_cover_1,
                    accentColorHex = 0xFF6D28D9,
                    isAppSelect = false,
                    isTalkOfTown = true,
                    isEditorsPick = false
                )
                upsertBook(created)
            }
        }
    }

    fun resetAllForTesting() {
        _appSettings.value = EditableAppSettings()
        _bookOverrides.value = emptyMap()
        _customBooks.value = emptyList()
        _deletedBookIds.value = emptySet()
        _moderationQueue.value = defaultModerationSeeds()
        _catalogVersion.update { it + 1 }
        prefs?.edit()?.clear()?.apply()
    }

    private fun defaultModerationSeeds(): List<BookContribution> = listOf(
        BookContribution(
            id = "mod_seed_1",
            userId = "community_reader_1",
            bookTitle = "The Salt Collector's Almanac",
            authorName = "Devika Nair",
            publisher = "Penguin Literary",
            language = "English",
            genre = "Fiction",
            notes = "Winner of the Malabar Short Story Fellowship; coastal folklore anthology.",
            status = "pending"
        ),
        BookContribution(
            id = "mod_seed_2",
            userId = "community_reader_2",
            bookTitle = "Neeli Dhoop",
            authorName = "Harshvardhan Kulkarni",
            publisher = "Rajkamal Heritage",
            language = "Hindi",
            genre = "Fiction",
            notes = "Classic 1980s Varanasi campus novel requested by Hindi readers club.",
            status = "pending"
        )
    )

    private fun loadFromPrefs(sp: SharedPreferences) {
        sp.getString(KEY_SETTINGS, null)?.let { raw ->
            runCatching {
                val o = JSONObject(raw)
                _appSettings.value = EditableAppSettings(
                    appName = o.optString("appName", "nokhook").ifBlank { "nokhook" },
                    tagline = o.optString("tagline", "Read, Rate & Discuss"),
                    announcementBanner = o.optString("announcementBanner", ""),
                    spotlightTitle = o.optString("spotlightTitle", ""),
                    spotlightSubtitle = o.optString("spotlightSubtitle", ""),
                    requireLoginForReviews = o.optBoolean("requireLoginForReviews", true),
                    allowContributions = o.optBoolean("allowContributions", true),
                    maintenanceMode = o.optBoolean("maintenanceMode", false)
                )
            }
        }

        sp.getString(KEY_BOOK_OVERRIDES, null)?.let { raw ->
            runCatching {
                val root = JSONObject(raw)
                val overridesMap = mutableMapOf<String, Book>()
                val overridesArr = root.optJSONArray("overrides") ?: JSONArray()
                for (i in 0 until overridesArr.length()) {
                    bookFromJson(overridesArr.getJSONObject(i))?.let { overridesMap[it.id] = it }
                }
                val customList = mutableListOf<Book>()
                val customArr = root.optJSONArray("custom") ?: JSONArray()
                for (i in 0 until customArr.length()) {
                    bookFromJson(customArr.getJSONObject(i))?.let { customList.add(it) }
                }
                val deletedSet = mutableSetOf<String>()
                val deletedArr = root.optJSONArray("deleted") ?: JSONArray()
                for (i in 0 until deletedArr.length()) {
                    deletedSet.add(deletedArr.getString(i))
                }
                _bookOverrides.value = overridesMap
                _customBooks.value = customList
                _deletedBookIds.value = deletedSet
                _catalogVersion.update { it + 1 }
            }
        }

        sp.getString(KEY_MODERATION_QUEUE, null)?.let { raw ->
            runCatching {
                val arr = JSONArray(raw)
                val loaded = mutableListOf<BookContribution>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    loaded.add(
                        BookContribution(
                            id = o.optString("id"),
                            userId = o.optString("userId"),
                            bookTitle = o.optString("bookTitle"),
                            authorName = o.optString("authorName"),
                            publisher = o.optString("publisher"),
                            language = o.optString("language", "English"),
                            genre = o.optString("genre", "Fiction"),
                            notes = o.optString("notes"),
                            status = o.optString("status", "pending")
                        )
                    )
                }
                if (loaded.isNotEmpty()) _moderationQueue.value = loaded
            }
        }
    }

    private fun persistSettings() {
        val sp = prefs ?: return
        val s = _appSettings.value
        val obj = JSONObject().apply {
            put("appName", s.appName)
            put("tagline", s.tagline)
            put("announcementBanner", s.announcementBanner)
            put("spotlightTitle", s.spotlightTitle)
            put("spotlightSubtitle", s.spotlightSubtitle)
            put("requireLoginForReviews", s.requireLoginForReviews)
            put("allowContributions", s.allowContributions)
            put("maintenanceMode", s.maintenanceMode)
        }
        sp.edit().putString(KEY_SETTINGS, obj.toString()).apply()
    }

    private fun persistBookEdits() {
        val sp = prefs ?: return
        val root = JSONObject().apply {
            put("overrides", JSONArray().apply {
                _bookOverrides.value.values.forEach { put(bookToJson(it)) }
            })
            put("custom", JSONArray().apply {
                _customBooks.value.forEach { put(bookToJson(it)) }
            })
            put("deleted", JSONArray().apply {
                _deletedBookIds.value.forEach { put(it) }
            })
        }
        sp.edit().putString(KEY_BOOK_OVERRIDES, root.toString()).apply()
    }

    private fun persistModerationQueue() {
        val sp = prefs ?: return
        val arr = JSONArray().apply {
            _moderationQueue.value.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("userId", c.userId)
                    put("bookTitle", c.bookTitle)
                    put("authorName", c.authorName)
                    put("publisher", c.publisher)
                    put("language", c.language)
                    put("genre", c.genre)
                    put("notes", c.notes)
                    put("status", c.status)
                })
            }
        }
        sp.edit().putString(KEY_MODERATION_QUEUE, arr.toString()).apply()
    }

    private fun bookToJson(b: Book): JSONObject = JSONObject().apply {
        put("id", b.id)
        put("title", b.title)
        put("subtitle", b.subtitle)
        put("authorId", b.authorId)
        put("authorName", b.authorName)
        put("publisherId", b.publisherId)
        put("publisherName", b.publisherName)
        put("year", b.year)
        put("pages", b.pages)
        put("estimatedHours", b.estimatedHours)
        put("language", b.language)
        put("country", b.country)
        put("ageRating", b.ageRating)
        put("genre", b.genre)
        put("subGenre", b.subGenre)
        put("overview", b.overview)
        put("coverRes", b.coverRes)
        put("coverImageUrl", b.coverImageUrl)
        put("accentColorHex", b.accentColorHex)
        put("isAppSelect", b.isAppSelect)
        put("isTalkOfTown", b.isTalkOfTown)
        put("isEditorsPick", b.isEditorsPick)
        put("isUpcoming", b.isUpcoming)
        put("releaseDateLabel", b.releaseDateLabel)
        put("isSeriesOrManga", b.isSeriesOrManga)
        put("seriesLabel", b.seriesLabel)
        put("isBeginnerFriendly", b.isBeginnerFriendly)
        put("monthlyRank", b.monthlyRank)
        put("mustReadVotes", b.ratingStats.mustReadVotes)
        put("worthReadingVotes", b.ratingStats.worthReadingVotes)
        put("lightReadVotes", b.ratingStats.lightReadVotes)
        put("skipVotes", b.ratingStats.skipVotes)
        put("vibes", JSONArray().apply {
            b.vibes.forEach { v ->
                put(JSONObject().apply {
                    put("name", v.name)
                    put("percentage", v.percentage)
                    put("colorHex", v.colorHex)
                })
            }
        })
    }

    private fun bookFromJson(o: JSONObject): Book? {
        val id = o.optString("id")
        if (id.isBlank()) return null
        val seedMatch = CatalogSeed.seedBooks.firstOrNull { it.id == id }
        val vibesArr = o.optJSONArray("vibes")
        val vibes = if (vibesArr != null && vibesArr.length() > 0) {
            (0 until vibesArr.length()).map { idx ->
                val vo = vibesArr.getJSONObject(idx)
                VibeSlice(
                    name = vo.optString("name", "Inspirational"),
                    percentage = vo.optInt("percentage", 25),
                    colorHex = vo.optLong("colorHex", 0xFF00C288)
                )
            }
        } else {
            seedMatch?.vibes ?: listOf(
                VibeSlice("Inspirational", 25, 0xFF00C288),
                VibeSlice("Dark", 25, 0xFFE53935),
                VibeSlice("Fast-paced", 25, 0xFF7B61FF),
                VibeSlice("Educational", 25, 0xFFF5A623)
            )
        }
        return Book(
            id = id,
            title = o.optString("title", seedMatch?.title.orEmpty()),
            subtitle = o.optString("subtitle", seedMatch?.subtitle.orEmpty()),
            authorId = o.optString("authorId", seedMatch?.authorId ?: "auth_community"),
            authorName = o.optString("authorName", seedMatch?.authorName.orEmpty()),
            publisherId = o.optString("publisherId", seedMatch?.publisherId ?: "pub_community"),
            publisherName = o.optString("publisherName", seedMatch?.publisherName.orEmpty()),
            year = o.optInt("year", seedMatch?.year ?: 2026),
            pages = o.optInt("pages", seedMatch?.pages ?: 300),
            estimatedHours = o.optInt("estimatedHours", seedMatch?.estimatedHours ?: 5),
            language = o.optString("language", seedMatch?.language ?: "English"),
            country = o.optString("country", seedMatch?.country ?: "India"),
            ageRating = o.optString("ageRating", seedMatch?.ageRating ?: "13+"),
            genre = o.optString("genre", seedMatch?.genre ?: "Fiction"),
            subGenre = o.optString("subGenre", seedMatch?.subGenre ?: "General"),
            awards = seedMatch?.awards ?: emptyList(),
            overview = o.optString("overview", seedMatch?.overview.orEmpty()),
            vibes = vibes,
            ratingStats = RatingMeterStats(
                mustReadVotes = o.optInt("mustReadVotes", seedMatch?.ratingStats?.mustReadVotes ?: 10),
                worthReadingVotes = o.optInt("worthReadingVotes", seedMatch?.ratingStats?.worthReadingVotes ?: 10),
                lightReadVotes = o.optInt("lightReadVotes", seedMatch?.ratingStats?.lightReadVotes ?: 5),
                skipVotes = o.optInt("skipVotes", seedMatch?.ratingStats?.skipVotes ?: 1)
            ),
            coverRes = o.optInt("coverRes", seedMatch?.coverRes ?: R.drawable.img_book_cover_1).let {
                if (it == 0) R.drawable.img_book_cover_1 else it
            },
            accentColorHex = o.optLong("accentColorHex", seedMatch?.accentColorHex ?: 0xFF6D28D9),
            isAppSelect = o.optBoolean("isAppSelect", seedMatch?.isAppSelect ?: false),
            isTalkOfTown = o.optBoolean("isTalkOfTown", seedMatch?.isTalkOfTown ?: false),
            isEditorsPick = o.optBoolean("isEditorsPick", seedMatch?.isEditorsPick ?: false),
            isUpcoming = o.optBoolean("isUpcoming", seedMatch?.isUpcoming ?: false),
            releaseDateLabel = o.optString("releaseDateLabel", seedMatch?.releaseDateLabel.orEmpty()),
            interestedByFilter = seedMatch?.interestedByFilter ?: emptyMap(),
            isSeriesOrManga = o.optBoolean("isSeriesOrManga", seedMatch?.isSeriesOrManga ?: false),
            seriesLabel = o.optString("seriesLabel", seedMatch?.seriesLabel.orEmpty()),
            isBeginnerFriendly = o.optBoolean("isBeginnerFriendly", seedMatch?.isBeginnerFriendly ?: false),
            monthlyRank = o.optInt("monthlyRank", seedMatch?.monthlyRank ?: 99),
            buyLinks = seedMatch?.buyLinks ?: emptyList(),
            recommendations = seedMatch?.recommendations ?: emptyList(),
            coverImageUrl = o.optString("coverImageUrl", seedMatch?.coverImageUrl.orEmpty())
        )
    }
}
