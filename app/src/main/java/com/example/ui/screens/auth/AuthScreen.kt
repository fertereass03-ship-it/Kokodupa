package com.example.ui.screens.auth

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.components.AvatarCropFitDialog
import com.example.ui.components.UserAvatar
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.GradientOrangeYellow
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AvatarManager
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val authScope = rememberCoroutineScope()
    var authFittingMediaPath by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            authScope.launch {
                val saveResult = AvatarManager.saveAvatarFromUri(context, uri)
                saveResult.onSuccess { localPath ->
                    authFittingMediaPath = localPath
                }.onFailure { ex ->
                    Toast.makeText(context, ex.message ?: "Ошибка выбора фото", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Logo & Title
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(GradientOrangeYellow)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ANIWERTI",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = 2.sp
                )
            )

            Text(
                text = "Твой персональный мир аниме",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tab Selector: Вход / Регистрация
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = SurfaceCard,
                contentColor = PrimaryYellow,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                        color = PrimaryYellow,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = uiState.selectedTab == AuthTab.LOGIN,
                    onClick = { viewModel.setTab(AuthTab.LOGIN) },
                    text = {
                        Text(
                            text = "Войти",
                            fontWeight = if (uiState.selectedTab == AuthTab.LOGIN) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    },
                    modifier = Modifier.testTag("auth_tab_login")
                )
                Tab(
                    selected = uiState.selectedTab == AuthTab.REGISTER,
                    onClick = { viewModel.setTab(AuthTab.REGISTER) },
                    text = {
                        Text(
                            text = "Регистрация",
                            fontWeight = if (uiState.selectedTab == AuthTab.REGISTER) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    },
                    modifier = Modifier.testTag("auth_tab_register")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Error Banner
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.errorMessage?.let { errorMsg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x33FF5252)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("auth_error_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMsg,
                                color = Color(0xFFFF8A80),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Input Fields Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.selectedTab == AuthTab.REGISTER) {
                        // Nickname Field
                        OutlinedTextField(
                            value = uiState.displayName,
                            onValueChange = { viewModel.setDisplayName(it) },
                            label = { Text("Имя пользователя (Никнейм)") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryYellow)
                            },
                            singleLine = true,
                            colors = getTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_register_name_input"),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Email Field
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { viewModel.setEmail(it) },
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryYellow)
                        },
                        singleLine = true,
                        colors = getTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input"),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.setPassword(it) },
                        label = { Text("Пароль (минимум 6 символов)") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryYellow)
                        },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                Icon(
                                    imageVector = if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Показать пароль",
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = getTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input"),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (uiState.selectedTab == AuthTab.REGISTER) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (uiState.selectedTab == AuthTab.LOGIN) {
                                    viewModel.login(onSuccess = onAuthSuccess)
                                }
                            }
                        )
                    )

                    if (uiState.selectedTab == AuthTab.REGISTER) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Confirm Password Field
                        OutlinedTextField(
                            value = uiState.confirmPassword,
                            onValueChange = { viewModel.setConfirmPassword(it) },
                            label = { Text("Подтвердите пароль") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryYellow)
                            },
                            trailingIcon = {
                                IconButton(onClick = { viewModel.toggleConfirmPasswordVisibility() }) {
                                    Icon(
                                        imageVector = if (uiState.isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Показать пароль",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            colors = getTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_confirm_password_input"),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.register(onSuccess = onAuthSuccess)
                                }
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Modern Profile Avatar Picker
                        val isCustomSelected = uiState.avatarUrl.isNotBlank() && uiState.avatarUrl != AvatarManager.APP_ICON_AVATAR

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceVariantDark.copy(alpha = 0.6f))
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar circle with click to pick & camera badge
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    UserAvatar(
                                        avatarUrl = uiState.avatarUrl.ifBlank { AvatarManager.APP_ICON_AVATAR },
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, PrimaryYellow, CircleShape),
                                        showVideoBadge = false
                                    )
                                    // Camera / Edit icon badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryYellow)
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
                                        text = "Аватар профиля",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Фото, GIF или видео до 5 сек",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryYellow.copy(alpha = 0.7f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = PrimaryYellow,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "Галерея",
                                                color = PrimaryYellow,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (isCustomSelected) {
                                            OutlinedButton(
                                                onClick = { authFittingMediaPath = uiState.avatarUrl },
                                                shape = RoundedCornerShape(10.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Crop,
                                                    contentDescription = null,
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Подогнать",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.selectedTab == AuthTab.LOGIN) {
                        Spacer(modifier = Modifier.height(6.dp))

                        // Forgot Password Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { viewModel.setForgotPasswordDialog(true) },
                                modifier = Modifier.testTag("auth_forgot_password_btn")
                            ) {
                                Text(
                                    text = "Забыли пароль?",
                                    color = PrimaryYellow,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Main Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (uiState.selectedTab == AuthTab.LOGIN) {
                                viewModel.login(onSuccess = onAuthSuccess)
                            } else {
                                viewModel.register(onSuccess = onAuthSuccess)
                            }
                        },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_btn")
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (uiState.selectedTab == AuthTab.LOGIN) "Войти в аккаунт" else "Создать аккаунт",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Guest Mode / Skip
            TextButton(
                onClick = onContinueAsGuest,
                modifier = Modifier.testTag("auth_continue_guest_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Продолжить без авторизации",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Forgot Password Dialog
    if (uiState.showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setForgotPasswordDialog(false) },
            containerColor = SurfaceCard,
            title = {
                Text(
                    text = "Восстановление доступа",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Введите ваш email адрес, и мы вышлем ссылку для сброса пароля:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.forgotPasswordEmail,
                        onValueChange = { viewModel.setForgotPasswordEmail(it) },
                        label = { Text("Email") },
                        singleLine = true,
                        colors = getTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_reset_email_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    if (uiState.resetErrorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.resetErrorMessage ?: "",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp
                        )
                    }

                    if (uiState.resetMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.resetMessage ?: "",
                            color = Color(0xFF69F0AE),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.sendPasswordReset() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryYellow),
                    enabled = !uiState.isResetLoading,
                    modifier = Modifier.testTag("auth_send_reset_btn")
                ) {
                    if (uiState.isResetLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Отправить", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setForgotPasswordDialog(false) }) {
                    Text("Отмена", color = TextSecondary)
                }
            }
        )
    }

    if (authFittingMediaPath != null) {
        AvatarCropFitDialog(
            mediaPath = authFittingMediaPath!!,
            onDismiss = { authFittingMediaPath = null },
            onConfirm = { finalAvatarUrl ->
                viewModel.setAvatarUrl(finalAvatarUrl)
                authFittingMediaPath = null
            }
        )
    }
}

@Composable
private fun getTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryYellow,
    unfocusedBorderColor = Color(0x33FFFFFF),
    focusedLabelColor = PrimaryYellow,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = PrimaryYellow
)
