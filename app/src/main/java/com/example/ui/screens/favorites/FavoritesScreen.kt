package com.example.ui.screens.favorites

import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.repository.AnimeRepository
import com.example.data.api.models.ShikimoriAnimeDto
import com.example.data.db.FavoriteCategory
import com.example.data.db.WatchHistoryEntity
import com.example.ui.components.AnimeCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.QuickCategoryBottomSheet
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

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onAnimeClick: (Long) -> Unit,
    onWatchHistoryItemClick: (Long, String) -> Unit,
    onNavigateToCatalog: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedAnimeForCategory by remember { mutableStateOf<ShikimoriAnimeDto?>(null) }

    val allCount = favorites.size
    val watchingCount = favorites.count { it.category == FavoriteCategory.WATCHING }
    val planToWatchCount = favorites.count { it.category == FavoriteCategory.PLAN_TO_WATCH }
    val completedCount = favorites.count { it.category == FavoriteCategory.COMPLETED }
    val droppedCount = favorites.count { it.category == FavoriteCategory.DROPPED }
    val historyCount = history.size

    val getTabCount: (FavoritesTab) -> Int = { tab ->
        when (tab) {
            FavoritesTab.ALL -> allCount
            FavoritesTab.WATCHING -> watchingCount
            FavoritesTab.PLAN_TO_WATCH -> planToWatchCount
            FavoritesTab.COMPLETED -> completedCount
            FavoritesTab.DROPPED -> droppedCount
            FavoritesTab.HISTORY -> historyCount
        }
    }

    val currentTabCount = getTabCount(selectedTab)
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            // --- Top Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.favoritesTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )

                if (activeUser != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceVariant)
                            .border(1.dp, colors.primary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isUk) "$allCount в обраному" else "$allCount в избранном",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (activeUser != null && selectedTab == FavoritesTab.HISTORY && history.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearAllHistory() },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = if (isUk) "Очистити історію" else "Очистить историю",
                            tint = AccentOrange
                        )
                    }
                }
            }

            if (activeUser == null) {
                // Feature Gated Behind Account State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.primary.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(colors.primary.copy(alpha = 0.15f), CircleShape)
                                    .border(1.5.dp, colors.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = if (isUk) "Закладки та списки" else "Закладки и списки",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (isUk) "Створюйте власні списки «Дивлюсь», «В планах», «Переглянуто» та зберігайте історію перегляду. Функція доступна після створення або входу в акаунт."
                                       else "Создавайте собственные списки «Смотрю», «Планирую», «Просмотрено» и сохраняйте историю просмотра. Функция доступна после входа в аккаунт.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = colors.textSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onNavigateToProfile,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("favorites_login_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (isUk) "Створити акаунт / Увійти" else "Создать аккаунт / Войти",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // --- Top Category Counter Indicator Banner ---
                FavoriteCounterHeader(
                    selectedTab = selectedTab,
                    count = currentTabCount
                )

                Spacer(modifier = Modifier.height(6.dp))

                // --- Tabs Row ---
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(FavoritesTab.values()) { tab ->
                        val isSelected = tab == selectedTab
                        val tabCount = getTabCount(tab)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) colors.primary else colors.surface)
                                .border(
                                    1.dp,
                                    if (isSelected) colors.primary else Color(0x33FFFFFF),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectTab(tab) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("fav_tab_${tab.name}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tab.title,
                                color = if (isSelected) Color.Black else colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color.Black.copy(alpha = 0.2f)
                                        else colors.surfaceVariant
                                    )
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$tabCount",
                                    color = if (isSelected) Color.Black else colors.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

            // --- Content Section ---
            if (selectedTab == FavoritesTab.HISTORY) {
                if (history.isEmpty()) {
                    EmptyStateView(
                        title = if (isUk) "Історія переглядів порожня" else "История просмотров пуста",
                        subtitle = if (isUk) "Почніть дивитися аніме в каталозі, і воно з'явиться тут" else "Начните смотреть аниме в каталоге, и оно появится здесь",
                        actionTitle = if (isUk) "Перейти в каталог" else "Перейти в каталог",
                        onAction = onNavigateToCatalog
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(history, key = { it.animeId }) { item ->
                            HistoryListItem(
                                item = item,
                                onClick = { onWatchHistoryItemClick(item.animeId, item.russianName) }
                            )
                        }
                    }
                }
            } else {
                val filteredFavorites = when (selectedTab) {
                    FavoritesTab.ALL -> favorites
                    FavoritesTab.WATCHING -> favorites.filter { it.category == FavoriteCategory.WATCHING }
                    FavoritesTab.PLAN_TO_WATCH -> favorites.filter { it.category == FavoriteCategory.PLAN_TO_WATCH }
                    FavoritesTab.COMPLETED -> favorites.filter { it.category == FavoriteCategory.COMPLETED }
                    FavoritesTab.DROPPED -> favorites.filter { it.category == FavoriteCategory.DROPPED }
                    FavoritesTab.HISTORY -> emptyList()
                }

                if (filteredFavorites.isEmpty()) {
                    EmptyStateView(
                        title = if (isUk) "У цьому списку поки що порожньо" else "В этом списке пока пусто",
                        subtitle = if (isUk) "Додавайте улюблені тайтли в обране, щоб не загубити" else "Добавляйте любимые тайтлы в избранное, чтобы не потерять",
                        actionTitle = if (isUk) "Знайти аніме" else "Найти аниме",
                        onAction = onNavigateToCatalog
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredFavorites, key = { it.id }) { fav ->
                            val animeDto = ShikimoriAnimeDto(
                                id = fav.id,
                                name = fav.name,
                                russian = fav.russianName,
                                image = com.example.data.api.models.ShikimoriImageDto(fav.posterUrl, fav.posterUrl, null, null),
                                url = null,
                                kind = fav.kind,
                                score = fav.score,
                                status = null,
                                episodes = fav.episodesCount,
                                episodesAired = null,
                                airedOn = fav.year,
                                releasedOn = null
                            )
                            AnimeCard(
                                anime = animeDto,
                                onClick = { onAnimeClick(fav.id) },
                                onLongClick = { selectedAnimeForCategory = animeDto },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

        // Quick Category Bottom Sheet on long press in Favorites
        if (selectedAnimeForCategory != null) {
            val selected = selectedAnimeForCategory!!
            val animeName = selected.russian?.takeIf { it.isNotBlank() } ?: selected.name
            QuickCategoryBottomSheet(
                anime = selected,
                onDismiss = { selectedAnimeForCategory = null },
                onCategorySelected = { category ->
                    viewModel.setFavoriteCategory(selected, category)
                    Toast.makeText(context, "«$animeName» перемещено в «${category.displayName}»", Toast.LENGTH_SHORT).show()
                },
                onRemoveFavorite = {
                    viewModel.removeFavorite(selected.id)
                    Toast.makeText(context, "«$animeName» удалено из списков", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun HistoryListItem(
    item: WatchHistoryEntity,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("history_item_${item.animeId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant)
            ) {
                val coverUrl = AnimeRepository.resolveImageUrl(item.posterUrl, item.animeId, item.russianName)
                    .ifBlank { "https://shikimori.io/system/animes/original/${item.animeId}.jpg" }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.ic_poster_placeholder)
                        .error(R.drawable.ic_poster_placeholder)
                        .build(),
                    contentDescription = item.russianName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.Center)
                        .background(Color(0xCC000000), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                LinearProgressIndicator(
                    progress = { item.progressPercent / 100f },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = colors.primary,
                    trackColor = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.russianName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val epWord = if (isUk) "Серія" else "Серия"
                Text(
                    text = "$epWord ${item.episodeNumber} • ${item.voiceName}",
                    fontSize = 12.sp,
                    color = AccentOrange,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val watchedWord = if (isUk) "Переглянуто" else "Просмотрено"
                Text(
                    text = "$watchedWord ${item.progressPercent}%",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            }
        }
    }
}

@Composable
private fun FavoriteCounterHeader(
    selectedTab: FavoritesTab,
    count: Int
) {
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()

    val (titleText, descText, icon) = when (selectedTab) {
        FavoritesTab.ALL -> Triple(
            if (isUk) "Всі аніме в обраному" else "Все аниме в избранном",
            if (isUk) "Всі додані тайтли у вашій медіатеці" else "Все добавленные тайтлы в вашей медиатеке",
            Icons.Default.Bookmark
        )
        FavoritesTab.COMPLETED -> Triple(
            if (isUk) "Переглянуті аніме" else "Просмотренные аниме",
            if (isUk) "Тайтли, які ви повністю переглянули" else "Тайтлы, которые вы полностью посмотрели",
            Icons.Default.CheckCircle
        )
        FavoritesTab.WATCHING -> Triple(
            if (isUk) "Зараз дивлюсь" else "Сейчас смотрю",
            if (isUk) "Тайтли в процесі поточного перегляду" else "Тайтлы в процессе текущего просмотра",
            Icons.Default.PlayCircle
        )
        FavoritesTab.PLAN_TO_WATCH -> Triple(
            if (isUk) "У планах подивитися" else "В планах посмотреть",
            if (isUk) "Тайтли, збережені для майбутнього перегляду" else "Тайтлы, сохранённые для будущего просмотра",
            Icons.Default.Schedule
        )
        FavoritesTab.DROPPED -> Triple(
            if (isUk) "Кинуті тайтли" else "Брошенные тайтлы",
            if (isUk) "Тайтли, перегляд яких було зупинено" else "Тайтлы, просмотр которых был остановлен",
            Icons.Default.Cancel
        )
        FavoritesTab.HISTORY -> Triple(
            if (isUk) "Історія переглядів" else "История просмотров",
            if (isUk) "Всі відкриті серії та позиції відтворення" else "Все открытые серии и позиции воспроизведения",
            Icons.Default.History
        )
    }

    val countUnit = if (isUk) "аніме" else "аниме"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = descText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Count Badge Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primary)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$count $countUnit",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                )
            }
        }
    }
}

