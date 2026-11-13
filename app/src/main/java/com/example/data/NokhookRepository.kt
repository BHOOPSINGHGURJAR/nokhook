package com.example.data

import android.content.Context
import com.example.R
import com.example.model.BookCollection
import com.example.model.BookContribution
import com.example.model.BookReview
import com.example.model.DiscussionPost
import com.example.model.DiscussionReply
import com.example.model.FollowingEdge
import com.example.model.UserBookState
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NokhookRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    // --- Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile?> = runCatching {
        val path = "users/$userId"
        try {
            val snap = db.collection("users").document(userId).get().await()
            if (snap.exists()) snap.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE) else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun upsertUserProfile(
        username: String,
        firstName: String,
        lastName: String,
        countryCode: String,
        phoneNumber: String,
        phoneVerified: Boolean,
        bio: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "username" to username.trim(),
                    "firstName" to firstName.trim(),
                    "lastName" to lastName.trim(),
                    "countryCode" to countryCode.trim(),
                    "phoneNumber" to phoneNumber.trim(),
                    "phoneVerified" to phoneVerified,
                    "bio" to bio.trim(),
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updatePayload = mapOf(
                    "username" to username.trim(),
                    "firstName" to firstName.trim(),
                    "lastName" to lastName.trim(),
                    "countryCode" to countryCode.trim(),
                    "phoneNumber" to phoneNumber.trim(),
                    "phoneVerified" to phoneVerified,
                    "bio" to bio.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Book States (Read / TBR / Interested / 4-Tier Rating Vote) ---

    fun observeUserBookStates(userId: String): Flow<List<UserBookState>> = flow {
        val path = "users/$userId/bookStates"
        emitAll(
            db.collection("users").document(userId).collection("bookStates")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserBookState::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun setBookState(
        bookId: String,
        status: String,
        ratingTier: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("bookStates").document(bookId)
        try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "bookId" to bookId,
                    "status" to status,
                    "ratingTier" to ratingTier,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val payload = mapOf(
                    "status" to status,
                    "ratingTier" to ratingTier,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Following ---

    fun observeFollowing(userId: String): Flow<List<FollowingEdge>> = flow {
        val path = "users/$userId/following"
        emitAll(
            db.collection("users").document(userId).collection("following")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FollowingEdge::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun toggleFollow(
        targetUserId: String,
        targetUsername: String,
        targetDisplayName: String,
        currentlyFollowing: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("following").document(targetUserId)
        try {
            if (currentlyFollowing) {
                docRef.delete().await()
            } else {
                val payload = mapOf(
                    "targetUserId" to targetUserId,
                    "targetUsername" to targetUsername,
                    "targetDisplayName" to targetDisplayName,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Collections ---

    fun observeUserCollections(userId: String): Flow<List<BookCollection>> = flow {
        val path = "collections"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(BookCollection::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observePublicCollections(expectedUserId: String): Flow<List<BookCollection>> = flow {
        val path = "collections"
        emitAll(
            db.collection(path)
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(BookCollection::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, expectedUserId)
                    throw error
                }
        )
    }

    suspend fun createCollection(
        creatorName: String,
        creatorUsername: String,
        title: String,
        description: String,
        isPublic: Boolean,
        bookIds: List<String>,
        topicTag: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("collections").document()
        val payload = mapOf(
            "userId" to uid,
            "creatorName" to creatorName,
            "creatorUsername" to creatorUsername,
            "title" to title.trim(),
            "description" to description.trim(),
            "isPublic" to isPublic,
            "bookIds" to bookIds.distinct().take(50),
            "topicTag" to topicTag,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun addBookToCollection(collection: BookCollection, bookId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(collection.userId == uid) { "Cannot edit another user's collection" }
        val updatedBooks = (collection.bookIds + bookId).distinct().take(50)
        val docRef = db.collection("collections").document(collection.id)
        try {
            docRef.update(
                mapOf(
                    "bookIds" to updatedBooks,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Reviews ---

    fun observePublicReviews(expectedUserId: String): Flow<List<BookReview>> = flow {
        val path = "reviews"
        emitAll(
            db.collection(path)
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(BookReview::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, expectedUserId)
                    throw error
                }
        )
    }

    suspend fun postReview(
        bookId: String,
        bookTitle: String,
        authorName: String,
        authorUsername: String,
        ratingTier: String,
        topicTag: String,
        body: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("reviews").document()
        val payload = mapOf(
            "userId" to uid,
            "bookId" to bookId,
            "bookTitle" to bookTitle,
            "authorName" to authorName,
            "authorUsername" to authorUsername,
            "ratingTier" to ratingTier,
            "topicTag" to topicTag,
            "body" to body.trim(),
            "isPublic" to true,
            "likesCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Discussions & Nested Replies ---

    fun observePublicDiscussions(expectedUserId: String): Flow<List<DiscussionPost>> = flow {
        val path = "discussions"
        emitAll(
            db.collection(path)
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DiscussionPost::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, expectedUserId)
                    throw error
                }
        )
    }

    fun observeDiscussionReplies(discussionId: String, expectedUserId: String): Flow<List<DiscussionReply>> = flow {
        val path = "discussions/$discussionId/replies"
        emitAll(
            db.collection("discussions").document(discussionId)
                .collection("replies")
                .whereEqualTo("isPublic", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DiscussionReply::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, expectedUserId)
                    throw error
                }
        )
    }

    suspend fun createDiscussion(
        authorName: String,
        authorUsername: String,
        topicTag: String,
        bookId: String,
        title: String,
        body: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("discussions").document()
        val payload = mapOf(
            "userId" to uid,
            "authorName" to authorName,
            "authorUsername" to authorUsername,
            "topicTag" to topicTag,
            "bookId" to bookId,
            "title" to title.trim(),
            "body" to body.trim(),
            "isPublic" to true,
            "upvotes" to 0L,
            "repliesCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun postDiscussionReply(
        discussionId: String,
        authorName: String,
        authorUsername: String,
        parentReplyId: String,
        body: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("discussions").document(discussionId)
            .collection("replies").document()
        val payload = mapOf(
            "userId" to uid,
            "authorName" to authorName,
            "authorUsername" to authorUsername,
            "parentReplyId" to parentReplyId,
            "body" to body.trim(),
            "isPublic" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Crowdsourced Contributions ---

    fun observeUserContributions(userId: String): Flow<List<BookContribution>> = flow {
        val path = "contributions"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(BookContribution::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun submitContribution(
        bookTitle: String,
        authorName: String,
        publisher: String,
        language: String,
        genre: String,
        notes: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("contributions").document()
        val payload = mapOf(
            "userId" to uid,
            "bookTitle" to bookTitle.trim(),
            "authorName" to authorName.trim(),
            "publisher" to publisher.trim(),
            "language" to language.trim(),
            "genre" to genre.trim(),
            "notes" to notes.trim(),
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    private fun logStreamError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUserId: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUserId) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
