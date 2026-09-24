package com.example.data.session

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class UserSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String,
    val authProvider: String, // "EMAIL" or "GOOGLE"
    val sessionToken: String,
    val tokenExpiry: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // 30 days
)

class UserSessionManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _sessionFlow = MutableStateFlow<UserSession?>(loadSession())
    val sessionFlow: StateFlow<UserSession?> = _sessionFlow.asStateFlow()

    fun isLoggedIn(): Boolean {
        val session = _sessionFlow.value
        if (session == null) return false
        if (System.currentTimeMillis() > session.tokenExpiry) {
            clearSession()
            return false
        }
        return true
    }

    fun getCurrentUserId(): String? = _sessionFlow.value?.userId

    fun getSession(): UserSession? = _sessionFlow.value

    fun saveSession(session: UserSession) {
        prefs.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_NAME, session.displayName)
            .putString(KEY_AVATAR, session.avatarUrl)
            .putString(KEY_PROVIDER, session.authProvider)
            .putString(KEY_TOKEN, session.sessionToken)
            .putLong(KEY_EXPIRY, session.tokenExpiry)
            .apply()
        _sessionFlow.value = session
    }

    fun updateProfile(displayName: String? = null, avatarUrl: String? = null) {
        val current = _sessionFlow.value ?: return
        val updated = current.copy(
            displayName = displayName ?: current.displayName,
            avatarUrl = avatarUrl ?: current.avatarUrl
        )
        saveSession(updated)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _sessionFlow.value = null
    }

    private fun loadSession(): UserSession? {
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val expiry = prefs.getLong(KEY_EXPIRY, 0L)
        if (System.currentTimeMillis() > expiry) {
            prefs.edit().clear().apply()
            return null
        }
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val name = prefs.getString(KEY_NAME, "") ?: ""
        val avatar = prefs.getString(KEY_AVATAR, "") ?: ""
        val provider = prefs.getString(KEY_PROVIDER, "EMAIL") ?: "EMAIL"

        return UserSession(
            userId = userId,
            email = email,
            displayName = name,
            avatarUrl = avatar,
            authProvider = provider,
            sessionToken = token,
            tokenExpiry = expiry
        )
    }

    companion object {
        private const val PREFS_NAME = "aniwerti_secure_session"
        private const val KEY_USER_ID = "session_user_id"
        private const val KEY_EMAIL = "session_email"
        private const val KEY_NAME = "session_name"
        private const val KEY_AVATAR = "session_avatar"
        private const val KEY_PROVIDER = "session_provider"
        private const val KEY_TOKEN = "session_token"
        private const val KEY_EXPIRY = "session_expiry"

        @Volatile
        private var INSTANCE: UserSessionManager? = null

        fun getInstance(context: Context): UserSessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = UserSessionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        fun generateSessionToken(): String {
            return "SES-" + UUID.randomUUID().toString().replace("-", "")
        }

        fun generateUniqueUserId(): String {
            return "ANI-" + (100000 + (Math.random() * 900000).toInt())
        }
    }
}
