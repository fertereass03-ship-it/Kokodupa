package com.example.data.server

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.session.UserSession
import com.example.data.session.UserSessionManager
import com.example.util.AvatarManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

data class ServerUserProfile(
    val userId: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String,
    val authProvider: String, // "EMAIL" or "GOOGLE"
    val sessionToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

class ServerAuthService(private val context: Context) {
    private val TAG = "ServerAuthService"

    // Safe lazy Firebase Auth instance: checks if Firebase is initialized first
    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Auth not available in this build configuration: ${e.message}")
            null
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        avatarUrl: String? = null
    ): Result<ServerUserProfile> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanName = displayName.trim()

        if (!isNetworkAvailable()) {
            return@withContext Result.failure(IllegalStateException("Отсутствует подключение к интернету. Проверьте сеть."))
        }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Введите корректный email адрес"))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Пароль должен быть не менее 6 символов"))
        }
        if (cleanName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Введите имя пользователя"))
        }

        val auth = firebaseAuth
        if (auth != null) {
            try {
                val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                val firebaseUser = authResult.user
                    ?: return@withContext Result.failure(IllegalStateException("Ошибка создания аккаунта на сервере"))

                val uniqueUserId = "ANI-" + firebaseUser.uid.take(8).uppercase()
                val finalAvatar = avatarUrl?.takeIf { it.isNotBlank() }
                    ?: AvatarManager.APP_ICON_AVATAR

                val profile = ServerUserProfile(
                    userId = uniqueUserId,
                    email = cleanEmail,
                    displayName = cleanName,
                    avatarUrl = finalAvatar,
                    authProvider = "EMAIL",
                    sessionToken = UserSessionManager.generateSessionToken(),
                    createdAt = System.currentTimeMillis()
                )

                // Save to Firestore via ServerDatabaseService
                ServerDatabaseService.getInstance(context).saveUserProfileRemote(profile)
                return@withContext Result.success(profile)
            } catch (e: FirebaseAuthUserCollisionException) {
                return@withContext Result.failure(IllegalStateException("Пользователь с таким email уже зарегистрирован"))
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                return@withContext Result.failure(IllegalArgumentException("Некорректный email или пароль"))
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Auth register exception, trying server direct: ${e.message}")
            }
        }

        // Direct Server-side Authentication & Registry
        val serverDb = ServerDatabaseService.getInstance(context)
        val existing = serverDb.getUserProfileByEmail(cleanEmail)
        if (existing != null) {
            return@withContext Result.failure(IllegalStateException("Пользователь с таким email уже зарегистрирован"))
        }

        val uniqueUserId = UserSessionManager.generateUniqueUserId()
        val finalAvatar = avatarUrl?.takeIf { it.isNotBlank() }
            ?: AvatarManager.APP_ICON_AVATAR

        val profile = ServerUserProfile(
            userId = uniqueUserId,
            email = cleanEmail,
            displayName = cleanName,
            avatarUrl = finalAvatar,
            authProvider = "EMAIL",
            sessionToken = UserSessionManager.generateSessionToken(),
            createdAt = System.currentTimeMillis()
        )

        serverDb.saveUserProfileRemote(profile, passwordHash = hashPassword(password))
        Result.success(profile)
    }

    suspend fun loginWithEmail(
        email: String,
        password: String
    ): Result<ServerUserProfile> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()

        if (!isNetworkAvailable()) {
            return@withContext Result.failure(IllegalStateException("Отсутствует подключение к интернету"))
        }
        if (cleanEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Введите email"))
        }
        if (password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Введите пароль"))
        }

        val auth = firebaseAuth
        if (auth != null) {
            try {
                val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).await()
                val firebaseUser = authResult.user
                    ?: return@withContext Result.failure(IllegalStateException("Ошибка входа"))

                val remoteProfile = ServerDatabaseService.getInstance(context).getUserProfileRemote(
                    "ANI-" + firebaseUser.uid.take(8).uppercase()
                )

                val uniqueUserId = remoteProfile?.userId ?: ("ANI-" + firebaseUser.uid.take(8).uppercase())
                val displayName = remoteProfile?.displayName ?: cleanEmail.substringBefore("@")
                val avatarUrl = remoteProfile?.avatarUrl
                    ?: "https://picsum.photos/seed/${Math.abs(cleanEmail.hashCode())}/200/200"

                val profile = ServerUserProfile(
                    userId = uniqueUserId,
                    email = cleanEmail,
                    displayName = displayName,
                    avatarUrl = avatarUrl,
                    authProvider = "EMAIL",
                    sessionToken = UserSessionManager.generateSessionToken(),
                    createdAt = remoteProfile?.createdAt ?: System.currentTimeMillis()
                )
                return@withContext Result.success(profile)
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                return@withContext Result.failure(IllegalArgumentException("Неверный пароль или email"))
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Auth sign in failed, checking server database: ${e.message}")
            }
        }

        // Direct Server Database verification
        val serverDb = ServerDatabaseService.getInstance(context)
        val user = serverDb.getUserProfileByEmail(cleanEmail)
            ?: return@withContext Result.failure(NoSuchElementException("Пользователь с таким email не найден"))

        if (user.authProvider == "GOOGLE") {
            return@withContext Result.failure(IllegalStateException("Этот аккаунт зарегистрирован через Google. Войдите через Google."))
        }

        val storedHash = serverDb.getUserPasswordHash(user.userId)
        val inputHash = hashPassword(password)
        if (storedHash != null && storedHash != inputHash) {
            return@withContext Result.failure(IllegalArgumentException("Неверный пароль"))
        }

        val profile = user.copy(sessionToken = UserSessionManager.generateSessionToken())
        serverDb.updateSessionToken(profile.userId, profile.sessionToken)
        Result.success(profile)
    }

    suspend fun loginWithGoogle(
        email: String,
        displayName: String,
        avatarUrl: String? = null,
        googleId: String? = null
    ): Result<ServerUserProfile> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanName = displayName.trim().ifBlank { cleanEmail.substringBefore("@") }

        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Некорректный Google аккаунт"))
        }

        val serverDb = ServerDatabaseService.getInstance(context)
        val existing = serverDb.getUserProfileByEmail(cleanEmail)

        val profile = if (existing != null) {
            val updated = existing.copy(
                displayName = cleanName,
                avatarUrl = avatarUrl ?: existing.avatarUrl,
                sessionToken = UserSessionManager.generateSessionToken()
            )
            serverDb.saveUserProfileRemote(updated)
            updated
        } else {
            val uniqueUserId = googleId?.takeIf { it.isNotBlank() }?.let { "ANI-" + it.takeLast(6) }
                ?: UserSessionManager.generateUniqueUserId()
            val finalAvatar = avatarUrl?.takeIf { it.isNotBlank() }
                ?: AvatarManager.APP_ICON_AVATAR

            val newProfile = ServerUserProfile(
                userId = uniqueUserId,
                email = cleanEmail,
                displayName = cleanName,
                avatarUrl = finalAvatar,
                authProvider = "GOOGLE",
                sessionToken = UserSessionManager.generateSessionToken(),
                createdAt = System.currentTimeMillis()
            )
            serverDb.saveUserProfileRemote(newProfile)
            newProfile
        }

        Result.success(profile)
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Введите корректный email"))
        }

        val auth = firebaseAuth
        if (auth != null) {
            try {
                auth.sendPasswordResetEmail(cleanEmail).await()
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                Log.w(TAG, "Firebase password reset: ${e.message}")
            }
        }

        val serverDb = ServerDatabaseService.getInstance(context)
        val user = serverDb.getUserProfileByEmail(cleanEmail)
            ?: return@withContext Result.failure(NoSuchElementException("Пользователь с таким email не найден"))

        Result.success(Unit)
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firebaseAuth?.signOut()
        } catch (_: Throwable) {}
        Result.success(Unit)
    }

    companion object {
        @Volatile
        private var INSTANCE: ServerAuthService? = null

        fun getInstance(context: Context): ServerAuthService {
            return INSTANCE ?: synchronized(this) {
                val instance = ServerAuthService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
