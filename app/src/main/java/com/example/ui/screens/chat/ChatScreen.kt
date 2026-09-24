package com.example.ui.screens.chat

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.ChatMessageEntity
import com.example.data.settings.AppSettingsManager
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.PrimaryYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAnimeClick: (Long) -> Unit = {}
) {
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val friend by viewModel.friend.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val isOnline = friend?.let {
        (System.currentTimeMillis() - it.lastActive < 4 * 60 * 1000) || it.status.equals("В сети", ignoreCase = true)
    } ?: true
    val friendDisplayName = friend?.name?.ifBlank { viewModel.friendName } ?: viewModel.friendName
    val friendAvatarUrl = friend?.avatarUrl.orEmpty()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
    ) {
        // Modern Top App Bar with theme integration
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .statusBarsPadding()
                .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                .border(
                    width = 0.5.dp,
                    color = colors.glassBorder.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .testTag("chat_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isUk) "Назад" else "Назад",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Friend Avatar with Online / Offline Badge
            Box(modifier = Modifier.size(44.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant)
                        .border(1.5.dp, colors.primary.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        avatarUrl = friendAvatarUrl,
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = friendDisplayName,
                        showVideoBadge = false
                    )
                }

                // Online indicator
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .align(Alignment.BottomEnd)
                        .background(
                            if (isOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                            CircleShape
                        )
                        .border(2.dp, colors.surface, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friendDisplayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (isOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isOnline) {
                            if (isUk) "В мережі" else "В сети"
                        } else {
                            if (isUk) "Не в мережі" else "Не в сети"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isOnline) Color(0xFF4CAF50) else colors.textMuted,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Messages List
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isUk) "Поки немає повідомлень" else "Пока нет сообщений",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isUk)
                            "Напишіть першим або надішліть ID аніме (наприклад #16498), щоб поділитися тайтлом!"
                        else
                            "Напишите первым или отправьте ID аниме (например #16498), чтобы поделиться тайтлом!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        message = msg,
                        friendAvatarUrl = friendAvatarUrl,
                        friendDisplayName = friendDisplayName,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        // Modern Bottom Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .border(
                    width = 0.5.dp,
                    color = colors.glassBorder.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = state.inputText,
                onValueChange = { viewModel.onInputTextChange(it) },
                placeholder = {
                    Text(
                        text = if (isUk) "Повідомлення або #ID аніме..." else "Сообщение или #ID аниме...",
                        color = colors.textMuted,
                        fontSize = 14.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceVariant,
                    unfocusedContainerColor = colors.surfaceVariant,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input")
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = { viewModel.sendMessage() },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(listOf(colors.primary, colors.secondary))
                    )
                    .shadow(elevation = 6.dp, shape = CircleShape, ambientColor = colors.primary)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (isUk) "Надіслати" else "Отправить",
                    tint = Color.Black,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessageEntity,
    friendAvatarUrl: String = "",
    friendDisplayName: String = "",
    onAnimeClick: (Long) -> Unit
) {
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()
    val isMine = message.isMine
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    // Parse potential anime ID from text (e.g. #16498, ID: 16498, anime:16498)
    val animeId = extractAnimeId(message.text)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .border(1.dp, colors.primary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                UserAvatar(
                    avatarUrl = friendAvatarUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = friendDisplayName,
                    showVideoBadge = false
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMine) 18.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 18.dp
                    )
                )
                .background(
                    if (isMine) {
                        Brush.horizontalGradient(listOf(colors.secondary, colors.primary))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                colors.surface,
                                colors.surfaceVariant
                            )
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isMine) Color.Transparent else colors.glassBorder,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMine) 18.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 18.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = if (isMine) Color(0xFF111111) else colors.textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )

                // If message contains an anime ID, render a modern interactive link card
                if (animeId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isMine) Color(0x33000000) else colors.primary.copy(alpha = 0.12f)
                            )
                            .border(
                                1.dp,
                                if (isMine) Color(0x44000000) else colors.primary.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onAnimeClick(animeId) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("chat_anime_id_link_$animeId"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = if (isUk) "Дивитися аніме" else "Смотреть аниме",
                            tint = if (isMine) Color(0xFF111111) else colors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isUk) "Аніме #$animeId" else "Аниме #$animeId",
                                color = if (isMine) Color(0xFF111111) else colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isUk) "Натисніть для перегляду" else "Нажмите для просмотра",
                                color = if (isMine) Color(0x99000000) else colors.textSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = if (isMine) Color(0xFF111111) else colors.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        color = if (isMine) Color(0x99000000) else colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = Color(0x99000000),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun extractAnimeId(text: String): Long? {
    val regex = Regex("""(?:#|ID:?\s*#?|аниме\s*#?|anime\s*#?|animes/|id=)(\d{1,9})""", RegexOption.IGNORE_CASE)
    val match = regex.find(text)
    return match?.groupValues?.getOrNull(1)?.toLongOrNull()
}
