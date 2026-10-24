package com.finshield.app.data.auth

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Who is using the app. [Demo] is explicitly NOT an authenticated session. */
sealed interface Session {
    data object SignedOut : Session

    /** Local, temporary demo session. No account exists, nothing is stored or sent anywhere. */
    data class Demo(val displayName: String, val email: String? = null) : Session

    /** Reserved for the real Firebase-backed session (Milestone B). */
    data class Authenticated(val uid: String, val displayName: String?, val email: String?) : Session
}

val Session.isDemo: Boolean get() = this is Session.Demo

fun Session.greetingName(): String = when (this) {
    Session.SignedOut -> "there"
    is Session.Demo -> displayName
    is Session.Authenticated -> displayName ?: email?.substringBefore('@') ?: "there"
}

/**
 * The only auth surface the UI depends on. Replace [DemoAuthRepository] with a Firebase
 * implementation in AppContainer to go live; no screen code needs to change.
 */
interface AuthRepository {
    val session: StateFlow<Session>
    suspend fun signIn(email: String, password: String): Result<Session>
    suspend fun register(name: String, email: String, password: String): Result<Session>
    fun enterDemoMode()
    fun signOut()
}

/** Temporary frontend-phase implementation. Accepts any locally validated input and creates a Demo session. */
class DemoAuthRepository : AuthRepository {
    private val _session = MutableStateFlow<Session>(Session.SignedOut)
    override val session: StateFlow<Session> = _session.asStateFlow()

    override suspend fun signIn(email: String, password: String): Result<Session> {
        delay(350) // brief, so the loading state is visible
        val s = Session.Demo(displayName = nameFromEmail(email), email = email.trim())
        _session.value = s
        return Result.success(s)
    }

    override suspend fun register(name: String, email: String, password: String): Result<Session> {
        delay(350)
        val s = Session.Demo(displayName = name.trim().substringBefore(' '), email = email.trim())
        _session.value = s
        return Result.success(s)
    }

    override fun enterDemoMode() {
        _session.value = Session.Demo(displayName = "Guest", email = null)
    }

    override fun signOut() {
        _session.value = Session.SignedOut
    }

    private fun nameFromEmail(email: String): String =
        email.trim().substringBefore('@')
            .split('.', '_', '-')
            .firstOrNull { it.isNotBlank() }
            ?.replaceFirstChar { it.uppercase() }
            ?: "Guest"
}
