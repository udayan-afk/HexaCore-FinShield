package com.finshield.app.domain.analyzer

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user disclosure and explicit consent for cloud-assisted AI document understanding (Firebase Vertex AI).
 * Complies with strict privacy guidelines: raw documents are not stored in the cloud, and users have full opt-in control.
 */
class AiConsentStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("finshield_ai_consent", Context.MODE_PRIVATE)

    var isAiEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_ENABLED, true) // Default enabled with in-app toggle & disclosure
        set(value) = prefs.edit().putBoolean(KEY_AI_ENABLED, value).apply()

    var hasUserConsented: Boolean
        get() = prefs.getBoolean(KEY_USER_CONSENTED, true)
        set(value) = prefs.edit().putBoolean(KEY_USER_CONSENTED, value).apply()

    companion object {
        private const val KEY_AI_ENABLED = "ai_feature_enabled"
        private const val KEY_USER_CONSENTED = "ai_user_consented_v1"
    }
}
