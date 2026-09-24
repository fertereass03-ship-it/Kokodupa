package com.example.data.server

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.db.AnimeDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.FavoriteAnimeEntity
import com.example.data.db.FavoriteCategory
import com.example.data.db.FriendEntity
import com.example.data.db.PlayerSettingsEntity
import com.example.data.db.WatchHistoryEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ServerDatabaseService private constructor(private val context: Context) {
    private val TAG = "ServerDatabaseService"
    private val serverPrefs = context.applicationContext.getSharedPreferences("aniwerti_cloud_db", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    private val KV_BASE = "https://keyvalue.immanuel.co/api/KeyVal"
    private val KV_APP = "zh531s0d"
    private val CHUNK_SIZE = 100

    private fun safeKey(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(55)
    }

    private fun encodeValue(str: String): String {
        return Base64.encodeToString(
            str.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        ).trim()
    }

    private fun decodeValue(b64: String): String? {
        return try {
            var s = b64.trim().trim('"')
            if (s.isEmpty() || s == "null") return null
            val remainder = s.length % 4
            if (remainder > 0) {
                s += "=".repeat(4 - remainder)
            }
            val bytes = Base64.decode(s, Base64.URL_SAFE)
            String(bytes, Charsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }

    private fun kvPut(key: String, json: String): Boolean {
        return try {
            val sKey = safeKey(key)
            val b64 = encodeValue(json)
            val chunks = b64.chunked(CHUNK_SIZE)

            // 1. Save count
            val cntUrl = "$KV_BASE/UpdateValue/$KV_APP/${sKey}__cnt/${chunks.size}"
            val cntReq = Request.Builder()
                .url(cntUrl)
                .post(RequestBody.create(null, ByteArray(0)))
                .header("Content-Length", "0")
                .build()
            okHttpClient.newCall(cntReq).execute().close()

            // 2. Save each chunk
            for (i in chunks.indices) {
                val chunk = chunks[i]
                val chunkUrl = "$KV_BASE/UpdateValue/$KV_APP/${sKey}__$i/$chunk"
                val chunkReq = Request.Builder()
                    .url(chunkUrl)
                    .post(RequestBody.create(null, ByteArray(0)))
                    .header("Content-Length", "0")
                    .build()
                okHttpClient.newCall(chunkReq).execute().close()
            }
            true
        } catch (e: Throwable) {
            Log.w(TAG, "kvPut error for $key: ${e.message}")
            false
        }
    }

    private fun kvGet(key: String): String? {
        return try {
            val sKey = safeKey(key)
            val cntUrl = "$KV_BASE/GetValue/$KV_APP/${sKey}__cnt"
            val cntReq = Request.Builder().url(cntUrl).get().build()
            val cntStr = okHttpClient.newCall(cntReq).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string()?.trim()?.trim('"') else null
            }

            if (!cntStr.isNullOrBlank() && cntStr != "null") {
                val count = cntStr.toIntOrNull() ?: 0
                if (count > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until count) {
                        val partUrl = "$KV_BASE/GetValue/$KV_APP/${sKey}__$i"
                        val partReq = Request.Builder().url(partUrl).get().build()
                        val part = okHttpClient.newCall(partReq).execute().use { resp ->
                            if (resp.isSuccessful) resp.body?.string()?.trim()?.trim('"') else null
                        }
                        if (!part.isNullOrBlank() && part != "null") {
                            sb.append(part)
                        }
                    }
                    val fullB64 = sb.toString()
                    if (fullB64.isNotEmpty()) {
                        val decoded = decodeValue(fullB64)
                        if (decoded != null) return decoded
                    }
                }
            }

            // Fallback to direct key
            val directUrl = "$KV_BASE/GetValue/$KV_APP/$sKey"
            val directReq = Request.Builder().url(directUrl).get().build()
            okHttpClient.newCall(directReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) decodeValue(body) else null
                } else null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "kvGet error for $key: ${e.message}")
            null
        }
    }

    private val processedEventIds = java.util.Collections.synchronizedSet(java.util.LinkedHashSet<String>())

    private fun isEventProcessed(id: String): Boolean {
        if (id.isBlank()) return false
        if (processedEventIds.contains(id)) return true
        val saved = serverPrefs.getStringSet("processed_event_ids", emptySet()) ?: emptySet()
        return saved.contains(id)
    }

    private fun markEventProcessed(id: String) {
        if (id.isBlank()) return
        processedEventIds.add(id)
        if (processedEventIds.size > 500) {
            val iterator = processedEventIds.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
        val current = serverPrefs.getStringSet("processed_event_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(id)
        if (current.size > 500) {
            val toKeep = current.toList().takeLast(300).toSet()
            serverPrefs.edit().putStringSet("processed_event_ids", toKeep).apply()
        } else {
            serverPrefs.edit().putStringSet("processed_event_ids", current).apply()
        }
    }

    fun sendEvent(targetUserId: String, eventObj: JSONObject): Boolean {
        return try {
            val cleanId = targetUserId.trim()
            if (cleanId.isBlank()) return false
            val topic = "aniwerti_inbox_${safeKey(cleanId)}"
            val url = "https://ntfy.sh/$topic"
            val jsonBody = eventObj.toString()
            val req = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .header("Title", eventObj.optString("action", "Event"))
                .header("Priority", "high")
                .header("X-Priority", "5")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                resp.isSuccessful
            }
        } catch (e: Throwable) {
            Log.w(TAG, "sendEvent error to $targetUserId: ${e.message}")
            false
        }
    }

    fun sendChatEvent(senderId: String, recipientId: String, eventObj: JSONObject): Boolean {
        val convKey = getConversationKey(senderId, recipientId)
        val topic = "aniwerti_chat_${safeKey(convKey)}"
        val url = "https://ntfy.sh/$topic"
        val jsonBody = eventObj.toString()
        try {
            val req = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .header("Title", "Chat")
                .header("Priority", "high")
                .header("X-Priority", "5")
                .build()
            okHttpClient.newCall(req).execute().close()
        } catch (_: Throwable) {}
        // Also send to personal inbox
        return sendEvent(recipientId, eventObj)
    }

    private val streamingClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun streamChat(user1: String, user2: String, onEvent: (JSONObject) -> Unit) {
        try {
            val convKey = getConversationKey(user1, user2)
            val topic = "aniwerti_chat_${safeKey(convKey)}"
            val url = "https://ntfy.sh/$topic/json?since=10m"
            val req = Request.Builder().url(url).build()
            streamingClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val source = resp.body?.source() ?: return
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue
                        try {
                            val lineObj = JSONObject(trimmed)
                            if (lineObj.optString("event") == "message") {
                                val msgStr = lineObj.optString("message")
                                if (msgStr.isNotBlank()) {
                                    val eventJson = JSONObject(msgStr)
                                    val evId = lineObj.optString("id")
                                    if (evId.isNotBlank()) {
                                        eventJson.put("_eventId", evId)
                                    }
                                    onEvent(eventJson)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    fun streamInbox(myUserId: String, onEvent: (JSONObject) -> Unit) {
        try {
            val cleanId = myUserId.trim()
            if (cleanId.isBlank()) return
            val topic = "aniwerti_inbox_${safeKey(cleanId)}"
            val url = "https://ntfy.sh/$topic/json?since=30m"
            val req = Request.Builder().url(url).build()
            streamingClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val source = resp.body?.source() ?: return
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue
                        try {
                            val lineObj = JSONObject(trimmed)
                            if (lineObj.optString("event") == "message") {
                                val msgId = lineObj.optString("id")
                                if (msgId.isNotBlank() && isEventProcessed(msgId)) {
                                    continue
                                }
                                val msgStr = lineObj.optString("message")
                                if (msgStr.isNotBlank()) {
                                    val eventJson = JSONObject(msgStr)
                                    if (msgId.isNotBlank()) {
                                        eventJson.put("_eventId", msgId)
                                        markEventProcessed(msgId)
                                    }
                                    onEvent(eventJson)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    fun pollChatEvents(user1: String, user2: String): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        try {
            val convKey = getConversationKey(user1, user2)
            val topic = "aniwerti_chat_${safeKey(convKey)}"
            val url = "https://ntfy.sh/$topic/json?poll=1&since=10m"
            val req = Request.Builder().url(url).get().build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: return emptyList()
                    for (line in body.lines()) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue
                        try {
                            val lineObj = JSONObject(trimmed)
                            if (lineObj.optString("event") == "message") {
                                val msgStr = lineObj.optString("message")
                                if (msgStr.isNotBlank()) {
                                    val eventJson = JSONObject(msgStr)
                                    val evId = lineObj.optString("id")
                                    eventJson.put("_eventId", evId)
                                    result.add(eventJson)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Throwable) {}
        return result
    }

    fun pollEvents(myUserId: String): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        try {
            val cleanId = myUserId.trim()
            if (cleanId.isBlank()) return emptyList()
            val topic = "aniwerti_inbox_${safeKey(cleanId)}"
            val url = "https://ntfy.sh/$topic/json?poll=1&since=30m"
            val req = Request.Builder().url(url).get().build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: return emptyList()
                    val lines = body.lines()
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue
                        try {
                            val lineObj = JSONObject(trimmed)
                            if (lineObj.optString("event") == "message") {
                                val msgId = lineObj.optString("id")
                                if (msgId.isNotBlank() && isEventProcessed(msgId)) {
                                    // Already processed, skip to avoid repeating old requests
                                    continue
                                }
                                val msgStr = lineObj.optString("message")
                                if (msgStr.isNotBlank()) {
                                    val eventJson = JSONObject(msgStr)
                                    if (msgId.isNotBlank()) {
                                        eventJson.put("_eventId", msgId)
                                        markEventProcessed(msgId)
                                    }
                                    result.add(eventJson)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "pollEvents error for $myUserId: ${e.message}")
        }
        return result
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Firestore not available in current configuration: ${e.message}")
            null
        }
    }

    // --- Profile Management ---

    suspend fun saveUserProfileRemote(profile: ServerUserProfile, passwordHash: String? = null) = withContext(Dispatchers.IO) {
        val userObj = JSONObject().apply {
            put("userId", profile.userId)
            put("email", profile.email)
            put("displayName", profile.displayName)
            put("avatarUrl", profile.avatarUrl)
            put("authProvider", profile.authProvider)
            put("sessionToken", profile.sessionToken)
            put("createdAt", profile.createdAt)
            if (passwordHash != null) put("passwordHash", passwordHash)
        }
        val jsonStr = userObj.toString()
        serverPrefs.edit().putString("user_${profile.userId}", jsonStr).apply()
        serverPrefs.edit().putString("user_email_${profile.email.lowercase()}", profile.userId).apply()
        val nickKey = profile.displayName.trim().lowercase().replace(" ", "_")
        if (nickKey.length >= 2) {
            serverPrefs.edit().putString("nick_$nickKey", profile.userId).apply()
        }

        // Sync profile to cloud KV so other phones can discover this user
        kvPut("user_${profile.userId}", jsonStr)
        kvPut("user_id_${profile.userId.uppercase()}", profile.userId)
        if (profile.email.isNotBlank()) {
            kvPut("user_email_${profile.email.trim().lowercase()}", profile.userId)
        }
        if (nickKey.length >= 2) {
            kvPut("nick_$nickKey", profile.userId)
        }

        // Announce user profile in cloud directory so other devices can discover
        try {
            val dirObj = JSONObject().apply {
                put("action", "register")
                put("userId", profile.userId)
                put("displayName", profile.displayName)
                put("avatarUrl", profile.avatarUrl)
                put("email", profile.email)
            }
            val dirReq = Request.Builder()
                .url("https://ntfy.sh/aniwerti_directory")
                .post(dirObj.toString().toRequestBody("application/json".toMediaType()))
                .build()
            okHttpClient.newCall(dirReq).execute().close()
        } catch (_: Throwable) {}

        firestore?.let { db ->
            try {
                val data = hashMapOf(
                    "userId" to profile.userId,
                    "email" to profile.email,
                    "displayName" to profile.displayName,
                    "avatarUrl" to profile.avatarUrl,
                    "authProvider" to profile.authProvider,
                    "createdAt" to profile.createdAt
                )
                db.collection("users").document(profile.userId)
                    .set(data, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore saveUserProfile error: ${e.message}")
            }
        }
    }

    fun updatePresence(userId: String, timestamp: Long = System.currentTimeMillis()) {
        if (userId.isBlank()) return
        serverPrefs.edit().putLong("presence_$userId", timestamp).apply()
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                kvPut("presence_$userId", timestamp.toString())
            } catch (_: Throwable) {}
        }
    }

    suspend fun getLastActive(userId: String): Long = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext 0L
        val local = serverPrefs.getLong("presence_$userId", 0L)
        if (System.currentTimeMillis() - local < 3 * 60 * 1000) {
            return@withContext local
        }
        val remote = kvGet("presence_$userId")?.toLongOrNull() ?: 0L
        val best = maxOf(local, remote)
        if (best > local) {
            serverPrefs.edit().putLong("presence_$userId", best).apply()
        }
        return@withContext best
    }

    suspend fun getUserProfileRemote(userId: String): ServerUserProfile? = withContext(Dispatchers.IO) {
        val raw = serverPrefs.getString("user_$userId", null)
        if (raw != null) {
            try {
                val obj = JSONObject(raw)
                return@withContext ServerUserProfile(
                    userId = obj.getString("userId"),
                    email = obj.getString("email"),
                    displayName = obj.getString("displayName"),
                    avatarUrl = obj.getString("avatarUrl"),
                    authProvider = obj.getString("authProvider"),
                    sessionToken = obj.optString("sessionToken", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            } catch (_: Exception) {}
        }

        // Check remote cloud KV store
        val cloudJson = kvGet("user_$userId")
        if (cloudJson != null) {
            try {
                val obj = JSONObject(cloudJson)
                serverPrefs.edit().putString("user_$userId", cloudJson).apply()
                return@withContext ServerUserProfile(
                    userId = obj.getString("userId"),
                    email = obj.getString("email"),
                    displayName = obj.getString("displayName"),
                    avatarUrl = obj.getString("avatarUrl"),
                    authProvider = obj.getString("authProvider"),
                    sessionToken = obj.optString("sessionToken", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            } catch (_: Exception) {}
        }

        firestore?.let { db ->
            try {
                val doc = db.collection("users").document(userId).get().await()
                if (doc.exists()) {
                    return@withContext ServerUserProfile(
                        userId = doc.getString("userId") ?: userId,
                        email = doc.getString("email") ?: "",
                        displayName = doc.getString("displayName") ?: "",
                        avatarUrl = doc.getString("avatarUrl") ?: "",
                        authProvider = doc.getString("authProvider") ?: "EMAIL",
                        sessionToken = "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore getUserProfile error: ${e.message}")
            }
        }
        null
    }

    suspend fun getUserProfileByEmail(email: String): ServerUserProfile? = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val userId = serverPrefs.getString("user_email_$cleanEmail", null)
        if (userId != null) {
            return@withContext getUserProfileRemote(userId)
        }
        null
    }

    suspend fun getUserPasswordHash(userId: String): String? = withContext(Dispatchers.IO) {
        val raw = serverPrefs.getString("user_$userId", null) ?: return@withContext null
        try {
            val obj = JSONObject(raw)
            if (obj.has("passwordHash")) obj.getString("passwordHash") else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun updateSessionToken(userId: String, token: String) = withContext(Dispatchers.IO) {
        val raw = serverPrefs.getString("user_$userId", null) ?: return@withContext
        try {
            val obj = JSONObject(raw)
            obj.put("sessionToken", token)
            serverPrefs.edit().putString("user_$userId", obj.toString()).apply()
        } catch (_: Exception) {}
    }

    // --- Synchronize All User Data to/from Server ---

    suspend fun syncUserData(userId: String) = withContext(Dispatchers.IO) {
        val roomDb = AnimeDatabase.getDatabase(context)

        // 1. Sync Favorites from Server
        val remoteFavorites = getFavoritesRemote(userId)
        for (fav in remoteFavorites) {
            roomDb.favoriteDao().insertFavorite(fav)
        }

        // 2. Sync History from Server
        val remoteHistory = getWatchHistoryRemote(userId)
        for (hist in remoteHistory) {
            roomDb.watchHistoryDao().insertOrUpdateHistory(hist)
        }

        // 3. Sync Settings from Server
        val remoteSettings = getPlayerSettingsRemote(userId)
        if (remoteSettings != null) {
            roomDb.settingsDao().savePlayerSettings(remoteSettings)
        }

        // 4. Sync Friends from Server
        val remoteFriends = getFriendsRemote(userId)
        for (f in remoteFriends) {
            roomDb.socialDao().insertFriend(f)
        }
    }

    // --- Favorites Cloud Storage ---

    suspend fun saveFavoriteRemote(userId: String, favorite: FavoriteAnimeEntity) = withContext(Dispatchers.IO) {
        val key = "favs_$userId"
        val existing = getFavoritesRemote(userId).filter { it.id != favorite.id } + favorite
        val arr = JSONArray()
        for (f in existing) {
            val obj = JSONObject().apply {
                put("userId", f.userId)
                put("id", f.id)
                put("name", f.name)
                put("russianName", f.russianName)
                put("posterUrl", f.posterUrl)
                put("score", f.score)
                put("kind", f.kind)
                put("episodesCount", f.episodesCount)
                put("year", f.year)
                put("category", f.category.name)
                put("updatedAt", f.updatedAt)
            }
            arr.put(obj)
        }
        serverPrefs.edit().putString(key, arr.toString()).apply()

        firestore?.let { db ->
            try {
                val data = hashMapOf(
                    "userId" to favorite.userId,
                    "animeId" to favorite.id,
                    "name" to favorite.name,
                    "russianName" to favorite.russianName,
                    "posterUrl" to favorite.posterUrl,
                    "score" to favorite.score,
                    "kind" to favorite.kind,
                    "episodesCount" to favorite.episodesCount,
                    "year" to favorite.year,
                    "category" to favorite.category.name,
                    "updatedAt" to favorite.updatedAt
                )
                db.collection("users").document(userId)
                    .collection("favorites").document(favorite.id.toString())
                    .set(data, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore saveFavoriteRemote error: ${e.message}")
            }
        }
    }

    suspend fun deleteFavoriteRemote(userId: String, animeId: Long) = withContext(Dispatchers.IO) {
        val key = "favs_$userId"
        val updated = getFavoritesRemote(userId).filter { it.id != animeId }
        val arr = JSONArray()
        for (f in updated) {
            val obj = JSONObject().apply {
                put("userId", f.userId)
                put("id", f.id)
                put("name", f.name)
                put("russianName", f.russianName)
                put("posterUrl", f.posterUrl)
                put("score", f.score)
                put("kind", f.kind)
                put("episodesCount", f.episodesCount)
                put("year", f.year)
                put("category", f.category.name)
                put("updatedAt", f.updatedAt)
            }
            arr.put(obj)
        }
        serverPrefs.edit().putString(key, arr.toString()).apply()

        firestore?.let { db ->
            try {
                db.collection("users").document(userId)
                    .collection("favorites").document(animeId.toString())
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore deleteFavoriteRemote error: ${e.message}")
            }
        }
    }

    suspend fun getFavoritesRemote(userId: String): List<FavoriteAnimeEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<FavoriteAnimeEntity>()
        val raw = serverPrefs.getString("favs_$userId", null)
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val catName = obj.optString("category", FavoriteCategory.WATCHING.name)
                    val cat = try { FavoriteCategory.valueOf(catName) } catch (_: Exception) { FavoriteCategory.WATCHING }
                    list.add(
                        FavoriteAnimeEntity(
                            userId = userId,
                            id = obj.getLong("id"),
                            name = obj.getString("name"),
                            russianName = obj.getString("russianName"),
                            posterUrl = obj.getString("posterUrl"),
                            score = obj.getString("score"),
                            kind = obj.getString("kind"),
                            episodesCount = obj.getInt("episodesCount"),
                            year = obj.getString("year"),
                            category = cat,
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        list
    }

    // --- Watch History Cloud Storage ---

    suspend fun saveWatchHistoryRemote(userId: String, history: WatchHistoryEntity) = withContext(Dispatchers.IO) {
        val key = "history_$userId"
        val existing = getWatchHistoryRemote(userId).filter { it.animeId != history.animeId } + history
        val arr = JSONArray()
        for (h in existing) {
            val obj = JSONObject().apply {
                put("userId", h.userId)
                put("animeId", h.animeId)
                put("name", h.name)
                put("russianName", h.russianName)
                put("posterUrl", h.posterUrl)
                put("episodeNumber", h.episodeNumber)
                put("episodeTitle", h.episodeTitle)
                put("voiceName", h.voiceName)
                put("streamUrl", h.streamUrl)
                put("positionMs", h.positionMs)
                put("durationMs", h.durationMs)
                put("quality", h.quality)
                put("lastWatchedTimestamp", h.lastWatchedTimestamp)
            }
            arr.put(obj)
        }
        serverPrefs.edit().putString(key, arr.toString()).apply()

        firestore?.let { db ->
            try {
                val data = hashMapOf(
                    "userId" to history.userId,
                    "animeId" to history.animeId,
                    "name" to history.name,
                    "russianName" to history.russianName,
                    "posterUrl" to history.posterUrl,
                    "episodeNumber" to history.episodeNumber,
                    "episodeTitle" to history.episodeTitle,
                    "voiceName" to history.voiceName,
                    "streamUrl" to history.streamUrl,
                    "positionMs" to history.positionMs,
                    "durationMs" to history.durationMs,
                    "quality" to history.quality,
                    "lastWatchedTimestamp" to history.lastWatchedTimestamp
                )
                db.collection("users").document(userId)
                    .collection("watch_history").document(history.animeId.toString())
                    .set(data, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore saveWatchHistoryRemote error: ${e.message}")
            }
        }
    }

    suspend fun getWatchHistoryRemote(userId: String): List<WatchHistoryEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<WatchHistoryEntity>()
        val raw = serverPrefs.getString("history_$userId", null)
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        WatchHistoryEntity(
                            userId = userId,
                            animeId = obj.getLong("animeId"),
                            name = obj.getString("name"),
                            russianName = obj.getString("russianName"),
                            posterUrl = obj.getString("posterUrl"),
                            episodeNumber = obj.getInt("episodeNumber"),
                            episodeTitle = obj.optString("episodeTitle", "Серия"),
                            voiceName = obj.optString("voiceName", ""),
                            streamUrl = obj.optString("streamUrl", ""),
                            positionMs = obj.optLong("positionMs", 0L),
                            durationMs = obj.optLong("durationMs", 0L),
                            quality = obj.optString("quality", "1080p"),
                            lastWatchedTimestamp = obj.optLong("lastWatchedTimestamp", System.currentTimeMillis())
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        list
    }

    // --- Player Settings Cloud Storage ---

    suspend fun savePlayerSettingsRemote(userId: String, settings: PlayerSettingsEntity) = withContext(Dispatchers.IO) {
        val obj = JSONObject().apply {
            put("seekStepSeconds", settings.seekStepSeconds)
            put("autoSkip", settings.autoSkip)
            put("defaultSpeed", settings.defaultSpeed)
            put("defaultQuality", settings.defaultQuality)
            put("subtitlesEnabled", settings.subtitlesEnabled)
            put("hardwareAcceleration", settings.hardwareAcceleration)
        }
        serverPrefs.edit().putString("settings_$userId", obj.toString()).apply()

        firestore?.let { db ->
            try {
                val data = hashMapOf(
                    "seekStepSeconds" to settings.seekStepSeconds,
                    "autoSkip" to settings.autoSkip,
                    "defaultSpeed" to settings.defaultSpeed,
                    "defaultQuality" to settings.defaultQuality,
                    "subtitlesEnabled" to settings.subtitlesEnabled,
                    "hardwareAcceleration" to settings.hardwareAcceleration
                )
                db.collection("users").document(userId)
                    .collection("player_settings").document("player")
                    .set(data, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore savePlayerSettingsRemote error: ${e.message}")
            }
        }
    }

    suspend fun getPlayerSettingsRemote(userId: String): PlayerSettingsEntity? = withContext(Dispatchers.IO) {
        val raw = serverPrefs.getString("settings_$userId", null) ?: return@withContext null
        try {
            val obj = JSONObject(raw)
            PlayerSettingsEntity(
                id = 1,
                seekStepSeconds = obj.optInt("seekStepSeconds", 90),
                autoSkip = obj.optBoolean("autoSkip", true),
                defaultSpeed = obj.optDouble("defaultSpeed", 1.0).toFloat(),
                defaultQuality = obj.optString("defaultQuality", "1080p"),
                subtitlesEnabled = obj.optBoolean("subtitlesEnabled", false),
                hardwareAcceleration = obj.optBoolean("hardwareAcceleration", true)
            )
        } catch (_: Exception) {
            null
        }
    }

    // --- Friends Cloud Storage ---

    suspend fun searchUserRemote(query: String): ServerUserProfile? = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext null

        // 1. FAST CHECK: Search in all local registered users in serverPrefs (0ms)
        try {
            val allEntries = serverPrefs.all
            for ((k, v) in allEntries) {
                if (k.startsWith("user_") && !k.startsWith("user_email_") && !k.startsWith("user_id_") && v is String) {
                    try {
                        val obj = JSONObject(v)
                        val uId = obj.optString("userId", "")
                        val email = obj.optString("email", "")
                        val dName = obj.optString("displayName", "")
                        if (uId.equals(clean, ignoreCase = true) ||
                            uId.endsWith(clean, ignoreCase = true) ||
                            email.equals(clean, ignoreCase = true) ||
                            dName.equals(clean, ignoreCase = true) ||
                            dName.contains(clean, ignoreCase = true)
                        ) {
                            return@withContext ServerUserProfile(
                                userId = uId,
                                email = email,
                                displayName = dName,
                                avatarUrl = obj.optString("avatarUrl", ""),
                                authProvider = obj.optString("authProvider", "LOCAL"),
                                sessionToken = obj.optString("sessionToken", ""),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            )
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}

        // 2. FAST CHECK: Query directory announcements from ntfy.sh (<150ms)
        try {
            val dirUrl = "https://ntfy.sh/aniwerti_directory/json?poll=1&since=7d"
            val req = Request.Builder().url(dirUrl).get().build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    for (line in body.lines()) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty()) continue
                        try {
                            val lineObj = JSONObject(trimmed)
                            if (lineObj.optString("event") == "message") {
                                val msg = JSONObject(lineObj.optString("message", "{}"))
                                val uId = msg.optString("userId", "")
                                val dName = msg.optString("displayName", "")
                                val em = msg.optString("email", "")
                                if (uId.equals(clean, ignoreCase = true) ||
                                    uId.endsWith(clean, ignoreCase = true) ||
                                    dName.equals(clean, ignoreCase = true) ||
                                    dName.contains(clean, ignoreCase = true) ||
                                    em.equals(clean, ignoreCase = true)
                                ) {
                                    val profile = ServerUserProfile(
                                        userId = uId,
                                        email = em,
                                        displayName = dName,
                                        avatarUrl = msg.optString("avatarUrl", ""),
                                        authProvider = "CLOUD",
                                        sessionToken = "",
                                        createdAt = System.currentTimeMillis()
                                    )
                                    // Cache locally for next time
                                    saveUserProfileRemote(profile)
                                    return@withContext profile
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Direct ID in cache or remote KV
        getUserProfileRemote(clean)?.let { return@withContext it }
        getUserProfileRemote(clean.uppercase())?.let { return@withContext it }

        // If entered without ANI- prefix, try with ANI-
        if (!clean.startsWith("ANI-", ignoreCase = true)) {
            getUserProfileRemote("ANI-$clean")?.let { return@withContext it }
            val directFromNum = kvGet("user_id_ANI-${clean.uppercase()}")
            if (!directFromNum.isNullOrBlank()) {
                getUserProfileRemote(directFromNum)?.let { return@withContext it }
            }
        }

        // 4. Email lookup
        getUserProfileByEmail(clean)?.let { return@withContext it }
        val emailUserId = kvGet("user_email_${clean.lowercase()}")
        if (!emailUserId.isNullOrBlank()) {
            getUserProfileRemote(emailUserId)?.let { return@withContext it }
        }

        // 5. Search in cloud KV by nickname
        val nickKey = clean.lowercase().replace(" ", "_")
        val cloudUserId = kvGet("nick_$nickKey")
        if (!cloudUserId.isNullOrBlank()) {
            getUserProfileRemote(cloudUserId)?.let { return@withContext it }
        }

        // 6. Firestore search if available
        firestore?.let { db ->
            try {
                val snap = db.collection("users")
                    .whereEqualTo("email", clean.lowercase())
                    .get().await()
                if (!snap.isEmpty) {
                    val doc = snap.documents.first()
                    return@withContext ServerUserProfile(
                        userId = doc.getString("userId") ?: doc.id,
                        email = doc.getString("email") ?: "",
                        displayName = doc.getString("displayName") ?: "",
                        avatarUrl = doc.getString("avatarUrl") ?: "",
                        authProvider = doc.getString("authProvider") ?: "EMAIL",
                        sessionToken = "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore searchUserRemote error: ${e.message}")
            }
        }
        null
    }

    suspend fun saveFriendRemote(userId: String, friend: FriendEntity) = withContext(Dispatchers.IO) {
        val key = "friends_$userId"
        val existing = getFriendsRemote(userId).filter { it.userId != friend.userId } + friend
        val arr = JSONArray()
        for (f in existing) {
            val obj = JSONObject().apply {
                put("ownerUserId", f.ownerUserId)
                put("userId", f.userId)
                put("name", f.name)
                put("avatarUrl", f.avatarUrl)
                put("status", f.status)
                put("isPending", f.isPending)
                put("isIncoming", f.isIncoming)
                put("lastActive", f.lastActive)
            }
            arr.put(obj)
        }
        serverPrefs.edit().putString(key, arr.toString()).apply()

        // Push to cloud KV so the other device receives it in real time
        kvPut("friends_$userId", arr.toString())

        firestore?.let { db ->
            try {
                val data = hashMapOf(
                    "ownerUserId" to friend.ownerUserId,
                    "userId" to friend.userId,
                    "name" to friend.name,
                    "avatarUrl" to friend.avatarUrl,
                    "status" to friend.status,
                    "isPending" to friend.isPending,
                    "isIncoming" to friend.isIncoming,
                    "lastActive" to friend.lastActive
                )
                db.collection("users").document(userId).collection("friends").document(friend.userId)
                    .set(data, SetOptions.merge())
            } catch (e: Exception) {
                Log.w(TAG, "Firestore saveFriendRemote error: ${e.message}")
            }
        }
    }

    suspend fun deleteFriendRemote(userId: String, friendId: String) = withContext(Dispatchers.IO) {
        val key = "friends_$userId"
        val updated = getFriendsRemote(userId).filter { it.userId != friendId }
        val arr = JSONArray()
        for (f in updated) {
            val obj = JSONObject().apply {
                put("ownerUserId", f.ownerUserId)
                put("userId", f.userId)
                put("name", f.name)
                put("avatarUrl", f.avatarUrl)
                put("status", f.status)
                put("isPending", f.isPending)
                put("isIncoming", f.isIncoming)
                put("lastActive", f.lastActive)
            }
            arr.put(obj)
        }
        serverPrefs.edit().putString(key, arr.toString()).apply()
        kvPut("friends_$userId", arr.toString())

        firestore?.let { db ->
            try {
                db.collection("users").document(userId).collection("friends").document(friendId).delete()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore deleteFriendRemote error: ${e.message}")
            }
        }
    }

    suspend fun getFriendsRemote(userId: String): List<FriendEntity> = withContext(Dispatchers.IO) {
        val map = linkedMapOf<String, FriendEntity>()

        // 1. Local cache
        val raw = serverPrefs.getString("friends_$userId", null)
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val uId = obj.getString("userId")
                    map[uId] = FriendEntity(
                        ownerUserId = userId,
                        userId = uId,
                        name = obj.getString("name"),
                        avatarUrl = obj.getString("avatarUrl"),
                        status = obj.optString("status", "В сети"),
                        isPending = obj.optBoolean("isPending", false),
                        isIncoming = obj.optBoolean("isIncoming", false),
                        lastActive = obj.optLong("lastActive", System.currentTimeMillis())
                    )
                }
            } catch (_: Exception) {}
        }

        // 2. Real Cloud KV sync
        val cloudJson = kvGet("friends_$userId")
        if (!cloudJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(cloudJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val uId = obj.getString("userId")
                    map[uId] = FriendEntity(
                        ownerUserId = userId,
                        userId = uId,
                        name = obj.getString("name"),
                        avatarUrl = obj.getString("avatarUrl"),
                        status = obj.optString("status", "В сети"),
                        isPending = obj.optBoolean("isPending", false),
                        isIncoming = obj.optBoolean("isIncoming", false),
                        lastActive = obj.optLong("lastActive", System.currentTimeMillis())
                    )
                }
                serverPrefs.edit().putString("friends_$userId", cloudJson).apply()
            } catch (_: Exception) {}
        }

        // 3. Firestore fallback
        firestore?.let { db ->
            try {
                val snapshot = db.collection("users").document(userId).collection("friends").get().await()
                for (doc in snapshot.documents) {
                    val targetId = doc.getString("userId") ?: doc.id
                    if (!map.containsKey(targetId)) {
                        map[targetId] = FriendEntity(
                            ownerUserId = userId,
                            userId = targetId,
                            name = doc.getString("name") ?: "Друг",
                            avatarUrl = doc.getString("avatarUrl") ?: "",
                            status = doc.getString("status") ?: "В сети",
                            isPending = doc.getBoolean("isPending") ?: false,
                            isIncoming = doc.getBoolean("isIncoming") ?: false,
                            lastActive = doc.getLong("lastActive") ?: System.currentTimeMillis()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore getFriendsRemote error: ${e.message}")
            }
        }

        map.values.toList()
    }

    // --- Chat Messages Remote Storage ---

    data class RemoteChatMessage(
        val id: String,
        val senderId: String,
        val recipientId: String,
        val text: String,
        val timestamp: Long
    )

    fun getConversationKey(u1: String, u2: String): String {
        return if (u1 < u2) "${u1}__${u2}" else "${u2}__${u1}"
    }

    suspend fun saveMessageRemote(senderId: String, recipientId: String, text: String, timestamp: Long): RemoteChatMessage = withContext(Dispatchers.IO) {
        val convKey = getConversationKey(senderId, recipientId)
        val msgId = "${timestamp}_${(1000..9999).random()}"
        val msg = RemoteChatMessage(
            id = msgId,
            senderId = senderId,
            recipientId = recipientId,
            text = text,
            timestamp = timestamp
        )

        val key = "chat_msgs_$convKey"
        val existing = (getMessagesRemote(senderId, recipientId) + msg).takeLast(40)
        val arr = JSONArray()
        for (m in existing) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("senderId", m.senderId)
                put("recipientId", m.recipientId)
                put("text", m.text)
                put("timestamp", m.timestamp)
            }
            arr.put(obj)
        }
        serverPrefs.edit().putString(key, arr.toString()).apply()
        kvPut("chat_$convKey", arr.toString())

        firestore?.let { db ->
            try {
                val data = hashMapOf(
                    "id" to msg.id,
                    "senderId" to msg.senderId,
                    "recipientId" to msg.recipientId,
                    "text" to msg.text,
                    "timestamp" to msg.timestamp
                )
                db.collection("chats").document(convKey).collection("messages").document(msg.id)
                    .set(data, SetOptions.merge())
            } catch (e: Exception) {
                Log.w(TAG, "Firestore saveMessageRemote error: ${e.message}")
            }
        }

        msg
    }

    suspend fun getMessagesRemote(user1: String, user2: String): List<RemoteChatMessage> = withContext(Dispatchers.IO) {
        val convKey = getConversationKey(user1, user2)
        val map = linkedMapOf<String, RemoteChatMessage>()

        // 1. Local messages
        val raw = serverPrefs.getString("chat_msgs_$convKey", null)
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    map[id] = RemoteChatMessage(
                        id = id,
                        senderId = obj.getString("senderId"),
                        recipientId = obj.getString("recipientId"),
                        text = obj.getString("text"),
                        timestamp = obj.getLong("timestamp")
                    )
                }
            } catch (_: Exception) {}
        }

        // 2. Cloud messages
        val cloudJson = kvGet("chat_$convKey")
        if (!cloudJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(cloudJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    map[id] = RemoteChatMessage(
                        id = id,
                        senderId = obj.getString("senderId"),
                        recipientId = obj.getString("recipientId"),
                        text = obj.getString("text"),
                        timestamp = obj.getLong("timestamp")
                    )
                }
                serverPrefs.edit().putString("chat_msgs_$convKey", cloudJson).apply()
            } catch (_: Exception) {}
        }

        // 3. Firestore fallback
        firestore?.let { db ->
            try {
                val snapshot = db.collection("chats").document(convKey).collection("messages").get().await()
                for (doc in snapshot.documents) {
                    val id = doc.getString("id") ?: doc.id
                    if (!map.containsKey(id)) {
                        map[id] = RemoteChatMessage(
                            id = id,
                            senderId = doc.getString("senderId") ?: "",
                            recipientId = doc.getString("recipientId") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore getMessagesRemote error: ${e.message}")
            }
        }

        map.values.sortedBy { it.timestamp }
    }

    companion object {
        @Volatile
        private var INSTANCE: ServerDatabaseService? = null

        fun getInstance(context: Context): ServerDatabaseService {
            return INSTANCE ?: synchronized(this) {
                val instance = ServerDatabaseService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
