package com.example.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.data.ai.AiChatMessage
import com.example.data.ai.MessageSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.example.data.settings.AppThemeMode
import com.example.util.AvatarManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.data.api.AnimeTitleHelper
import com.example.data.db.FavoriteCategory
import com.example.data.api.models.ShikimoriAnimeDto
import com.example.ui.components.AnimeCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.QuickCategoryBottomSheet
import com.example.ui.components.SkeletonGrid
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.LocalAppColors
import com.example.data.settings.AppSettingsManager
import com.example.util.AppStrings
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    onAnimeClick: (Long) -> Unit,
    onPlayEpisodeClick: ((animeId: Long, episode: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scheduleState by viewModel.scheduleState.collectAsStateWithLifecycle()
    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.watchHistory.collectAsStateWithLifecycle()
    val scheduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val chatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var selectedAnimeForCategory by remember { mutableStateOf<ShikimoriAnimeDto?>(null) }

    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()

    val displayedAnimes = state.animes

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // --- Top Bar with Search, Filter and AI buttons ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.catalogTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 22.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // ИИ-помощник кнопка (гармоничная, не деформируется)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(AccentOrange, PrimaryYellow)
                            )
                        )
                        .clickable { viewModel.openAiChat() }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("catalog_ai_assistant_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = AppStrings.aiAssistantName,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = AppStrings.aiAssistantBtn,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Input Field
                TextField(
                    value = state.filter.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    placeholder = {
                        Text(
                            text = AppStrings.searchPlaceholder,
                            color = colors.textMuted,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = PrimaryYellow
                        )
                    },
                    trailingIcon = {
                        if (state.filter.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Очистить",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        viewModel.performSearch()
                    }),
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("catalog_search_input")
                        .border(1.dp, colors.primary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Filter Button with active badge
                val hasActiveFilters = state.filter.selectedGenreIds.isNotEmpty() ||
                        state.filter.selectedKind != null ||
                        state.filter.selectedStatus != null ||
                        state.filter.minYear > 1989

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (hasActiveFilters) colors.primary else colors.surface)
                        .border(1.dp, colors.primary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .clickable { viewModel.openFilterSheet() }
                        .testTag("catalog_filter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = AppStrings.filterTitle,
                        tint = if (hasActiveFilters) Color.Black else colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Schedule Button (Розклад)
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (scheduleState.isScheduleOpen) colors.primary else colors.surface)
                        .border(1.dp, colors.primary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .clickable { viewModel.openSchedule() }
                        .testTag("catalog_schedule_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = AppStrings.scheduleTab,
                        tint = if (scheduleState.isScheduleOpen) Color.Black else colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Category Tabs Row: Всі / Новинки / Популярне / Рекомендації
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data class CatalogTabItem(
                    val key: String,
                    val label: String,
                    val icon: androidx.compose.ui.graphics.vector.ImageVector
                )
                val tabOptions = listOf(
                    CatalogTabItem("ALL", AppStrings.filterAll, Icons.Default.GridView),
                    CatalogTabItem("POPULAR", AppStrings.filterPopular, Icons.Default.LocalFireDepartment),
                    CatalogTabItem("NEW", AppStrings.filterNew, Icons.Default.NewReleases),
                    CatalogTabItem("RECOMMENDATIONS", AppStrings.filterRecommendations, Icons.Default.AutoAwesome),
                    CatalogTabItem("ANONS", AppStrings.filterAnons, Icons.Default.CalendarMonth)
                )
                tabOptions.forEach { tab ->
                    val isSelected = state.filter.selectedTab == tab.key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) {
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(Color(0xFFFF8F00), Color(0xFFFFC400))
                                    )
                                } else {
                                    androidx.compose.ui.graphics.SolidColor(colors.surface)
                                }
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFFFF8F00) else Color(0xFFFFB300).copy(alpha = 0.55f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.selectTab(tab.key) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("catalog_tab_${tab.key.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.Black else Color(0xFFFFB300),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = tab.label,
                                color = if (isSelected) Color.Black else Color(0xFFFFB300),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // --- Active Filter Chips Bar (when any filter is active) ---
        val hasActiveFilterPills = state.filter.selectedGenreIds.isNotEmpty() ||
                state.filter.selectedKind != null ||
                state.filter.selectedStatus != null ||
                state.filter.selectedOrder != "popularity"

        if (hasActiveFilterPills) {
            ActiveFilterChipsRow(
                filter = state.filter,
                availableGenres = state.availableGenres,
                onRemoveGenre = { viewModel.removeGenre(it) },
                onRemoveKind = { viewModel.removeKind() },
                onRemoveStatus = { viewModel.removeStatus() },
                onResetOrder = { viewModel.updateFilters(state.filter.copy(selectedOrder = "popularity")) },
                onClearAll = { viewModel.resetFilters() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // --- Catalog Grid or States ---
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading || state.isSearching -> {
                    SkeletonGrid()
                }

                state.errorMessage != null -> {
                    ErrorStateView(
                        message = state.errorMessage.orEmpty(),
                        onRetry = { viewModel.performSearch() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                displayedAnimes.isEmpty() -> {
                    val subtitle = when {
                        state.filter.selectedTab == "POPULAR" ->
                            if (isUk) "Не вдалося завантажити популярне" else "Не удалось загрузить популярное"
                        state.filter.selectedTab == "RECOMMENDATIONS" ->
                            if (isUk) "Не вдалося завантажити рекомендації" else "Не удалось загрузить рекомендации"
                        state.filter.selectedTab == "NEW" ->
                            if (isUk) "Не вдалося завантажити новинки" else "Не удалось загрузить новинки"
                        state.filter.selectedTab == "ANONS" ->
                            if (isUk) "Не вдалося завантажити анонси" else "Не удалось загрузить анонсы"
                        state.filter.query.isNotBlank() && state.filter.selectedGenreIds.isNotEmpty() ->
                            if (isUk) "За запитом «${state.filter.query}» у вибраних жанрах нічого не знайдено"
                            else "По запросу «${state.filter.query}» в выбранных жанрах ничего не найдено"
                        state.filter.query.isNotBlank() ->
                            if (isUk) "За запитом «${state.filter.query}» нічого не знайдено"
                            else "По запросу «${state.filter.query}» ничего не найдено"
                        state.filter.selectedGenreIds.isNotEmpty() ->
                            if (isUk) "За вибраними жанрами нічого не знайдено"
                            else "По выбранным жанрам ничего не найдено"
                        else ->
                            if (isUk) "За вибраними фільтрами немає результатів"
                            else "По выбранным фильтрам нет результатов"
                    }
                    EmptyStateView(
                        title = AppStrings.nothingFound,
                        subtitle = subtitle,
                        actionTitle = AppStrings.resetFilters,
                        onAction = { viewModel.resetFilters() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    val gridState = rememberLazyGridState()

                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val totalItems = gridState.layoutInfo.totalItemsCount
                            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            totalItems > 0 && lastVisibleIndex >= totalItems - 6
                        }
                    }

                    LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore && !state.isLoading && !state.isSearching && !state.isLoadingMore && state.canLoadMore) {
                            viewModel.loadMore()
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isUk) "Знайдено аніме: ${displayedAnimes.size}" else "Найдено аниме: ${displayedAnimes.size}",
                                color = colors.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (state.isLoadingMore) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = colors.primary,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = if (isUk) "Завантаження ще..." else "Загрузка еще...",
                                        color = colors.primary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(displayedAnimes, key = { it.id }) { anime ->
                                val fav = favorites.find { it.id == anime.id }
                                val hist = history.find { it.animeId == anime.id }
                                AnimeCard(
                                    anime = anime,
                                    onClick = { onAnimeClick(anime.id) },
                                    onLongClick = { selectedAnimeForCategory = anime },
                                    watchProgress = hist?.progressPercent,
                                    isWatched = fav?.category == FavoriteCategory.COMPLETED,
                                    category = fav?.category,
                                    onToggleWatched = {
                                        val nowWatched = viewModel.toggleWatched(anime)
                                        val title = AnimeTitleHelper.getDisplayTitle(anime)
                                        val msg = if (nowWatched) AppStrings.markedWatchedToast else AppStrings.markedUnwatchedToast
                                        Toast.makeText(context, "$title: $msg", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (state.isLoadingMore) {
                                item(span = { GridItemSpan(2) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = colors.primary,
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Filter Bottom Sheet ---
    if (state.isFilterSheetOpen) {
        FilterBottomSheet(
            currentFilter = state.filter,
            availableGenres = state.availableGenres,
            onApply = { viewModel.updateFilters(it) },
            onApplyAndSearchGenres = { genreIds -> viewModel.applyGenresAndSearch(genreIds) },
            onReset = { viewModel.resetFilters() },
            onDismiss = { viewModel.closeFilterSheet() }
        )
    }

    // --- Quick Category Bottom Sheet on Long Click ---
    if (selectedAnimeForCategory != null) {
        val selected = selectedAnimeForCategory!!
        val animeName = selected.russian?.takeIf { it.isNotBlank() } ?: selected.name
        QuickCategoryBottomSheet(
            anime = selected,
            onDismiss = { selectedAnimeForCategory = null },
            onCategorySelected = { category ->
                viewModel.setFavoriteCategory(selected, category)
                Toast.makeText(context, "«$animeName» добавлено в «${category.displayName}»", Toast.LENGTH_SHORT).show()
            },
            onRemoveFavorite = {
                viewModel.removeFavorite(selected.id)
                Toast.makeText(context, "«$animeName» удалено из списков", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Anime Schedule Sheet
    if (scheduleState.isScheduleOpen) {
        AnimeScheduleSheet(
            state = scheduleState,
            sheetState = scheduleSheetState,
            onDismiss = { viewModel.closeSchedule() },
            onSelectDay = { viewModel.selectScheduleDay(it) },
            onSearchChange = { viewModel.onScheduleSearchQueryChange(it) },
            onRetry = { viewModel.loadSchedule(forceRefresh = true) },
            onAnimeClick = { animeId ->
                viewModel.closeSchedule()
                onAnimeClick(animeId)
            }
        )
    }

    // ANIWERTI-помощник (AI Chat) Sheet
    if (chatState.isOpen) {
        AiChatBottomSheet(
            state = chatState,
            sheetState = chatSheetState,
            onDismiss = { viewModel.closeAiChat() },
            onInputChange = { viewModel.onChatInputChange(it) },
            onAttachScreenshot = { viewModel.onAttachScreenshot(it) },
            onAttachVideo = { viewModel.onAttachVideo(it) },
            onRemoveMedia = { viewModel.onRemoveMedia() },
            onSendMessage = { prompt, imgUri, vidUri -> viewModel.sendChatMessage(prompt, imgUri, vidUri) },
            onClearHistory = { viewModel.clearChatHistory() },
            onOpenApiKeyDialog = { viewModel.openApiKeyDialog() },
            onCloseApiKeyDialog = { viewModel.closeApiKeyDialog() },
            onApiKeyInputChange = { viewModel.onApiKeyInputChange(it) },
            onSaveApiKey = { viewModel.saveApiKey(it) },
            onClearApiKey = { viewModel.clearApiKey() },
            onAnimeClick = { animeId ->
                viewModel.closeAiChat()
                onAnimeClick(animeId)
            },
            onPlayEpisodeClick = { animeId, episodeNum ->
                viewModel.closeAiChat()
                onPlayEpisodeClick?.invoke(animeId, episodeNum) ?: onAnimeClick(animeId)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterBottomSheet(
    currentFilter: CatalogFilterState,
    availableGenres: List<com.example.data.api.models.ShikimoriGenreDto>,
    onApply: (CatalogFilterState) -> Unit,
    onApplyAndSearchGenres: (Set<Long>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var filterDraft by remember { mutableStateOf(currentFilter) }
    var yearRange by remember {
        mutableStateOf(filterDraft.minYear.toFloat()..filterDraft.maxYear.toFloat())
    }
    var isGenrePickerOpen by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCard,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Фільтри каталогу",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Скинути всі",
                    color = AccentOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onReset() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- 1. Сортування (Uniform 2-column grid, equal sizes, no overlapping) ---
            FilterSectionTitle(title = "Сортування")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UniformFilterCell(
                        text = "За популярністю",
                        selected = filterDraft.selectedOrder == "popularity",
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedOrder = "popularity") }
                    )
                    UniformFilterCell(
                        text = "За рейтингом",
                        selected = filterDraft.selectedOrder == "ranked",
                        icon = Icons.Default.Star,
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedOrder = "ranked") }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UniformFilterCell(
                        text = "За датою виходу",
                        selected = filterDraft.selectedOrder == "aired_on",
                        icon = Icons.Default.CalendarToday,
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedOrder = "aired_on") }
                    )
                    UniformFilterCell(
                        text = "За назвою (А-Я)",
                        selected = filterDraft.selectedOrder == "name",
                        icon = Icons.Default.SortByAlpha,
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedOrder = "name") }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UniformFilterCell(
                        text = "За серіями",
                        selected = filterDraft.selectedOrder == "episodes",
                        icon = Icons.Default.VideoLibrary,
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedOrder = "episodes") }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- 2. Тип аніме (ТВ-Серіал, Фільм, OVA, ONA, Спешл - 3 columns, exactly equal sizes) ---
            FilterSectionTitle(title = "Тип аніме")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UniformFilterCell(
                        text = "Всі типи",
                        selected = filterDraft.selectedKind == null,
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedKind = null) }
                    )
                    UniformFilterCell(
                        text = "ТВ-Серіал",
                        selected = filterDraft.selectedKind == "tv",
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedKind = "tv") }
                    )
                    UniformFilterCell(
                        text = "Фільм",
                        selected = filterDraft.selectedKind == "movie",
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedKind = "movie") }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UniformFilterCell(
                        text = "OVA",
                        selected = filterDraft.selectedKind == "ova",
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedKind = "ova") }
                    )
                    UniformFilterCell(
                        text = "ONA",
                        selected = filterDraft.selectedKind == "ona",
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedKind = "ona") }
                    )
                    UniformFilterCell(
                        text = "Спешл",
                        selected = filterDraft.selectedKind == "special",
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedKind = "special") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- 3. Статус виходу (2 columns, equal sizes) ---
            FilterSectionTitle(title = "Статус виходу")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UniformFilterCell(
                        text = "Всі статуси",
                        selected = filterDraft.selectedStatus == null,
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedStatus = null) }
                    )
                    UniformFilterCell(
                        text = "Онґоінґ (виходить)",
                        selected = filterDraft.selectedStatus == "ongoing",
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedStatus = "ongoing") }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UniformFilterCell(
                        text = "Завершено",
                        selected = filterDraft.selectedStatus == "released",
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedStatus = "released") }
                    )
                    UniformFilterCell(
                        text = "Анонс",
                        selected = filterDraft.selectedStatus == "anons",
                        modifier = Modifier.weight(1f),
                        onClick = { filterDraft = filterDraft.copy(selectedStatus = "anons") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- 4. Рік виходу ---
            FilterSectionTitle(title = "Рік виходу: ${yearRange.start.toInt()} — ${yearRange.endInclusive.toInt()}")
            RangeSlider(
                value = yearRange,
                onValueChange = { range ->
                    yearRange = range
                    filterDraft = filterDraft.copy(
                        minYear = range.start.toInt(),
                        maxYear = range.endInclusive.toInt()
                    )
                },
                valueRange = 1989f..currentYear.toFloat(),
                steps = (currentYear - 1989 - 1).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryYellow,
                    activeTrackColor = PrimaryYellow,
                    inactiveTrackColor = SurfaceVariantDark
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // --- 5. Жанри аніме ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Жанри аніме (${filterDraft.selectedGenreIds.size} обрано)",
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                if (filterDraft.selectedGenreIds.isNotEmpty()) {
                    Text(
                        text = "Очистити жанри",
                        color = Color(0xFFFF6B6B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                filterDraft = filterDraft.copy(selectedGenreIds = emptySet())
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Кнопка відкриття повного каталогу жанрів
            Button(
                onClick = { isGenrePickerOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_genre_picker_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceVariantDark,
                    contentColor = PrimaryYellow
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryYellow.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (filterDraft.selectedGenreIds.isEmpty()) 
                        "Відкрити список жанрів (${availableGenres.size})" 
                    else 
                        "Змінити жанри (${filterDraft.selectedGenreIds.size} обрано)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Швидкий вибір популярних жанрів:",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableGenres, key = { it.id }) { genre ->
                    val isSelected = filterDraft.selectedGenreIds.contains(genre.id)
                    val genreName = genre.russian?.takeIf { it.isNotBlank() } ?: genre.name
                    UniformFilterChip(
                        text = genreName,
                        selected = isSelected,
                        onClick = {
                            val newSet = filterDraft.selectedGenreIds.toMutableSet()
                            if (isSelected) newSet.remove(genre.id) else newSet.add(genre.id)
                            filterDraft = filterDraft.copy(selectedGenreIds = newSet)
                        }
                    )
                }
            }

            // Діалог вибору жанрів з пошуком та миттєвим застосуванням
            if (isGenrePickerOpen) {
                GenreSelectionDialog(
                    availableGenres = availableGenres,
                    selectedGenreIds = filterDraft.selectedGenreIds,
                    onDismiss = { isGenrePickerOpen = false },
                    onSaveToDraft = { updatedSelection ->
                        filterDraft = filterDraft.copy(selectedGenreIds = updatedSelection)
                        isGenrePickerOpen = false
                    },
                    onApplyAndSearch = { updatedSelection ->
                        isGenrePickerOpen = false
                        onApplyAndSearchGenres(updatedSelection)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Apply Button ---
            Button(
                onClick = { onApply(filterDraft) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("filter_apply_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryYellow,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Застосувати фільтри", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun GenreSelectionDialog(
    availableGenres: List<com.example.data.api.models.ShikimoriGenreDto>,
    selectedGenreIds: Set<Long>,
    onDismiss: () -> Unit,
    onSaveToDraft: (Set<Long>) -> Unit,
    onApplyAndSearch: (Set<Long>) -> Unit
) {
    var tempSelection by remember { mutableStateOf(selectedGenreIds) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredGenres = remember(availableGenres, searchQuery) {
        if (searchQuery.isBlank()) {
            availableGenres
        } else {
            val q = searchQuery.trim().lowercase()
            availableGenres.filter { genre ->
                val uk = (genre.russian ?: "").lowercase()
                val en = genre.name.lowercase()
                uk.contains(q) || en.contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { onDismiss() }
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceCard)
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) { }
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Жанри аніме",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (tempSelection.isNotEmpty()) {
                        Text(
                            text = "Скинути (${tempSelection.size})",
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { tempSelection = emptySet() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрити",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Пошук по жанрах
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Пошук жанру...", color = TextMuted, fontSize = 14.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистити", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantDark,
                        unfocusedContainerColor = SurfaceVariantDark,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Знайдено: ${filteredGenres.size} • Обрано: ${tempSelection.size}",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Прокручуваний список жанрів
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .height(350.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceVariantDark.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredGenres, key = { it.id }) { genre ->
                        val isSelected = tempSelection.contains(genre.id)
                        val genreName = genre.russian?.takeIf { it.isNotBlank() } ?: genre.name

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryYellow.copy(alpha = 0.12f) else Color(0x0AFFFFFF))
                                .border(
                                    width = if (isSelected) 1.dp else 0.5.dp,
                                    color = if (isSelected) PrimaryYellow.copy(alpha = 0.65f) else Color(0x15FFFFFF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    val newSet = tempSelection.toMutableSet()
                                    if (isSelected) newSet.remove(genre.id) else newSet.add(genre.id)
                                    tempSelection = newSet
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = genreName,
                                    color = if (isSelected) PrimaryYellow else TextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (genre.name != genreName) {
                                    Text(
                                        text = genre.name,
                                        color = TextMuted,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }

                            // Minimalist, subtle quick search icon
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { onApplyAndSearch(setOf(genre.id)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Шукати цей жанр",
                                    tint = TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Minimalist check indicator
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (isSelected) PrimaryYellow else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) PrimaryYellow else Color(0x33FFFFFF),
                                        RoundedCornerShape(5.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопки: Скасувати, Зберегти вибір та Застосувати й шукати одразу
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
                    ) {
                        Text("Скасувати", color = TextSecondary, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { onSaveToDraft(tempSelection) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryYellow.copy(alpha = 0.5f))
                    ) {
                        Text("Зберегти", color = PrimaryYellow, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { onApplyAndSearch(tempSelection) },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryYellow,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (tempSelection.isEmpty()) "Показати всі" else "Шукати (${tempSelection.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFilterChipsRow(
    filter: CatalogFilterState,
    availableGenres: List<com.example.data.api.models.ShikimoriGenreDto>,
    onRemoveGenre: (Long) -> Unit,
    onRemoveKind: () -> Unit,
    onRemoveStatus: () -> Unit,
    onResetOrder: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selected genres
        items(filter.selectedGenreIds.toList(), key = { "genre_$it" }) { genreId ->
            val genreObj = availableGenres.find { it.id == genreId }
            val name = genreObj?.russian ?: genreObj?.name ?: "Жанр #$genreId"
            FilterPill(
                text = name,
                onRemove = { onRemoveGenre(genreId) }
            )
        }

        // Kind
        if (filter.selectedKind != null) {
            val kindLabel = when (filter.selectedKind) {
                "tv" -> "ТВ-Серіал"
                "movie" -> "Фільм"
                "ova" -> "OVA"
                "ona" -> "ONA"
                "special" -> "Спешл"
                else -> filter.selectedKind
            }
            item(key = "kind") {
                FilterPill(
                    text = kindLabel,
                    onRemove = onRemoveKind
                )
            }
        }

        // Status
        if (filter.selectedStatus != null) {
            val statusLabel = when (filter.selectedStatus) {
                "ongoing" -> "Онґоінґ"
                "released" -> "Завершено"
                "anons" -> "Анонс"
                else -> filter.selectedStatus
            }
            item(key = "status") {
                FilterPill(
                    text = statusLabel,
                    onRemove = onRemoveStatus
                )
            }
        }

        // Order if not popularity
        if (filter.selectedOrder != "popularity") {
            val orderLabel = when (filter.selectedOrder) {
                "ranked" -> "За рейтингом"
                "aired_on" -> "За датою"
                "name" -> "За назвою"
                "episodes" -> "За серіями"
                else -> filter.selectedOrder
            }
            item(key = "order") {
                FilterPill(
                    text = orderLabel,
                    onRemove = onResetOrder
                )
            }
        }

        // Clear all button
        item(key = "clear_all") {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33FF6B6B))
                    .border(1.dp, Color(0x66FF6B6B), RoundedCornerShape(8.dp))
                    .clickable { onClearAll() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Скинути все",
                        tint = Color(0xFFFF8E8E),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Скинути все",
                        color = Color(0xFFFF8E8E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PrimaryYellow.copy(alpha = 0.2f))
            .border(1.dp, PrimaryYellow, RoundedCornerShape(8.dp))
            .clickable { onRemove() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                color = PrimaryYellow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Видалити",
                tint = PrimaryYellow,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun UniformFilterCell(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PrimaryYellow else SurfaceVariantDark)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) PrimaryYellow else Color(0x2BFFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.Black else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = if (selected) Color.Black else TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun UniformFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PrimaryYellow else SurfaceVariantDark)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) PrimaryYellow else Color(0x2BFFFFFF),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        color = TextSecondary,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun CustomChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PrimaryYellow else SurfaceVariantDark)
            .border(0.5.dp, if (selected) PrimaryYellow else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else TextPrimary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// --- ANIWERTI-помощник (Мини-чат) Bottom Sheet ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiChatBottomSheet(
    state: AiChatUiState,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onInputChange: (String) -> Unit,
    onAttachScreenshot: (String?) -> Unit,
    onAttachVideo: (String?) -> Unit,
    onRemoveMedia: () -> Unit,
    onSendMessage: (String?, String?, String?) -> Unit,
    onClearHistory: () -> Unit,
    onOpenApiKeyDialog: () -> Unit,
    onCloseApiKeyDialog: () -> Unit,
    onApiKeyInputChange: (String) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onAnimeClick: (Long) -> Unit,
    onPlayEpisodeClick: (Long, Int) -> Unit
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()
    val settingsState by AppSettingsManager.settingsState.collectAsState()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            onAttachScreenshot(it.toString())
            onSendMessage(null, it.toString(), null)
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onAttachVideo(it.toString()) }
    }

    // Auto-scroll to bottom on new message
    androidx.compose.runtime.LaunchedEffect(state.messages.size, state.isLoading) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // Dialog for Gemini API Key setup
    if (state.isApiKeyDialogOpen) {
        Dialog(
            onDismissRequest = onCloseApiKeyDialog,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isUk) "Налаштування Gemini AI" else "Настройка Gemini AI",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                            Text(
                                text = if (state.hasApiKey) (if (isUk) "✅ Ключ підключено" else "✅ Ключ подключен")
                                       else (if (isUk) "Опціонально (для безлімітного ШІ)" else "Опционально (для безлимитного ИИ)"),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (state.hasApiKey) colors.primary else colors.textMuted
                                )
                            )
                        }
                    }

                    Text(
                        text = if (isUk) {
                            "Підключіть свій безкоштовний API ключ Google AI Studio (Gemini) для 100% точного розпізнавання відео, скріншотів, персонажів та детальних відповідей на будь-які запитання."
                        } else {
                            "Подключите свой бесплатный API ключ Google AI Studio (Gemini) для 100% точного распознавания видео, скриншотов, персонажей и подробных ответов на любые вопросы."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.textSecondary,
                            lineHeight = 18.sp
                        )
                    )

                    OutlinedTextField(
                        value = state.apiKeyInput,
                        onValueChange = onApiKeyInputChange,
                        placeholder = { Text("AIzaSy...", color = colors.textMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.primary.copy(alpha = 0.25f),
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.hasApiKey) {
                            Button(
                                onClick = onClearApiKey,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x22FF5252),
                                    contentColor = Color(0xFFFF5252)
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isUk) "Видалити" else "Удалить", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = {
                                if (state.apiKeyInput.isNotBlank()) {
                                    onSaveApiKey(state.apiKeyInput.trim())
                                } else {
                                    onCloseApiKeyDialog()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isUk) "Зберегти" else "Сохранить", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.92f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
        ) {
            // Header: Title & Actions (including Theme Toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(colors.secondary, colors.primary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = AppStrings.aiAssistantName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.primary.copy(alpha = 0.18f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (state.hasApiKey) "Gemini Flash AI" else "Smart Online",
                                    color = colors.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = if (isUk) "Пошук за відео та скріншотами, відповіді на питання" else "Поиск по видео и скриншотам, ответы на вопросы",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.textMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Theme Switcher button (Dark / Original Dark / Light)
                    IconButton(
                        onClick = {
                            val nextTheme = when (settingsState.themeMode) {
                                AppThemeMode.DARK -> AppThemeMode.ORIGINAL_DARK
                                AppThemeMode.ORIGINAL_DARK -> AppThemeMode.LIGHT
                                AppThemeMode.LIGHT -> AppThemeMode.DARK
                            }
                            AppSettingsManager.setThemeMode(nextTheme)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        val (icon, tint) = when (settingsState.themeMode) {
                            AppThemeMode.DARK -> Icons.Default.NightlightRound to colors.primary
                            AppThemeMode.ORIGINAL_DARK -> Icons.Default.DarkMode to colors.primary
                            AppThemeMode.LIGHT -> Icons.Default.LightMode to colors.primary
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = if (isUk) "Тема оформлення" else "Тема оформления",
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenApiKeyDialog,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = AppStrings.aiApiKeyTitle,
                            tint = if (state.hasApiKey) colors.primary else colors.textMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = AppStrings.aiClearChat,
                            tint = colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = AppStrings.close,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = colors.glassBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    AiMessageBubble(
                        message = msg,
                        onAnimeClick = onAnimeClick,
                        onPlayEpisodeClick = onPlayEpisodeClick
                    )
                }

                if (state.isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colors.primary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = if (state.isVideoAttachment) {
                                    if (isUk) "ШІ аналізує відеоряд та розпізнає аніме..." else "ИИ анализирует видеоряд и распознает аниме..."
                                } else {
                                    if (isUk) "ШІ-асистент ANIWERTI аналізує запит..." else "ИИ-помощник ANIWERTI анализирует запрос..."
                                },
                                fontSize = 13.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Attached video / screenshot preview if selected
            if (state.attachedImageUri != null || state.attachedVideoUri != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceVariant)
                        .border(1.dp, colors.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.attachedImageUri != null) {
                                coil.compose.AsyncImage(
                                    model = AvatarManager.getAvatarModel(state.attachedImageUri),
                                    contentDescription = if (state.isVideoAttachment) "Прев'ю відео" else "Скріншот",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            if (state.isVideoAttachment) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x66000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                        Column {
                            Text(
                                text = if (state.isVideoAttachment) {
                                    if (isUk) "🎬 Відео прикріплено" else "🎬 Видео прикреплено"
                                } else {
                                    if (isUk) "🖼️ Скріншот прикріплено" else "🖼️ Скриншот прикреплен"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                            Text(
                                text = if (state.isVideoAttachment) {
                                    if (isUk) "ШІ знайде аніме за відеорядом" else "ИИ найдет аниме по видеоряду"
                                } else {
                                    if (isUk) "ШІ знайде це аніме за кадром" else "ИИ найдет это аниме по кадру"
                                },
                                fontSize = 10.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                    IconButton(
                        onClick = onRemoveMedia,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isUk) "Видалити" else "Удалить",
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Bottom Message Input Row with Video & Image buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video Picker Button
                IconButton(
                    onClick = {
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = if (isUk) "Знайти аніме за відео" else "Найти аниме по видео",
                        tint = if (state.isVideoAttachment) colors.primary else colors.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Photo Picker Button
                IconButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = if (isUk) "Прикріпити скріншот" else "Прикрепить скриншот",
                        tint = if (state.attachedImageUri != null && !state.isVideoAttachment) colors.primary else colors.textMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                TextField(
                    value = state.currentInput,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            text = if (state.isVideoAttachment) {
                                if (isUk) "Натисніть відправити для пошуку відео..." else "Нажмите отправить для поиска видео..."
                            } else if (state.attachedImageUri != null) {
                                if (isUk) "Напишіть коментар або надішліть..." else "Напишите комментарий или отправьте..."
                            } else {
                                if (isUk) "Запитайте що завгодно або знайдіть аніме..." else "Спросите что угодно или найдите аниме..."
                            },
                            color = colors.textMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .border(1.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceVariant,
                        unfocusedContainerColor = colors.surfaceVariant,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        focusManager.clearFocus()
                        onSendMessage(null, null, null)
                    })
                )

                Spacer(modifier = Modifier.width(6.dp))

                val canSend = (state.currentInput.isNotBlank() || state.attachedImageUri != null || state.attachedVideoUri != null) && !state.isLoading

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (canSend) colors.primary else colors.surfaceVariant
                        )
                        .border(1.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable(enabled = canSend) {
                            focusManager.clearFocus()
                            onSendMessage(null, null, null)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isUk) "Відправити" else "Отправить",
                        tint = if (canSend) Color.Black else colors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: AiChatMessage,
    onAnimeClick: (Long) -> Unit,
    onPlayEpisodeClick: (Long, Int) -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) colors.primary else colors.surfaceVariant)
                    .border(
                        1.dp,
                        if (isUser) Color.Transparent else colors.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    // Video presentation if message has video
                    if (message.isVideo) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!message.imageUri.isNullOrBlank()) {
                                coil.compose.AsyncImage(
                                    model = AvatarManager.getAvatarModel(message.imageUri),
                                    contentDescription = if (isUk) "Кадр із відео" else "Кадр из видео",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x55000000))
                            )
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary.copy(alpha = 0.9f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xCC000000))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isUk) "Відео" else "Видео",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (message.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else if (!message.imageUri.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = AvatarManager.getAvatarModel(message.imageUri),
                            contentDescription = if (isUk) "Скріншот" else "Скриншот",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        if (message.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            color = if (isUser) Color.Black else colors.textPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Target Anime & Episode Action Buttons
        val targetAnimeId = message.matchedAnimeId ?: message.recommendedAnimes.firstOrNull()?.id
        val targetEpisode = message.matchedEpisode
        val seasonLabel = message.matchedSeasonName ?: (if (isUk) "Сезон 1" else "Сезон 1")

        if (targetAnimeId != null) {
            val primaryAnime = message.recommendedAnimes.firstOrNull { it.id == targetAnimeId }
                ?: message.recommendedAnimes.firstOrNull()
            val primaryTitle = if (primaryAnime != null) {
                com.example.data.api.AnimeTitleHelper.getDisplayTitle(primaryAnime)
            } else {
                seasonLabel
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Direct "Дивитися епізод" button (if episode is identified)
            if (targetEpisode != null) {
                Button(
                    onClick = { onPlayEpisodeClick(targetAnimeId, targetEpisode) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = if (isUser) 0.dp else 36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isUk) "Дивитися: $seasonLabel • $targetEpisode серія ▶" else "Смотреть: $seasonLabel • $targetEpisode серия ▶",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // 2. Direct button to navigate to this exact season
            OutlinedButton(
                onClick = { onAnimeClick(targetAnimeId) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.textPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (isUser) 0.dp else 36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUk) "Перейти на цей сезон: «$primaryTitle»" else "Перейти на этот сезон: «$primaryTitle»",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Recommended Anime Cards Carousel
        if (message.recommendedAnimes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isUk) "Знайдені тайтли (${message.recommendedAnimes.size}):" else "Найденные тайтлы (${message.recommendedAnimes.size}):",
                color = colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 36.dp, bottom = 6.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(message.recommendedAnimes) { anime ->
                    AiRecommendedAnimeCard(
                        anime = anime,
                        onClick = { onAnimeClick(anime.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AiRecommendedAnimeCard(
    anime: ShikimoriAnimeDto,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()
    val title = com.example.data.api.AnimeTitleHelper.getDisplayTitle(anime)
    val imageUrl = com.example.data.repository.AnimeRepository.resolveImageUrl(
        anime.image?.original ?: anime.image?.preview,
        animeId = anime.id,
        animeName = anime.name
    )

    Card(
        modifier = Modifier
            .width(135.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                coil.compose.AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Score Badge
                anime.score?.let { score ->
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = score,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = title,
                    maxLines = 2,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val year = anime.airedOn?.take(4) ?: "2026"
                val epInfo = com.example.data.api.AnimeEpisodeHelper.getEpisodeInfo(anime, isUk)
                val eps = epInfo.formattedText.ifBlank {
                    anime.episodes?.let { "$it ${AppStrings.episodesShort}" } ?: ""
                }
                Text(
                    text = if (eps.isNotBlank()) "$year • $eps" else year,
                    fontSize = 10.sp,
                    color = if (epInfo.isOngoing && epInfo.airedEpisodes != null && (epInfo.totalEpisodes == null || epInfo.airedEpisodes < epInfo.totalEpisodes)) {
                        androidx.compose.ui.graphics.Color(0xFF00E676)
                    } else {
                        colors.textMuted
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Action "Перейти" label
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.primary.copy(alpha = 0.15f))
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isUk) "Перейти" else "Перейти",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

