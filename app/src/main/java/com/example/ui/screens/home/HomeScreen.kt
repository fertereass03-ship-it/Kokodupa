package com.example.ui.screens.home

import android.widget.Toast
import com.example.data.db.FavoriteCategory
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.api.models.ShikimoriAnimeDto
import com.example.data.db.WatchHistoryEntity
import com.example.data.repository.AnimeRepository
import com.example.ui.components.AnimeCard
import com.example.ui.components.ErrorStateView
import com.example.ui.components.QuickCategoryBottomSheet
import com.example.ui.components.SkeletonBanner
import com.example.ui.components.SkeletonHorizontalList
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.GradientDarkOverlay
import com.example.ui.theme.GradientOrangeYellow
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.RatingGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.LocalAppColors
import com.example.data.settings.AppSettingsManager
import com.example.util.AppStrings
import com.example.data.api.AnimeTitleHelper

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAnimeClick: (Long) -> Unit,
    onWatchClick: (Long, String) -> Unit,
    onNavigateToCatalog: () -> Unit,
    onNavigateToCatalogWithPreset: (String) -> Unit = { onNavigateToCatalog() },
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.watchHistory.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedAnimeForCategory by remember { mutableStateOf<ShikimoriAnimeDto?>(null) }

    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()

    if (state.isLoading) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            SkeletonBanner()
            Spacer(modifier = Modifier.height(16.dp))
            SkeletonHorizontalList()
            Spacer(modifier = Modifier.height(16.dp))
            SkeletonHorizontalList()
        }
        return
    }

    if (state.errorMessage != null && state.bannerAnime == null) {
        ErrorStateView(
            message = state.errorMessage.orEmpty(),
            onRetry = { viewModel.loadHomeData() },
            modifier = modifier.fillMaxSize().background(colors.background)
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // --- 1. Top Hero Banner (Аниме дня / Новинки 2026) ---
            state.bannerAnime?.let { banner ->
                item {
                    HeroBanner(
                        anime = banner,
                        isFavorite = state.isBannerFavorite,
                        onWatchClick = {
                            if (com.example.data.api.AnimeEpisodeHelper.isAnnouncement(banner)) {
                                onAnimeClick(banner.id)
                            } else {
                                val title = AnimeTitleHelper.getDisplayTitle(banner)
                                onWatchClick(banner.id, title)
                            }
                        },
                        onFavoriteClick = { viewModel.toggleBannerFavorite() },
                        onDetailsClick = { onAnimeClick(banner.id) }
                    )
                }
            }

            // --- 2. Continue Watching (Продолжить просмотр) ---
            if (activeUser != null && history.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = if (isUk) "Продовжити перегляд" else "Продолжить просмотр",
                        onSeeAll = null
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(history) { item ->
                            ContinueWatchingCard(
                                item = item,
                                onClick = { onWatchClick(item.animeId, item.russianName) }
                            )
                        }
                    }
                }
            }

            // --- 3. Популярное ---
            if (state.popularAnimes.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = if (isUk) "Популярне" else "Популярное",
                        onSeeAll = { onNavigateToCatalogWithPreset("popular") }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.popularAnimes, key = { it.id }) { anime ->
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
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
            }

            // --- 4. Новинки 2026 ---
            if (state.releases2026.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = if (isUk) "Новинки 2026 року 🔥" else "Новинки 2026 года 🔥",
                        onSeeAll = { onNavigateToCatalogWithPreset("releases2026") }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.releases2026, key = { it.id }) { anime ->
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
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
            }

            // --- 5. Рекомендации ---
            if (state.recommendations.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = if (isUk) "Рекомендації для вас" else "Рекомендации для вас",
                        onSeeAll = { onNavigateToCatalogWithPreset("top100") }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.recommendations, key = { it.id }) { anime ->
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
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick Category Bottom Sheet on long press
        if (selectedAnimeForCategory != null) {
            val selected = selectedAnimeForCategory!!
            val animeName = AnimeTitleHelper.getDisplayTitle(selected)
            QuickCategoryBottomSheet(
                anime = selected,
                onDismiss = { selectedAnimeForCategory = null },
                onCategorySelected = { category ->
                    viewModel.setFavoriteCategory(selected, category)
                    val catName = if (isUk) category.ukDisplayName else category.displayName
                    val toastMsg = if (isUk) "«$animeName» додано в «$catName»" else "«$animeName» добавлено в «$catName»"
                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                },
                onRemoveFavorite = {
                    viewModel.removeFavorite(selected.id)
                    val removeMsg = if (isUk) "«$animeName» видалено зі списків" else "«$animeName» удалено из списков"
                    Toast.makeText(context, removeMsg, Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (state.showAuthNotice) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissAuthNotice() },
                title = {
                    Text(
                        if (isUk) "Потрібен акаунт" else "Нужен аккаунт",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        if (isUk) "Для додавання аніме в закладки та списки перегляду необхідно створити акаунт або увійти в систему."
                        else "Для добавления аниме в закладки и списки необходимо войти или создать аккаунт.",
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.dismissAuthNotice()
                            onNavigateToProfile()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black)
                    ) {
                        Text(if (isUk) "Увійти / Створити" else "Войти / Создать", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissAuthNotice() }) {
                        Text(if (isUk) "Скасувати" else "Отмена", color = colors.textSecondary)
                    }
                },
                containerColor = colors.surface
            )
        }
    }
}

