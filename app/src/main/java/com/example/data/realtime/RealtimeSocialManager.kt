package com.example.data.realtime

import android.content.Context
import android.util.Log
import com.example.data.db.AnimeDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.FriendEntity
import com.example.data.server.ServerDatabaseService
import com.example.util.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Unified Real-Time Social Manager.
 * Orchestrates:
 * 1. Firebase Firestore onSnapshot listeners (addSnapshotListener) for real-time database streaming
 * 2. High-speed WebSockets (wss://) for instant event delivery (<50ms)
 * 3. In-memory RealtimeSocialBus for zero-latency local dispatch (0ms)
 * 4. Reactive Room DB bindings which automatically update Jetpack Compose StateFlows
 */
class RealtimeSocialManager private constructor(private val context: Context) {
    private val TAG = "RealtimeSocialManager"
    private val appContext = context.applicationContext
    private val database = AnimeDatabase.getDatabase(appContext)
    private val socialDao = database.socialDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val webSocketManager = NtfyWebSocketManager()

    private var activeUserId: String? = null
    private var activeChatFriendId: String? = null

    // Firestore registrations
    private var firestoreFriendsRegistration: ListenerRegistration? = null
    private var firestoreInboxRegistration: ListenerRegistration? = null
    private val firestoreChatRegistrations = ConcurrentHashMap<String, ListenerRegistration>()

    // Local bus collection job
    private var busJob: Job? = null

    private val processedMessageKeys = ConcurrentHashMap.newKeySet<String>()

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Firestore not available: ${e.message}")
            null
        }
    }

    init {
        // Collect from the in-memory event bus
        busJob = scope.launch {
            RealtimeSocialBus.events.collectLatest { event ->
                handleBusEvent(event)
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

    /**
     * Start real-time listeners for the currently logged-in user.
     * Starts Firestore snapshot listeners and WebSocket connection.
     */
    fun startUserListeners(userId: String) {
        val cleanId = userId.trim()
        if (cleanId.isBlank()) return
        if (activeUserId == cleanId) return

        stopUserListeners()
        activeUserId = cleanId

        // 1. Subscribe to WebSocket inbox topic
        val inboxTopic = "aniwerti_inbox_${safeKey(cleanId)}"
        webSocketManager.subscribe(inboxTopic) { eventJson ->
            scope.launch {
                handleIncomingEventJson(cleanId, eventJson)
            }
        }

        // 2. Attach Firestore snapshot listener for Friends & Requests
        attachFirestoreFriendsListener(cleanId)

        // 3. Attach Firestore snapshot listener for Inbox
        attachFirestoreInboxListener(cleanId)
    }

    /**
     * Stop user listeners upon logout or user change.
     */
    fun stopUserListeners() {
        activeUserId?.let { uid ->
            val inboxTopic = "aniwerti_inbox_${safeKey(uid)}"
            webSocketManager.unsubscribe(inboxTopic)
        }
        firestoreFriendsRegistration?.remove()
        firestoreFriendsRegistration = null

        firestoreInboxRegistration?.remove()
        firestoreInboxRegistration = null

        activeUserId = null
    }

    /**
     * Start real-time chat listener for an active conversation with [friendId].
     * Attaches both Firestore onSnapshot on `chats/{convKey}/messages`
     * and WebSocket stream on `aniwerti_chat_{convKey}`.
     */
    fun startChatListeners(myUserId: String, friendId: String) {
        val convKey = getConversationKey(myUserId, friendId)
        val chatTopic = "aniwerti_chat_${safeKey(convKey)}"

        // 1. WebSocket chat stream
        webSocketManager.subscribe(chatTopic) { eventJson ->
            scope.launch {
                val sId = eventJson.optString("senderId")
                val text = eventJson.optString("text")
                val timestamp = eventJson.optLong("timestamp", System.currentTimeMillis())

                if (text.isNotBlank() && sId.isNotBlank()) {
                    insertReceivedMessage(
                        ownerUserId = myUserId,
                        friendId = friendId,
                        senderId = sId,
                        text = text,
                        timestamp = timestamp
                    )
                }
            }
        }

        // 2. Firestore snapshot listener
        attachFirestoreChatListener(convKey, myUserId, friendId)
    }

    /**
     * Stop chat listener when leaving the conversation screen.
     */
    fun stopChatListeners(myUserId: String, friendId: String) {
        val convKey = getConversationKey(myUserId, friendId)
        val chatTopic = "aniwerti_chat_${safeKey(convKey)}"
        webSocketManager.unsubscribe(chatTopic)

        firestoreChatRegistrations.remove(convKey)?.remove()
    }

    private fun attachFirestoreFriendsListener(userId: String) {
        val db = firestore ?: return
        try {
            firestoreFriendsRegistration?.remove()
            firestoreFriendsRegistration = db.collection("users")
                .document(userId)
                .collection("friends")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore friends onSnapshot error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener

                    scope.launch {
                        for (dc in snapshot.documentChanges) {
                            val doc = dc.document
                            val targetId = doc.getString("userId") ?: doc.id
                            when (dc.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val isPending = doc.getBoolean("isPending") ?: false
                                    val isIncoming = doc.getBoolean("isIncoming") ?: false
                                    val name = doc.getString("name") ?: "Друг"
                                    val avatarUrl = doc.getString("avatarUrl") ?: ""
                                    val status = doc.getString("status") ?: "В сети"
                                    val lastActive = doc.getLong("lastActive") ?: System.currentTimeMillis()

                                    val friendEntity = FriendEntity(
                                        ownerUserId = userId,
                                        userId = targetId,
                                        name = name,
                                        avatarUrl = avatarUrl,
                                        status = status,
                                        isPending = isPending,
                                        isIncoming = isIncoming,
                                        lastActive = lastActive
                                    )
                                    socialDao.insertFriend(friendEntity)

                                    if (isPending && isIncoming && dc.type == DocumentChange.Type.ADDED) {
                                        NotificationHelper.showFriendRequestNotification(
                                            appContext,
                                            name,
                                            targetId
                                        )
                                    }
                                }
                                DocumentChange.Type.REMOVED -> {
                                    socialDao.deleteFriend(userId, targetId)
                                }
                            }
                        }
                    }
                }
        } catch (e: Throwable) {
            Log.w(TAG, "attachFirestoreFriendsListener failed: ${e.message}")
        }
    }

    private fun attachFirestoreInboxListener(userId: String) {
        val db = firestore ?: return
        try {
            firestoreInboxRegistration?.remove()
            firestoreInboxRegistration = db.collection("users")
                .document(userId)
                .collection("inbox")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch {
                        for (dc in snapshot.documentChanges) {
                            if (dc.type == DocumentChange.Type.ADDED) {
                                val doc = dc.document
                                val data = doc.data ?: continue
                                val json = try {
                                    JSONObject(data as Map<*, *>)
                                } catch (_: Throwable) {
                                    JSONObject(data.toString())
                                }
                                handleIncomingEventJson(userId, json)
                            }
                        }
                    }
                }
        } catch (e: Throwable) {
            Log.w(TAG, "attachFirestoreInboxListener failed: ${e.message}")
        }
    }

    private fun attachFirestoreChatListener(convKey: String, myUserId: String, friendId: String) {
        val db = firestore ?: return
        if (firestoreChatRegistrations.containsKey(convKey)) return

        try {
            val reg = db.collection("chats")
                .document(convKey)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore chat onSnapshot error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener

                    scope.launch {
                        for (dc in snapshot.documentChanges) {
                            if (dc.type == DocumentChange.Type.ADDED) {
                                val doc = dc.document
                                val sId = doc.getString("senderId") ?: ""
                                val text = doc.getString("text") ?: ""
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                if (text.isNotBlank() && sId.isNotBlank()) {
                                    insertReceivedMessage(
                                        ownerUserId = myUserId,
                                        friendId = friendId,
                                        senderId = sId,
                                        text = text,
                                        timestamp = timestamp
                                    )
                                }
                            }
                        }
                    }
                }
            firestoreChatRegistrations[convKey] = reg
        } catch (e: Throwable) {
            Log.w(TAG, "attachFirestoreChatListener failed: ${e.message}")
        }
    }

    private suspend fun insertReceivedMessage(
        ownerUserId: String,
        friendId: String,
        senderId: String,
        text: String,
        timestamp: Long
    ) {
        val dedupeKey = "${ownerUserId}_${friendId}_${timestamp}_${text.hashCode()}"
        if (processedMessageKeys.contains(dedupeKey)) return

        val exists = socialDao.hasMessage(ownerUserId, friendId, timestamp, text) > 0
        if (!exists) {
            processedMessageKeys.add(dedupeKey)
            if (processedMessageKeys.size > 1000) {
                processedMessageKeys.clear()
            }
            socialDao.insertMessage(
                ChatMessageEntity(
                    ownerUserId = ownerUserId,
                    friendId = friendId,
                    senderId = senderId,
                    text = text,
                    timestamp = timestamp,
                    isMine = (senderId == ownerUserId),
                    status = "SENT"
                )
            )
        }
    }

    private suspend fun handleIncomingEventJson(myId: String, ev: JSONObject) {
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
                    NotificationHelper.showFriendRequestNotification(
                        appContext,
                        senderName,
                        senderId
                    )
                } else if (existing.isPending && !existing.isIncoming) {
                    // Mutual request: auto-accept
                    val accepted = incomingReq.copy(isPending = false, isIncoming = false, status = "В сети")
                    socialDao.insertFriend(accepted)
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
                    insertReceivedMessage(myId, senderId, senderId, text, timestamp)
                    if (activeChatFriendId != senderId) {
                        NotificationHelper.showChatMessageNotification(
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

    private suspend fun handleBusEvent(event: RealtimeEvent) {
        val myId = activeUserId ?: return
        when (event) {
            is RealtimeEvent.FriendRequest -> {
                if (event.targetId == myId) {
                    val incomingReq = FriendEntity(
                        ownerUserId = myId,
                        userId = event.senderId,
                        name = event.senderName,
                        avatarUrl = event.senderAvatar,
                        status = "Входящая заявка",
                        isPending = true,
                        isIncoming = true,
                        lastActive = event.timestamp
                    )
                    socialDao.insertFriend(incomingReq)
                    NotificationHelper.showFriendRequestNotification(
                        appContext,
                        event.senderName,
                        event.senderId
                    )
                }
            }
            is RealtimeEvent.FriendAccepted -> {
                if (event.targetId == myId) {
                    val accepted = FriendEntity(
                        ownerUserId = myId,
                        userId = event.senderId,
                        name = event.senderName,
                        avatarUrl = event.senderAvatar,
                        status = "В сети",
                        isPending = false,
                        isIncoming = false,
                        lastActive = event.timestamp
                    )
                    socialDao.insertFriend(accepted)
                }
            }
            is RealtimeEvent.FriendDeclined -> {
                if (event.targetId == myId) {
                    socialDao.deleteFriend(myId, event.senderId)
                }
            }
            is RealtimeEvent.ChatMessage -> {
                if (event.targetId == myId) {
                    insertReceivedMessage(
                        ownerUserId = myId,
                        friendId = event.senderId,
                        senderId = event.senderId,
                        text = event.text,
                        timestamp = event.timestamp
                    )
                    if (activeChatFriendId != event.senderId) {
                        NotificationHelper.showChatMessageNotification(
                            appContext,
                            event.senderName,
                            event.senderId,
                            event.text
                        )
                    }
                }
            }
        }
    }

    private fun safeKey(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(55)
    }

    private fun getConversationKey(u1: String, u2: String): String {
        return if (u1 < u2) "${u1}__${u2}" else "${u2}__${u1}"
    }

    companion object {
        @Volatile
        private var INSTANCE: RealtimeSocialManager? = null

        fun getInstance(context: Context): RealtimeSocialManager {
            return INSTANCE ?: synchronized(this) {
                val instance = RealtimeSocialManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
