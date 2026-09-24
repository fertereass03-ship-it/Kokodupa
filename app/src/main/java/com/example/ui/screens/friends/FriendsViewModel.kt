package com.example.ui.screens.friends

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.FriendEntity
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class FriendsUiState(
    val searchQuery: String = "",
    val searchResult: FriendEntity? = null,
    val isSearching: Boolean = false,
    val isAddDialogOpen: Boolean = false,
    val messageNotice: String? = null
)

class FriendsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SocialRepository(application)
    private var searchJob: Job? = null

    val activeUser = repository.getActiveUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentUserId: StateFlow<String> = repository.getActiveUserFlow()
        .map { it?.userId ?: repository.defaultUserId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.defaultUserId)

    val currentUserName: StateFlow<String> = repository.getActiveUserFlow()
        .map { it?.displayName ?: repository.defaultUserName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.defaultUserName)

    val friends: StateFlow<List<FriendEntity>> = repository.getFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomingRequests: StateFlow<List<FriendEntity>> = repository.getIncomingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outgoingRequests: StateFlow<List<FriendEntity>> = repository.getOutgoingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        // Automatically sync friends, presence and incoming requests frequently (<1.2s)
        viewModelScope.launch {
            while (isActive) {
                try {
                    repository.updateMyPresence()
                    repository.syncFriends()
                } catch (_: Throwable) {}
                delay(1200)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                repository.syncFriends()
            } catch (_: Throwable) {}
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        val clean = query.trim()
        searchJob?.cancel()
        if (clean.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResult = null, messageNotice = null, isSearching = false)
            return
        }
        if (clean.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(200)
                searchUserById()
            }
        }
    }

    fun openAddDialog() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(isAddDialogOpen = true, searchQuery = "", searchResult = null, messageNotice = null)
        refresh()
    }

    fun closeAddDialog() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(isAddDialogOpen = false)
    }

    fun searchUserById() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, messageNotice = null)
            val result = repository.findUserById(query)
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                searchResult = result,
                messageNotice = if (result == null) "Пользователь с таким ID или никнеймом не найден" else null
            )
        }
    }

    fun addFriend(friend: FriendEntity) {
        viewModelScope.launch {
            val success = repository.sendFriendRequest(friend)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    searchResult = friend.copy(isPending = true, isIncoming = false),
                    messageNotice = "Заявка в друзья успешно отправлена!"
                )
            }
            repository.syncFriends()
        }
    }

    fun acceptFriendRequest(userId: String) {
        viewModelScope.launch {
            repository.acceptFriendRequest(userId)
            repository.syncFriends()
        }
    }

    fun declineFriendRequest(userId: String) {
        viewModelScope.launch {
            repository.declineFriendRequest(userId)
            repository.syncFriends()
        }
    }

    fun cancelFriendRequest(userId: String) {
        viewModelScope.launch {
            repository.cancelFriendRequest(userId)
            repository.syncFriends()
        }
    }

    fun removeFriend(userId: String) {
        viewModelScope.launch {
            repository.removeFriend(userId)
            repository.syncFriends()
        }
    }
}

