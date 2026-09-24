package com.example.ui.screens.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ChatMessageEntity
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ChatUiState(
    val inputText: String = "",
    val isSending: Boolean = false
)

class ChatViewModel(
    application: Application,
    private val friendId: String,
    val friendName: String
) : AndroidViewModel(application) {
    private val repository = SocialRepository(application)

    val friendIdValue: String get() = friendId

    val messages: StateFlow<List<ChatMessageEntity>> = repository.getMessagesForFriend(friendId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friend: StateFlow<com.example.data.db.FriendEntity?> = repository.getFriendFlow(friendId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        repository.setActiveChatFriend(friendId)

        // 1. Initial cold history sync
        viewModelScope.launch {
            try {
                repository.syncRemoteHistory(friendId)
            } catch (_: Throwable) {}
        }

        // 2. Real-time streaming conversation channel for instant (<50ms) message arrival
        viewModelScope.launch {
            val myId = repository.getCurrentUserId()
            while (isActive) {
                try {
                    repository.streamConversation(myId, friendId)
                } catch (_: Throwable) {
                    delay(1500)
                }
            }
        }

        // 3. Ultra-fast fallback polling loop (600ms)
        viewModelScope.launch {
            while (isActive) {
                try {
                    repository.syncMessagesWithFriend(friendId)
                } catch (_: Throwable) {}
                delay(600)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.clearActiveChatFriend(friendId)
    }

    fun onInputTextChange(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        _uiState.value = _uiState.value.copy(inputText = "")
        viewModelScope.launch {
            repository.sendMessage(friendId, text)
            repository.syncMessagesWithFriend(friendId)
        }
    }
}

