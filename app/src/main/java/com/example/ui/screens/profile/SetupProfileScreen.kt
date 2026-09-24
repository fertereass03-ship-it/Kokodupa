package com.example.ui.screens.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.components.UserAvatar
import com.example.util.AvatarManager
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupProfileScreen(
    email: String,
    initialDisplayName: String,
    avatarUrl: String,
    googleId: String? = null,
    viewModel: ProfileViewModel,
    onProfileSaved: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Default clean nickname from initial displayName or email prefix
    val defaultNick = remember(initialDisplayName, email) {
        val raw = if (initialDisplayName.isNotBlank()) initialDisplayName else email.substringBefore("@")
        raw.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(20)
    }

    var nickname by remember { mutableStateOf(defaultNick) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Validation rules
    val trimmed = nickname.trim()
    val isTooShort = trimmed.length < 3
    val isTooLong = trimmed.length > 20
    val hasSpaces = trimmed.contains(" ")
    val hasInvalidChars = !trimmed.all { it.isLetterOrDigit() || it == '_' || it == '-' }
    val isValid = trimmed.length in 3..20 && !hasSpaces && !hasInvalidChars

    val validationError: String? = when {
        trimmed.isEmpty() -> "Введіть нікнейм"
        isTooShort -> "Нікнейм занадто короткий (мінімум 3 символи)"
        isTooLong -> "Максимальна довжина 20 символів"
        hasSpaces -> "Нікнейм не повинен містити пробілів"
        hasInvalidChars -> "Дозволені лише літери, цифри, _ та -"
        else -> null
    }

    fun submitProfile() {
        if (!isValid || isLoading) return
        focusManager.clearFocus()
        isLoading = true
        errorMessage = null

        viewModel.loginWithGoogle(
            email = email,
            displayName = trimmed,
            avatarUrl = avatarUrl,
            googleId = googleId,
            onSuccess = {
                isLoading = false
                Toast.makeText(
                    context,
                    "Ласкаво просимо, $trimmed! Профіль успішно збережено.",
                    Toast.LENGTH_SHORT
                ).show()
                onProfileSaved()
            },
            onError = { err ->
                isLoading = false
                errorMessage = err
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("setup_profile_screen"),
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Встановлення нікнейму",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Крок 2 з 2 • Нова реєстрація",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = PrimaryYellow,
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step indicator bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Step 1: Google Auth (Completed)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF4CAF50))
                )
                // Step 2: Setup Profile (Active)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PrimaryYellow)
                )
            }

            // Google Account Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x334285F4))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        UserAvatar(
                            avatarUrl = avatarUrl.ifEmpty { AvatarManager.APP_ICON_AVATAR },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0x664285F4), CircleShape)
                        )
                        // Google "G" Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color(0xFF4285F4)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x224285F4))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Google акаунт підключено",
                                    color = Color(0xFF64B5F6),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = email,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        if (initialDisplayName.isNotBlank() && initialDisplayName != email) {
                            Text(
                                text = initialDisplayName,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Оберіть ваш нікнейм",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Цей нікнейм бачитимуть інші користувачі у списках друзів, відгуках та чатах додатка ANIWERTI.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Nickname Input Field
            OutlinedTextField(
                value = nickname,
                onValueChange = { input ->
                    if (input.length <= 20) {
                        nickname = input.filter { !it.isWhitespace() }
                        errorMessage = null
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nickname_input_field"),
                label = { Text("Унікальний нікнейм") },
                placeholder = { Text("наприклад, Senpai_2026") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isValid) PrimaryYellow else TextSecondary
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isValid) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Коректно",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (nickname.isNotEmpty()) {
                            IconButton(onClick = { nickname = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Очистити",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                isError = validationError != null && nickname.isNotEmpty(),
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (nickname.isNotEmpty() && validationError != null) {
                                validationError
                            } else {
                                "Літери, цифри, _ та - (без пробілів)"
                            },
                            color = if (nickname.isNotEmpty() && validationError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                TextSecondary
                            },
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${trimmed.length}/20",
                            color = if (trimmed.length > 20) MaterialTheme.colorScheme.error else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submitProfile() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryYellow,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedLabelColor = PrimaryYellow,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = PrimaryYellow,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Suggestions / Quick Nicknames
            val suggestions = remember(email, initialDisplayName) {
                val cleanEmailName = email.substringBefore("@").replace(".", "_").replace("-", "_")
                listOf(
                    cleanEmailName.take(15),
                    "${cleanEmailName.take(12)}_2026",
                    "Anime_${cleanEmailName.take(10)}"
                ).filter { it.length in 3..20 }.distinct()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Варіанти:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                suggestions.forEach { sug ->
                    Surface(
                        onClick = {
                            nickname = sug
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (nickname == sug) PrimaryYellow.copy(alpha = 0.2f) else SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (nickname == sug) PrimaryYellow else Color(0x22FFFFFF)
                        )
                    ) {
                        Text(
                            text = sug,
                            color = if (nickname == sug) PrimaryYellow else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Error display
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x33F44336)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66F44336))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data Security & Storage Guarantee Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.6f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x18FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = PrimaryYellow,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Збереження даних: профіль (Google ID, email, аватар та ваш новий нікнейм) буде надійно збережено в базі даних додатка. Після цього ви відразу потрапите на головний екран і зможете зберігати перегляди та історію.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Submit Button
            Button(
                onClick = { submitProfile() },
                enabled = isValid && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_profile_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryYellow,
                    contentColor = Color.Black,
                    disabledContainerColor = PrimaryYellow.copy(alpha = 0.3f),
                    disabledContentColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.Black,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Збереження профілю...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = "Зберегти профіль та почати",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cancel / Back Button
            TextButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Скасувати та повернутися назад",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
