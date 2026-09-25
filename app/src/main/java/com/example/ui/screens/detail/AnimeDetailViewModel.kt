package com.example.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.models.AnimeVoiceTranslation
import com.example.data.api.models.RelatedAnimeItem
import com.example.data.api.models.ShikimoriAnimeDetailDto
import com.example.data.db.FavoriteAnimeEntity
import com.example.data.db.FavoriteCategory
import com.example.data.db.WatchHistoryEntity
import com.example.data.repository.AnimeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AnimeDetailUiState(
    val isLoading: Boolean = true,
    val anime: ShikimoriAnimeDetailDto? = null,
    val translations: List<AnimeVoiceTranslation> = emptyList(),
    val isTranslationsLoading: Boolean = false,
    val screenshots: List<String> = emptyList(),
    val relatedAnime: List<RelatedAnimeItem> = emptyList(),
    val similarAnime: List<com.example.data.api.models.ShikimoriAnimeDto> = emptyList(),
    val isRelatedExpanded: Boolean = false,
    val selectedScreenshot: String? = null,
    val favorite: FavoriteAnimeEntity? = null,
    val history: WatchHistoryEntity? = null,
    val isCategoryPickerOpen: Boolean = false,
    val showAuthNotice: Boolean = false,
    val pendingCategory: FavoriteCategory? = null,
    val errorMessage: String? = null
)

class AnimeDetailViewModel(
    application: Application,
    private val animeId: Long
) : AndroidViewModel(application) {
    private val repository = AnimeRepository(application)

    val activeUser = repository.activeUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(AnimeDetailUiState())
    val uiState: StateFlow<AnimeDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
        observeLocalData()
    }

    private fun observeLocalData() {
        viewModelScope.launch {
            repository.getFavoriteById(animeId).collect { fav ->
                _uiState.value = _uiState.value.copy(favorite = fav)
            }
        }
        viewModelScope.launch {
            repository.getHistoryForAnime(animeId).collect { hist ->
                _uiState.value = _uiState.value.copy(history = hist)
            }
        }
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val detailsDeferred = async { repository.getAnimeDetails(animeId) }
                val screenshotsDeferred = async { repository.getAnimeScreenshots(animeId) }
                val similarDeferred = async { repository.getSimilarAnime(animeId) }

                val details = detailsDeferred.await()
                val screenshots = screenshotsDeferred.await()
                val similar = similarDeferred.await()

                val title = details.russian?.takeIf { it.isNotBlank() } ?: details.name

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    anime = details,
                    screenshots = screenshots,
                    similarAnime = similar,
                    isTranslationsLoading = true,
                    errorMessage = null
                )

                // Load real Anixart related releases
                viewModelScope.launch {
                    try {
                        val related = repository.getRelatedAnime(animeId, title)
                        _uiState.value = _uiState.value.copy(relatedAnime = related)
                    } catch (e: Exception) {
                        android.util.Log.w("AnimeDetailVM", "Error loading related anime: ${e.message}")
                    }
                }

                val isAnons = com.example.data.api.AnimeEpisodeHelper.isAnnouncement(details)
                if (!isAnons) {
                    val isMovie = details.kind == "movie" || (details.episodes ?: 0) == 1
                    viewModelScope.launch {
                        try {
                            val trans = repository.getVoiceTranslations(animeId, title, isMovie)
                            _uiState.value = _uiState.value.copy(
                                translations = trans,
                                isTranslationsLoading = false
                            )
                        } catch (e: Exception) {
                            _uiState.value = _uiState.value.copy(isTranslationsLoading = false)
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        translations = emptyList(),
                        isTranslationsLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Не удалось загрузить информацию"
                )
            }
        }
    }

    fun toggleRelatedExpanded() {
        _uiState.value = _uiState.value.copy(isRelatedExpanded = !_uiState.value.isRelatedExpanded)
    }

    fun selectScreenshot(url: String?) {
        _uiState.value = _uiState.value.copy(selectedScreenshot = url)
    }

    fun openCategoryPicker() {
        _uiState.value = _uiState.value.copy(isCategoryPickerOpen = true)
    }

    fun selectCategoryWithAuthPrompt(category: FavoriteCategory) {
        if (repository.isLoggedIn()) {
            setFavoriteCategory(category)
        } else {
            // Only if user has no account at all
            _uiState.value = _uiState.value.copy(
                pendingCategory = category,
                showAuthNotice = true,
                isCategoryPickerOpen = false
            )
        }
    }

    fun confirmPendingCategory() {
        val cat = _uiState.value.pendingCategory
        if (cat != null) {
            setFavoriteCategory(cat)
        }
        _uiState.value = _uiState.value.copy(
            pendingCategory = null,
            showAuthNotice = false
        )
    }

    fun dismissAuthNotice() {
        _uiState.value = _uiState.value.copy(
            showAuthNotice = false,
            pendingCategory = null
        )
    }

    fun closeCategoryPicker() {
        _uiState.value = _uiState.value.copy(isCategoryPickerOpen = false)
    }

    fun setFavoriteCategory(category: FavoriteCategory) {
        val anime = _uiState.value.anime ?: return
        viewModelScope.launch {
            val displayName = anime.russian?.takeIf { it.isNotBlank() } ?: anime.name
            val poster = AnimeRepository.resolveImageUrl(
                anime.image?.original ?: anime.image?.preview,
                animeId = anime.id,
                animeName = "${anime.russian ?: ""} ${anime.name}"
            )
            repository.setFavoriteCategoryDirect(
                animeId = anime.id,
                name = anime.name,
                russianName = displayName,
                posterUrl = poster,
                score = anime.score ?: "8.0",
                kind = anime.kind ?: "tv",
                episodesCount = anime.episodes ?: 12,
                year = anime.airedOn?.take(4) ?: "2026",
                category = category
            )
            closeCategoryPicker()
        }
    }

    fun removeFavorite() {
        viewModelScope.launch {
            repository.removeFavoriteDirect(animeId)
            closeCategoryPicker()
        }
    }
}
