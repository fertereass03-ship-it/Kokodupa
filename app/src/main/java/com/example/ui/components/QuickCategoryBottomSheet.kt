package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.RatingGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCategoryBottomSheet(
    anime: ShikimoriAnimeDto?,
    onDismiss: () -> Unit,
    onCategorySelected: (FavoriteCategory) -> Unit,
    onRemoveFavorite: (() -> Unit)? = null
) {
    if (anime == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val posterUrl = AnimeRepository.resolveImageUrl(
        anime.image?.original ?: anime.image?.preview,
        animeId = anime.id,
        animeName = "${anime.russian ?: ""} ${anime.name}"
    )
    val displayName = anime.russian?.takeIf { it.isNotBlank() } ?: anime.name
    val scoreText = anime.score?.takeIf { it.isNotBlank() && it != "0.0" } ?: "—"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x44FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Anime Header info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .border(1.dp, Color(0x22FFC400), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = displayName,
                    modifier = Modifier
                        .size(width = 52.dp, height = 72.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = RatingGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = scoreText,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${anime.kind?.uppercase() ?: "TV"} • ${anime.airedOn?.take(4) ?: "2026"}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Добавить в список:",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Category options
            CategoryOptionItem(
                title = "Переглянуто / Просмотрено",
                subtitle = "Тайтл повністю завершено",
                icon = Icons.Default.CheckCircle,
                accentColor = Color(0xFF4CAF50),
                testTag = "category_completed_btn",
                onClick = {
                    onCategorySelected(FavoriteCategory.COMPLETED)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category options
            CategoryOptionItem(
                title = "У планах / В планах",
                subtitle = "Хочу подивитися пізніше",
                icon = Icons.Default.Bookmark,
                accentColor = PrimaryYellow,
                testTag = "category_plan_btn",
                onClick = {
                    onCategorySelected(FavoriteCategory.PLAN_TO_WATCH)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            CategoryOptionItem(
                title = "Дивлюся / Смотрю",
                subtitle = "У процесі активного перегляду",
                icon = Icons.Default.PlayCircle,
                accentColor = AccentOrange,
                testTag = "category_watching_btn",
                onClick = {
                    onCategorySelected(FavoriteCategory.WATCHING)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            CategoryOptionItem(
                title = "Кинуто / Брошено",
                subtitle = "Перегляд зупинено",
                icon = Icons.Default.Cancel,
                accentColor = Color(0xFFE53935),
                testTag = "category_dropped_btn",
                onClick = {
                    onCategorySelected(FavoriteCategory.DROPPED)
                    onDismiss()
                }
            )

            if (onRemoveFavorite != null) {
                Spacer(modifier = Modifier.height(8.dp))
                CategoryOptionItem(
                    title = "Удалить из списков",
                    subtitle = "Убрать из избранного и закладок",
                    icon = Icons.Default.Delete,
                    accentColor = TextMuted,
                    testTag = "category_remove_btn",
                    onClick = {
                        onRemoveFavorite()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(0.5.dp, Color(0x33333333), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}
