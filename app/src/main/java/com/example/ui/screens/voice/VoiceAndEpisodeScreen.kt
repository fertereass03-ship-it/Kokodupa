package com.example.ui.screens.voice

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.models.AnimeEpisode
import com.example.data.api.models.AnimeVoiceTranslation
import com.example.ui.components.ErrorStateView
import com.example.ui.components.SkeletonGrid
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VoiceAndEpisodeScreen(
    viewModel: VoiceAndEpisodeViewModel,
    onBackClick: () -> Unit,
    onEpisodeSelected: (episodeNum: Int, voiceName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(42.dp)
                    .background(SurfaceCard, CircleShape)
                    .border(1.dp, Color(0x33FFC400), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = state.anime?.russian ?: state.anime?.name ?: if (state.isMovie) "Аниме-фильм" else "Выбор серии",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (state.isMovie) "Озвучка фильма" else "Озвучка и серии",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        if (state.isLoading) {
            SkeletonGrid(modifier = Modifier.fillMaxSize())
            return
        }

        if (state.errorMessage != null) {
            ErrorStateView(
                message = state.errorMessage.orEmpty(),
                onRetry = { viewModel.loadData() }
            )
            return
        }

        // --- 1. Horizontal Dubbing Voice Selector ---
        Text(
            text = if (state.isMovie) "Озвучки фильма (много вариантов)" else "Много озвучек",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(state.voices) { voice ->
                val isSelected = voice.id == state.selectedVoice?.id
                VoiceChip(
                    voice = voice,
                    isSelected = isSelected,
                    onClick = { viewModel.selectVoice(voice) }
                )
            }
        }

        // --- 2. Movie Card or Episode Selection Grid ---
        if (state.isMovie) {
            Text(
                text = "Фильм",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .border(1.dp, Color(0x33FFC400), RoundedCornerShape(16.dp))
                    .clickable {
                        val voiceName = state.selectedVoice?.name ?: "AniLibria"
                        onEpisodeSelected(1, voiceName)
                    }
                    .padding(18.dp)
                    .testTag("movie_play_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(PrimaryYellow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Смотреть фильм",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Полная версия фильма",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Озвучка: ${state.selectedVoice?.name ?: "Основная"} • HLS / Kodik Stream",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Список серий (${state.episodes.size})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                state.selectedVoice?.let {
                    Text(
                        text = it.name,
                        color = PrimaryYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.episodes, key = { it.number }) { episode ->
                    EpisodeItemCard(
                        episode = episode,
                        onClick = {
                            val voiceName = state.selectedVoice?.name ?: "AniLibria"
                            onEpisodeSelected(episode.number, voiceName)
                        },
                        onToggleWatched = {
                            viewModel.toggleEpisodeWatched(episode.number)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceChip(
    voice: AnimeVoiceTranslation,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PrimaryYellow else SurfaceCard)
            .border(1.dp, if (isSelected) PrimaryYellow else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("voice_chip_${voice.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = null,
                tint = if (isSelected) Color.Black else PrimaryYellow,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = voice.name,
                color = if (isSelected) Color.Black else TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun EpisodeItemCard(
    episode: AnimeEpisode,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("episode_card_${episode.number}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (episode.isWatched || episode.watchedPercent >= 90) Color(0x66FFC400) else Color(0x1FFFFFFF)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(SurfaceVariantDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Серия ${episode.number}",
                        tint = PrimaryYellow,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable { onToggleWatched() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (episode.isWatched || episode.watchedPercent >= 90) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Outlined.CheckCircle
                        },
                        contentDescription = if (episode.isWatched) "Просмотрено" else "Отметить просмотренным",
                        tint = if (episode.isWatched || episode.watchedPercent >= 90) {
                            PrimaryYellow
                        } else {
                            Color(0x33FFFFFF)
                        },
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Серия ${episode.number}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Text(
                text = "${episode.durationMin} мин",
                fontSize = 11.sp,
                color = TextMuted
            )

            if (episode.watchedPercent > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { episode.watchedPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = PrimaryYellow,
                    trackColor = SurfaceVariantDark
                )
            }
        }
    }
}
