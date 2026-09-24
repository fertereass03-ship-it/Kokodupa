package com.example.ui.components

import com.example.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.Movie
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign
import com.example.data.api.AniListService
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.api.models.ShikimoriAnimeDto
import com.example.data.db.FavoriteCategory
import com.example.data.repository.AnimeRepository
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.GradientDarkOverlay
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.RatingGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceVariantDark
import com.example.data.api.AnimeEpisodeHelper
import com.example.data.api.AnimeTitleHelper
import com.example.data.settings.AppSettingsManager
import com.example.ui.theme.LocalAppColors
import com.example.util.AppStrings
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimeCard(
    anime: ShikimoriAnimeDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    watchProgress: Int? = null,
    isWatched: Boolean = false,
    category: FavoriteCategory? = null,
    onToggleWatched: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val appSettings by AppSettingsManager.settingsState.collectAsState()
    val colors = LocalAppColors.current

    // Live reactive episode updates and cover resolution
    val liveEpisodes by AnimeEpisodeHelper.liveEpisodesFlow.collectAsState()
    val liveCovers by AniListService.coversFlow.collectAsState()

    // Trigger background check for live voiced episodes from Kodik
    LaunchedEffect(anime.id) {
        AnimeEpisodeHelper.requestLiveUpdate(
            animeId = anime.id,
            name = anime.name,
            russian = anime.russian,
            episodesTotal = anime.episodes,
            episodesAired = anime.episodesAired,
            status = anime.status
        )
    }

    val posterUrl = liveCovers[anime.id.toString()] ?: AnimeRepository.resolveImageUrl(
        anime.image?.original ?: anime.image?.preview,
        animeId = anime.id,
        animeName = "${anime.russian ?: ""} ${anime.name}"
    )

    // Build resilient ordered candidate URLs so anime posters always load without failure
    val candidateUrls = remember(anime.id, posterUrl) {
        val list = mutableListOf<String>()
        if (posterUrl.isNotBlank()) list.add(posterUrl)
        if (anime.id > 0) {
            val shikiOrig = "https://shikimori.io/system/animes/original/${anime.id}.jpg"
            val shikiPrev = "https://shikimori.io/system/animes/preview/${anime.id}.jpg"
            val shikiDesu = "https://desu.shikimori.one/system/animes/original/${anime.id}.jpg"
            if (!list.contains(shikiOrig)) list.add(shikiOrig)
            if (!list.contains(shikiPrev)) list.add(shikiPrev)
            if (!list.contains(shikiDesu)) list.add(shikiDesu)
        }
        val orig = anime.image?.original
        if (!orig.isNullOrBlank()) {
            val resolved = AnimeRepository.resolveImageUrl(orig, anime.id, anime.name)
            if (resolved.isNotBlank() && !list.contains(resolved)) list.add(resolved)
        }
        val prev = anime.image?.preview
        if (!prev.isNullOrBlank()) {
            val resolved = AnimeRepository.resolveImageUrl(prev, anime.id, anime.name)
            if (resolved.isNotBlank() && !list.contains(resolved)) list.add(resolved)
        }
        list
    }

    var currentCandidateIndex by remember(anime.id, posterUrl) { mutableIntStateOf(0) }
    val activeUrl = candidateUrls.getOrNull(currentCandidateIndex) ?: posterUrl

    val displayName = AnimeTitleHelper.getLocalizedTitle(anime.id, anime.russian, anime.name)
    val isUk = AppSettingsManager.isUkrainian()
    val scoreText = anime.score?.takeIf { it.isNotBlank() && it != "0.0" } ?: "—"
    val kindText = when (anime.kind?.lowercase()) {
        "tv" -> "TV"
        "movie" -> if (isUk) "Фільм" else "Фильм"
        "ova" -> "OVA"
        "ona" -> "ONA"
        "special" -> if (isUk) "Спешл" else "Спешл"
        else -> anime.kind?.uppercase() ?: "TV"
    }
    val yearText = anime.airedOn?.take(4) ?: "2026"

    val effectivelyWatched = isWatched || category == FavoriteCategory.COMPLETED || (watchProgress != null && watchProgress >= 95)
    val isWatching = !effectivelyWatched && (category == FavoriteCategory.WATCHING || (watchProgress != null && watchProgress > 0))
    val isPlanned = !effectivelyWatched && !isWatching && category == FavoriteCategory.PLAN_TO_WATCH

    Card(
        modifier = modifier
            .testTag("anime_card_${anime.id}")
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(colors.surfaceVariant)
            ) {
                if (activeUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(activeUrl)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_poster_placeholder)
                            .error(R.drawable.ic_poster_placeholder)
                            .build(),
                        contentDescription = displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = {
                            if (currentCandidateIndex + 1 < candidateUrls.size) {
                                currentCandidateIndex++
                            } else {
                                AniListService.triggerBackgroundCoverLookup(anime.id, anime.name)
                            }
                        }
                    )
                } else {
                    // Graceful fallback when poster URL is not yet resolved (NEVER a pure black screen)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF2C2E3C), Color(0xFF161722))
                                )
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = colors.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = displayName,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Top-right rating badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            color = Color(0xDD0B0B0B),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(0.5.dp, Color(0x44FFC400), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Рейтинг",
                            tint = RatingGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = scoreText,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Top-left Kind badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(
                            brush = Brush.horizontalGradient(listOf(AccentOrange, PrimaryYellow)),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = kindText,
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Bottom gradient shadow
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Brush.verticalGradient(GradientDarkOverlay))
                )

                // Read/Watched status badge
                if (effectivelyWatched) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .background(
                                color = Color(0xEE1B5E20),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(0.5.dp, Color(0xFF81C784), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isUk) "Переглянуто" else "Просмотрено",
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (isWatching) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .background(
                                color = Color(0xEE3E2723),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(0.5.dp, AccentOrange, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = AccentOrange,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (watchProgress != null && watchProgress > 0) "$watchProgress%" else if (isUk) "Дивлюся" else "Смотрю",
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (isPlanned) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .background(
                                color = Color(0xEE263238),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(0.5.dp, PrimaryYellow, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = PrimaryYellow,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isUk) "У планах" else "В планах",
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Watch progress indicator bar
                if (watchProgress != null && watchProgress > 0) {
                    LinearProgressIndicator(
                        progress = { watchProgress / 100f },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp),
                        color = if (effectivelyWatched) Color(0xFF4CAF50) else colors.primary,
                        trackColor = Color(0x66000000)
                    )
                }
            }

            // Info section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                val epInfo = liveEpisodes[anime.id] ?: AnimeEpisodeHelper.getEpisodeInfo(anime, isUk)
                val episodesText = epInfo.formattedText

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = yearText,
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (episodesText.isNotBlank()) {
                        Text(
                            text = episodesText,
                            fontSize = 11.sp,
                            color = if (epInfo.isOngoing && epInfo.airedEpisodes != null && (epInfo.totalEpisodes == null || epInfo.airedEpisodes < epInfo.totalEpisodes)) {
                                Color(0xFF00E676)
                            } else {
                                colors.textMuted
                            },
                            fontWeight = if (epInfo.isOngoing) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
