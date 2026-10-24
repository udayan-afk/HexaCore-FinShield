package com.finshield.app.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication session state.
 * Firebase Authentication is the sole source of authentication in the application.
 */
sealed interface Session {
    data object SignedOut : Session

    data class Authenticated(
        val uid: String,
        val displayName: String?,
        val email: String?,
    ) : Session
}

val Session.isAuthenticated: Boolean get() = this is Session.Authenticated

fun Session.greetingName(): String = when (this) {
    Session.SignedOut -> "there"
    is Session.Authenticated -> displayName ?: email?.substringBefore('@') ?: "there"
}

/**
 * Centralized authentication contract implemented by [FirebaseAuthRepository].
 */
interface AuthRepository {
    val session: StateFlow<Session>
    suspend fun signIn(email: String, password: String): Result<Session>
    suspend fun register(name: String, email: String, password: String): Result<Session>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun signOut()
}
