package com.example.data

import com.example.model.Book
import com.example.model.BookContribution
import com.example.model.RatingMeterStats
import com.example.model.VibeSlice
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Deployment endpoint configuration for the separated Vercel Backend Functions (/backend/api).
 * Note: Only public endpoint URL belongs here; NeonDB, Cloudinary, and Firebase Admin secrets
 * stay strictly on Vercel server environment variables (/backend/.env.example).
 */
object BackendConfig {
    const val API_BASE_URL: String = ""
    const val CLOUDINARY_CLOUD_NAME: String = "t3xbg4cq"

    fun isConfigured(): Boolean = API_BASE_URL.isNotBlank()

    fun cloudinaryImageUrl(publicId: String): String {
        val clean = publicId.trim().trimStart('/')
        return if (clean.isBlank()) ""
        else "https://res.cloudinary.com/$CLOUDINARY_CLOUD_NAME/image/upload/f_auto,q_auto/$clean"
    }
}

data class AdminOverviewRemoteStats(
    val totalBooks: Int,
    val totalVotes: Int,
    val totalReviews: Int,
    val pendingContributions: Int,
    val featuredBooks: Int
)

class NokhookBackendClient(
    private val auth: FirebaseAuth? = null,
    private val baseUrl: String = BackendConfig.API_BASE_URL
) {

    fun isConfigured(): Boolean = baseUrl.isNotBlank()

    suspend fun fetchRemoteSettings(): Result<EditableAppSettings> = runCatching {
        val json = request("GET", "/api/settings", authToken = null, body = null)
        val obj = JSONObject(json)
        val data = obj.optJSONObject("settings") ?: obj
        EditableAppSettings(
            appName = data.optString("appName", "nokhook").ifBlank { "nokhook" },
            tagline = data.optString("tagline", "Read, Rate & Discuss"),
            announcementBanner = data.optString("announcementBanner", ""),
            spotlightTitle = data.optString("spotlightTitle", ""),
            spotlightSubtitle = data.optString("spotlightSubtitle", ""),
            requireLoginForReviews = data.optBoolean("requireLoginForReviews", true),
            allowContributions = data.optBoolean("allowContributions", true),
            maintenanceMode = data.optBoolean("maintenanceMode", false)
        )
    }

    suspend fun verifyAdminSession(): Result<Boolean> = runCatching {
        val token = currentIdToken() ?: error("Sign in with an authorized admin account first.")
        val json = request("GET", "/api/admin/me", authToken = token, body = null)
        val obj = JSONObject(json)
        obj.optBoolean("isAdmin", false)
    }

    suspend fun saveRemoteSettings(settings: EditableAppSettings): Result<EditableAppSettings> = runCatching {
        val token = currentIdToken() ?: error("Admin authentication required")
        val payload = JSONObject().apply {
            put("appName", settings.appName.ifBlank { "nokhook" })
            put("tagline", settings.tagline.ifBlank { "Read, Rate & Discuss" })
            put("announcementBanner", settings.announcementBanner)
            put("spotlightTitle", settings.spotlightTitle.ifBlank { "Discover Your Next Obsession" })
            put("spotlightSubtitle", settings.spotlightSubtitle)
            put("requireLoginForReviews", settings.requireLoginForReviews)
            put("allowContributions", settings.allowContributions)
            put("maintenanceMode", settings.maintenanceMode)
        }
        request("PUT", "/api/settings", authToken = token, body = payload.toString())
        settings
    }

    suspend fun upsertRemoteBook(book: Book, isNew: Boolean): Result<Unit> = runCatching {
        val token = currentIdToken() ?: error("Admin authentication required")
        val vibesArr = JSONArray().apply {
            book.vibes.forEach { v ->
                put(
                    JSONObject().apply {
                        put("name", v.name)
                        put("percentage", v.percentage)
                        put("colorHex", "#${(v.colorHex and 0xFFFFFFL).toString(16).padStart(6, '0').uppercase()}")
                    }
                )
            }
        }
        val ratingStatsObj = JSONObject().apply {
            put("mustReadVotes", book.ratingStats.mustReadVotes)
            put("worthReadingVotes", book.ratingStats.worthReadingVotes)
            put("lightReadVotes", book.ratingStats.lightReadVotes)
            put("skipVotes", book.ratingStats.skipVotes)
        }
        val payload = JSONObject().apply {
            put("id", book.id)
            put("title", book.title)
            put("subtitle", book.subtitle)
            put("authorName", book.authorName)
            put("publisherName", book.publisherName)
            put("year", book.year)
            put("pages", book.pages)
            put("language", book.language)
            put("country", book.country)
            put("genre", book.genre)
            put("subGenre", book.subGenre)
            put("overview", book.overview)
            put("coverUrl", book.coverImageUrl)
            put("isAppSelect", book.isAppSelect)
            put("isTalkOfTown", book.isTalkOfTown)
            put("isEditorsPick", book.isEditorsPick)
            put("isUpcoming", book.isUpcoming)
            put("releaseDateLabel", book.releaseDateLabel)
            put("monthlyRank", book.monthlyRank)
            put("ratingStats", ratingStatsObj)
            put("vibes", vibesArr)
        }
        val method = if (isNew) "POST" else "PUT"
        request(method, "/api/books", authToken = token, body = payload.toString())
        Unit
    }

    suspend fun fetchAdminOverview(): Result<AdminOverviewRemoteStats> = runCatching {
        val token = currentIdToken() ?: error("Admin authentication required")
        val json = request("GET", "/api/admin/overview", authToken = token, body = null)
        val obj = JSONObject(json)
        val metrics = obj.optJSONObject("stats") ?: obj.optJSONObject("metrics") ?: obj
        AdminOverviewRemoteStats(
            totalBooks = metrics.optInt("totalBooks", 0),
            totalVotes = metrics.optInt("totalRatings", metrics.optInt("totalVotes", 0)),
            totalReviews = metrics.optInt("totalReviews", 0),
            pendingContributions = metrics.optInt("pendingContributions", 0),
            featuredBooks = metrics.optInt("featuredBooks", 0)
        )
    }

    suspend fun moderateRemoteContribution(contributionId: String, status: String): Result<Unit> = runCatching {
        val token = currentIdToken() ?: error("Admin authentication required")
        val payload = JSONObject().apply {
            put("id", contributionId)
            put("status", status)
        }
        request("PATCH", "/api/contributions", authToken = token, body = payload.toString())
        Unit
    }

    private suspend fun currentIdToken(): String? {
        val user = auth?.currentUser ?: return null
        return runCatching { user.getIdToken(false).await().token }.getOrNull()
    }

    private suspend fun request(
        method: String,
        path: String,
        authToken: String?,
        body: String?
    ): String = withContext(Dispatchers.IO) {
        val cleanBase = baseUrl.trimEnd('/')
        require(cleanBase.isNotBlank()) { "Vercel Backend URL is not configured yet" }
        val conn = (URL("$cleanBase$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            if (!authToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $authToken")
            }
            if (body != null) {
                doOutput = true
            }
        }
        try {
            if (body != null) {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
            }.orEmpty()
            if (code !in 200..299) {
                val msg = runCatching { JSONObject(responseText).optString("error") }
                    .getOrNull()
                    ?.ifBlank { null }
                    ?: "HTTP $code from backend"
                error(msg)
            }
            responseText
        } finally {
            conn.disconnect()
        }
    }
}
