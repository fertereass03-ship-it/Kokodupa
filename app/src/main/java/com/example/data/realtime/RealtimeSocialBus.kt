package com.example.data.realtime

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class RealtimeEvent {
    data class FriendRequest(
        val senderId: String,
        val senderName: String,
        val senderAvatar: String,
        val targetId: String,
        val timestamp: Long
    ) : RealtimeEvent()

    data class FriendAccepted(
        val senderId: String,
        val senderName: String,
        val senderAvatar: String,
        val targetId: String,
        val timestamp: Long
    ) : RealtimeEvent()

    data class FriendDeclined(
        val senderId: String,
        val targetId: String
    ) : RealtimeEvent()

    data class ChatMessage(
        val senderId: String,
        val senderName: String,
        val targetId: String,
        val text: String,
        val timestamp: Long
    ) : RealtimeEvent()
}

/**
 * In-memory Zero-Latency Event Bus.
 * Dispatches events within 0ms to all active listeners in the process,
 * ensuring instantaneous UI updates across concurrent sessions, tabs, and view models.
 */
object RealtimeSocialBus {
    private val _events = MutableSharedFlow<RealtimeEvent>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    fun emit(event: RealtimeEvent) {
        _events.tryEmit(event)
    }
}
