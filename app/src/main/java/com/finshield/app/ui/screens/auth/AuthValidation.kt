package com.finshield.app.ui.screens.auth

import android.util.Patterns

/** Pure form-validation helpers (no Android UI, no Firebase). Return an error message or null when valid. */
object AuthValidation {
    const val MIN_PASSWORD_LENGTH = 8

    private val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    fun email(value: String): String? {
        val v = value.trim()
        val isValid = try {
            Patterns.EMAIL_ADDRESS?.matcher(v)?.matches() ?: EMAIL_REGEX.matches(v)
        } catch (e: Throwable) {
            EMAIL_REGEX.matches(v)
        }
        return when {
            v.isEmpty() -> "Enter your email address"
            !isValid -> "Enter a valid email address"
            else -> null
        }
    }

    fun loginPassword(value: String): String? =
        if (value.isEmpty()) "Enter your password" else null

    fun newPassword(value: String): String? = when {
        value.isEmpty() -> "Create a password"
        value.length < MIN_PASSWORD_LENGTH -> "Use at least $MIN_PASSWORD_LENGTH characters"
        value.none { it.isLetter() } || value.none { it.isDigit() } -> "Include both letters and numbers"
        else -> null
    }

    fun confirmPassword(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Re-enter your password"
        password != confirm -> "Passwords do not match"
        else -> null
    }

    fun name(value: String): String? =
        if (value.trim().length < 2) "Enter your full name" else null
}
