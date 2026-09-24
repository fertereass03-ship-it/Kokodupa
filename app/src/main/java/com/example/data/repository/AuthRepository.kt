package com.example.data.repository

import android.content.Context
import com.example.data.db.AnimeDatabase
import com.example.data.db.UserAccountEntity
import com.example.data.server.ServerAuthService
import com.example.data.server.ServerDatabaseService
import com.example.data.server.ServerUserProfile
import com.example.data.session.UserSession
import com.example.data.session.UserSessionManager
import com.example.util.AvatarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {
    private val database = AnimeDatabase.getDatabase(context)
    private val userAccountDao = database.userAccountDao()
    private val sessionManager = UserSessionManager.getInstance(context)
    private val serverAuthService = ServerAuthService.getInstance(context)
    private val serverDatabaseService = ServerDatabaseService.getInstance(context)

    init {
        // Guarantee clean start with zero placeholder/fake accounts, and ensure real avatars use the app icon by default
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                userAccountDao.getAllAccountsDirect().forEach { acc ->
                    if (acc.userId == "guest_default" || acc.userId == "guest" || (acc.authProvider == "LOCAL" && acc.userId.startsWith("ANI-") && acc.passwordHash == null)) {
                        userAccountDao.deleteUser(acc.userId)
                    } else if (acc.avatarUrl.isBlank() || acc.avatarUrl.contains("picsum.photos")) {
                        userAccountDao.insertOrUpdate(acc.copy(avatarUrl = AvatarManager.APP_ICON_AVATAR))
                        if (acc.isCurrentActive) {
                            sessionManager.updateProfile(avatarUrl = AvatarManager.APP_ICON_AVATAR)
                        }
                    }
                }
                val active = userAccountDao.getActiveUserDirect()
                if (active == null || active.userId == "guest_default" || active.userId == "guest" || (active.authProvider == "LOCAL" && active.passwordHash == null)) {
                    sessionManager.clearSession()
                    userAccountDao.deactivateAll()
                }
            } catch (_: Exception) {}
        }
    }

    val activeUser: Flow<UserAccountEntity?> = kotlinx.coroutines.flow.combine(
        sessionManager.sessionFlow,
        userAccountDao.getActiveUser()
    ) { session, dbUser ->
        if (session != null && session.userId.isNotBlank() && session.userId != "guest_default" && session.userId != "guest" && !(session.authProvider == "LOCAL" && session.userId.startsWith("ANI-"))) {
            UserAccountEntity(
                userId = session.userId,
                email = session.email,
                displayName = session.displayName,
                avatarUrl = session.avatarUrl,
                authProvider = session.authProvider,
                passwordHash = null,
                isCurrentActive = true,
                createdAt = System.currentTimeMillis()
            )
        } else if (dbUser != null && dbUser.userId.isNotBlank() && dbUser.userId != "guest_default" && dbUser.userId != "guest" && !(dbUser.authProvider == "LOCAL" && dbUser.userId.startsWith("ANI-") && dbUser.passwordHash == null)) {
            sessionManager.saveSession(
                UserSession(
                    userId = dbUser.userId,
                    email = dbUser.email,
                    displayName = dbUser.displayName,
                    avatarUrl = dbUser.avatarUrl ?: "",
                    authProvider = dbUser.authProvider,
                    sessionToken = UserSessionManager.generateSessionToken()
                )
            )
            dbUser
        } else {
            null
        }
    }

    val allAccounts: Flow<List<UserAccountEntity>> = userAccountDao.getAllAccounts()

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn() || sessionManager.getSession() != null

    suspend fun getActiveUserDirect(): UserAccountEntity? = withContext(Dispatchers.IO) {
        val session = sessionManager.getSession()
        if (session != null && session.userId.isNotBlank() && session.userId != "guest_default" && session.userId != "guest" && !(session.authProvider == "LOCAL" && session.userId.startsWith("ANI-"))) {
            return@withContext UserAccountEntity(
                userId = session.userId,
                email = session.email,
                displayName = session.displayName,
                avatarUrl = session.avatarUrl,
                authProvider = session.authProvider,
                passwordHash = null,
                isCurrentActive = true,
                createdAt = System.currentTimeMillis()
            )
        }
        val dbActive = userAccountDao.getActiveUserDirect()
        if (dbActive != null && dbActive.userId != "guest_default" && dbActive.userId != "guest" && !(dbActive.authProvider == "LOCAL" && dbActive.userId.startsWith("ANI-") && dbActive.passwordHash == null)) {
            val restored = UserSession(
                userId = dbActive.userId,
                email = dbActive.email,
                displayName = dbActive.displayName,
                avatarUrl = dbActive.avatarUrl ?: "",
                authProvider = dbActive.authProvider,
                sessionToken = UserSessionManager.generateSessionToken()
            )
            sessionManager.saveSession(restored)
            return@withContext dbActive
        }
        null
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        avatarUrl: String? = null
    ): Result<UserAccountEntity> = withContext(Dispatchers.IO) {
        val finalAvatar = avatarUrl?.takeIf { it.isNotBlank() } ?: AvatarManager.APP_ICON_AVATAR
        val result = serverAuthService.registerWithEmail(email, password, displayName, finalAvatar)
        result.mapCatching { profile ->
            val session = UserSession(
                userId = profile.userId,
                email = profile.email,
                displayName = profile.displayName,
                avatarUrl = profile.avatarUrl,
                authProvider = profile.authProvider,
                sessionToken = profile.sessionToken
            )
            sessionManager.saveSession(session)

            val localUser = UserAccountEntity(
                userId = profile.userId,
                email = profile.email,
                displayName = profile.displayName,
                avatarUrl = profile.avatarUrl,
                authProvider = profile.authProvider,
                passwordHash = null,
                isCurrentActive = true,
                createdAt = profile.createdAt
            )
            userAccountDao.deactivateAll()
            userAccountDao.insertOrUpdate(localUser)
            database.favoriteDao().migrateGuestFavorites(profile.userId)
            database.watchHistoryDao().migrateGuestHistory(profile.userId)

            // Sync with Cloud
            serverDatabaseService.syncUserData(profile.userId)

            localUser
        }
    }

    suspend fun loginWithEmail(
        email: String,
        password: String
    ): Result<UserAccountEntity> = withContext(Dispatchers.IO) {
        val result = serverAuthService.loginWithEmail(email, password)
        result.mapCatching { profile ->
            val session = UserSession(
                userId = profile.userId,
                email = profile.email,
                displayName = profile.displayName,
                avatarUrl = profile.avatarUrl,
                authProvider = profile.authProvider,
                sessionToken = profile.sessionToken
            )
            sessionManager.saveSession(session)

            val localUser = UserAccountEntity(
                userId = profile.userId,
                email = profile.email,
                displayName = profile.displayName,
                avatarUrl = profile.avatarUrl,
                authProvider = profile.authProvider,
                passwordHash = null,
                isCurrentActive = true,
                createdAt = profile.createdAt
            )
            userAccountDao.deactivateAll()
            userAccountDao.insertOrUpdate(localUser)
            database.favoriteDao().migrateGuestFavorites(profile.userId)
            database.watchHistoryDao().migrateGuestHistory(profile.userId)

            // Sync user's cloud data (favorites, history, player settings, friends) from server
            serverDatabaseService.syncUserData(profile.userId)

            localUser
        }
    }

    suspend fun loginWithGoogle(
        email: String,
        displayName: String,
        avatarUrl: String? = null,
        googleId: String? = null
    ): Result<UserAccountEntity> = withContext(Dispatchers.IO) {
        val finalAvatar = avatarUrl?.takeIf { it.isNotBlank() } ?: AvatarManager.APP_ICON_AVATAR
        val result = serverAuthService.loginWithGoogle(email, displayName, finalAvatar, googleId)
        result.mapCatching { profile ->
            val session = UserSession(
                userId = profile.userId,
                email = profile.email,
                displayName = profile.displayName,
                avatarUrl = profile.avatarUrl,
                authProvider = profile.authProvider,
                sessionToken = profile.sessionToken
            )
            sessionManager.saveSession(session)

            val localUser = UserAccountEntity(
                userId = profile.userId,
                email = profile.email,
                displayName = profile.displayName,
                avatarUrl = profile.avatarUrl,
                authProvider = profile.authProvider,
                passwordHash = null,
                isCurrentActive = true,
                createdAt = profile.createdAt
            )
            userAccountDao.deactivateAll()
            userAccountDao.insertOrUpdate(localUser)
            database.favoriteDao().migrateGuestFavorites(profile.userId)
            database.watchHistoryDao().migrateGuestHistory(profile.userId)

            // Download cloud data from server
            serverDatabaseService.syncUserData(profile.userId)

            localUser
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        serverAuthService.sendPasswordResetEmail(email)
    }

    suspend fun switchAccount(userId: String) = withContext(Dispatchers.IO) {
        val account = userAccountDao.getUserById(userId) ?: return@withContext
        val session = UserSession(
            userId = account.userId,
            email = account.email,
            displayName = account.displayName,
            avatarUrl = account.avatarUrl ?: "",
            authProvider = account.authProvider,
            sessionToken = UserSessionManager.generateSessionToken()
        )
        sessionManager.saveSession(session)
        userAccountDao.deactivateAll()
        userAccountDao.activateUser(userId)
        serverDatabaseService.syncUserData(userId)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        serverAuthService.logout()
        sessionManager.clearSession()
        userAccountDao.deactivateAll()
    }

    suspend fun removeSavedAccount(userId: String) = withContext(Dispatchers.IO) {
        if (sessionManager.getCurrentUserId() == userId) {
            logout()
        }
        userAccountDao.deleteUser(userId)
    }

    suspend fun updateProfile(
        displayName: String,
        avatarUrl: String
    ): Result<UserAccountEntity> = withContext(Dispatchers.IO) {
        val current = getActiveUserDirect()
            ?: return@withContext Result.failure(IllegalStateException("Пользователь не авторизован"))

        val cleanName = displayName.trim().ifBlank { current.displayName }
        val cleanAvatar = avatarUrl.ifBlank { current.avatarUrl }

        sessionManager.updateProfile(cleanName, cleanAvatar)

        val updated = current.copy(
            displayName = cleanName,
            avatarUrl = cleanAvatar
        )
        userAccountDao.insertOrUpdate(updated)

        // Sync to cloud server
        val serverProfile = ServerUserProfile(
            userId = updated.userId,
            email = updated.email,
            displayName = updated.displayName,
            avatarUrl = updated.avatarUrl,
            authProvider = updated.authProvider,
            sessionToken = sessionManager.getSession()?.sessionToken ?: ""
        )
        serverDatabaseService.saveUserProfileRemote(serverProfile)

        Result.success(updated)
    }

    suspend fun updateDisplayName(displayName: String): Result<UserAccountEntity> = withContext(Dispatchers.IO) {
        val current = getActiveUserDirect()
            ?: return@withContext Result.failure(IllegalStateException("Пользователь не авторизован"))
        val cleanName = displayName.trim()
        if (cleanName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Никнейм не может быть пустым"))
        }

        sessionManager.updateProfile(displayName = cleanName)
        val updated = current.copy(displayName = cleanName)
        userAccountDao.insertOrUpdate(updated)

        val serverProfile = ServerUserProfile(
            userId = updated.userId,
            email = updated.email,
            displayName = updated.displayName,
            avatarUrl = updated.avatarUrl,
            authProvider = updated.authProvider,
            sessionToken = sessionManager.getSession()?.sessionToken ?: ""
        )
        serverDatabaseService.saveUserProfileRemote(serverProfile)

        Result.success(updated)
    }

    suspend fun updateAvatar(avatarUrl: String): Result<UserAccountEntity> = withContext(Dispatchers.IO) {
        val current = getActiveUserDirect()
            ?: return@withContext Result.failure(IllegalStateException("Пользователь не авторизован"))

        sessionManager.updateProfile(avatarUrl = avatarUrl)
        val updated = current.copy(avatarUrl = avatarUrl)
        userAccountDao.insertOrUpdate(updated)

        val serverProfile = ServerUserProfile(
            userId = updated.userId,
            email = updated.email,
            displayName = updated.displayName,
            avatarUrl = updated.avatarUrl,
            authProvider = updated.authProvider,
            sessionToken = sessionManager.getSession()?.sessionToken ?: ""
        )
        serverDatabaseService.saveUserProfileRemote(serverProfile)

        Result.success(updated)
    }
}
