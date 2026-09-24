package com.example.ui.screens.friends

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.FriendEntity
import com.example.data.settings.AppSettingsManager
import com.example.ui.components.EmptyStateView
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.LocalAppColors

private enum class FriendsTab {
    ALL, INCOMING, OUTGOING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onOpenChat: (friendId: String, friendName: String) -> Unit,
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()

    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val incomingRequests by viewModel.incomingRequests.collectAsStateWithLifecycle()
    val outgoingRequests by viewModel.outgoingRequests.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(FriendsTab.ALL) }
    var friendToDelete by remember { mutableStateOf<FriendEntity?>(null) }

    // Modern Delete Confirmation Dialog
    if (friendToDelete != null) {
        AlertDialog(
            onDismissRequest = { friendToDelete = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = colors.surface,
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5252).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (isUk) "Видалити з друзів?" else "Удалить из друзей?",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = if (isUk)
                        "Ви впевнені, що хочете видалити ${friendToDelete?.name} зі списку друзів? Історія листування залишиться збереженою."
                    else
                        "Вы уверены, что хотите удалить ${friendToDelete?.name} из списка друзей? История переписки останется сохранённой.",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        friendToDelete?.let { viewModel.removeFriend(it.userId) }
                        friendToDelete = null
                        Toast.makeText(
                            context,
                            if (isUk) "Друга видалено" else "Друг удалён",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = if (isUk) "Видалити" else "Удалить",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { friendToDelete = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isUk) "Скасувати" else "Отмена",
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Modern Top App Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isUk) "Друзі та Спільнота" else "Друзья и Сообщество",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        fontSize = 24.sp
                    )
                )
                if (activeUser != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceVariant.copy(alpha = 0.6f))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(currentUserId))
                                Toast.makeText(
                                    context,
                                    if (isUk) "ID скопійовано: $currentUserId" else "ID скопирован в буфер: $currentUserId",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ID: $currentUserId",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = if (isUk) "Копіювати ID" else "Копировать ID",
                            tint = colors.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    Text(
                        text = if (isUk) "Вхід не виконано" else "Вход не выполнен",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AccentOrange,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Modern Add Friend Action Button
            IconButton(
                onClick = {
                    if (activeUser != null) {
                        viewModel.openAddDialog()
                    } else {
                        onNavigateToProfile()
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(colors.primary, colors.secondary))
                    )
                    .shadow(elevation = 6.dp, shape = CircleShape, ambientColor = colors.primary)
                    .testTag("add_friend_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = if (isUk) "Додати друга" else "Добавить друга",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (activeUser == null) {
            // Not logged in empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
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
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.15f))
                                .border(1.5.dp, colors.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = if (isUk) "Друзі та спілкування" else "Друзья и общение",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isUk)
                                "Знаходьте друзів за унікальним ID або нікнеймом, додавайте їх до списку, обмінюйтесь повідомленнями та діліться тайтлами. Доступно після створення або входу в акаунт."
                            else
                                "Находите друзей по уникальному ID или никнейму, добавляйте их в список, обменивайтесь сообщениями и делитесь тайтлами. Доступно после создания или входа в аккаунт.",
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
                                .testTag("friends_login_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp)
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
            val hasAnyData = incomingRequests.isNotEmpty() || outgoingRequests.isNotEmpty() || friends.isNotEmpty()

            if (!hasAnyData) {
                EmptyStateView(
                    title = if (isUk) "У вас поки немає друзів" else "У вас пока нет друзей",
                    subtitle = if (isUk)
                        "Натисніть кнопку додавання, щоб знайти друзів за ID або нікнеймом. Після підтвердження заявки ви зможете листуватися!"
                    else
                        "Нажмите кнопку добавления, чтобы найти друзей по ID или имени. После подтверждения заявки вы сможете переписываться!",
                    actionTitle = if (isUk) "Знайти і додати друга" else "Найти и добавить друга",
                    onAction = { viewModel.openAddDialog() }
                )
            } else {
                // Sleek Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterTabChip(
                            label = if (isUk) "Всі друзі" else "Все друзья",
                            count = friends.size,
                            isSelected = selectedTab == FriendsTab.ALL,
                            onClick = { selectedTab = FriendsTab.ALL }
                        )
                    }

                    if (incomingRequests.isNotEmpty()) {
                        item {
                            FilterTabChip(
                                label = if (isUk) "Вхідні заявки" else "Входящие",
                                count = incomingRequests.size,
                                isSelected = selectedTab == FriendsTab.INCOMING,
                                isHighlighted = true,
                                onClick = { selectedTab = FriendsTab.INCOMING }
                            )
                        }
                    }

                    if (outgoingRequests.isNotEmpty()) {
                        item {
                            FilterTabChip(
                                label = if (isUk) "Очікують" else "Ожидают",
                                count = outgoingRequests.size,
                                isSelected = selectedTab == FriendsTab.OUTGOING,
                                onClick = { selectedTab = FriendsTab.OUTGOING }
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // --- Incoming Requests Section ---
                    if ((selectedTab == FriendsTab.ALL || selectedTab == FriendsTab.INCOMING) && incomingRequests.isNotEmpty()) {
                        item {
                            Text(
                                text = if (isUk) "Вхідні заявки (${incomingRequests.size})" else "Входящие заявки (${incomingRequests.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                ),
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        items(incomingRequests, key = { "in_${it.userId}" }) { req ->
                            IncomingRequestCard(
                                request = req,
                                onAccept = { viewModel.acceptFriendRequest(req.userId) },
                                onDecline = { viewModel.declineFriendRequest(req.userId) }
                            )
                        }
                    }

                    // --- Outgoing Requests Section ---
                    if ((selectedTab == FriendsTab.ALL || selectedTab == FriendsTab.OUTGOING) && outgoingRequests.isNotEmpty()) {
                        item {
                            Text(
                                text = if (isUk) "Очікують підтвердження (${outgoingRequests.size})" else "Ожидают подтверждения (${outgoingRequests.size})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                ),
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                            )
                        }

                        items(outgoingRequests, key = { "out_${it.userId}" }) { req ->
                            OutgoingRequestCard(
                                request = req,
                                onChat = { onOpenChat(req.userId, req.name) },
                                onCancel = { viewModel.cancelFriendRequest(req.userId) }
                            )
                        }
                    }

                    // --- Confirmed Friends Section ---
                    if (selectedTab == FriendsTab.ALL || selectedTab == FriendsTab.INCOMING && incomingRequests.isEmpty()) {
                        item {
                            Text(
                                text = if (isUk) "Мої друзі (${friends.size})" else "Мои друзья (${friends.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                ),
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }

                        if (friends.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.glassBorder)
                                ) {
                                    Text(
                                        text = if (isUk)
                                            "Поки що немає підтверджених друзів. Коли друг прийме вашу заявку, він з'явиться тут і ви зможете відкрити чат!"
                                        else
                                            "Пока нет подтвержденных друзей. Когда друг примет вашу заявку, он появится здесь и вы сможете открыть чат!",
                                        color = colors.textMuted,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(friends, key = { it.userId }) { friend ->
                                FriendListItem(
                                    friend = friend,
                                    onChatClick = { onOpenChat(friend.userId, friend.name) },
                                    onDeleteClick = { friendToDelete = friend }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Add Friend Bottom Sheet Dialog ---
    if (state.isAddDialogOpen) {
        AddFriendBottomSheet(
            query = state.searchQuery,
            isSearching = state.isSearching,
            searchResult = state.searchResult,
            notice = state.messageNotice,
            currentUserId = currentUserId,
            friends = friends,
            outgoingRequests = outgoingRequests,
            incomingRequests = incomingRequests,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onSearch = { viewModel.searchUserById() },
            onAdd = { viewModel.addFriend(it) },
            onAccept = { viewModel.acceptFriendRequest(it) },
            onOpenChat = { uId, uName ->
                viewModel.closeAddDialog()
                onOpenChat(uId, uName)
            },
            onDismiss = { viewModel.closeAddDialog() }
        )
    }
}

@Composable
private fun FilterTabChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current

    val bgColor = when {
        isSelected -> colors.primary
        isHighlighted -> colors.primary.copy(alpha = 0.15f)
        else -> colors.surface
    }

    val contentColor = when {
        isSelected -> Color.Black
        isHighlighted -> colors.primary
        else -> colors.textSecondary
    }

    val borderColor = when {
        isSelected -> colors.primary
        isHighlighted -> colors.primary.copy(alpha = 0.5f)
        else -> colors.glassBorder
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (isSelected) Color.Black.copy(alpha = 0.18f) else colors.surfaceVariant
                )
                .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: FriendEntity,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .testTag("incoming_request_${request.userId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(50.dp)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant)
                        .border(1.5.dp, colors.primary.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        avatarUrl = request.avatarUrl,
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = request.name,
                        showVideoBadge = false
                    )
                }

                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFF4CAF50), CircleShape)
                        .border(2.dp, colors.surface, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = if (isUk) "Хоче додати вас у друзі" else "Хочет добавить вас в друзья",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.primary, fontSize = 12.sp)
                )
                Text(
                    text = "ID: ${request.userId}",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Accept button
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("accept_request_${request.userId}")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isUk) "Прийняти" else "Принять", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Decline button
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFF5252).copy(alpha = 0.12f))
                        .testTag("decline_request_${request.userId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isUk) "Відхилити" else "Отклонить",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OutgoingRequestCard(
    request: FriendEntity,
    onChat: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
            .testTag("outgoing_request_${request.userId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .border(1.dp, colors.glassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                UserAvatar(
                    avatarUrl = request.avatarUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = request.name,
                    showVideoBadge = false
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = if (isUk) "Заявку надіслано" else "Заявка отправлена",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textMuted, fontSize = 11.sp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Open Chat
                IconButton(
                    onClick = onChat,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.primary.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Чат",
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Cancel Request
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("cancel_request_${request.userId}"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.glassBorder)
                ) {
                    Text(
                        text = if (isUk) "Скасувати" else "Отменить",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

/**
 * Modernized Friend Item Card with:
 * - Theme-adaptive styling (blends with light/dark seamlessly)
 * - Online / Offline indicator badge
 * - Quick Chat action
 * - Beautiful, modern soft-danger Delete button
 */
@Composable
private fun FriendListItem(
    friend: FriendEntity,
    onChatClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()

    val isOnline = (System.currentTimeMillis() - friend.lastActive < 4 * 60 * 1000) ||
            friend.status.equals("В сети", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, colors.glassBorder, RoundedCornerShape(18.dp))
            .clickable { onChatClick() }
            .testTag("friend_item_${friend.userId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Friend Avatar with Online / Offline indicator
            Box(modifier = Modifier.size(52.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant)
                        .border(1.5.dp, colors.primary.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        avatarUrl = friend.avatarUrl,
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = friend.name,
                        showVideoBadge = false
                    )
                }

                // Online/Offline status dot
                Box(
                    modifier = Modifier
                        .size(14.dp)
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
                    text = friend.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ID: ${friend.userId}",
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                }
            }

            // Chat Action Button
            IconButton(
                onClick = onChatClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = 0.15f))
                    .testTag("chat_friend_${friend.userId}")
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = if (isUk) "Чат" else "Чат",
                    tint = colors.primary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Modern, Sleek Delete Button ("кнопка видалення")
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF5252).copy(alpha = 0.10f))
                    .border(
                        1.dp,
                        Color(0xFFFF5252).copy(alpha = 0.25f),
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("delete_friend_${friend.userId}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = if (isUk) "Видалити друга" else "Удалить друга",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFriendBottomSheet(
    query: String,
    isSearching: Boolean,
    searchResult: FriendEntity?,
    notice: String?,
    currentUserId: String,
    friends: List<FriendEntity>,
    outgoingRequests: List<FriendEntity>,
    incomingRequests: List<FriendEntity>,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAdd: (FriendEntity) -> Unit,
    onAccept: (String) -> Unit,
    onOpenChat: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isUk = AppSettingsManager.isUkrainian()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.glassBorder.copy(alpha = 0.8f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Modern Header with glowing icon badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(colors.primary.copy(alpha = 0.25f), colors.primary.copy(alpha = 0.08f))
                            )
                        )
                        .border(1.dp, colors.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUk) "Додати друга" else "Добавить друга",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isUk) "Знайдіть друзів за ніком, ID або email" else "Найдите друзей по нику, ID или email",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Quick Copy ID banner
            if (currentUserId.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.55f))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(currentUserId))
                            Toast.makeText(
                                context,
                                if (isUk) "Ваш ID скопійовано: $currentUserId" else "Ваш ID скопирован: $currentUserId",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isUk) "Ваш ID:" else "Ваш ID:",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentUserId,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Скопировать",
                                tint = colors.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isUk) "Скопіювати" else "Скопировать",
                                color = colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Modern Search Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = if (isUk) "Введіть нік або ID..." else "Введите ник или ID...",
                            color = colors.textMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (query.isNotBlank()) colors.primary else colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Очистить",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceVariant,
                        unfocusedContainerColor = colors.surfaceVariant.copy(alpha = 0.7f),
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.primary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (query.isNotBlank()) colors.primary.copy(alpha = 0.7f) else colors.glassBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .testTag("friend_id_input")
                )

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onSearch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("friend_search_button")
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = if (isUk) "Знайти" else "Найти",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Notice / Alert
            if (notice != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentOrange.copy(alpha = 0.12f))
                        .border(1.dp, AccentOrange.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = notice,
                        color = AccentOrange,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Search Result Card
            if (searchResult != null) {
                val isAlreadyFriend = friends.any { it.userId.equals(searchResult.userId, ignoreCase = true) }
                val isOutgoing = outgoingRequests.any { it.userId.equals(searchResult.userId, ignoreCase = true) } ||
                        (searchResult.isPending && !searchResult.isIncoming)
                val isIncoming = incomingRequests.any { it.userId.equals(searchResult.userId, ignoreCase = true) } ||
                        (searchResult.isPending && searchResult.isIncoming)

                Spacer(modifier = Modifier.height(18.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, colors.primary.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .testTag("friend_search_result_card"),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isUk) "Знайдений профіль" else "Найденный профиль",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(60.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(colors.background)
                                        .border(2.dp, colors.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    UserAvatar(
                                        avatarUrl = searchResult.avatarUrl,
                                        modifier = Modifier.fillMaxSize(),
                                        contentDescription = searchResult.name,
                                        showVideoBadge = false
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.BottomEnd)
                                        .background(Color(0xFF4CAF50), CircleShape)
                                        .border(2.dp, colors.surfaceVariant, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = searchResult.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "ID: ${searchResult.userId}",
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        when {
                            isAlreadyFriend -> {
                                Text(
                                    text = if (isUk) "✓ Цей користувач вже у ваших друзях" else "✓ Этот пользователь уже у вас в друзьях",
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Button(
                                    onClick = { onOpenChat(searchResult.userId, searchResult.name) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isUk) "Написати повідомлення" else "Написать сообщение",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            isIncoming -> {
                                Text(
                                    text = if (isUk) "📩 Користувач надіслав вам запит" else "📩 Пользователь отправил вам заявку",
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Button(
                                    onClick = {
                                        onAccept(searchResult.userId)
                                        onOpenChat(searchResult.userId, searchResult.name)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isUk) "Прийняти заявку і відкрити чат" else "Принять заявку и открыть чат",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            isOutgoing -> {
                                Text(
                                    text = if (isUk) "⏳ Заявка очікує підтвердження" else "⏳ Заявка ожидает подтверждения",
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Button(
                                    onClick = { onOpenChat(searchResult.userId, searchResult.name) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isUk) "Відкрити чат" else "Открыть чат",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { onAdd(searchResult) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("confirm_add_friend_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isUk) "Надіслати заявку в друзі" else "Отправить заявку в друзья",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
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
