package com.example.ui.screens.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.models.ShikimoriAnimeDto
import com.example.data.db.FavoriteAnimeEntity
import com.example.data.db.FavoriteCategory
import com.example.data.db.WatchHistoryEntity
import com.example.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FavoritesTab(val ruTitle: String, val ukTitle: String) {
    ALL("Все", "Всі"),
    WATCHING("Смотрю", "Дивлюсь"),
    PLAN_TO_WATCH("Планирую", "В планах"),
    COMPLETED("Просмотрено", "Переглянуто"),
    DROPPED("Брошено", "Кинуто"),
    HISTORY("История", "Історія");

    val title: String
        get() = if (com.example.data.settings.AppSettingsManager.isUkrainian()) ukTitle else ruTitle
}

data class FavoritesUiState(
    val selectedTab: FavoritesTab = FavoritesTab.ALL,
    val favorites: List<FavoriteAnimeEntity> = emptyList(),
    val history: List<WatchHistoryEntity> = emptyList()
)

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AnimeRepository(application)

    val activeUser = repository.activeUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedTab = MutableStateFlow(FavoritesTab.ALL)
    val selectedTab: StateFlow<FavoritesTab> = _selectedTab.asStateFlow()

    val favorites: StateFlow<List<FavoriteAnimeEntity>> = repository.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<WatchHistoryEntity>> = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: FavoritesTab) {
        _selectedTab.value = tab
    }

    fun removeFavorite(animeId: Long) {
        viewModelScope.launch {
            repository.removeFavoriteDirect(animeId)
        }
    }

    fun setFavoriteCategory(anime: ShikimoriAnimeDto, category: FavoriteCategory) {
        viewModelScope.launch {
            val displayName = anime.russian?.takeIf { it.isNotBlank() } ?: anime.name
            repository.setFavoriteCategoryDirect(
                animeId = anime.id,
                name = anime.name,
                russianName = displayName,
                posterUrl = AnimeRepository.resolveImageUrl(
                    anime.image?.original ?: anime.image?.preview,
                    animeId = anime.id,
                    animeName = anime.name
                ),
                score = anime.score ?: "8.0",
                kind = anime.kind ?: "tv",
                episodesCount = anime.episodes ?: 12,
                year = anime.airedOn?.take(4) ?: "2026",
                category = category
            )
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
