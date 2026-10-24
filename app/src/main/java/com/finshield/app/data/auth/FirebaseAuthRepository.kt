package com.finshield.app.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Production Firebase Authentication repository implementing [AuthRepository].
 * Manages user sign-in, registration, password resets, persistent auth listener,
 * and profile synchronisation with Cloud Firestore.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userProfileRepo: UserProfileRepository = UserProfileRepository(),
) : AuthRepository {

    private val _session = MutableStateFlow<Session>(Session.SignedOut)
    override val session: StateFlow<Session> = _session.asStateFlow()

    init {
        // 1. Initial synchronous state check to avoid splash flicker on cold launch
        val initialUser = auth.currentUser
        if (initialUser != null) {
            _session.value = Session.Authenticated(
                uid = initialUser.uid,
                displayName = initialUser.displayName ?: initialUser.email?.substringBefore('@'),
                email = initialUser.email,
            )
        }

        // 2. Attach persistent Firebase AuthStateListener
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                val current = _session.value
                val resolvedName = if (current is Session.Authenticated && current.uid == user.uid && !current.displayName.isNullOrBlank()) {
                    current.displayName
                } else {
                    user.displayName ?: user.email?.substringBefore('@')
                }
                _session.value = Session.Authenticated(
                    uid = user.uid,
                    displayName = resolvedName,
                    email = user.email,
                )
            } else {
                _session.value = Session.SignedOut
            }
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Session> {
        val cleanEmail = email.trim()
        return try {
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase returned an empty user session.")

            // Fetch profile from Firestore to obtain full name
            var resolvedName: String? = user.displayName
            try {
                val profileResult = userProfileRepo.getProfile(user.uid)
                val profile = profileResult.getOrNull()
                if (profile != null && profile.fullName.isNotBlank()) {
                    resolvedName = profile.fullName
                } else {
                    // Safe recovery: ensure profile document exists in Firestore
                    userProfileRepo.createOrUpdateProfile(
                        uid = user.uid,
                        fullName = resolvedName ?: cleanEmail.substringBefore('@'),
                        email = user.email ?: cleanEmail,
                    )
                }
            } catch (e: Exception) {
                // Non-fatal if offline; auth still succeeds
            }

            val session = Session.Authenticated(
                uid = user.uid,
                displayName = resolvedName ?: cleanEmail.substringBefore('@'),
                email = user.email ?: cleanEmail,
            )
            _session.value = session
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseError(e), e))
        }
    }

    override suspend fun register(name: String, email: String, password: String): Result<Session> {
        val cleanName = name.trim()
        val cleanEmail = email.trim()

        return try {
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user ?: throw IllegalStateException("Account creation returned an empty user.")

            // 1. Update Firebase Auth displayName
            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                user.updateProfile(profileUpdates).await()
            } catch (e: Exception) {
                // Non-fatal if display name update fails
            }

            // 2. Create users/{uid} in Cloud Firestore
            try {
                userProfileRepo.createOrUpdateProfile(
                    uid = user.uid,
                    fullName = cleanName,
                    email = user.email ?: cleanEmail,
                ).getOrThrow()
            } catch (firestoreError: Exception) {
                // Safe handling of partially completed registration:
                // User account was created in Auth; allow session to continue without crashing
            }

            val session = Session.Authenticated(
                uid = user.uid,
                displayName = cleanName,
                email = user.email ?: cleanEmail,
            )
            _session.value = session
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseError(e), e))
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        val cleanEmail = email.trim()
        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseError(e), e))
        }
    }

    override fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {}
        _session.value = Session.SignedOut
    }

    private fun mapFirebaseError(e: Throwable): String {
        return when (e) {
            is FirebaseAuthUserCollisionException ->
                "An account already exists with this email address. Please log in instead."
            is FirebaseAuthWeakPasswordException ->
                "Password is too weak. Please use at least 8 characters including letters and numbers."
            is FirebaseAuthInvalidCredentialsException ->
                "Incorrect email or password. Please verify your credentials and try again."
            is FirebaseAuthInvalidUserException -> {
                if (e.errorCode == "ERROR_USER_DISABLED") {
                    "This account has been disabled. Please contact support."
                } else {
                    "No account found with this email. Please check your email or register."
                }
            }
            is FirebaseTooManyRequestsException ->
                "Too many unsuccessful attempts. Please wait a few minutes before trying again."
            is FirebaseNetworkException ->
                "Network connection error. Please check your internet connection."
            else ->
                e.localizedMessage ?: "Authentication operation failed. Please try again."
        }
    }
}
