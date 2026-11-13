package com.example

import com.example.auth.OtpSessionState
import com.example.auth.OtpVerificationEngine
import com.example.auth.OtpVerifyResult
import com.example.auth.VerificationChannel
import com.example.model.RatingMeterStats
import com.example.model.RatingTier
import com.example.model.VibeSlice
import com.example.ui.components.computeRatingWeightedEmotionalVibes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpVerificationEngineTest {

    @Test
    fun startSession_setsSixtySecondCountdownAndSixDigitCode() {
        val session = OtpVerificationEngine.startSession(
            current = OtpSessionState(),
            countryCode = "+91",
            mobileNumber = "7023337229"
        )

        assertTrue(session.isSent)
        assertFalse(session.isVerified)
        assertFalse(session.isExpired)
        assertEquals(60, session.remainingSeconds)
        assertEquals(VerificationChannel.PHONE, session.channel)
        assertEquals("+91 7023337229", session.destinationLabel)
        assertEquals(6, session.expectedCode.length)
        assertTrue(session.expectedCode.all(Char::isDigit))
        assertFalse(session.canResend)
    }

    @Test
    fun emailChannelOtp_andMutualExclusivityWhenSwitchingChannels() {
        val emailSession = OtpVerificationEngine.startSessionForChannel(
            current = OtpSessionState(),
            channel = VerificationChannel.EMAIL,
            destination = "reader@nokhook.app",
            codeOverride = "554433"
        )
        assertEquals(VerificationChannel.EMAIL, emailSession.channel)
        assertEquals("reader@nokhook.app", emailSession.destinationLabel)
        assertEquals(60, emailSession.remainingSeconds)

        val (verifiedEmail, res) = OtpVerificationEngine.updateEnteredCode(emailSession, "554433")
        assertTrue(res is OtpVerifyResult.Verified)
        assertTrue(verifiedEmail.isVerified)

        val switchedToPhone = OtpVerificationEngine.invalidateForDestinationOrChannelChange(
            verifiedEmail,
            VerificationChannel.PHONE
        )
        assertEquals(VerificationChannel.PHONE, switchedToPhone.channel)
        assertFalse(switchedToPhone.isSent)
        assertFalse(switchedToPhone.isVerified)
        assertEquals("", switchedToPhone.destinationLabel)
        assertEquals(0, switchedToPhone.remainingSeconds)
    }

    @Test
    fun updateEnteredCode_verifiesMatchingSixDigitsWithinSixtySeconds() {
        var session = OtpVerificationEngine.startSession(
            current = OtpSessionState(),
            countryCode = "+91",
            mobileNumber = "7023337229",
            codeOverride = "482910"
        )

        session = OtpVerificationEngine.onTick(session)
        assertEquals(59, session.remainingSeconds)

        val (verifiedSession, result) = OtpVerificationEngine.updateEnteredCode(session, "482910")
        assertTrue(result is OtpVerifyResult.Verified)
        assertTrue(verifiedSession.isVerified)
        assertNull(verifiedSession.errorMessage)
    }

    @Test
    fun wrongOtp_rejectsAndLocksAfterFiveAttempts() {
        var session = OtpVerificationEngine.startSession(
            current = OtpSessionState(),
            countryCode = "+1",
            mobileNumber = "4155551234",
            codeOverride = "654321"
        )

        for (attempt in 1..4) {
            val (next, result) = OtpVerificationEngine.updateEnteredCode(session, "111111")
            session = next
            assertTrue(result is OtpVerifyResult.InvalidCode)
            assertEquals(5 - attempt, session.attemptsRemaining)
            assertFalse(session.isVerified)
            assertNotNull(session.errorMessage)
        }

        val (locked, lockResult) = OtpVerificationEngine.updateEnteredCode(session, "111111")
        assertTrue(lockResult is OtpVerifyResult.LockedOut)
        assertTrue(locked.isLockedOut)
        assertEquals(0, locked.attemptsRemaining)

        val (_, blockedResult) = OtpVerificationEngine.updateEnteredCode(locked, "654321")
        assertTrue(blockedResult is OtpVerifyResult.LockedOut)
    }

    @Test
    fun tickingSixtySeconds_expiresOtpAndEnablesResend() {
        var session = OtpVerificationEngine.startSession(
            current = OtpSessionState(),
            countryCode = "+91",
            mobileNumber = "9876543210",
            codeOverride = "908172"
        )

        repeat(60) {
            session = OtpVerificationEngine.onTick(session)
        }

        assertEquals(0, session.remainingSeconds)
        assertTrue(session.isExpired)
        assertTrue(session.canResend)

        val (_, verifyExpired) = OtpVerificationEngine.updateEnteredCode(session, "908172")
        assertTrue(verifyExpired is OtpVerifyResult.Expired)

        val resent = OtpVerificationEngine.startSession(
            current = session,
            countryCode = "+91",
            mobileNumber = "9876543210",
            codeOverride = "135790"
        )
        assertEquals(60, resent.remainingSeconds)
        assertFalse(resent.isExpired)
        assertEquals("", resent.enteredCode)
        assertEquals(5, resent.attemptsRemaining)
    }

    @Test
    fun changingPhoneNumber_invalidatesActiveOtpSession() {
        val session = OtpVerificationEngine.startSession(
            current = OtpSessionState(),
            countryCode = "+91",
            mobileNumber = "7023337229",
            codeOverride = "112233"
        )
        val (verified, _) = OtpVerificationEngine.updateEnteredCode(session, "112233")
        assertTrue(verified.isVerified)

        val reset = OtpVerificationEngine.invalidateForPhoneChange(verified)
        assertFalse(reset.isSent)
        assertFalse(reset.isVerified)
        assertEquals(0, reset.remainingSeconds)
    }

    @Test
    fun ratingWeightedEmotionalVibes_sumsTo100AndContainsCanonicalTags() {
        val baseVibes = listOf(
            VibeSlice("Suspense", 45, 0xFF4F46E5),
            VibeSlice("Psychology", 35, 0xFFE05D44),
            VibeSlice("Mystery", 20, 0xFF0D9488)
        )
        val stats = RatingMeterStats(mustReadVotes = 420, worthReadingVotes = 210, lightReadVotes = 45, skipVotes = 18)

        val weighted = computeRatingWeightedEmotionalVibes(
            baseVibes = baseVibes,
            ratingStats = stats,
            userSelectedTier = RatingTier.MUST_READ
        )

        assertEquals(4, weighted.size)
        assertEquals(100, weighted.sumOf { it.percentage })
        val names = weighted.map { it.name }.toSet()
        assertTrue(names.containsAll(setOf("Inspirational", "Dark", "Fast-paced", "Educational")))
    }
}
