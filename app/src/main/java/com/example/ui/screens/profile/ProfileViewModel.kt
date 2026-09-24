package com.example.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.FavoriteAnimeEntity
import com.example.data.db.FavoriteCategory
import com.example.data.db.PlayerSettingsEntity
import com.example.data.db.UserAccountEntity
import com.example.data.db.WatchHistoryEntity
import com.example.data.repository.AnimeRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WatchedAnimeItem(
    val animeId: Long,
    val title: String,
    val posterUrl: String,
    val subtitle: String,
    val episodeNumber: Int,
    val progress: Float = 1f,
    val isCompleted: Boolean = false
)

data class ProfileStats(
    val watchedAnimeCount: Int = 0,
    val favoritesCount: Int = 0,
    val totalWatchedMinutes: Int = 0
) {
    val formattedHours: String
        get() = when {
            totalWatchedMinutes == 0 -> "0 ч"
            totalWatchedMinutes < 60 -> "$totalWatchedMinutes мин"
            totalWatchedMinutes % 60 == 0 -> "${totalWatchedMinutes / 60} ч"
            else -> String.format(java.util.Locale.US, "%.1f ч", totalWatchedMinutes / 60.0)
        }
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val animeRepository = AnimeRepository(application)
    private val authRepository = AuthRepository(application)

    val currentUser: StateFlow<UserAccountEntity?> = authRepository.activeUser
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    val allAccounts: StateFlow<List<UserAccountEntity>> = authRepository.allAccounts
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = animeRepository.getAllHistory()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val favorites: StateFlow<List<FavoriteAnimeEntity>> = animeRepository.getAllFavorites()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val watchedAnimeList: StateFlow<List<WatchedAnimeItem>> = combine(
        animeRepository.getAllFavorites(),
        animeRepository.getAllHistory()
    ) { favs, history ->
        val list = mutableListOf<WatchedAnimeItem>()
        val historyAnimeIds = mutableSetOf<Long>()

        for (item in history) {
            historyAnimeIds.add(item.animeId)
            val prog = if (item.durationMs > 0) {
                (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 1f
            list.add(
                WatchedAnimeItem(
                    animeId = item.animeId,
                    title = item.russianName.ifBlank { item.name },
                    posterUrl = item.posterUrl,
                    subtitle = "Серия ${item.episodeNumber}",
                    episodeNumber = item.episodeNumber,
                    progress = prog,
                    isCompleted = prog >= 0.9f
                )
            )
        }

        val completed = favs.filter { it.category == FavoriteCategory.COMPLETED }
        for (fav in completed) {
            if (!historyAnimeIds.contains(fav.id)) {
                list.add(
                    WatchedAnimeItem(
                        animeId = fav.id,
                        title = fav.russianName.ifBlank { fav.name },
                        posterUrl = fav.posterUrl,
                        subtitle = "Просмотрено (${fav.episodesCount.coerceAtLeast(1)} эп.)",
                        episodeNumber = fav.episodesCount.coerceAtLeast(1),
                        progress = 1f,
                        isCompleted = true
                    )
                )
            }
        }

        val watching = favs.filter { it.category == FavoriteCategory.WATCHING }
        for (fav in watching) {
            if (!historyAnimeIds.contains(fav.id) && !list.any { it.animeId == fav.id }) {
                list.add(
                    WatchedAnimeItem(
                        animeId = fav.id,
                        title = fav.russianName.ifBlank { fav.name },
                        posterUrl = fav.posterUrl,
                        subtitle = "Смотрю",
                        episodeNumber = 1,
                        progress = 0.5f,
                        isCompleted = false
                    )
                )
            }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playerSettings: StateFlow<PlayerSettingsEntity> = settingsRepository.getPlayerSettings()
        .map {
            it ?: PlayerSettingsEntity(
                id = 1,
                seekStepSeconds = 90,
                autoSkip = true,
                defaultSpeed = 1.0f,
                defaultQuality = "1080p",
                subtitlesEnabled = false
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            PlayerSettingsEntity()
        )

    val stats: StateFlow<ProfileStats> = combine(
        animeRepository.getAllFavorites(),
        animeRepository.getAllHistory()
    ) { favs, history ->
        // 1. Реальное количество просмотренных аниме:
        // Аниме, просмотренные через плеер (в истории) + отмеченные в категорию "Просмотрено"
        val completedInFavs = favs.filter { it.category == FavoriteCategory.COMPLETED }.map { it.id }.toSet()
        val watchedInHistory = history.map { it.animeId }.toSet()
        val allWatchedIds = completedInFavs + watchedInHistory
        val realWatchedCount = allWatchedIds.size

        // 2. Реальное количество аниме в избранном (все категории)
        val realFavoritesCount = favs.size

        // 3. Реальное время просмотра (часы/минуты)
        var totalMins = 0
        val historyMinutesByAnime = mutableMapOf<Long, Int>()
        for (item in history) {
            val prevEpisodesMins = if (item.episodeNumber > 1) (item.episodeNumber - 1) * 24 else 0
            val currEpisodeMins = (item.positionMs / 1000 / 60).toInt()
            val animeMins = prevEpisodesMins + currEpisodeMins
            val currentMax = historyMinutesByAnime[item.animeId] ?: 0
            if (animeMins > currentMax) {
                historyMinutesByAnime[item.animeId] = animeMins
            }
        }
        totalMins += historyMinutesByAnime.values.sum()

        // Для аниме, завершённых вручную и отсутствующих в истории плеера
        for (fav in favs.filter { it.category == FavoriteCategory.COMPLETED }) {
            if (!historyMinutesByAnime.containsKey(fav.id)) {
                totalMins += fav.episodesCount.coerceAtLeast(1) * 24
            }
        }

        ProfileStats(
            watchedAnimeCount = realWatchedCount,
            favoritesCount = realFavoritesCount,
            totalWatchedMinutes = totalMins
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileStats())

    fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        avatarUrl: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = authRepository.registerWithEmail(email, password, displayName, avatarUrl)
            result.onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Ошибка регистрации") }
        }
    }

    fun loginWithEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = authRepository.loginWithEmail(email, password)
            result.onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Ошибка входа") }
        }
    }

    fun loginWithGoogle(
        email: String,
        displayName: String,
        avatarUrl: String?,
        googleId: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = authRepository.loginWithGoogle(
                email = email,
                displayName = displayName,
                avatarUrl = avatarUrl,
                googleId = googleId
            )
            result.onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Ошибка входа через Google") }
        }
    }

    fun switchAccount(userId: String) {
        viewModelScope.launch {
            authRepository.switchAccount(userId)
        }
    }

    fun removeSavedAccount(userId: String) {
        viewModelScope.launch {
            authRepository.removeSavedAccount(userId)
        }
    }

    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete?.invoke()
        }
    }

    fun updateProfile(displayName: String, avatarUrl: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.updateProfile(displayName, avatarUrl)
            onComplete()
        }
    }

    fun updateDisplayName(
        displayName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = authRepository.updateDisplayName(displayName)
            result.onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Ошибка обновления никнейма") }
        }
    }

    fun updateAvatar(
        avatarUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = authRepository.updateAvatar(avatarUrl)
            result.onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Ошибка обновления аватарки") }
        }
    }

    fun updateAutoSkip(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoSkip(enabled)
        }
    }

    fun updateSeekStep(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.updateSeekStep(seconds)
        }
    }

    fun updateDefaultQuality(quality: String) {
        viewModelScope.launch {
            settingsRepository.updateDefaultQuality(quality)
        }
    }

    fun updateDefaultSpeed(speed: Float) {
        viewModelScope.launch {
            settingsRepository.updateDefaultSpeed(speed)
        }
    }
}
