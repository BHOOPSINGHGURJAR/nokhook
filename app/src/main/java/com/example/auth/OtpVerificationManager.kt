package com.example.auth

import java.security.SecureRandom

enum class VerificationChannel(val label: String) {
    PHONE("Mobile Number"),
    EMAIL("Email")
}

data class OtpSessionState(
    val sessionId: Int = 0,
    val channel: VerificationChannel = VerificationChannel.PHONE,
    val destinationLabel: String = "",
    val expectedCode: String = "",
    val enteredCode: String = "",
    val remainingSeconds: Int = 0,
    val isSent: Boolean = false,
    val isVerified: Boolean = false,
    val isExpired: Boolean = false,
    val attemptsRemaining: Int = MAX_ATTEMPTS,
    val errorMessage: String? = null
) {
    val isLockedOut: Boolean
        get() = isSent && !isVerified && attemptsRemaining <= 0

    val canResend: Boolean
        get() = isSent && !isVerified && remainingSeconds == 0

    companion object {
        const val OTP_DURATION_SECONDS = 60
        const val OTP_LENGTH = 6
        const val MAX_ATTEMPTS = 5
    }
}

sealed interface OtpVerifyResult {
    data object Verified : OtpVerifyResult
    data class InvalidCode(val attemptsRemaining: Int) : OtpVerifyResult
    data object Expired : OtpVerifyResult
    data object LockedOut : OtpVerifyResult
    data object Incomplete : OtpVerifyResult
}

object OtpVerificationEngine {
    private val secureRandom = SecureRandom()

    fun generateCode(): String {
        val value = secureRandom.nextInt(900_000) + 100_000
        return value.toString()
    }

    fun startSession(
        current: OtpSessionState,
        countryCode: String,
        mobileNumber: String,
        codeOverride: String? = null
    ): OtpSessionState {
        val normalizedPhone = "${countryCode.trim()} ${mobileNumber.trim()}".trim()
        return startSessionForChannel(
            current = current,
            channel = VerificationChannel.PHONE,
            destination = normalizedPhone,
            codeOverride = codeOverride
        )
    }

    fun startSessionForChannel(
        current: OtpSessionState,
        channel: VerificationChannel,
        destination: String,
        codeOverride: String? = null
    ): OtpSessionState {
        val code = codeOverride ?: generateCode()
        return OtpSessionState(
            sessionId = current.sessionId + 1,
            channel = channel,
            destinationLabel = destination.trim(),
            expectedCode = code,
            enteredCode = "",
            remainingSeconds = OtpSessionState.OTP_DURATION_SECONDS,
            isSent = true,
            isVerified = false,
            isExpired = false,
            attemptsRemaining = OtpSessionState.MAX_ATTEMPTS,
            errorMessage = null
        )
    }

    fun onTick(current: OtpSessionState): OtpSessionState {
        if (!current.isSent || current.isVerified) return current
        val nextSeconds = (current.remainingSeconds - 1).coerceAtLeast(0)
        val expired = nextSeconds == 0
        return current.copy(
            remainingSeconds = nextSeconds,
            isExpired = expired,
            errorMessage = if (expired) {
                "OTP expired after ${OtpSessionState.OTP_DURATION_SECONDS}s. Tap Resend OTP."
            } else {
                current.errorMessage
            }
        )
    }

    fun updateEnteredCode(
        current: OtpSessionState,
        rawInput: String
    ): Pair<OtpSessionState, OtpVerifyResult?> {
        if (!current.isSent || current.isVerified) {
            return current to null
        }
        val digits = rawInput.filter(Char::isDigit).take(OtpSessionState.OTP_LENGTH)
        val updated = current.copy(
            enteredCode = digits,
            errorMessage = if (digits.length < OtpSessionState.OTP_LENGTH && !current.isExpired && !current.isLockedOut) {
                null
            } else {
                current.errorMessage
            }
        )
        return if (digits.length == OtpSessionState.OTP_LENGTH) {
            verify(updated)
        } else {
            updated to null
        }
    }

    fun verify(current: OtpSessionState): Pair<OtpSessionState, OtpVerifyResult> {
        if (!current.isSent) {
            val err = current.copy(errorMessage = "Request an OTP first")
            return err to OtpVerifyResult.Incomplete
        }
        if (current.isVerified) {
            return current to OtpVerifyResult.Verified
        }
        if (current.isLockedOut) {
            val err = current.copy(errorMessage = "Too many incorrect attempts. Tap Resend OTP for a new code.")
            return err to OtpVerifyResult.LockedOut
        }
        if (current.isExpired || current.remainingSeconds <= 0) {
            val err = current.copy(
                remainingSeconds = 0,
                isExpired = true,
                errorMessage = "OTP expired after ${OtpSessionState.OTP_DURATION_SECONDS}s. Tap Resend OTP."
            )
            return err to OtpVerifyResult.Expired
        }
        if (current.enteredCode.length < OtpSessionState.OTP_LENGTH) {
            val err = current.copy(
                errorMessage = "Enter the 6-digit OTP sent to ${current.destinationLabel}"
            )
            return err to OtpVerifyResult.Incomplete
        }
        return if (current.enteredCode == current.expectedCode) {
            val verified = current.copy(
                isVerified = true,
                isExpired = false,
                errorMessage = null
            )
            verified to OtpVerifyResult.Verified
        } else {
            val remainingAttempts = (current.attemptsRemaining - 1).coerceAtLeast(0)
            val msg = if (remainingAttempts == 0) {
                "Too many incorrect attempts. Tap Resend OTP for a new code."
            } else {
                "Incorrect OTP code ($remainingAttempts attempts left)"
            }
            val failed = current.copy(
                attemptsRemaining = remainingAttempts,
                errorMessage = msg
            )
            val result = if (remainingAttempts == 0) {
                OtpVerifyResult.LockedOut
            } else {
                OtpVerifyResult.InvalidCode(remainingAttempts)
            }
            failed to result
        }
    }

    fun invalidateForPhoneChange(current: OtpSessionState): OtpSessionState {
        return invalidateForDestinationOrChannelChange(current, current.channel)
    }

    fun invalidateForDestinationOrChannelChange(
        current: OtpSessionState,
        nextChannel: VerificationChannel
    ): OtpSessionState {
        return OtpSessionState(
            sessionId = current.sessionId,
            channel = nextChannel
        )
    }
}
