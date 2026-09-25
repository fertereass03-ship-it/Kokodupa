package com.example.data.realtime

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * High-performance WebSocket connection manager.
 * Connects directly to real-time WebSocket topics (wss://ntfy.sh/<topic>/ws)
 * providing < 50ms latency for chat messages and friend requests without polling.
 */
class NtfyWebSocketManager(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for websockets
        .pingInterval(20, TimeUnit.SECONDS) // automated ping/pong keep-alive
        .build()
) {
    private val TAG = "NtfyWebSocketManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private data class TopicSubscription(
        val topic: String,
        val callback: (JSONObject) -> Unit,
        var webSocket: WebSocket? = null,
        var reconnectJob: Job? = null,
        var isClosed: Boolean = false,
        var retryCount: Int = 0
    )

    private val subscriptions = ConcurrentHashMap<String, TopicSubscription>()

    fun subscribe(topic: String, onEvent: (JSONObject) -> Unit) {
        val existing = subscriptions[topic]
        if (existing != null && !existing.isClosed && existing.webSocket != null) {
            return
        }

        val sub = TopicSubscription(topic = topic, callback = onEvent)
        subscriptions[topic] = sub
        connect(sub)
    }

    fun unsubscribe(topic: String) {
        val sub = subscriptions.remove(topic) ?: return
        sub.isClosed = true
        sub.reconnectJob?.cancel()
        try {
            sub.webSocket?.close(1000, "Normal closure")
        } catch (_: Throwable) {}
        sub.webSocket = null
    }

    fun unsubscribeAll() {
        for (topic in subscriptions.keys) {
            unsubscribe(topic)
        }
    }

    private fun connect(sub: TopicSubscription) {
        if (sub.isClosed) return

        val wsUrl = "wss://ntfy.sh/${sub.topic}/ws"
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        sub.webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected to topic: ${sub.topic}")
                sub.retryCount = 0
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val frame = JSONObject(text)
                    val eventType = frame.optString("event")
                    if (eventType == "message") {
                        val msgStr = frame.optString("message")
                        if (msgStr.isNotBlank()) {
                            val eventJson = JSONObject(msgStr)
                            val evId = frame.optString("id")
                            if (evId.isNotBlank()) {
                                eventJson.put("_eventId", evId)
                            }
                            sub.callback(eventJson)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing WS frame on ${sub.topic}: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed for topic: ${sub.topic}")
                if (!sub.isClosed) {
                    scheduleReconnect(sub)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure for ${sub.topic}: ${t.message}")
                if (!sub.isClosed) {
                    scheduleReconnect(sub)
                }
            }
        })
    }

    private fun scheduleReconnect(sub: TopicSubscription) {
        if (sub.isClosed) return
        sub.reconnectJob?.cancel()
        sub.reconnectJob = scope.launch {
            sub.retryCount = (sub.retryCount + 1).coerceAtMost(10)
            val backoffMs = (sub.retryCount * 1000L).coerceIn(1000L, 8000L)
            delay(backoffMs)
            if (isActive && !sub.isClosed) {
                connect(sub)
            }
        }
    }
}
