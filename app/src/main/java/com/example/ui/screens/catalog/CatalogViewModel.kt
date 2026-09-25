package com.example.ui.screens.catalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.models.ShikimoriAnimeDto
import com.example.data.api.models.ShikimoriGenreDto
import com.example.data.db.FavoriteAnimeEntity
import com.example.data.db.FavoriteCategory
import com.example.data.db.WatchHistoryEntity
import com.example.data.repository.AnimeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import com.example.data.ai.AiChatMessage
import com.example.data.ai.AniwertiAiAssistant
import com.example.data.ai.MessageSender
import com.example.data.settings.AppSettingsManager
import com.example.util.AppStrings

data class AiChatUiState(
    val isOpen: Boolean = false,
    val isLoading: Boolean = false,
    val messages: List<AiChatMessage> = listOf(
        AiChatMessage(
            sender = MessageSender.AI,
            text = AppStrings.aiAssistantGreeting
        )
    ),
    val currentInput: String = "",
    val attachedImageUri: String? = null,
    val attachedVideoUri: String? = null,
    val isVideoAttachment: Boolean = false,
    val isApiKeyDialogOpen: Boolean = false,
    val apiKeyInput: String = "",
    val hasApiKey: Boolean = false
)

data class CatalogFilterState(
    val query: String = "",
    val selectedGenreIds: Set<Long> = emptySet(),
    val minYear: Int = 1989,
    val maxYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedKind: String? = null, // tv, movie, ova, ona, special, music
    val selectedStatus: String? = null, // ongoing, released, anons
    val selectedOrder: String = "popularity", // popularity, ranked, aired_on, name, episodes
    val minScore: Int? = null,
    val watchStatusFilter: String = "ALL", // legacy fallback
    val selectedTab: String = "ALL" // ALL, NEW, POPULAR, RECOMMENDATIONS
)

data class CatalogUiState(
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val animes: List<ShikimoriAnimeDto> = emptyList(),
    val availableGenres: List<ShikimoriGenreDto> = emptyList(),
    val filter: CatalogFilterState = CatalogFilterState(),
    val isFilterSheetOpen: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true
)

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val isScheduleOpen: Boolean = false,
    val items: List<com.example.data.api.models.ScheduleItem> = emptyList(),
    val selectedDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let { d ->
        when (d) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    },
    val searchQuery: String = "",
    val errorMessage: String? = null
)

class CatalogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AnimeRepository(application)
    private val aiAssistant = AniwertiAiAssistant(application, repository)

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private val _scheduleState = MutableStateFlow(
        ScheduleUiState(
            items = com.example.data.repository.AnimeScheduleData.getRealShikimoriSchedule(),
            isLoading = false
        )
    )
    val scheduleState: StateFlow<ScheduleUiState> = _scheduleState.asStateFlow()

    private val _chatState = MutableStateFlow(AiChatUiState())
    val chatState: StateFlow<AiChatUiState> = _chatState.asStateFlow()

    val favorites: StateFlow<List<FavoriteAnimeEntity>> = repository.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

    init {
        loadInitialData()
    }

    fun setWatchStatusFilter(status: String) {
        val updated = _uiState.value.filter.copy(watchStatusFilter = status)
        _uiState.value = _uiState.value.copy(filter = updated)
    }

    fun selectTab(tab: String) {
        val currentTab = _uiState.value.filter.selectedTab
        if (currentTab == tab && _uiState.value.animes.isNotEmpty() && !_uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(
            filter = _uiState.value.filter.copy(selectedTab = tab)
        )
        loadTabData(tab)
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

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val genres = repository.getGenres()
            _uiState.value = _uiState.value.copy(availableGenres = genres)
            performSearch()
        }
        // Pre-fetch live schedule in background for fresh live ongoings
        viewModelScope.launch {
            try {
                val list = repository.getSchedule(forceRefresh = true)
                if (list.isNotEmpty()) {
                    _scheduleState.value = _scheduleState.value.copy(items = list)
                }
            } catch (_: Exception) {}
        }
    }

    fun onQueryChange(newQuery: String) {
        val currentFilter = _uiState.value.filter.copy(query = newQuery, selectedTab = "ALL")
        _uiState.value = _uiState.value.copy(filter = currentFilter)

        // 400ms Debounce search as requested
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            performSearch()
        }
    }

    fun openFilterSheet() {
        _uiState.value = _uiState.value.copy(isFilterSheetOpen = true)
    }

    fun closeFilterSheet() {
        _uiState.value = _uiState.value.copy(isFilterSheetOpen = false)
    }

    fun updateFilters(newFilter: CatalogFilterState) {
        _uiState.value = _uiState.value.copy(
            filter = newFilter.copy(selectedTab = "ALL"),
            isFilterSheetOpen = false
        )
        performSearch()
    }

    fun resetFilters() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        _uiState.value = _uiState.value.copy(
            filter = CatalogFilterState(
                query = "",
                minYear = 1989,
                maxYear = currentYear,
                selectedTab = "ALL"
            ),
            isFilterSheetOpen = false
        )
        performSearch()
    }

    fun removeGenre(genreId: Long) {
        val newGenres = _uiState.value.filter.selectedGenreIds - genreId
        val updated = _uiState.value.filter.copy(selectedGenreIds = newGenres)
        _uiState.value = _uiState.value.copy(filter = updated)
        performSearch()
    }

    fun removeKind() {
        val updated = _uiState.value.filter.copy(selectedKind = null)
        _uiState.value = _uiState.value.copy(filter = updated)
        performSearch()
    }

    fun removeStatus() {
        val updated = _uiState.value.filter.copy(selectedStatus = null)
        _uiState.value = _uiState.value.copy(filter = updated)
        performSearch()
    }

    fun applyPreset(preset: String) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        when (preset) {
            "anons", "announcements", "upcoming" -> {
                _uiState.value = _uiState.value.copy(isFilterSheetOpen = false)
                selectTab("ANONS")
                return
            }
            "popular", "top_popular" -> {
                _uiState.value = _uiState.value.copy(isFilterSheetOpen = false)
                selectTab("POPULAR")
                return
            }
            "top100", "ranked" -> {
                _uiState.value = _uiState.value.copy(isFilterSheetOpen = false)
                selectTab("RECOMMENDATIONS")
                return
            }
            "releases2026" -> {
                _uiState.value = _uiState.value.copy(isFilterSheetOpen = false)
                selectTab("NEW")
                return
            }
            "releases2024" -> {
                _uiState.value = _uiState.value.copy(
                    filter = CatalogFilterState(
                        query = "",
                        selectedOrder = "popularity",
                        minYear = 2024,
                        maxYear = 2024
                    ),
                    isFilterSheetOpen = false
                )
            }
        }
        performSearch()
    }

    fun selectSingleGenreAndSearch(genreId: Long) {
        val updated = _uiState.value.filter.copy(
            selectedGenreIds = setOf(genreId),
            query = ""
        )
        _uiState.value = _uiState.value.copy(
            filter = updated,
            isFilterSheetOpen = false
        )
        performSearch()
    }

    fun applyGenresAndSearch(genreIds: Set<Long>) {
        val updated = _uiState.value.filter.copy(
            selectedGenreIds = genreIds,
            query = ""
        )
        _uiState.value = _uiState.value.copy(
            filter = updated,
            isFilterSheetOpen = false
        )
        performSearch()
    }

    fun performSearch() {
        val currentTab = _uiState.value.filter.selectedTab
        if (currentTab != "ALL" && _uiState.value.filter.query.isBlank() && _uiState.value.filter.selectedGenreIds.isEmpty()) {
            loadTabData(currentTab)
            return
        }
        loadTabData("ALL")
    }

    fun loadTabData(tab: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSearching = true,
                errorMessage = null,
                currentPage = 1,
                canLoadMore = true
            )
            try {
                val results = when (tab) {
                    "ANONS" -> {
                        val raw = repository.getAnonsAnimes(limit = 100, page = 1)
                        raw.filter { com.example.data.api.AnimeEpisodeHelper.isAnnouncement(it) || it.status.equals("anons", ignoreCase = true) }
                    }
                    "NEW" -> {
                        val raw = repository.get2026Releases(limit = 100, page = 1)
                        raw.filter { !com.example.data.api.AnimeEpisodeHelper.isAnnouncement(it) && !it.status.equals("anons", ignoreCase = true) }
                    }
                    "POPULAR" -> {
                        val raw = repository.getPopularAnimes(limit = 100, page = 1)
                        raw.filter { !com.example.data.api.AnimeEpisodeHelper.isAnnouncement(it) && !it.status.equals("anons", ignoreCase = true) }
                    }
                    "RECOMMENDATIONS" -> {
                        val raw = repository.getRecommendations(limit = 100, page = 1)
                        raw.filter { !com.example.data.api.AnimeEpisodeHelper.isAnnouncement(it) && !it.status.equals("anons", ignoreCase = true) }
                    }
                    else -> {
                        val filter = _uiState.value.filter
                        val genreParam = if (filter.selectedGenreIds.isNotEmpty()) {
                            filter.selectedGenreIds.joinToString(",")
                        } else null
                        val searchResults = repository.searchCatalog(
                            query = filter.query,
                            page = 1,
                            limit = 100,
                            order = filter.selectedOrder,
                            kind = filter.selectedKind,
                            status = filter.selectedStatus,
                            genreIds = genreParam,
                            minYear = filter.minYear,
                            maxYear = filter.maxYear,
                            score = filter.minScore
                        )

                        // When in default "ALL" tab without active filters, prepend the Yani catalog items
                        val combined = if (filter.query.isBlank() && filter.selectedGenreIds.isEmpty() && filter.selectedKind == null && filter.selectedStatus == null) {
                            val yaniItems = com.example.data.api.YaniCatalogService.getCatalog().map { item ->
                                with(com.example.data.api.YaniCatalogService) { item.toShikimoriAnimeDto() }
                            }
                            val yaniIds = yaniItems.map { it.id }.toSet()
                            yaniItems + searchResults.filter { it.id !in yaniIds }
                        } else {
                            searchResults
                        }

                        // Announcements should ONLY appear when user explicitly selected ANONS tab or filtered by status anons
                        if (filter.selectedStatus == null) {
                            combined.filter { !com.example.data.api.AnimeEpisodeHelper.isAnnouncement(it) && !it.status.equals("anons", ignoreCase = true) }
                        } else {
                            combined
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSearching = false,
                    animes = results,
                    errorMessage = null,
                    currentPage = 1,
                    canLoadMore = results.size >= 40
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSearching = false,
                    errorMessage = e.localizedMessage ?: "Ошибка при поиске"
                )
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isSearching || currentState.isLoadingMore || !currentState.canLoadMore) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            val nextPage = currentState.currentPage + 1
            val tab = currentState.filter.selectedTab

            try {
                val nextBatch = when (tab) {
                    "ANONS" -> repository.getAnonsAnimes(limit = 50, page = nextPage + 1)
                        .filter { com.example.data.api.AnimeEpisodeHelper.isAnnouncement(it) || it.status.equals("anons", ignoreCase = true) }
                    "NEW" -> repository.get2026Releases(limit = 50, page = nextPage + 1)
                        .filter { !com.example.data.api.AnimeEpisodeHelper.isAnnouncement(it) && !it.status.equals("anons", ignoreCase = true) }
                    "POPULAR" -> repository.getPopularAnimes(limit = 50, page = nextPage + 1)
                        .filter { !com.example.data.api.AnimeEpisodeHelper.isAnnouncement(it) && !it.status.equals("anons", ignoreCase = true) }
                    "RECOMMENDATIONS" -> repository.getRecommendations(limit = 50, page = nextPage + 1)
                        .filter { !com.example.data.api.AnimeEpisodeHelper.isAnnouncement(it) && !it.status.equals("anons", ignoreCase = true) }
                    else -> {
                        val filter = currentState.filter
                        val genreParam = if (filter.selectedGenreIds.isNotEmpty()) {
                            filter.selectedGenreIds.joinToString(",")
                        } else null
                        val rawMore = repository.searchCatalog(
                            query = filter.query,
                            page = nextPage,
                            limit = 50,
                            order = filter.selectedOrder,
                            kind = filter.selectedKind,
                            status = filter.selectedStatus,
                            genreIds = genreParam,
                            minYear = filter.minYear,
                            maxYear = filter.maxYear,
                            score = filter.minScore
                        )
                        if (filter.selectedStatus == null) {
                            rawMore.filter { !com.example.data.api.AnimeEpisodeHelper.isAnnouncement(it) && !it.status.equals("anons", ignoreCase = true) }
                        } else {
                            rawMore
                        }
                    }
                }

                if (nextBatch.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false, canLoadMore = false)
                } else {
                    val existingIds = currentState.animes.map { it.id }.toSet()
                    val newUnique = nextBatch.filter { it.id !in existingIds }
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        animes = currentState.animes + newUnique,
                        currentPage = nextPage,
                        canLoadMore = nextBatch.size >= 20
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun setFavoriteCategory(anime: ShikimoriAnimeDto, category: com.example.data.db.FavoriteCategory) {
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

    fun openSchedule() {
        _scheduleState.value = _scheduleState.value.copy(isScheduleOpen = true)
        if (_scheduleState.value.items.isEmpty()) {
            loadSchedule(forceRefresh = false)
        }
    }

    fun closeSchedule() {
        _scheduleState.value = _scheduleState.value.copy(isScheduleOpen = false)
    }

    fun loadSchedule(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (_scheduleState.value.items.isEmpty()) {
                _scheduleState.value = _scheduleState.value.copy(isLoading = true, errorMessage = null)
            }
            try {
                val list = repository.getSchedule(forceRefresh = forceRefresh)
                _scheduleState.value = _scheduleState.value.copy(
                    isLoading = false,
                    items = list,
                    errorMessage = if (list.isEmpty()) "Не удалось получить расписание с Shikimori" else null
                )
            } catch (e: Exception) {
                _scheduleState.value = _scheduleState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Ошибка загрузки расписания"
                )
            }
        }
    }

    fun selectScheduleDay(day: Int) {
        _scheduleState.value = _scheduleState.value.copy(selectedDay = day)
    }

    fun onScheduleSearchQueryChange(query: String) {
        _scheduleState.value = _scheduleState.value.copy(searchQuery = query)
    }

    // --- ANIWERTI-помощник (AI Chat) Methods ---
    fun openAiChat() {
        val hasKey = aiAssistant.hasApiKey()
        val currentKey = aiAssistant.getEffectiveApiKey()
        _chatState.value = _chatState.value.copy(
            isOpen = true,
            hasApiKey = hasKey,
            apiKeyInput = currentKey
        )
    }

    fun closeAiChat() {
        _chatState.value = _chatState.value.copy(isOpen = false, isApiKeyDialogOpen = false)
    }

    fun openApiKeyDialog() {
        _chatState.value = _chatState.value.copy(
            isApiKeyDialogOpen = true,
            apiKeyInput = aiAssistant.getEffectiveApiKey()
        )
    }

    fun closeApiKeyDialog() {
        _chatState.value = _chatState.value.copy(isApiKeyDialogOpen = false)
    }

    fun onApiKeyInputChange(input: String) {
        _chatState.value = _chatState.value.copy(apiKeyInput = input)
    }

    fun saveApiKey(key: String) {
        aiAssistant.saveApiKey(key)
        _chatState.value = _chatState.value.copy(
            isApiKeyDialogOpen = false,
            hasApiKey = aiAssistant.hasApiKey(),
            apiKeyInput = aiAssistant.getEffectiveApiKey()
        )
    }

    fun clearApiKey() {
        aiAssistant.saveApiKey("")
        _chatState.value = _chatState.value.copy(
            isApiKeyDialogOpen = false,
            hasApiKey = false,
            apiKeyInput = ""
        )
    }

    fun onChatInputChange(input: String) {
        _chatState.value = _chatState.value.copy(currentInput = input)
    }

    fun onAttachScreenshot(uriString: String?) {
        _chatState.value = _chatState.value.copy(
            attachedImageUri = uriString,
            attachedVideoUri = null,
            isVideoAttachment = false
        )
    }

    fun onAttachVideo(uriString: String?) {
        if (uriString == null) {
            _chatState.value = _chatState.value.copy(
                attachedImageUri = null,
                attachedVideoUri = null,
                isVideoAttachment = false
            )
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val thumb = AniwertiAiAssistant.generateVideoThumbnailUri(getApplication(), uriString)
            _chatState.value = _chatState.value.copy(
                attachedImageUri = thumb ?: uriString,
                attachedVideoUri = uriString,
                isVideoAttachment = true
            )
        }
    }

    fun onRemoveScreenshot() {
        _chatState.value = _chatState.value.copy(
            attachedImageUri = null,
            attachedVideoUri = null,
            isVideoAttachment = false
        )
    }

    fun onRemoveMedia() {
        _chatState.value = _chatState.value.copy(
            attachedImageUri = null,
            attachedVideoUri = null,
            isVideoAttachment = false
        )
    }

    fun sendChatMessage(
        promptText: String? = null,
        overrideImageUri: String? = null,
        overrideVideoUri: String? = null
    ) {
        val isVideo = overrideVideoUri != null || _chatState.value.isVideoAttachment
        val query = (promptText ?: _chatState.value.currentInput).trim()
        val imageToAttach = overrideImageUri ?: _chatState.value.attachedImageUri
        val videoToAttach = overrideVideoUri ?: _chatState.value.attachedVideoUri

        if (query.isBlank() && imageToAttach == null && videoToAttach == null) return

        val isUk = AppSettingsManager.isUkrainian()
        val defaultText = when {
            isVideo -> if (isUk) "🎬 [Пошук аніме за відео]" else "🎬 [Поиск аниме по видео]"
            imageToAttach != null -> if (isUk) "🖼️ [Пошук аніме за скріншотом]" else "🖼️ [Поиск аниме по скриншоту]"
            else -> query
        }

        val userMessage = AiChatMessage(
            sender = MessageSender.USER,
            text = if (query.isNotBlank()) query else defaultText,
            imageUri = imageToAttach,
            videoUri = videoToAttach,
            isVideo = isVideo
        )

        val updatedMessages = _chatState.value.messages + userMessage
        _chatState.value = _chatState.value.copy(
            messages = updatedMessages,
            currentInput = "",
            attachedImageUri = null,
            attachedVideoUri = null,
            isVideoAttachment = false,
            isLoading = true
        )

        viewModelScope.launch {
            try {
                val response = aiAssistant.generateAnswer(
                    userPrompt = query,
                    history = updatedMessages,
                    imageUri = if (!isVideo) imageToAttach else null,
                    videoUri = if (isVideo) videoToAttach else null
                )
                val aiMessage = AiChatMessage(
                    sender = MessageSender.AI,
                    text = response.replyText,
                    recommendedAnimes = response.recommendedAnimes,
                    matchedAnimeId = response.matchedAnimeId,
                    matchedEpisode = response.matchedEpisode,
                    matchedSeasonName = response.matchedSeasonName,
                    matchedTimecodeSeconds = response.matchedTimecodeSeconds,
                    matchedSimilarity = response.matchedSimilarity
                )
                _chatState.value = _chatState.value.copy(
                    messages = _chatState.value.messages + aiMessage,
                    isLoading = false
                )
            } catch (e: Exception) {
                val errorMessage = AiChatMessage(
                    sender = MessageSender.AI,
                    text = if (isUk) "Вибачте, сталася помилка: ${e.localizedMessage ?: "Спробуйте ще раз пізніше."}"
                           else "Извините, произошла ошибка: ${e.localizedMessage ?: "Попробуйте позже."}"
                )
                _chatState.value = _chatState.value.copy(
                    messages = _chatState.value.messages + errorMessage,
                    isLoading = false
                )
            }
        }
    }

    fun clearChatHistory() {
        _chatState.value = _chatState.value.copy(
            messages = listOf(
                AiChatMessage(
                    sender = MessageSender.AI,
                    text = AppStrings.aiAssistantGreeting
                )
            ),
            attachedImageUri = null
        )
    }
}
