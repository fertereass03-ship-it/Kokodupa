package com.example.data.repository

import android.content.Context
import com.example.data.db.AnimeDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.FriendEntity
import com.example.data.server.ServerDatabaseService
import com.example.data.server.ServerUserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SocialRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = AnimeDatabase.getDatabase(context)
    private val socialDao = database.socialDao()
    private val userAccountDao = database.userAccountDao()
    private val authRepository = AuthRepository(context)
    private val serverDatabaseService = ServerDatabaseService.getInstance(context)

    companion object {
        @Volatile
        var activeChatFriendId: String? = null

        private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private var backgroundListenerStarted = false
    }

    private var lastFullSyncTime = 0L

    init {
        synchronized(SocialRepository::class.java) {
            if (!backgroundListenerStarted) {
                backgroundListenerStarted = true
                repoScope.launch {
                    authRepository.activeUser.collectLatest { user ->
                        if (user != null && user.userId.isNotBlank()) {
                            val myId = user.userId

                            // 1. Continuous real-time streaming connection for instant delivery (<50ms)
                            launch {
                                while (isActive) {
                                    try {
                                        streamInboxEvents(myId)
                                    } catch (_: Throwable) {
                                        delay(2000)
                                    }
                                }
                            }

                            // 2. Fast background polling fallback every 2.5 seconds
                            launch {
                                while (isActive) {
                                    try {
                                        syncFriends()
                                    } catch (_: Throwable) {}
                                    delay(2500)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun setActiveChatFriend(friendId: String?) {
        activeChatFriendId = friendId
    }

    fun clearActiveChatFriend(friendId: String) {
        if (activeChatFriendId == friendId) {
            activeChatFriendId = null
        }
    }

    val defaultUserId: String = "guest"
    val defaultUserName: String = "Гость"

    fun getActiveUserFlow(): Flow<com.example.data.db.UserAccountEntity?> = authRepository.activeUser

    suspend fun getActiveUserDirect(): com.example.data.db.UserAccountEntity? = authRepository.getActiveUserDirect()

    suspend fun getCurrentUserId(): String = authRepository.getActiveUserDirect()?.userId ?: ""

    suspend fun getCurrentUserName(): String = authRepository.getActiveUserDirect()?.displayName ?: "Пользователь"

    suspend fun getCurrentUserAvatar(): String = authRepository.getActiveUserDirect()?.avatarUrl ?: ""

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getFriends(): Flow<List<FriendEntity>> = authRepository.activeUser.flatMapLatest { user ->
        if (user != null) socialDao.getFriends(user.userId)
        else flowOf(emptyList())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getPendingRequests(): Flow<List<FriendEntity>> = authRepository.activeUser.flatMapLatest { user ->
        if (user != null) socialDao.getPendingRequests(user.userId)
        else flowOf(emptyList())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getIncomingRequests(): Flow<List<FriendEntity>> = authRepository.activeUser.flatMapLatest { user ->
        if (user != null) socialDao.getIncomingRequests(user.userId)
        else flowOf(emptyList())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getOutgoingRequests(): Flow<List<FriendEntity>> = authRepository.activeUser.flatMapLatest { user ->
        if (user != null) socialDao.getOutgoingRequests(user.userId)
        else flowOf(emptyList())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getMessagesForFriend(friendId: String): Flow<List<ChatMessageEntity>> =
        authRepository.activeUser.flatMapLatest { user ->
            if (user != null) socialDao.getMessagesForFriend(user.userId, friendId)
            else flowOf(emptyList())
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getFriendFlow(friendId: String): Flow<FriendEntity?> =
        authRepository.activeUser.flatMapLatest { user ->
            if (user != null) socialDao.getFriendFlow(user.userId, friendId)
            else flowOf(null)
        }

    suspend fun findUserById(queryId: String): FriendEntity? = withContext(Dispatchers.IO) {
        val cleanInput = queryId.trim()
        if (cleanInput.isBlank()) return@withContext null
        val myUser = authRepository.getActiveUserDirect() ?: return@withContext null
        val myId = myUser.userId
        if (cleanInput.equals(myId, ignoreCase = true) || cleanInput.equals(myUser.email, ignoreCase = true)) {
            return@withContext null
        }

        // 1. Check local friends and pending requests already added
        val local = socialDao.getFriendById(myId, cleanInput)
        if (local != null) return@withContext local
        
        // Also check if matches without ANI- or uppercase
        val localAlt = socialDao.getFriendById(myId, if (cleanInput.startsWith("ANI-", ignoreCase = true)) cleanInput.removePrefix("ANI-") else "ANI-$cleanInput")
        if (localAlt != null) return@withContext localAlt

        // 2. Check local accounts in database
        val allLocalAccounts = userAccountDao.getAllAccountsDirect()
        val matchedAccount = allLocalAccounts.firstOrNull {
            it.userId != myId && (
                it.userId.equals(cleanInput, ignoreCase = true) ||
                it.userId.endsWith(cleanInput, ignoreCase = true) ||
                it.email.equals(cleanInput, ignoreCase = true) ||
                it.displayName.equals(cleanInput, ignoreCase = true) ||
                it.displayName.contains(cleanInput, ignoreCase = true)
            )
        }
        if (matchedAccount != null) {
            val existingInDao = socialDao.getFriendById(myId, matchedAccount.userId)
            if (existingInDao != null) return@withContext existingInDao
            return@withContext FriendEntity(
                ownerUserId = myId,
                userId = matchedAccount.userId,
                name = matchedAccount.displayName,
                avatarUrl = matchedAccount.avatarUrl,
                status = "Пользователь ANIWERTI",
                isPending = false,
                isIncoming = false
            )
        }

        // 3. Search in remote server database (cloud)
        var remoteProfile = serverDatabaseService.searchUserRemote(cleanInput)
        if (remoteProfile == null && !cleanInput.startsWith("ANI-", ignoreCase = true)) {
            remoteProfile = serverDatabaseService.searchUserRemote("ANI-$cleanInput")
        }

        if (remoteProfile != null && remoteProfile.userId != myId) {
            val existingInDao = socialDao.getFriendById(myId, remoteProfile.userId)
            if (existingInDao != null) return@withContext existingInDao
            return@withContext FriendEntity(
                ownerUserId = myId,
                userId = remoteProfile.userId,
                name = remoteProfile.displayName,
                avatarUrl = remoteProfile.avatarUrl,
                status = "Пользователь ANIWERTI",
                isPending = false,
                isIncoming = false
            )
        }

        null
    }

    suspend fun sendFriendRequest(friend: FriendEntity): Boolean = withContext(Dispatchers.IO) {
        val myUser = authRepository.getActiveUserDirect() ?: return@withContext false
        val myId = myUser.userId
        val targetId = friend.userId
        if (myId.isBlank() || targetId.isBlank() || myId == targetId) return@withContext false

        // Prevent reverting confirmed friendships
        val existing = socialDao.getFriendById(myId, targetId)
        if (existing != null && !existing.isPending) {
            // Already confirmed friends!
            return@withContext true
        }
        if (existing != null && existing.isPending && existing.isIncoming) {
            // Other person already sent us a request! Accept it right now!
            return@withContext acceptFriendRequest(targetId)
        }

        val now = System.currentTimeMillis()

        // 1. My side: Outgoing pending friend request (awaiting friend's confirmation)
        val outgoing = FriendEntity(
            ownerUserId = myId,
            userId = targetId,
            name = friend.name,
            avatarUrl = friend.avatarUrl,
            status = "Ожидает подтверждения",
            isPending = true,
            isIncoming = false,
            lastActive = now
        )
        socialDao.insertFriend(outgoing)
        serverDatabaseService.saveFriendRemote(myId, outgoing)

        // 2. Friend's side: Incoming pending friend request (friend must accept)
        val incoming = FriendEntity(
            ownerUserId = targetId,
            userId = myId,
            name = myUser.displayName,
            avatarUrl = myUser.avatarUrl,
            status = "Входящая заявка",
            isPending = true,
            isIncoming = true,
            lastActive = now
        )
        socialDao.insertFriend(incoming)
        serverDatabaseService.saveFriendRemote(targetId, incoming)

        // 3. Send real-time notification event via ntfy.sh with high priority
        val event = JSONObject().apply {
            put("action", "friend_request")
            put("senderId", myId)
            put("senderName", myUser.displayName)
            put("senderAvatar", myUser.avatarUrl)
            put("targetId", targetId)
            put("timestamp", now)
        }
        serverDatabaseService.sendEvent(targetId, event)

        true
    }

    suspend fun acceptFriendRequest(friendUserId: String): Boolean = withContext(Dispatchers.IO) {
        val myUser = authRepository.getActiveUserDirect() ?: return@withContext false
        val myId = myUser.userId
        if (myId.isBlank() || friendUserId.isBlank()) return@withContext false

        val now = System.currentTimeMillis()

        // 1. Update recipient (my) friend entry to accepted
        val existingB = socialDao.getFriendById(myId, friendUserId)
        val friendProfile = serverDatabaseService.getUserProfileRemote(friendUserId)
            ?: userAccountDao.getUserById(friendUserId)?.let {
                ServerUserProfile(it.userId, it.email, it.displayName, it.avatarUrl, it.authProvider)
            }

        val friendName = existingB?.name ?: friendProfile?.displayName ?: "Друг"
        val friendAvatar = existingB?.avatarUrl ?: friendProfile?.avatarUrl ?: ""

        val acceptedMySide = FriendEntity(
            ownerUserId = myId,
            userId = friendUserId,
            name = friendName,
            avatarUrl = friendAvatar,
            status = "В сети",
            isPending = false,
            isIncoming = false,
            lastActive = now
        )
        socialDao.insertFriend(acceptedMySide)
        serverDatabaseService.saveFriendRemote(myId, acceptedMySide)

        // 2. Update sender's friend entry to accepted as well
        val acceptedSenderSide = FriendEntity(
            ownerUserId = friendUserId,
            userId = myId,
            name = myUser.displayName,
            avatarUrl = myUser.avatarUrl,
            status = "В сети",
            isPending = false,
            isIncoming = false,
            lastActive = now
        )
        socialDao.insertFriend(acceptedSenderSide)
        serverDatabaseService.saveFriendRemote(friendUserId, acceptedSenderSide)

        // 3. Notify sender that request was accepted
        val event = JSONObject().apply {
            put("action", "friend_accepted")
            put("senderId", myId)
            put("senderName", myUser.displayName)
            put("senderAvatar", myUser.avatarUrl)
            put("targetId", friendUserId)
            put("timestamp", now)
        }
        serverDatabaseService.sendEvent(friendUserId, event)

        true
    }

    suspend fun declineFriendRequest(friendUserId: String): Boolean = withContext(Dispatchers.IO) {
        val myId = getCurrentUserId()
        if (myId.isBlank() || friendUserId.isBlank()) return@withContext false

        // Remove from both sides
        socialDao.deleteFriend(myId, friendUserId)
        serverDatabaseService.deleteFriendRemote(myId, friendUserId)

        socialDao.deleteFriend(friendUserId, myId)
        serverDatabaseService.deleteFriendRemote(friendUserId, myId)

        val event = JSONObject().apply {
            put("action", "friend_declined")
            put("senderId", myId)
            put("targetId", friendUserId)
        }
        serverDatabaseService.sendEvent(friendUserId, event)

        true
    }

    suspend fun cancelFriendRequest(friendUserId: String): Boolean = withContext(Dispatchers.IO) {
        declineFriendRequest(friendUserId)
    }

    suspend fun removeFriend(userId: String) = withContext(Dispatchers.IO) {
        declineFriendRequest(userId)
    }

    private suspend fun handleInboxEvent(myId: String, ev: JSONObject) {
        val action = ev.optString("action")
        val senderId = ev.optString("senderId")
        if (senderId.isBlank() || senderId == myId) return

        when (action) {
            "friend_request" -> {
                val existing = socialDao.getFriendById(myId, senderId)
                if (existing != null && !existing.isPending) {
                    return
                }

                val senderName = ev.optString("senderName", "Пользователь")
                val senderAvatar = ev.optString("senderAvatar", "")
                val incomingReq = FriendEntity(
                    ownerUserId = myId,
                    userId = senderId,
                    name = senderName,
                    avatarUrl = senderAvatar,
                    status = "Входящая заявка",
                    isPending = true,
                    isIncoming = true,
                    lastActive = ev.optLong("timestamp", System.currentTimeMillis())
                )

                if (existing == null) {
                    socialDao.insertFriend(incomingReq)
                    com.example.util.NotificationHelper.showFriendRequestNotification(
                        appContext,
                        senderName,
                        senderId
                    )
                } else if (existing.isPending && !existing.isIncoming) {
                    acceptFriendRequest(senderId)
                }
            }
            "friend_accepted" -> {
                val senderName = ev.optString("senderName", "Друг")
                val senderAvatar = ev.optString("senderAvatar", "")
                val accepted = FriendEntity(
                    ownerUserId = myId,
                    userId = senderId,
                    name = senderName,
                    avatarUrl = senderAvatar,
                    status = "В сети",
                    isPending = false,
                    isIncoming = false,
                    lastActive = ev.optLong("timestamp", System.currentTimeMillis())
                )
                socialDao.insertFriend(accepted)
            }
            "friend_declined" -> {
                socialDao.deleteFriend(myId, senderId)
            }
            "chat_message" -> {
                val text = ev.optString("text")
                val timestamp = ev.optLong("timestamp", System.currentTimeMillis())
                val senderName = ev.optString("senderName", "Друг")
                if (text.isNotBlank()) {
                    val exists = socialDao.hasMessage(myId, senderId, timestamp, text) > 0
                    if (!exists) {
                        socialDao.insertMessage(
                            ChatMessageEntity(
                                ownerUserId = myId,
                                friendId = senderId,
                                senderId = senderId,
                                text = text,
                                timestamp = timestamp,
                                isMine = false,
                                status = "SENT"
                            )
                        )
                        if (activeChatFriendId != senderId) {
                            com.example.util.NotificationHelper.showChatMessageNotification(
                                appContext,
                                senderName,
                                senderId,
                                text
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun streamInboxEvents(myId: String) = withContext(Dispatchers.IO) {
        serverDatabaseService.streamInbox(myId) { ev ->
            repoScope.launch {
                try {
                    handleInboxEvent(myId, ev)
                } catch (_: Throwable) {}
            }
        }
    }

    suspend fun streamConversation(myId: String, friendId: String) = withContext(Dispatchers.IO) {
        serverDatabaseService.streamChat(myId, friendId) { ev ->
            val sId = ev.optString("senderId")
            val text = ev.optString("text")
            val timestamp = ev.optLong("timestamp", System.currentTimeMillis())
            if (text.isNotBlank() && sId.isNotBlank() && sId != myId) {
                repoScope.launch {
                    val exists = socialDao.hasMessage(myId, friendId, timestamp, text) > 0
                    if (!exists) {
                        socialDao.insertMessage(
                            ChatMessageEntity(
                                ownerUserId = myId,
                                friendId = friendId,
                                senderId = sId,
                                text = text,
                                timestamp = timestamp,
                                isMine = false,
                                status = "SENT"
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun syncFriends() = withContext(Dispatchers.IO) {
        val myUser = authRepository.getActiveUserDirect() ?: return@withContext
        val myId = myUser.userId
        if (myId.isBlank()) return@withContext

        // 1. Process real-time inbox events from ntfy.sh (fast)
        try {
            val events = serverDatabaseService.pollEvents(myId)
            for (ev in events) {
                handleInboxEvent(myId, ev)
            }
        } catch (_: Throwable) {}

        // 2. Heavy cloud sync throttled to once every 40 seconds
        val now = System.currentTimeMillis()
        if (now - lastFullSyncTime > 40_000L) {
            lastFullSyncTime = now

            try {
                val remoteFriends = serverDatabaseService.getFriendsRemote(myId)
                for (f in remoteFriends) {
                    val existing = socialDao.getFriendById(myId, f.userId)
                    if (existing != null && !existing.isPending && f.isPending) {
                        continue
                    }
                    if (f.isPending && f.isIncoming && existing == null) {
                        com.example.util.NotificationHelper.showFriendRequestNotification(
                            appContext,
                            f.name.ifBlank { "Пользователь" },
                            f.userId
                        )
                    }
                    socialDao.insertFriend(f)
                }
            } catch (_: Throwable) {}

            try {
                serverDatabaseService.updatePresence(myId)
                val friends = socialDao.getAllFriendsDirect(myId)
                for (f in friends) {
                    val lastActiveRemote = serverDatabaseService.getLastActive(f.userId)
                    val effectiveLastActive = maxOf(f.lastActive, lastActiveRemote)
                    val isOnline = (System.currentTimeMillis() - effectiveLastActive) < 3 * 60 * 1000

                    val localAcc = userAccountDao.getUserById(f.userId)
                    val remoteProfile = serverDatabaseService.getUserProfileRemote(f.userId)

                    val latestAvatar: String = when {
                        !remoteProfile?.avatarUrl.isNullOrBlank() -> remoteProfile.avatarUrl
                        !localAcc?.avatarUrl.isNullOrBlank() -> localAcc.avatarUrl
                        else -> f.avatarUrl
                    }
                    val latestName: String = when {
                        !remoteProfile?.displayName.isNullOrBlank() -> remoteProfile.displayName
                        !localAcc?.displayName.isNullOrBlank() -> localAcc.displayName
                        else -> f.name
                    }

                    val newStatus = if (isOnline) "В сети" else "Не в сети"

                    if (latestAvatar != f.avatarUrl || latestName != f.name || effectiveLastActive != f.lastActive || newStatus != f.status) {
                        socialDao.insertFriend(
                            f.copy(
                                avatarUrl = latestAvatar,
                                name = latestName,
                                lastActive = effectiveLastActive,
                                status = newStatus
                            )
                        )
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    suspend fun updateMyPresence() = withContext(Dispatchers.IO) {
        val myUser = authRepository.getActiveUserDirect() ?: return@withContext
        serverDatabaseService.updatePresence(myUser.userId)
    }

    suspend fun sendMessage(friendId: String, text: String): Boolean = withContext(Dispatchers.IO) {
        val myUser = authRepository.getActiveUserDirect() ?: return@withContext false
        val myId = myUser.userId
        if (myId.isBlank() || friendId.isBlank()) return@withContext false
        val now = System.currentTimeMillis()

        // 1. Save for sender (isMine = true) - Immediate local display (0ms latency)
        val userMsg = ChatMessageEntity(
            ownerUserId = myId,
            friendId = friendId,
            senderId = myId,
            text = text,
            timestamp = now,
            isMine = true,
            status = "SENT"
        )
        socialDao.insertMessage(userMsg)

        // 2. Save locally for recipient as well (isMine = false) if on same device or active
        val recipientMsg = ChatMessageEntity(
            ownerUserId = friendId,
            friendId = myId,
            senderId = myId,
            text = text,
            timestamp = now,
            isMine = false,
            status = "SENT"
        )
        socialDao.insertMessage(recipientMsg)

        // 3. Instant push event via ntfy.sh (both direct chat topic and inbox)
        val msgEvent = JSONObject().apply {
            put("action", "chat_message")
            put("senderId", myId)
            put("senderName", myUser.displayName)
            put("friendId", myId)
            put("targetId", friendId)
            put("text", text)
            put("timestamp", now)
        }
        serverDatabaseService.sendChatEvent(myId, friendId, msgEvent)

        // 4. Save remote message asynchronously in background - NEVER block sending!
        repoScope.launch {
            try {
                serverDatabaseService.saveMessageRemote(myId, friendId, text, now)
            } catch (_: Throwable) {}
        }

        true
    }

    suspend fun syncMessagesWithFriend(friendId: String) = withContext(Dispatchers.IO) {
        val myUser = authRepository.getActiveUserDirect() ?: return@withContext
        val myId = myUser.userId
        if (myId.isBlank() || friendId.isBlank()) return@withContext

        // Fast chat events polling from ntfy.sh
        try {
            val chatEvents = serverDatabaseService.pollChatEvents(myId, friendId)
            for (ev in chatEvents) {
                val sId = ev.optString("senderId")
                val text = ev.optString("text")
                val timestamp = ev.optLong("timestamp", System.currentTimeMillis())
                if (text.isNotBlank() && sId.isNotBlank()) {
                    val exists = socialDao.hasMessage(myId, friendId, timestamp, text) > 0
                    if (!exists) {
                        socialDao.insertMessage(
                            ChatMessageEntity(
                                ownerUserId = myId,
                                friendId = friendId,
                                senderId = sId,
                                text = text,
                                timestamp = timestamp,
                                isMine = (sId == myId),
                                status = "SENT"
                            )
                        )
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    suspend fun syncRemoteHistory(friendId: String) = withContext(Dispatchers.IO) {
        val myUser = authRepository.getActiveUserDirect() ?: return@withContext
        val myId = myUser.userId
        if (myId.isBlank() || friendId.isBlank()) return@withContext

        try {
            val remoteMessages = serverDatabaseService.getMessagesRemote(myId, friendId)
            for (rm in remoteMessages) {
                val exists = socialDao.hasMessage(myId, friendId, rm.timestamp, rm.text) > 0
                if (!exists) {
                    socialDao.insertMessage(
                        ChatMessageEntity(
                            ownerUserId = myId,
                            friendId = friendId,
                            senderId = rm.senderId,
                            text = rm.text,
                            timestamp = rm.timestamp,
                            isMine = (rm.senderId == myId),
                            status = "SENT"
                        )
                    )
                }
            }
        } catch (_: Throwable) {}
    }
}

