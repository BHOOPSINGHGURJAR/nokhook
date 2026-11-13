package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NokhookRepository
import com.example.model.BookCollection
import com.example.model.BookContribution
import com.example.model.BookReview
import com.example.model.DiscussionPost
import com.example.model.DiscussionReply
import com.example.model.FollowingEdge
import com.example.model.RatingTier
import com.example.model.ReadingStatus
import com.example.model.UserBookState
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class AuthenticatedViewModel(
    private val repository: NokhookRepository,
    val currentUserId: String
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error -> emit(UiState.Error(unwrapFirestoreError(error, "profile"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val bookStates: StateFlow<Map<String, UserBookState>> = repository.observeUserBookStates(currentUserId)
        .map { list -> list.associateBy { it.bookId } }
        .catch { error ->
            unwrapFirestoreError(error, "bookStates")
            emit(emptyMap())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyMap())

    val following: StateFlow<List<FollowingEdge>> = repository.observeFollowing(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error, "following")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val userCollections: StateFlow<List<BookCollection>> = repository.observeUserCollections(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error, "userCollections")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val publicCollections: StateFlow<List<BookCollection>> = repository.observePublicCollections(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error, "publicCollections")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val publicReviews: StateFlow<List<BookReview>> = repository.observePublicReviews(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error, "publicReviews")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val publicDiscussions: StateFlow<List<DiscussionPost>> = repository.observePublicDiscussions(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error, "publicDiscussions")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val contributions: StateFlow<List<BookContribution>> = repository.observeUserContributions(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error, "contributions")
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun observeDiscussionReplies(discussionId: String): Flow<List<DiscussionReply>> {
        return repository.observeDiscussionReplies(discussionId, currentUserId)
            .catch { error ->
                unwrapFirestoreError(error, "replies")
                emit(emptyList())
            }
    }

    fun saveUserProfile(
        username: String,
        firstName: String,
        lastName: String,
        countryCode: String,
        phoneNumber: String,
        phoneVerified: Boolean,
        bio: String
    ) {
        viewModelScope.launch {
            val res = repository.upsertUserProfile(
                username = username,
                firstName = firstName,
                lastName = lastName,
                countryCode = countryCode,
                phoneNumber = phoneNumber,
                phoneVerified = phoneVerified,
                bio = bio
            )
            _actionMessage.value = if (res.isSuccess) {
                "Reader profile saved"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to save profile"
            }
        }
    }

    fun ensureDefaultProfileIfMissing(
        draft: PendingRegistrationDraft?,
        displayNameFallback: String?
    ) {
        val current = (userProfile.value as? UiState.Success)?.data
        if (current != null) return
        viewModelScope.launch {
            val existing = repository.getUserProfile(currentUserId).getOrNull()
            if (existing == null) {
                val parts = (displayNameFallback ?: "Nokhook Reader").trim().split(" ")
                val first = draft?.firstName?.ifBlank { null } ?: parts.firstOrNull().orEmpty().ifBlank { "Reader" }
                val last = draft?.lastName?.ifBlank { null } ?: parts.drop(1).joinToString(" ").ifBlank { "Member" }
                val rawHandle = draft?.username?.ifBlank { null }
                    ?: (first.lowercase().filter { it.isLetterOrDigit() } + "_" + currentUserId.take(4))
                val usePhone = draft?.verifiedMethod != "email" && draft?.phoneVerified == true
                repository.upsertUserProfile(
                    username = rawHandle.take(28),
                    firstName = first,
                    lastName = last,
                    countryCode = if (usePhone) draft?.countryCode.orEmpty() else "",
                    phoneNumber = if (usePhone) draft?.phoneNumber.orEmpty() else "",
                    phoneVerified = usePhone,
                    bio = "Exploring great stories on nokhook"
                )
            }
        }
    }

    fun toggleBookStatus(bookId: String, targetStatus: ReadingStatus) {
        viewModelScope.launch {
            val current = bookStates.value[bookId]
            val nextStatus = if (current?.status == targetStatus.key) {
                ReadingStatus.NONE.key
            } else {
                targetStatus.key
            }
            val currentTier = current?.ratingTier ?: ""
            val res = repository.setBookState(bookId, nextStatus, currentTier)
            _actionMessage.value = if (res.isSuccess) {
                when (nextStatus) {
                    ReadingStatus.READ.key -> "Marked as Read"
                    ReadingStatus.TBR.key -> "Saved to Read Later (TBR)"
                    ReadingStatus.INTERESTED.key -> "Marked Interested — release alert set"
                    else -> "Removed from shelf"
                }
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Could not update shelf"
            }
        }
    }

    fun voteRatingTier(bookId: String, tier: RatingTier) {
        viewModelScope.launch {
            val current = bookStates.value[bookId]
            val currentStatus = current?.status ?: ReadingStatus.NONE.key
            val res = repository.setBookState(bookId, currentStatus, tier.key)
            _actionMessage.value = if (res.isSuccess) {
                "Rated: ${tier.label}"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Could not save rating"
            }
        }
    }

    fun createCollection(
        title: String,
        description: String,
        isPublic: Boolean,
        initialBookIds: List<String>,
        topicTag: String
    ) {
        viewModelScope.launch {
            val profile = (userProfile.value as? UiState.Success)?.data
            val name = listOfNotNull(profile?.firstName, profile?.lastName)
                .joinToString(" ").ifBlank { "Nokhook Reader" }
            val username = profile?.username?.ifBlank { null } ?: "reader_${currentUserId.take(4)}"
            val res = repository.createCollection(
                creatorName = name,
                creatorUsername = username,
                title = title,
                description = description,
                isPublic = isPublic,
                bookIds = initialBookIds,
                topicTag = topicTag
            )
            _actionMessage.value = if (res.isSuccess) {
                "Collection \"$title\" created"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to create collection"
            }
        }
    }

    fun addBookToCollection(collection: BookCollection, bookId: String) {
        viewModelScope.launch {
            val res = repository.addBookToCollection(collection, bookId)
            _actionMessage.value = if (res.isSuccess) {
                "Added to ${collection.title}"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update collection"
            }
        }
    }

    fun postReview(
        bookId: String,
        bookTitle: String,
        ratingTier: RatingTier,
        topicTag: String,
        body: String
    ) {
        viewModelScope.launch {
            val profile = (userProfile.value as? UiState.Success)?.data
            val name = listOfNotNull(profile?.firstName, profile?.lastName)
                .joinToString(" ").ifBlank { "Nokhook Reader" }
            val username = profile?.username?.ifBlank { null } ?: "reader_${currentUserId.take(4)}"
            val res = repository.postReview(
                bookId = bookId,
                bookTitle = bookTitle,
                authorName = name,
                authorUsername = username,
                ratingTier = ratingTier.key,
                topicTag = topicTag,
                body = body
            )
            _actionMessage.value = if (res.isSuccess) {
                "Review published to community"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to post review"
            }
        }
    }

    fun createDiscussion(
        topicTag: String,
        bookId: String,
        title: String,
        body: String
    ) {
        viewModelScope.launch {
            val profile = (userProfile.value as? UiState.Success)?.data
            val name = listOfNotNull(profile?.firstName, profile?.lastName)
                .joinToString(" ").ifBlank { "Nokhook Reader" }
            val username = profile?.username?.ifBlank { null } ?: "reader_${currentUserId.take(4)}"
            val res = repository.createDiscussion(
                authorName = name,
                authorUsername = username,
                topicTag = topicTag,
                bookId = bookId,
                title = title,
                body = body
            )
            _actionMessage.value = if (res.isSuccess) {
                "Discussion started"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to create discussion"
            }
        }
    }

    fun postReply(discussionId: String, parentReplyId: String, body: String) {
        viewModelScope.launch {
            val profile = (userProfile.value as? UiState.Success)?.data
            val name = listOfNotNull(profile?.firstName, profile?.lastName)
                .joinToString(" ").ifBlank { "Nokhook Reader" }
            val username = profile?.username?.ifBlank { null } ?: "reader_${currentUserId.take(4)}"
            val res = repository.postDiscussionReply(
                discussionId = discussionId,
                authorName = name,
                authorUsername = username,
                parentReplyId = parentReplyId,
                body = body
            )
            _actionMessage.value = if (res.isSuccess) {
                "Reply posted"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to post reply"
            }
        }
    }

    fun toggleFollow(targetUserId: String, targetUsername: String, targetDisplayName: String) {
        viewModelScope.launch {
            val isFollowing = following.value.any { it.targetUserId == targetUserId }
            val res = repository.toggleFollow(targetUserId, targetUsername, targetDisplayName, isFollowing)
            _actionMessage.value = if (res.isSuccess) {
                if (isFollowing) "Unfollowed @$targetUsername" else "Following @$targetUsername"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update follow status"
            }
        }
    }

    fun submitContribution(
        bookTitle: String,
        authorName: String,
        publisher: String,
        language: String,
        genre: String,
        notes: String
    ) {
        viewModelScope.launch {
            val res = repository.submitContribution(
                bookTitle = bookTitle,
                authorName = authorName,
                publisher = publisher,
                language = language,
                genre = genre,
                notes = notes
            )
            _actionMessage.value = if (res.isSuccess) {
                com.example.data.CatalogOverrideStore.recordSubmittedContribution(
                    BookContribution(
                        id = "sub_${System.currentTimeMillis()}",
                        userId = currentUserId,
                        bookTitle = bookTitle,
                        authorName = authorName,
                        publisher = publisher,
                        language = language,
                        genre = genre,
                        notes = notes,
                        status = "pending"
                    )
                )
                "Submitted \"$bookTitle\" for editorial approval"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Submission failed"
            }
        }
    }

    private fun unwrapFirestoreError(error: Throwable, tag: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing $tag", root)
        return root.message ?: "Failed to sync $tag"
    }

    private companion object {
        const val TAG = "AuthVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
