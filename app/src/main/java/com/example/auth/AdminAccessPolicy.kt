package com.example.auth

import com.google.firebase.auth.FirebaseUser
import java.util.Locale

/**
 * Strict Admin RBAC Allowlist Policy.
 * Only these exact two authorized editor accounts may access Admin Panel controls
 * inside Settings or mutate live catalog, banners, and moderation state.
 */
object AdminAccessPolicy {
    val ADMIN_EMAILS: Set<String> = setOf(
        "bhoopsinghgurjar001@gmail.com",
        "bs9396684@gmail.com"
    )

    fun normalizeEmail(email: String?): String =
        email?.trim()?.lowercase(Locale.ROOT).orEmpty()

    fun isAdminEmail(email: String?): Boolean {
        val normalized = normalizeEmail(email)
        return normalized.isNotEmpty() && normalized in ADMIN_EMAILS
    }

    fun isAdminUser(user: FirebaseUser?): Boolean {
        if (user == null) return false
        return isAdminEmail(user.email)
    }
}
