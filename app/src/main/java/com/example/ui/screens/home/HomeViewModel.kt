package com.example.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.models.ShikimoriAnimeDto
import com.example.data.db.FavoriteCategory
import com.example.data.db.WatchHistoryEntity
import com.example.data.repository.AnimeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val bannerAnime: ShikimoriAnimeDto? = null,
    val popularAnimes: List<ShikimoriAnimeDto> = emptyList(),
    val releases2026: List<ShikimoriAnimeDto> = emptyList(),
    val recommendations: List<ShikimoriAnimeDto> = emptyList(),
    val isBannerFavorite: Boolean = false,
    val errorMessage: String? = null,
    val showAuthNotice: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AnimeRepository(application)

    val activeUser = repository.activeUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites = repository.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                coroutineScope {
                    val bannerDeferred = async { runCatching { repository.getDailyBannerAnime() }.getOrNull() }
                    val popularDeferred = async { runCatching { repository.getPopularAnimes(30) }.getOrDefault(emptyList()) }
                    val new2026Deferred = async { runCatching { repository.get2026Releases(30) }.getOrDefault(emptyList()) }
                    val recsDeferred = async { runCatching { repository.getRecommendations(30) }.getOrDefault(emptyList()) }

                    val banner = bannerDeferred.await()
                    val popular = popularDeferred.await()
                    val new2026 = new2026Deferred.await()
                    val recs = recsDeferred.await()

                    val effectiveBanner = banner ?: popular.firstOrNull() ?: new2026.firstOrNull()
                    val hasData = effectiveBanner != null || popular.isNotEmpty() || new2026.isNotEmpty()

                    _uiState.value = HomeUiState(
                        isLoading = false,
                        bannerAnime = effectiveBanner,
                        popularAnimes = popular,
                        releases2026 = new2026,
                        recommendations = recs,
                        isBannerFavorite = false,
                        errorMessage = if (!hasData) "Не удалось загрузить данные. Проверьте подключение к сети." else null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Не удалось загрузить данные"
                )
            }
        }
    }

    fun toggleBannerFavorite() {
        val banner = _uiState.value.bannerAnime ?: return
        if (activeUser.value == null && !repository.isLoggedIn()) {
            _uiState.value = _uiState.value.copy(showAuthNotice = true)
            return
        }
        viewModelScope.launch {
            val displayName = banner.russian?.takeIf { it.isNotBlank() } ?: banner.name
            repository.toggleFavorite(
                animeId = banner.id,
                name = banner.name,
                russianName = displayName,
                posterUrl = AnimeRepository.resolveImageUrl(
                    banner.image?.original ?: banner.image?.preview,
                    animeId = banner.id,
                    animeName = banner.name
                ),
                score = banner.score ?: "8.0",
                kind = banner.kind ?: "tv",
                episodesCount = banner.episodes ?: 12,
                year = banner.airedOn?.take(4) ?: "2026",
                category = FavoriteCategory.WATCHING
            )
            _uiState.value = _uiState.value.copy(isBannerFavorite = !_uiState.value.isBannerFavorite)
        }
    }

    fun setFavoriteCategory(anime: ShikimoriAnimeDto, category: FavoriteCategory) {
        if (activeUser.value == null && !repository.isLoggedIn()) {
            _uiState.value = _uiState.value.copy(showAuthNotice = true)
            return
        }
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

    fun removeFavorite(animeId: Long) {
        viewModelScope.launch {
            repository.removeFavoriteDirect(animeId)
        }
    }

    fun toggleWatched(anime: ShikimoriAnimeDto): Boolean {
        val currentFav = favorites.value.find { it.id == anime.id }
        val isCompleted = currentFav?.category == FavoriteCategory.COMPLETED
        viewModelScope.launch {
            if (isCompleted) {
                repository.removeFavoriteDirect(anime.id)
            } else {
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
                    category = FavoriteCategory.COMPLETED
                )
            }
        }
        return !isCompleted
    }

    fun dismissAuthNotice() {
        _uiState.value = _uiState.value.copy(showAuthNotice = false)
    }
}
