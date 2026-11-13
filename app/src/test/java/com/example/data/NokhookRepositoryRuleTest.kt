package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class NokhookRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertAndObserveUserProfile_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = NokhookRepository(firestore, auth)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.upsertUserProfile(
                username = "alice_reader",
                firstName = "Alice",
                lastName = "Verma",
                countryCode = "+91",
                phoneNumber = "9876543210",
                phoneVerified = true,
                bio = "Loves mystery novels"
            )
        }
        assertTrue(saveResult.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it != null }
        }
        assertNotNull(profile)
        assertEquals("alice_reader", profile?.username)
        assertEquals(true, profile?.phoneVerified)
    }

    @Test
    fun getUserProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = NokhookRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertUserProfile(
                username = "alice_reader",
                firstName = "Alice",
                lastName = "Verma",
                countryCode = "+91",
                phoneNumber = "9876543210",
                phoneVerified = true,
                bio = ""
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = NokhookRepository(firestore, auth)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getUserProfile(aliceUid)
        }
        assertTrue(result.isFailure)
        val firestoreEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun setBookState_andObserveBookStates_succeedsForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = NokhookRepository(firestore, auth)
        val bookId = "book_${UUID.randomUUID().toString().take(8)}"

        val writeRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.setBookState(bookId = bookId, status = "read", ratingTier = "must_read")
        }
        assertTrue(writeRes.isSuccess)

        val states = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserBookStates(uid).first { list -> list.any { it.bookId == bookId } }
        }
        assertEquals("must_read", states.first { it.bookId == bookId }.ratingTier)
    }

    @Test
    fun postReview_andObservePublicReviews_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = NokhookRepository(firestore, auth)
        val bookId = "book_${UUID.randomUUID().toString().take(8)}"

        val reviewId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.postReview(
                bookId = bookId,
                bookTitle = "Midnight Lighthouse",
                authorName = "Alice Verma",
                authorUsername = "alice_reader",
                ratingTier = "must_read",
                topicTag = "Fiction",
                body = "Captivating atmosphere from start to finish."
            ).getOrThrow()
        }
        assertTrue(reviewId.isNotEmpty())

        val reviews = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePublicReviews(uid).first { list -> list.any { it.id == reviewId } }
        }
        assertTrue(reviews.any { it.id == reviewId && it.isPublic })
    }

    @Test
    fun createCollection_andSubmitContribution_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = NokhookRepository(firestore, auth)

        val colId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createCollection(
                creatorName = "Alice Verma",
                creatorUsername = "alice_reader",
                title = "Rainy Day Reads",
                description = "Cozy books",
                isPublic = true,
                bookIds = listOf("book_1"),
                topicTag = "Fiction"
            ).getOrThrow()
        }
        assertTrue(colId.isNotEmpty())

        val contribId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitContribution(
                bookTitle = "Godaan",
                authorName = "Munshi Premchand",
                publisher = "Hans Prakashan",
                language = "Hindi",
                genre = "Classic Fiction",
                notes = "Classic Hindi novel"
            ).getOrThrow()
        }
        assertTrue(contribId.isNotEmpty())

        val contribs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserContributions(uid).first { list -> list.any { it.id == contribId } }
        }
        assertEquals("pending", contribs.first { it.id == contribId }.status)
    }

    @Test
    fun observeUserBookStates_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = NokhookRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeUserBookStates("some_uid_123").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull(caught)
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_nokhook@test.com"
        const val BOB_EMAIL = "bob_nokhook@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
