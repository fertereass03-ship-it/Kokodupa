package com.example.ui.screens.profile

import com.example.R
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.data.db.PlayerSettingsEntity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.db.UserAccountEntity
import com.example.ui.components.AvatarCropFitDialog
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.GradientOrangeYellow
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AvatarManager
import com.example.util.AppStrings
import com.example.data.settings.AppSettingsManager
import com.example.ui.theme.LocalAppColors
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onAnimeClick: (Long) -> Unit = { _ -> },
    onWatchClick: (Long, String) -> Unit = { _, _ -> },
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToSetupProfile: (email: String, displayName: String, avatarUrl: String) -> Unit = { _, _, _ -> },
    onNavigateToAuth: () -> Unit = {},
    onLogoutSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val settings by viewModel.playerSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var showSettingsCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var settingsInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var showAuthSheet by remember { mutableStateOf(false) }
    var authSheetInitialTab by remember { mutableIntStateOf(0) }
    var showChangeNicknameDialog by remember { mutableStateOf(false) }
    var showAvatarPickerDialog by remember { mutableStateOf(false) }
    var isAvatarProcessing by remember { mutableStateOf(false) }
    var fittingMediaPath by remember { mutableStateOf<String?>(null) }

    // Android Photo Picker Launcher (zero-permission compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isAvatarProcessing = true
            coroutineScope.launch {
                val saveResult = AvatarManager.saveAvatarFromUri(context, uri)
                isAvatarProcessing = false
                saveResult.onSuccess { localPath ->
                    fittingMediaPath = localPath
                }.onFailure { ex ->
                    Toast.makeText(
                        context,
                        ex.message ?: "Ошибка при выборе файла",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val appSettings by AppSettingsManager.settingsState.collectAsState()
    val colors = LocalAppColors.current
    val isUk = AppSettingsManager.isUkrainian()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 0. Top Bar ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.profileTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                IconButton(
                    onClick = {
                        settingsInitialTab = 0
                        showSettingsSheet = true
                    },
                    modifier = Modifier.testTag("profile_top_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = AppStrings.settingsTitle,
                        tint = colors.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // --- 1. User Header Profile ---
        item {
            val user = currentUser
            if (user != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_user_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar with Clickable Picker & Animated Border
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .border(2.5.dp, Brush.horizontalGradient(GradientOrangeYellow), CircleShape)
                                    .clickable { showAvatarPickerDialog = true }
                                    .testTag("profile_avatar_clickable")
                            ) {
                                UserAvatar(
                                    avatarUrl = user.avatarUrl,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isAvatarProcessing) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0x88000000)),
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

                            // Camera / Edit overlay badge
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                                    .border(2.dp, colors.background, CircleShape)
                                    .clickable { showAvatarPickerDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = if (isUk) "Змінити аватар" else "Сменить аватар",
                                    tint = Color.Black,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isUk) "Натисніть на фото для зміни (Фото ≤ 5МБ, GIF ≤ 20МБ)" else "Нажмите на фото для смены (Фото ≤ 5МБ, GIF ≤ 20МБ)",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // User Display Name & Edit Pencil Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showChangeNicknameDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { showChangeNicknameDialog = true },
                                modifier = Modifier
                                    .size(26.dp)
                                    .testTag("edit_nickname_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = if (isUk) "Змінити нікнейм" else "Изменить никнейм",
                                    tint = colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Email & Provider badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = user.email,
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (user.authProvider == "GOOGLE") Color(0x334285F4)
                                        else colors.primary.copy(alpha = 0.2f)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (user.authProvider == "GOOGLE") "Google" else "Email",
                                    color = if (user.authProvider == "GOOGLE") Color(0xFF82B1FF) else colors.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // User ID with copy button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceVariant)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(user.userId))
                                    val idMsg = if (isUk) "ID скопійовано: ${user.userId}" else "ID скопирован: ${user.userId}"
                                    Toast.makeText(context, idMsg, Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("copy_user_id_button")
                        ) {
                            Text(
                                text = "ID: ${user.userId}",
                                color = colors.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = if (isUk) "Копіювати ID" else "Копировать ID",
                                tint = colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Profile actions: Edit Nickname & Logout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showChangeNicknameDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_nickname_action_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isUk) "Змінити нікнейм" else "Сменить никнейм", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.logout(
                                        onComplete = {
                                            val outMsg = if (isUk) "Ви вийшли з акаунта" else "Вы вышли из аккаунта"
                                            Toast.makeText(context, outMsg, Toast.LENGTH_SHORT).show()
                                            onLogoutSuccess()
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("logout_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = colors.textMuted
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.glassBorder),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isUk) "Вийти" else "Выйти", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                // Not logged in Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guest_profile_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceVariant)
                                .border(1.5.dp, colors.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (isUk) "Особистий кабінет ANIWERTI" else "Личный кабинет ANIWERTI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isUk) "Створіть акаунт за допомогою Email, щоб ваш прогрес та закладки надійно зберігалися."
                            else "Создайте аккаунт с помощью Email, чтобы ваш прогресс и закладки сохранялись надёжно.",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Create Account Button
                        Button(
                            onClick = {
                                authSheetInitialTab = 0
                                showAuthSheet = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("register_new_account_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isUk) "Створити акаунт" else "Создать аккаунт",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Login Existing Account Button
                        OutlinedButton(
                            onClick = {
                                authSheetInitialTab = 1
                                showAuthSheet = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("login_existing_account_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colors.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isUk) "Вже є акаунт? Увійти" else "Уже есть аккаунт? Войти",
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }


        // --- 3. Bookmarks Quick Access Card ---
        if (currentUser != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToFavorites() }
                        .testTag("my_bookmarks_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.glassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (isUk) "Мої закладки та обране" else "Мои закладки и избранное",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isUk) "Збережені тайтли та списки для перегляду" else "Сохранённые тайтлы и списки для просмотра",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // --- 4. Multi-Account Section ---
        val otherAccounts = allAccounts.filter { it.userId != currentUser?.userId }
        if (otherAccounts.isNotEmpty() || currentUser != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.glassBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SwitchAccount,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isUk) "Акаунти на пристрої" else "Аккаунты на устройстве",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                )
                            }
                            TextButton(
                                onClick = {
                                    authSheetInitialTab = 0
                                    showAuthSheet = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isUk) "Створити новий" else "Создать новый", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (otherAccounts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            otherAccounts.forEach { acc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.surfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                                .background(colors.primary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            UserAvatar(
                                                avatarUrl = acc.avatarUrl,
                                                modifier = Modifier.fillMaxSize(),
                                                showVideoBadge = false
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = acc.displayName,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = acc.email,
                                                color = colors.textSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(
                                            onClick = {
                                                viewModel.switchAccount(acc.userId)
                                                val swMsg = if (isUk) "Перемкнуто на ${acc.displayName}" else "Переключено на ${acc.displayName}"
                                                Toast.makeText(context, swMsg, Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(if (isUk) "Увійти" else "Войти", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.removeSavedAccount(acc.userId)
                                                val remMsg = if (isUk) "Акаунт видалено з пристрою" else "Аккаунт удалён с устройства"
                                                Toast.makeText(context, remMsg, Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = if (isUk) "Видалити з пристрою" else "Удалить с устройства",
                                                tint = colors.textMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs & BottomSheets ---

    // Auth BottomSheet (Register / Login with Email)
    if (showAuthSheet) {
        AuthBottomSheet(
            initialTab = authSheetInitialTab,
            onDismiss = { showAuthSheet = false },
            onRegisterEmail = { email, password, name, avatar ->
                viewModel.registerWithEmail(
                    email = email,
                    password = password,
                    displayName = name,
                    avatarUrl = avatar,
                    onSuccess = {
                        showAuthSheet = false
                        Toast.makeText(context, "Аккаунт успешно создан!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onLoginEmail = { email, password ->
                viewModel.loginWithEmail(
                    email = email,
                    password = password,
                    onSuccess = {
                        showAuthSheet = false
                        Toast.makeText(context, "Вход выполнен успешно!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    // 3. Change Nickname Dialog
    if (showChangeNicknameDialog && currentUser != null) {
        ChangeNicknameDialog(
            currentName = currentUser!!.displayName,
            onDismiss = { showChangeNicknameDialog = false },
            onSave = { newName ->
                showChangeNicknameDialog = false
                viewModel.updateDisplayName(
                    displayName = newName,
                    onSuccess = {
                        Toast.makeText(context, "Никнейм изменен на \"$newName\"", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    // 4. Avatar Source Chooser Dialog (Gallery + Standard/Fit)
    if (showAvatarPickerDialog) {
        val currentAv = currentUser?.avatarUrl ?: ""
        val canAdjustCurrent = currentAv.isNotBlank() &&
                currentAv != AvatarManager.APP_ICON_AVATAR

        AvatarSourceChooserDialog(
            currentAvatar = currentAv,
            onDismiss = { showAvatarPickerDialog = false },
            onPickFromGallery = {
                showAvatarPickerDialog = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onAdjustCurrent = if (canAdjustCurrent) {
                {
                    showAvatarPickerDialog = false
                    fittingMediaPath = currentAv
                }
            } else null,
            onSelectPreset = { presetUrl ->
                showAvatarPickerDialog = false
                viewModel.updateAvatar(
                    avatarUrl = presetUrl,
                    onSuccess = {
                        Toast.makeText(context, "Аватарка обновлена!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    // Interactive Avatar Cropping & Fitting Dialog
    if (fittingMediaPath != null) {
        AvatarCropFitDialog(
            mediaPath = fittingMediaPath!!,
            onDismiss = { fittingMediaPath = null },
            onConfirm = { finalAvatarUrl ->
                fittingMediaPath = null
                viewModel.updateAvatar(
                    avatarUrl = finalAvatarUrl,
                    onSuccess = {
                        Toast.makeText(context, "Аватарка успішно оновлена!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    // 5. Full Settings BottomSheet (App Settings + Player Settings)
    if (showSettingsSheet) {
        FullSettingsBottomSheet(
            initialTab = settingsInitialTab,
            settings = settings,
            onUpdateAutoSkip = { viewModel.updateAutoSkip(it) },
            onUpdateSeekStep = { viewModel.updateSeekStep(it) },
            onUpdateDefaultQuality = { viewModel.updateDefaultQuality(it) },
            onUpdateDefaultSpeed = { viewModel.updateDefaultSpeed(it) },
            onClearCache = {
                Toast.makeText(context, AppStrings.cacheClearedSuccess, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

@Composable
private fun ChangeNicknameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    val isError = name.trim().isBlank()
    val isUk = AppSettingsManager.isUkrainian()
    val colors = LocalAppColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isUk) "Змінити нікнейм" else "Изменить никнейм",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isUk) "Новий нікнейм буде відображатися в профілі, коментарях та чатах з друзями."
                    else "Новый никнейм будет отображаться в профиле, комментариях и чатах с друзьями.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 25) name = it },
                    label = { Text(if (isUk) "Нікнейм" else "Никнейм") },
                    singleLine = true,
                    isError = isError,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isError) {
                                Text(if (isUk) "Нікнейм не може бути порожнім" else "Никнейм не может быть пустым", color = Color(0xFFFF5252))
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            Text("${name.length}/25", color = colors.textMuted)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nickname_edit_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.glassBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = colors.textSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isError) {
                        onSave(name.trim())
                    }
                },
                enabled = !isError,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    disabledContainerColor = colors.primary.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_nickname_btn")
            ) {
                Text(if (isUk) "Зберегти" else "Сохранить", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isUk) "Скасувати" else "Отмена", color = colors.textSecondary)
            }
        }
    )
}

@Composable
private fun AvatarSourceChooserDialog(
    currentAvatar: String,
    onDismiss: () -> Unit,
    onPickFromGallery: () -> Unit,
    onAdjustCurrent: (() -> Unit)? = null,
    onSelectPreset: (presetUrl: String) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(currentAvatar) }
    val isUk = AppSettingsManager.isUkrainian()
    val colors = LocalAppColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isUk) "Вибір аватарки" else "Выбор аватарки",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Gallery Picker Action Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, Brush.horizontalGradient(GradientOrangeYellow), RoundedCornerShape(14.dp))
                        .clickable { onPickFromGallery() }
                        .testTag("pick_from_gallery_card"),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isUk) "Обрати з галереї" else "Выбрать из галереи",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isUk) "Фото до 5 МБ • GIF та Відео (до 5 сек)" else "Фото до 5 МБ • GIF и Видео (до 5 сек)",
                                color = colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = colors.primary
                        )
                    }
                }

                // Adjust current custom avatar if available
                if (onAdjustCurrent != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .clickable { onAdjustCurrent() }
                            .testTag("adjust_current_avatar_card"),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Crop,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isUk) "Підігнати поточну аватарку" else "Подогнать текущую аватарку",
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isUk) "Налаштувати масштаб та положення у колечку" else "Настроить масштаб и положение в кружке",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = colors.primary
                            )
                        }
                    }
                }

                if (currentAvatar.isNotBlank() && currentAvatar != AvatarManager.APP_ICON_AVATAR) {
                    Divider(color = colors.glassBorder)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectPreset(AvatarManager.APP_ICON_AVATAR)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.glassBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isUk) "Скинути на стандартний аватар" else "Сбросить на стандартный аватар",
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isUk) "Встановити логотип Aniwerti за замовчуванням" else "Установить логотип Aniwerti по умолчанию",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isUk) "Закрити" else "Закрыть", color = colors.textSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthBottomSheet(
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    onRegisterEmail: (email: String, password: String, name: String, avatar: String?) -> Unit,
    onLoginEmail: (email: String, password: String) -> Unit
) {
    val authContext = LocalContext.current
    val authScope = rememberCoroutineScope()
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(AvatarManager.APP_ICON_AVATAR) }
    var passwordVisible by remember { mutableStateOf(false) }
    var authFittingPath by remember { mutableStateOf<String?>(null) }
    val isUk = AppSettingsManager.isUkrainian()
    val colors = LocalAppColors.current

    val authGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            authScope.launch {
                val saveResult = AvatarManager.saveAvatarFromUri(authContext, uri)
                saveResult.onSuccess { localPath ->
                    authFittingPath = localPath
                }.onFailure { ex ->
                    val errTxt = ex.message ?: if (isUk) "Помилка вибору фото" else "Ошибка выбора фото"
                    Toast.makeText(authContext, errTxt, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = null
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isUk) "Авторизація в ANIWERTI" else "Авторизация в ANIWERTI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        )
                        Text(
                            text = if (isUk) "Створюйте акаунти та перемикайтеся миттєво" else "Создавайте аккаунты и переключайтесь мгновенно",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = if (isUk) "Закрити" else "Закрыть", tint = colors.textSecondary)
                    }
                }
            }

            // Tabs for Register & Login
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = colors.surfaceVariant,
                    contentColor = colors.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = colors.primary,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                if (isUk) "Реєстрація" else "Регистрация",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) colors.primary else colors.textSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                if (isUk) "Вхід" else "Вход",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) colors.primary else colors.textSecondary
                            )
                        }
                    )
                }
            }

            // Form inputs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (selectedTab == 0) {
                        // Display Name
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text(if (isUk) "Ім'я користувача / Нікнейм" else "Имя пользователя / Никнейм") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = colors.primary)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.glassBorder,
                                focusedLabelColor = colors.primary,
                                unfocusedLabelColor = colors.textSecondary,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            )
                        )

                        // Avatar selector - Modern sleek profile avatar uploader
                        val isCustomAvatar = selectedAvatar != AvatarManager.APP_ICON_AVATAR && selectedAvatar.isNotBlank()
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, colors.glassBorder, RoundedCornerShape(18.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar circle with touch feedback & camera badge
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clickable {
                                            authGalleryLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    UserAvatar(
                                        avatarUrl = selectedAvatar.ifBlank { AvatarManager.APP_ICON_AVATAR },
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, colors.primary, CircleShape),
                                        showVideoBadge = false
                                    )
                                    // Camera / Edit icon badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary)
                                            .padding(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isUk) "Аватар профілю" else "Аватар профиля",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isUk) "Фото, GIF або відео до 5 сек" else "Фото, GIF или видео до 5 сек",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                authGalleryLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.7f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoLibrary,
                                                contentDescription = null,
                                                tint = colors.primary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (isUk) "Галерея" else "Галерея",
                                                color = colors.primary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (isCustomAvatar) {
                                            OutlinedButton(
                                                onClick = { authFittingPath = selectedAvatar },
                                                shape = RoundedCornerShape(10.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.glassBorder),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Crop,
                                                    contentDescription = null,
                                                    tint = colors.textSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isUk) "Підігнати" else "Подогнать",
                                                    color = colors.textSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Email input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(if (isUk) "Email адреса" else "Email адрес") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = colors.primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.glassBorder,
                            focusedLabelColor = colors.primary,
                            unfocusedLabelColor = colors.textSecondary,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )

                    // Password input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isUk) "Пароль" else "Пароль") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = colors.primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isUk) "Показати пароль" else "Показать пароль",
                                    tint = colors.textSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.glassBorder,
                            focusedLabelColor = colors.primary,
                            unfocusedLabelColor = colors.textSecondary,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                }
            }

            // Submit button
            item {
                Button(
                    onClick = {
                        if (selectedTab == 0) {
                            onRegisterEmail(email, password, displayName, selectedAvatar)
                        } else {
                            onLoginEmail(email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auth_submit_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(
                        text = if (selectedTab == 0) {
                            if (isUk) "Створити акаунт" else "Создать аккаунт"
                        } else {
                            if (isUk) "Увійти" else "Войти"
                        },
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    if (authFittingPath != null) {
        AvatarCropFitDialog(
            mediaPath = authFittingPath!!,
            onDismiss = { authFittingPath = null },
            onConfirm = { finalAvatarUrl ->
                selectedAvatar = finalAvatarUrl
                authFittingPath = null
            }
        )
    }
}
