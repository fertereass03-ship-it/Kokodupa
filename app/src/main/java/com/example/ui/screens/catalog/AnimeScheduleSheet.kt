package com.example.ui.screens.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.data.api.models.ScheduleItem
import com.example.data.repository.AnimeRepository
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Calendar

private val DAYS = listOf(
    0 to "Все",
    1 to "Пн",
    2 to "Вт",
    3 to "Ср",
    4 to "Чт",
    5 to "Пт",
    6 to "Сб",
    7 to "Вс"
)

private val DAY_FULL_NAMES = mapOf(
    1 to "Понедельник",
    2 to "Вторник",
    3 to "Среда",
    4 to "Четверг",
    5 to "Пятница",
    6 to "Суббота",
    7 to "Воскресенье"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeScheduleSheet(
    state: ScheduleUiState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onSearchChange: (String) -> Unit,
    onRetry: () -> Unit,
    onAnimeClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayIsoDay = remember {
        when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    // Filter items by day and search query
    val filteredItems = remember(state.items, state.selectedDay, state.searchQuery) {
        state.items.filter { item ->
            val matchesDay = (state.selectedDay == 0 || item.dayOfWeek == state.selectedDay)
            val matchesQuery = if (state.searchQuery.isBlank()) true else {
                val q = state.searchQuery.trim().lowercase()
                item.anime.name.lowercase().contains(q) ||
                        (item.anime.russian?.lowercase()?.contains(q) == true)
            }
            matchesDay && matchesQuery
        }
    }

    // Counts per day for badges
    val dayCounts = remember(state.items) {
        val map = mutableMapOf<Int, Int>()
        map[0] = state.items.size
        for (item in state.items) {
            map[item.dayOfWeek] = (map[item.dayOfWeek] ?: 0) + 1
        }
        map
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundDark,
        dragHandle = null,
        modifier = modifier.testTag("anime_schedule_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .background(BackgroundDark)
                .padding(top = 16.dp)
        ) {
            // --- Header Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FFC400)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = PrimaryYellow,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Расписание онгоингов",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Календарь Shikimori новинок 2026 по дням недели",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.testTag("schedule_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить расписание",
                        tint = PrimaryYellow
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("schedule_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Search Row ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Search in Schedule
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Поиск в расписании по названию...", fontSize = 13.sp, color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = PrimaryYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Очистить",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryYellow,
                        unfocusedBorderColor = Color(0x22FFFFFF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("schedule_search_input")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Day of Week Tabs ---
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_day_selector"),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DAYS) { (dayNum, dayLabel) ->
                    val isSelected = state.selectedDay == dayNum
                    val isToday = (dayNum == todayIsoDay)
                    val count = dayCounts[dayNum] ?: 0

                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectDay(dayNum) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dayLabel,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                if (isToday && dayNum != 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.Black else PrimaryYellow)
                                    )
                                }
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$count",
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.Black.copy(alpha = 0.75f) else TextMuted
                                    )
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary,
                            selectedContainerColor = PrimaryYellow,
                            selectedLabelColor = Color.Black
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color(0x22FFFFFF),
                            selectedBorderColor = PrimaryYellow,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("schedule_day_chip_$dayNum")
                    )
                }
            }

            // Subtitle with selected day info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentDayTitle = if (state.selectedDay == 0) "Всі онґоїнґи" else DAY_FULL_NAMES[state.selectedDay] ?: ""
                Text(
                    text = "$currentDayTitle (${filteredItems.size})",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = PrimaryYellow,
                        strokeWidth = 2.dp
                    )
                }
            }

            // --- Main Content: Grid of Schedule Items ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    state.isLoading && state.items.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = PrimaryYellow)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Загрузка расписания онгоингов...", color = TextMuted, fontSize = 13.sp)
                            }
                        }
                    }

                    state.errorMessage != null && state.items.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = state.errorMessage,
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Попробовать снова", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    filteredItems.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state.searchQuery.isNotBlank()) "Ничего не найдено по запросу" else "На этот день нет запланированных серий",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("schedule_grid"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filteredItems, key = { "${it.anime.id}_${it.dayOfWeek}" }) { scheduleItem ->
                                ScheduleAnimeCard(
                                    item = scheduleItem,
                                    onClick = { onAnimeClick(scheduleItem.anime.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleAnimeCard(
    item: ScheduleItem,
    onClick: () -> Unit
) {
    val anime = item.anime
    val context = LocalContext.current
    val liveCovers by com.example.data.api.AniListService.coversFlow.collectAsState()
    val liveEpisodes by com.example.data.api.AnimeEpisodeHelper.liveEpisodesFlow.collectAsState()

    androidx.compose.runtime.LaunchedEffect(anime.id) {
        com.example.data.api.AnimeEpisodeHelper.requestLiveUpdate(
            animeId = anime.id,
            name = anime.name,
            russian = anime.russian,
            episodesTotal = anime.episodes,
            episodesAired = anime.episodesAired,
            status = anime.status
        )
    }

    val posterUrl = liveCovers[anime.id.toString()] ?: remember(anime.id, anime.image?.original, anime.image?.preview) {
        val raw = anime.image?.original ?: anime.image?.preview
        AnimeRepository.resolveImageUrl(
            raw,
            animeId = anime.id,
            animeName = anime.name
        )
    }
    val displayName = anime.russian?.takeIf { it.isNotBlank() } ?: anime.name

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(
                1.dp,
                Color(0x33FFC400),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag("schedule_item_${anime.id}")
    ) {
        Column {
            // Poster Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(posterUrl.ifBlank { if (anime.id > 0) "https://shikimori.io/system/animes/original/${anime.id}.jpg" else "https://shikimori.io/system/animes/original/5114.jpg" })
                        .placeholder(com.example.R.drawable.ic_poster_placeholder)
                        .error(com.example.R.drawable.ic_poster_placeholder)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .crossfade(true)
                        .build(),
                    contentDescription = displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top badges row: Rating & Day/Time
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rating
                    if (!anime.score.isNullOrBlank() && anime.score != "0.0") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xCC000000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = PrimaryYellow,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = anime.score,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Time or day badge
                    if (item.formattedTime.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xDDFFC400))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.formattedTime,
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Aired of total / Next Episode pill at bottom of poster
                val epInfo = liveEpisodes[anime.id] ?: com.example.data.api.AnimeEpisodeHelper.getEpisodeInfo(anime)
                val epText = epInfo.formattedText.ifBlank {
                    if (item.nextEpisode != null && item.nextEpisode > 0) {
                        val aired = (item.nextEpisode - 1).coerceAtLeast(0)
                        val total = item.anime.episodes?.takeIf { it > 0 }
                            ?: if (aired > 12) (if (aired <= 14) 14 else 24) else 12
                        if (aired in 1 until total) "$aired из $total сер." else "${item.nextEpisode} серия"
                    } else ""
                }

                if (epText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xDD000000))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = epText,
                            color = PrimaryYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Anime info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!anime.name.equals(displayName, ignoreCase = true)) {
                    Text(
                        text = anime.name,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.dayName,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Смотреть",
                            color = PrimaryYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = PrimaryYellow,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
