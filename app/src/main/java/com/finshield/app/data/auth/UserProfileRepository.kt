package com.finshield.app.data.auth

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Data model for user profile stored in Cloud Firestore under `users/{uid}`.
 * Strictly adheres to allowed schema without storing passwords or credentials.
 */
data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

/**
 * Repository responsible for reading and writing user profile documents in Cloud Firestore.
 */
class UserProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    /**
     * Retrieves the profile document for [uid] from Cloud Firestore.
     * Returns null if the document does not exist yet.
     */
    suspend fun getProfile(uid: String): Result<UserProfile?> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
            if (!snapshot.exists()) {
                Result.success(null)
            } else {
                val profile = UserProfile(
                    uid = snapshot.getString("uid") ?: uid,
                    fullName = snapshot.getString("fullName") ?: "",
                    email = snapshot.getString("email") ?: "",
                    createdAt = snapshot.getTimestamp("createdAt"),
                    updatedAt = snapshot.getTimestamp("updatedAt"),
                )
                Result.success(profile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates or updates the user profile document in `users/{uid}`.
     * Preserves existing `createdAt` on updates and sets `updatedAt` to server timestamp.
     */
    suspend fun createOrUpdateProfile(
        uid: String,
        fullName: String,
        email: String,
    ): Result<UserProfile> {
        return try {
            val docRef = firestore.collection(USERS_COLLECTION).document(uid)
            val snapshot = docRef.get().await()
            val now = FieldValue.serverTimestamp()

            val data = if (snapshot.exists()) {
                mapOf(
                    "uid" to uid,
                    "fullName" to fullName.trim(),
                    "email" to email.trim(),
                    "updatedAt" to now,
                )
            } else {
                mapOf(
                    "uid" to uid,
                    "fullName" to fullName.trim(),
                    "email" to email.trim(),
                    "createdAt" to now,
                    "updatedAt" to now,
                )
            }

            docRef.set(data, SetOptions.merge()).await()
            Result.success(
                UserProfile(
                    uid = uid,
                    fullName = fullName.trim(),
                    email = email.trim(),
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val USERS_COLLECTION = "users"
    }
}