@Composable
private fun HeroBanner(
    anime: ShikimoriAnimeDto,
    isFavorite: Boolean,
    onWatchClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()
    val isAnons = com.example.data.api.AnimeEpisodeHelper.isAnnouncement(anime)
    val anonsDate = if (isAnons) com.example.data.api.AnimeEpisodeHelper.formatAnnouncementDate(anime.airedOn, isUk) else null

    val posterUrl = AnimeRepository.resolveImageUrl(
        anime.image?.original ?: anime.image?.preview,
        animeId = anime.id,
        animeName = "${anime.russian ?: ""} ${anime.name}"
    )
    val russianTitle = AnimeTitleHelper.getDisplayTitle(anime)
    val origTitle = anime.name
    val score = anime.score?.takeIf { it.isNotBlank() && it != "0.0" } ?: "8.8"
    val year = anime.airedOn?.take(4) ?: "2026"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp)
            .clickable { onDetailsClick() }
    ) {
        // Backdrop Image
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(posterUrl)
                .crossfade(true)
                .placeholder(R.drawable.ic_poster_placeholder)
                .error(R.drawable.ic_poster_placeholder)
                .build(),
            contentDescription = russianTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient for contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(GradientDarkOverlay))
        )

        // Top App Bar branding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(GradientOrangeYellow),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ANIWERTI",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .background(
                        if (isAnons) Color(0xDDFF8F00) else Color(0xAA000000),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isAnons) Color(0xFFFFB300) else colors.primary.copy(alpha = 0.4f),
                        CircleShape
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isAnons) (if (isUk) "Анонс" else "Анонс") else (if (isUk) "Новинка 2026" else "Новинка 2026"),
                    color = if (isAnons) Color.Black else colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Info & Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Badges row
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAnons) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFFF8F00), Color(0xFFFFC400))),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Анонс",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    if (!anonsDate.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xEE2A1C00), RoundedCornerShape(6.dp))
                                .border(0.8.dp, Color(0xFFFFB300).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = anonsDate,
                                    color = Color(0xFFFFE082),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(Color(0xDD0B0B0B), RoundedCornerShape(6.dp))
                            .border(0.5.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = if (isUk) "Рейтинг" else "Рейтинг",
                                tint = RatingGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = score,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$year • ${(anime.kind ?: "TV").uppercase()}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = russianTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (origTitle != russianTitle) {
                Text(
                    text = origTitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.7f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isAnons) {
                    Button(
                        onClick = onDetailsClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("banner_details_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Анонс",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isUk) "Анонс • Детальніше" else "Анонс • Подробнее",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Button(
                        onClick = onWatchClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("banner_watch_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = if (isUk) "Дивитися" else "Смотреть",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isUk) "Дивитися" else "Смотреть",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(colors.surfaceVariant, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .testTag("banner_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isUk) "Обране" else "Избранное",
                        tint = if (isFavorite) colors.primary else colors.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            ),
            modifier = if (onSeeAll != null) {
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSeeAll() }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            } else Modifier
        )
        Spacer(modifier = Modifier.weight(1f))
        if (onSeeAll != null) {
            Text(
                text = if (isUk) "Всі" else "Все",
                color = colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSeeAll() }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: WatchHistoryEntity,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()

    Card(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("continue_watch_${item.animeId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(colors.surfaceVariant)
            ) {
                val coverData = AnimeRepository.resolveImageUrl(item.posterUrl, item.animeId, item.russianName)
                    .ifBlank { "https://shikimori.io/system/animes/original/${item.animeId}.jpg" }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverData)
                        .crossfade(true)
                        .placeholder(R.drawable.ic_poster_placeholder)
                        .error(R.drawable.ic_poster_placeholder)
                        .build(),
                    contentDescription = item.russianName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Play icon overlay
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                        .background(Color(0xCC000000), CircleShape)
                        .border(1.dp, colors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = if (isUk) "Продовжити" else "Продолжить",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                LinearProgressIndicator(
                    progress = { item.progressPercent / 100f },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = colors.primary,
                    trackColor = Color(0x66000000)
                )
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.russianName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = colors.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val epWord = if (isUk) "Серія" else "Серия"
                    Text(
                        text = "$epWord ${item.episodeNumber} • ${item.progressPercent}%",
                        fontSize = 12.sp,
                        color = AccentOrange,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
