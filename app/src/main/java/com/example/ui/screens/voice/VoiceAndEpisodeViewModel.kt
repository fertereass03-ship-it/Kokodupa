package com.example.ui.screens.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.models.AnimeEpisode
import com.example.data.api.models.AnimeVoiceTranslation
import com.example.data.api.models.ShikimoriAnimeDetailDto
import com.example.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VoiceAndEpisodeUiState(
    val isLoading: Boolean = true,
    val anime: ShikimoriAnimeDetailDto? = null,
    val isMovie: Boolean = false,
    val voices: List<AnimeVoiceTranslation> = emptyList(),
    val selectedVoice: AnimeVoiceTranslation? = null,
    val episodes: List<AnimeEpisode> = emptyList(),
    val errorMessage: String? = null
)

class VoiceAndEpisodeViewModel(
    application: Application,
    private val animeId: Long
) : AndroidViewModel(application) {
    private val repository = AnimeRepository(application)

    private val _uiState = MutableStateFlow(VoiceAndEpisodeUiState())
    val uiState: StateFlow<VoiceAndEpisodeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val anime = repository.getAnimeDetails(animeId)
                val isMovie = anime.kind == "movie" || (anime.episodes ?: 0) == 1

                if (com.example.data.api.AnimeEpisodeHelper.isAnnouncement(anime)) {
                    val isUk = com.example.data.settings.AppSettingsManager.isUkrainian()
                    val dateText = com.example.data.api.AnimeEpisodeHelper.formatAnnouncementDate(anime.airedOn, isUk, anime.description)
                    val msg = if (isUk) "Це аніме ще не вийшло (Анонс). $dateText" else "Это аниме ещё не вышло (Анонс). $dateText"
                    _uiState.value = VoiceAndEpisodeUiState(
                        isLoading = false,
                        anime = anime,
                        isMovie = isMovie,
                        voices = emptyList(),
                        selectedVoice = null,
                        episodes = emptyList(),
                        errorMessage = msg
                    )
                    return@launch
                }

                val searchTitle = anime.russian ?: anime.name
                val voices = repository.getVoiceTranslations(animeId, searchTitle, isMovie = isMovie)
                val lastVoice = repository.getLastWatchedVoice(animeId)
                val selected = if (!lastVoice.isNullOrBlank()) {
                    voices.find { it.name.equals(lastVoice, ignoreCase = true) }
                        ?: voices.find { it.name.contains(lastVoice, ignoreCase = true) }
                        ?: voices.firstOrNull()
                } else {
                    voices.firstOrNull()
                }
                val episodes = repository.getEpisodes(
                    animeId = animeId,
                    totalEpisodes = if (isMovie) 1 else (anime.episodes ?: 12),
                    selectedVoice = selected,
                    isMovie = isMovie
                )

                _uiState.value = VoiceAndEpisodeUiState(
                    isLoading = false,
                    anime = anime,
                    isMovie = isMovie,
                    voices = voices,
                    selectedVoice = selected,
                    episodes = episodes,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Ошибка загрузки серий"
                )
            }
        }
    }

    fun selectVoice(voice: AnimeVoiceTranslation) {
        viewModelScope.launch {
            repository.setLastWatchedVoice(animeId, voice.name)
            val currentAnime = _uiState.value.anime
            val isMovie = _uiState.value.isMovie
            val episodes = repository.getEpisodes(
                animeId = animeId,
                totalEpisodes = if (isMovie) 1 else (currentAnime?.episodes ?: 12),
                selectedVoice = voice,
                isMovie = isMovie
            )
            _uiState.value = _uiState.value.copy(
                selectedVoice = voice,
                episodes = episodes
            )
        }
    }

    fun toggleEpisodeWatched(episodeNum: Int) {
        viewModelScope.launch {
            val voice = _uiState.value.selectedVoice ?: return@launch
            val currentEps = _uiState.value.episodes
            val target = currentEps.find { it.number == episodeNum } ?: return@launch
            val newWatched = !target.isWatched
            repository.markVoiceEpisodeWatched(animeId, voice.name, episodeNum, newWatched)
            val episodes = repository.getEpisodes(
                animeId = animeId,
                totalEpisodes = if (_uiState.value.isMovie) 1 else (_uiState.value.anime?.episodes ?: 12),
                selectedVoice = voice,
                isMovie = _uiState.value.isMovie
            )
            _uiState.value = _uiState.value.copy(episodes = episodes)
        }
    }
}
