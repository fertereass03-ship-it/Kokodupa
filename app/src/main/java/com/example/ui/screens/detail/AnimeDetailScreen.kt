package com.example.ui.screens.detail

import com.example.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.api.models.RelatedAnimeItem
import com.example.data.db.FavoriteCategory
import com.example.data.repository.AnimeRepository
import com.example.ui.components.AnimeCard
import com.example.ui.components.ErrorStateView
import com.example.ui.components.SkeletonBanner
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.GradientDarkOverlay
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.RatingGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(
    viewModel: AnimeDetailViewModel,
    onBackClick: () -> Unit,
    onWatchClick: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    onAnimeClick: (Long) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAuth: () -> Unit = onNavigateToProfile
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (state.isLoading) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark)
        ) {
            SkeletonBanner(modifier = Modifier.height(280.dp))
        }
        return
    }

    val anime = state.anime
    if (anime == null) {
        ErrorStateView(
            message = state.errorMessage ?: "Аниме не найдено",
            onRetry = { viewModel.loadDetails() },
            modifier = modifier.fillMaxSize().background(BackgroundDark)
        )
        return
    }

    val liveCovers by com.example.data.api.AniListService.coversFlow.collectAsState()
    val liveEpisodes by com.example.data.api.AnimeEpisodeHelper.liveEpisodesFlow.collectAsState()

    LaunchedEffect(anime.id) {
        com.example.data.api.AnimeEpisodeHelper.requestLiveUpdate(
            animeId = anime.id,
            name = anime.name,
            russian = anime.russian,
            episodesTotal = anime.episodes,
            episodesAired = anime.episodesAired,
            status = anime.status
        )
    }

    val posterUrl = (liveCovers[anime.id.toString()] ?: AnimeRepository.resolveImageUrl(
        anime.image?.original ?: anime.image?.preview,
        animeId = anime.id,
        animeName = "${anime.russian ?: ""} ${anime.name}"
    )).ifBlank { "https://shikimori.io/system/animes/original/${anime.id}.jpg" }
    val displayName = anime.russian?.takeIf { it.isNotBlank() } ?: anime.name
    val origName = anime.name
    val score = anime.score?.takeIf { it.isNotBlank() && it != "0.0" } ?: "8.8"
    val year = anime.airedOn?.take(4) ?: "2026"
    val kind = when (anime.kind?.lowercase()) {
        "tv" -> "TV Сериал"
        "movie" -> "Фильм"
        "ova" -> "OVA"
        "ona" -> "ONA"
        "special" -> "Спешл"
        else -> anime.kind?.uppercase() ?: "TV"
    }
    val status = when (anime.status?.lowercase()) {
        "ongoing" -> "Онгоинг"
        "released" -> "Завершено"
        "anons" -> "Анонс"
        else -> anime.status ?: "Выходит"
    }

    // Copy anime ID to clipboard
    fun copyAnimeIdToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Anime ID", "#${anime.id}")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "ID #${anime.id} скопирован!", Toast.LENGTH_SHORT).show()
    }

    fun shareAnimeWithFriend() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "🎬 Смотри аниме \"$displayName\" в ANIWERTI! ID: #${anime.id}"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Поделиться аниме с другом"))
    }

    var isDescriptionExpanded by remember { mutableStateOf(false) }

    val cleanedDescription = remember(anime.description, anime.russian, anime.name) {
        val genresList = anime.genres?.mapNotNull { it.russian?.takeIf { r -> r.isNotBlank() } ?: it.name }
        com.example.util.TextCleaner.cleanAnimeDescription(
            rawText = anime.description,
            russianTitle = anime.russian,
            origTitle = anime.name,
            genres = genresList
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // --- 1. Top Header Banner with Backdrop, Navigation & Clickable ID above poster ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    // Blurred/Dimmed Backdrop
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(posterUrl)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_poster_placeholder)
                            .error(R.drawable.ic_poster_placeholder)
                            .build(),
                        contentDescription = displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient overlay to blend smoothly into dark background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(GradientDarkOverlay))
                    )

                    // Top Action Bar with Back, Clickable ID (above poster), Share, Bookmark
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x99000000), CircleShape)
                                .border(1.dp, Color(0x33FFC400), CircleShape)
                                .testTag("detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Clickable ID badge above poster - tap directly to copy!
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xDD0B0B0B))
                                .border(1.dp, PrimaryYellow, RoundedCornerShape(10.dp))
                                .clickable { copyAnimeIdToClipboard() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("detail_id_chip"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Скопировать ID",
                                    tint = PrimaryYellow,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "ID: #${anime.id}",
                                    color = PrimaryYellow,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = { shareAnimeWithFriend() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x99000000), CircleShape)
                                .border(1.dp, Color(0x33FFC400), CircleShape)
                                .testTag("detail_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Поделиться",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { viewModel.openCategoryPicker() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x99000000), CircleShape)
                                .border(1.dp, Color(0x33FFC400), CircleShape)
                                .testTag("detail_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (state.favorite != null) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "В избранное",
                                tint = if (state.favorite != null) PrimaryYellow else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // --- 2. Small Poster next to Title, Metadata & Action Button ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-36).dp)
                ) {
                    val totalEp = anime.episodes ?: 12
                    val voiceDubs = state.translations.filter { it.type != "sub" }
                    val maxVoiceEp = voiceDubs.maxOfOrNull { it.episodesCount }
                        ?: state.translations.maxOfOrNull { it.episodesCount }

                    val isOngoingStatus = anime.status?.contains("ongoing", ignoreCase = true) == true
                    val isAnonsStatus = anime.status?.contains("anons", ignoreCase = true) == true

                    val isUk = com.example.data.settings.AppSettingsManager.isUkrainian()
                    val helperInfo = liveEpisodes[anime.id] ?: com.example.data.api.AnimeEpisodeHelper.getEpisodeInfo(anime, isUk)
                    val effectiveTotal = helperInfo.totalEpisodes ?: totalEp

                    // Accurate voiced episodes calculation: Kodik translations or live update is absolute priority
                    val actualAiredEp: Int? = when {
                        maxVoiceEp != null && maxVoiceEp > 0 -> {
                            val base = maxOf(anime.episodesAired ?: 0, helperInfo.airedEpisodes ?: 0)
                            maxOf(maxVoiceEp, base)
                        }
                        helperInfo.airedEpisodes != null && helperInfo.airedEpisodes > 0 -> helperInfo.airedEpisodes
                        anime.episodesAired != null && anime.episodesAired > 0 -> anime.episodesAired
                        else -> null
                    }

                    val isOngoing = isOngoingStatus || (actualAiredEp != null && actualAiredEp < effectiveTotal && !isAnonsStatus) || helperInfo.isOngoing

                    val episodesLabel = when {
                        isAnonsStatus -> if (isUk) "Анонс ($effectiveTotal сер.)" else "Анонс ($effectiveTotal эп.)"
                        isOngoing && actualAiredEp != null && actualAiredEp < effectiveTotal -> {
                            if (isUk) "$actualAiredEp з $effectiveTotal сер." else "$actualAiredEp из $effectiveTotal сер."
                        }
                        isOngoing && actualAiredEp != null && actualAiredEp >= effectiveTotal -> {
                            if (isUk) "$effectiveTotal сер." else "$effectiveTotal эп."
                        }
                        isOngoing -> if (isUk) "Виходить ($effectiveTotal сер.)" else "Выходит ($effectiveTotal эп.)"
                        else -> if (isUk) "$effectiveTotal сер." else "$effectiveTotal эп."
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(152.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Adaptive poster card next to title
                        Card(
                            modifier = Modifier
                                .width(102.dp)
                                .height(152.dp)
                                .shadow(8.dp, RoundedCornerShape(14.dp))
                                .border(1.2.dp, Color(0x66FFC400), RoundedCornerShape(14.dp))
                                .testTag("detail_poster_image"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (!posterUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(posterUrl)
                                            .crossfade(true)
                                            .placeholder(R.drawable.ic_poster_placeholder)
                                            .error(R.drawable.ic_poster_placeholder)
                                            .build(),
                                        contentDescription = displayName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF2C2E3C), Color(0xFF161722))
                                                )
                                            )
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Movie,
                                                contentDescription = null,
                                                tint = PrimaryYellow,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = displayName,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                textAlign = TextAlign.Center,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Rating tag on poster
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .background(Color(0xEE0B0B0B), RoundedCornerShape(6.dp))
                                        .border(0.5.dp, RatingGold, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = RatingGold,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = score,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title, dubbing info, and metadata strictly within poster bounds
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(152.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        lineHeight = 20.sp
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (origName != displayName) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = origName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Status badge & Year / Kind
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(SurfaceVariantDark, RoundedCornerShape(6.dp))
                                            .border(0.5.dp, Color(0x44FF9100), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    ) {
                                        Text(
                                            text = status,
                                            color = AccentOrange,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Text(
                                        text = "$year • $kind" + if (anime.duration != null && anime.duration > 0) " • ${anime.duration} мин." else "",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Badges: Episodes count & Dubbing count
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF1E222D), RoundedCornerShape(6.dp))
                                            .border(0.8.dp, Color(0x555C6B73), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    ) {
                                        Text(
                                            text = episodesLabel,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF162A1B), RoundedCornerShape(6.dp))
                                            .border(0.8.dp, Color(0xFF4CAF50), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = null,
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (state.isTranslationsLoading) {
                                                    "Озвучено: ..."
                                                } else {
                                                    "Озвучено: ${maxVoiceEp ?: actualAiredEp ?: "?"} эп."
                                                },
                                                color = Color(0xFF81C784),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Watch Progress Banner if watched
                    state.history?.let { hist ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFC400))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Вы остановились на: Серия ${hist.episodeNumber}",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${hist.progressPercent}%",
                                        color = PrimaryYellow,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { hist.progressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp),
                                    color = PrimaryYellow,
                                    trackColor = SurfaceVariantDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Row: Watch button (slightly resized) + Status selector cell
                    val currentCategory = state.favorite?.category
                    val statusLabel = when (currentCategory) {
                        FavoriteCategory.COMPLETED -> "Просмотрено"
                        FavoriteCategory.WATCHING -> "Смотрю"
                        FavoriteCategory.PLAN_TO_WATCH -> "Планирую"
                        FavoriteCategory.DROPPED -> "Брошено"
                        null -> "Не смотрю"
                    }
                    val statusTint = when (currentCategory) {
                        FavoriteCategory.COMPLETED -> Color(0xFF4CAF50)
                        FavoriteCategory.WATCHING -> AccentOrange
                        FavoriteCategory.PLAN_TO_WATCH -> PrimaryYellow
                        FavoriteCategory.DROPPED -> Color(0xFFFF5252)
                        null -> TextSecondary
                    }
                    val statusIcon = when (currentCategory) {
                        FavoriteCategory.COMPLETED -> Icons.Default.CheckCircle
                        FavoriteCategory.WATCHING -> Icons.Default.PlayCircleOutline
                        FavoriteCategory.PLAN_TO_WATCH -> Icons.Default.Bookmark
                        FavoriteCategory.DROPPED -> Icons.Default.Cancel
                        null -> Icons.Default.BookmarkBorder
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Resized "Смотреть" Button
                        Button(
                            onClick = { onWatchClick(anime.id, displayName) },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp)
                                .testTag("detail_watch_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryYellow,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Смотреть",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.history != null) "Продолжить" else "Смотреть",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // "Статус" Cell Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceCard)
                                .border(
                                    1.dp,
                                    if (currentCategory != null) statusTint.copy(alpha = 0.5f) else Color(0x33FFFFFF),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.openCategoryPicker() }
                                .padding(horizontal = 10.dp)
                                .testTag("detail_status_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = "Статус",
                                    tint = statusTint,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Статус",
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        lineHeight = 11.sp
                                    )
                                    Text(
                                        text = statusLabel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = statusTint,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 3. Genres ---
            item {
                anime.genres?.takeIf { it.isNotEmpty() }?.let { genres ->
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .offset(y = (-20).dp)
                    ) {
                        Text(
                            text = "Жанры",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            genres.forEach { genre ->
                                val name = genre.russian?.takeIf { it.isNotBlank() } ?: genre.name
                                Box(
                                    modifier = Modifier
                                        .background(SurfaceVariantDark, RoundedCornerShape(8.dp))
                                        .border(0.5.dp, Color(0x33FFC400), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = name,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. Description / Synopsis ---
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .offset(y = (-10).dp)
                        .animateContentSize()
                ) {
                    Text(
                        text = "Описание",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cleanedDescription,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            lineHeight = 22.sp,
                            fontSize = 13.5.sp
                        ),
                        maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (cleanedDescription.length > 120) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isDescriptionExpanded) "Свернуть" else "Читать далее",
                                color = PrimaryYellow,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isDescriptionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isDescriptionExpanded) "Свернуть" else "Читать далее",
                                tint = PrimaryYellow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // --- 5. Anime Screenshots Gallery (10 items) ---
            if (state.screenshots.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                tint = PrimaryYellow,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Кадры из аниме",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(SurfaceVariantDark, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${state.screenshots.size} кадров",
                                    color = PrimaryYellow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(state.screenshots) { index, imageUrl ->
                                Card(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .height(128.dp)
                                        .clickable { viewModel.selectScreenshot(imageUrl) }
                                        .testTag("screenshot_card_$index"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFC400))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Кадр из аниме #${index + 1}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${index + 1}/10",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 6. Real Related Anime from Shikimori (Chronological order by year, expandable) ---
            if (state.relatedAnime.isNotEmpty()) {
                item {
                    val displayedList = if (state.isRelatedExpanded) {
                        state.relatedAnime
                    } else {
                        state.relatedAnime.take(2)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = PrimaryYellow,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Связанные релизы (${state.relatedAnime.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Хронология франшизы",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            displayedList.forEach { item ->
                                RelatedAnimeCard(
                                    item = item,
                                    onClick = { onAnimeClick(item.id) }
                                )
                            }
                        }

                        // Expand / Collapse button if more than 2 related releases exist
                        if (state.relatedAnime.size > 2) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { viewModel.toggleRelatedExpanded() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("toggle_related_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SurfaceVariantDark,
                                    contentColor = PrimaryYellow
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFC400))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (state.isRelatedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = PrimaryYellow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (state.isRelatedExpanded) {
                                            "Свернуть список (показано ${state.relatedAnime.size})"
                                        } else {
                                            "Развернуть все связанные (${state.relatedAnime.size})"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 7. Similar Anime & Recommendations (Похожие аниме и рекомендации) ---
            if (state.similarAnime.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Recommend,
                                contentDescription = null,
                                tint = AccentOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Похожие аниме и рекомендации",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${state.similarAnime.size}",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.similarAnime, key = { it.id }) { item ->
                                Box(modifier = Modifier.width(135.dp)) {
                                    AnimeCard(
                                        anime = item,
                                        onClick = { onAnimeClick(item.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 8. Additional Metadata & Clickable ID Info Card ---
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = "Информация",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x22FFC400))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Clickable ID Row inside info
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { copyAnimeIdToClipboard() }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "ID тайтла", color = TextMuted, fontSize = 13.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${anime.id}",
                                        color = PrimaryYellow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Скопировать",
                                        tint = PrimaryYellow,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            InfoRow(label = "Эпизоды", value = "${anime.episodes ?: "12"} серий")
                            InfoRow(label = "Длительность", value = "${anime.duration ?: 24} мин. / эп.")
                            anime.studios?.firstOrNull()?.name?.let { studio ->
                                InfoRow(label = "Студия", value = studio)
                            }
                            InfoRow(label = "Первоисточник", value = anime.franchise ?: "Манга")
                        }
                    }
                }
            }
        }
    }

    // --- Fullscreen Screenshot Viewer Dialog ---
    state.selectedScreenshot?.let { imageUrl ->
        Dialog(
            onDismissRequest = { viewModel.selectScreenshot(null) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE050505))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Кадр в полном размере",
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = { viewModel.selectScreenshot(null) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 40.dp, end = 20.dp)
                        .size(44.dp)
                        .background(Color(0x88000000), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // --- Category Picker Bottom Sheet ---
    if (state.isCategoryPickerOpen) {
        CategoryPickerBottomSheet(
            currentCategory = state.favorite?.category,
            onSelectCategory = { category ->
                viewModel.setFavoriteCategory(category)
                Toast.makeText(
                    context,
                    "Статус збережено: ${category.displayName}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onRemove = {
                viewModel.removeFavorite()
                Toast.makeText(context, "Видалено зі списків", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { viewModel.closeCategoryPicker() }
        )
    }

    if (state.showAuthNotice) {
        val pendingCat = state.pendingCategory
        val statusName = when (pendingCat) {
            FavoriteCategory.COMPLETED -> "«Переглянуто» / «Просмотрено»"
            FavoriteCategory.DROPPED -> "«Кинуто» / «Брошено»"
            FavoriteCategory.WATCHING -> "«Дивлюся» / «Смотрю»"
            FavoriteCategory.PLAN_TO_WATCH -> "«У планах» / «Планирую»"
            else -> "статусу перегляду"
        }
        AlertDialog(
            onDismissRequest = { viewModel.dismissAuthNotice() },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = PrimaryYellow,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Створення / Вхід в акаунт",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Для додавання статусу $statusName необхідно створити акаунт.",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Навіть якщо ви вже створили акаунт раніше, ви можете створити новий акаунт або підтвердити збереження в поточний профіль.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissAuthNotice()
                        onNavigateToAuth()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow, contentColor = Color.Black)
                ) {
                    Text("Створити акаунт", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.dismissAuthNotice() }) {
                        Text("Скасувати", color = TextSecondary)
                    }
                    if (pendingCat != null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.confirmPendingCategory()
                                Toast.makeText(
                                    context,
                                    "Статус збережено: $statusName",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            border = BorderStroke(1.dp, PrimaryYellow.copy(alpha = 0.5f))
                        ) {
                            Text("Зберегти зараз", color = PrimaryYellow)
                        }
                    }
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun RelatedAnimeCard(
    item: RelatedAnimeItem,
    onClick: () -> Unit
) {
    val cardBorder = if (item.isCurrent) {
        androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryYellow)
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFC400))
    }
    val cardBackground = if (item.isCurrent) {
        Color(0xFF262117)
    } else {
        SurfaceCard
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clickable { onClick() }
            .testTag("related_anime_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(14.dp),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prominent Year Badge (Fixed width)
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(68.dp)
                    .background(
                        if (item.isCurrent) Color(0x44FFC400) else Color(0x2AFFE082),
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        if (item.isCurrent) 1.5.dp else 1.dp,
                        if (item.isCurrent) PrimaryYellow else Color(0x55FFC400),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (item.isCurrent) Icons.Default.PlayArrow else Icons.Default.CalendarMonth,
                        contentDescription = "Год",
                        tint = PrimaryYellow,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.year,
                        color = PrimaryYellow,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Poster image (Fixed uniform dimensions)
            AsyncImage(
                model = AnimeRepository.resolveImageUrl(item.posterUrl, item.id, item.russianName),
                contentDescription = item.russianName,
                modifier = Modifier
                    .width(52.dp)
                    .height(76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        if (item.isCurrent) 1.dp else 0.5.dp,
                        if (item.isCurrent) PrimaryYellow else Color(0x33FFC400),
                        RoundedCornerShape(8.dp)
                    ),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Anime info & relationship
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.russianName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (item.isCurrent) PrimaryYellow else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.name != item.russianName && item.name.isNotBlank()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Relation chip
                    Box(
                        modifier = Modifier
                            .background(
                                if (item.isCurrent) Color(0x33FFC400) else SurfaceVariantDark,
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                0.5.dp,
                                if (item.isCurrent) PrimaryYellow else Color(0x44FF9100),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.relationRussian,
                            color = if (item.isCurrent) PrimaryYellow else AccentOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }

                    // Score chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0x33000000), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = RatingGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = item.score,
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${item.kind} • ${item.episodes} эп.",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (item.isCurrent) {
                Box(
                    modifier = Modifier
                        .background(Color(0x33FFC400), CircleShape)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Сейчас",
                        color = PrimaryYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Перейти",
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerBottomSheet(
    currentCategory: FavoriteCategory?,
    onSelectCategory: (FavoriteCategory) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = SurfaceCard,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = PrimaryYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Выберите статус аниме",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Просмотрено
            StatusOptionRow(
                title = "Переглянуто / Просмотрено",
                subtitle = "Аніме переглянуто повністю",
                icon = Icons.Default.CheckCircle,
                tint = Color(0xFF4CAF50),
                isSelected = currentCategory == FavoriteCategory.COMPLETED,
                onClick = {
                    onSelectCategory(FavoriteCategory.COMPLETED)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Смотрю
            StatusOptionRow(
                title = "Дивлюся / Смотрю",
                subtitle = "У процесі активного перегляду",
                icon = Icons.Default.PlayCircleOutline,
                tint = AccentOrange,
                isSelected = currentCategory == FavoriteCategory.WATCHING,
                onClick = {
                    onSelectCategory(FavoriteCategory.WATCHING)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Планирую
            StatusOptionRow(
                title = "У планах / Планирую",
                subtitle = "Додано до списку очікування",
                icon = Icons.Default.Bookmark,
                tint = PrimaryYellow,
                isSelected = currentCategory == FavoriteCategory.PLAN_TO_WATCH,
                onClick = {
                    onSelectCategory(FavoriteCategory.PLAN_TO_WATCH)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Брошено
            StatusOptionRow(
                title = "Кинуто / Брошено",
                subtitle = "Перегляд зупинено або покинуто",
                icon = Icons.Default.Cancel,
                tint = Color(0xFFFF5252),
                isSelected = currentCategory == FavoriteCategory.DROPPED,
                onClick = {
                    onSelectCategory(FavoriteCategory.DROPPED)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Не смотрю
            StatusOptionRow(
                title = "Не смотрю",
                subtitle = "Убрать из списков и медиатеки",
                icon = Icons.Default.RemoveCircleOutline,
                tint = TextMuted,
                isSelected = currentCategory == null,
                onClick = {
                    onRemove()
                    Toast.makeText(context, "Статус: Не смотрю (удалено из списков)", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatusOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) SurfaceVariantDark else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) tint.copy(alpha = 0.6f) else Color(0x1AFFFFFF),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isSelected) tint else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
