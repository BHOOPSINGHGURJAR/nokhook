package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val countryCode: String = "+91",
    val phoneNumber: String = "",
    val phoneVerified: Boolean = false,
    val bio: String = "",
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserBookState(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookId: String = "",
    val status: String = "none",
    val ratingTier: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FollowingEdge(
    @DocumentId val id: String = "",
    val targetUserId: String = "",
    val targetUsername: String = "",
    val targetDisplayName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BookCollection(
    @DocumentId val id: String = "",
    val userId: String = "",
    val creatorName: String = "",
    val creatorUsername: String = "",
    val title: String = "",
    val description: String = "",
    @get:PropertyName("isPublic") @set:PropertyName("isPublic")
    var isPublic: Boolean = true,
    val bookIds: List<String> = emptyList(),
    val topicTag: String = "General",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BookReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val ratingTier: String = "must_read",
    val topicTag: String = "Fiction",
    val body: String = "",
    @get:PropertyName("isPublic") @set:PropertyName("isPublic")
    var isPublic: Boolean = true,
    val likesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DiscussionPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val topicTag: String = "Fiction",
    val bookId: String = "",
    val title: String = "",
    val body: String = "",
    @get:PropertyName("isPublic") @set:PropertyName("isPublic")
    var isPublic: Boolean = true,
    val upvotes: Long = 0L,
    val repliesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DiscussionReply(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val parentReplyId: String = "",
    val body: String = "",
    @get:PropertyName("isPublic") @set:PropertyName("isPublic")
    var isPublic: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BookContribution(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookTitle: String = "",
    val authorName: String = "",
    val publisher: String = "",
    val language: String = "English",
    val genre: String = "Fiction",
    val notes: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
