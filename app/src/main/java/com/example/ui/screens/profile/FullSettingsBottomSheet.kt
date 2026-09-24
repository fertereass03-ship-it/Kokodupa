package com.example.ui.screens.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PlayerSettingsEntity
import com.example.data.settings.AppLanguage
import com.example.data.settings.AppSettingsManager
import com.example.data.settings.AppThemeMode
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullSettingsBottomSheet(
    initialTab: Int = 0, // 0 = App settings, 1 = Player settings
    settings: PlayerSettingsEntity,
    onUpdateAutoSkip: (Boolean) -> Unit,
    onUpdateSeekStep: (Int) -> Unit,
    onUpdateDefaultQuality: (String) -> Unit,
    onUpdateDefaultSpeed: (Float) -> Unit,
    onClearCache: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val appSettings by AppSettingsManager.settingsState.collectAsState()
    val colors = LocalAppColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x55FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = AppStrings.settingsTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = AppStrings.close,
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tabs: [Додаток / Приложение] vs [Плеєр / Плеер]
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = colors.surfaceVariant,
                contentColor = colors.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = colors.primary,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = AppStrings.tabAppSettings,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) colors.primary else colors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = AppStrings.tabPlayerSettings,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) colors.primary else colors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (selectedTab == 0) {
                    // ==========================================
                    // 1. APP SETTINGS: LANGUAGE & THEME
                    // ==========================================

                    // Language Selection Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.languageSectionTitle,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = AppStrings.languageDescription,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    // Language Chips / Buttons (Українська / Русский)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppLanguage.values().forEach { lang ->
                            val isSelected = appSettings.language == lang
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) colors.primary.copy(alpha = 0.2f)
                                        else colors.surfaceVariant
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) colors.primary else colors.glassBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { AppSettingsManager.setLanguage(lang) }
                                    .padding(vertical = 12.dp, horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.UKRAINIAN) "🇺🇦 " else "🇷🇺 ",
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = lang.displayName,
                                        color = if (isSelected) colors.primary else colors.textPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = colors.glassBorder
                    )

                    // Theme Selection Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = colors.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.themeSectionTitle,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = AppStrings.themeDescription,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Themes list: Original, Light, Dark
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppThemeMode.values().forEach { mode ->
                            val isSelected = appSettings.themeMode == mode
                            val themeTitle = when (mode) {
                                AppThemeMode.ORIGINAL_DARK -> AppStrings.themeOriginal
                                AppThemeMode.LIGHT -> AppStrings.themeLight
                                AppThemeMode.DARK -> AppStrings.themeDark
                            }
                            val themeSubtitle = when (mode) {
                                AppThemeMode.ORIGINAL_DARK -> if (AppSettingsManager.isUkrainian()) "Фірмовий темний стиль з неоновим золотом" else "Фирменный тёмный стиль с неоновым золотом"
                                AppThemeMode.LIGHT -> if (AppSettingsManager.isUkrainian()) "Яскравий світлий інтерфейс" else "Яркий светлый интерфейс"
                                AppThemeMode.DARK -> if (AppSettingsManager.isUkrainian()) "Глибокий чорний OLED стиль" else "Глубокий чёрный OLED стиль"
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { AppSettingsManager.setThemeMode(mode) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) colors.primary else colors.glassBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (mode) {
                                                        AppThemeMode.ORIGINAL_DARK -> PrimaryYellow
                                                        AppThemeMode.LIGHT -> Color(0xFFF7F8FA)
                                                        AppThemeMode.DARK -> Color.Black
                                                    }
                                                )
                                                .border(1.dp, Color.Gray, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = themeTitle,
                                                color = colors.textPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = themeSubtitle,
                                                color = colors.textSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(colors.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                } else {
                    // ==========================================
                    // 2. PLAYER SETTINGS (Existing logic)
                    // ==========================================

                    // Auto-skip setting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = AppStrings.autoSkipOpening,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = AppStrings.autoSkipDesc,
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = settings.autoSkip,
                            onCheckedChange = onUpdateAutoSkip,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = colors.primary,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.surfaceVariant
                            )
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = colors.glassBorder
                    )

                    // Seek Step
                    Text(
                        text = AppStrings.seekStepTitle,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 30, 60, 90).forEach { sec ->
                            val isSelected = settings.seekStepSeconds == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.primary else colors.surfaceVariant)
                                    .clickable { onUpdateSeekStep(sec) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$sec " + if (AppSettingsManager.isUkrainian()) "сек" else "сек",
                                    color = if (isSelected) Color.Black else colors.textSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = colors.glassBorder
                    )

                    // Quality
                    Text(
                        text = AppStrings.defaultQualityTitle,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("1080p", "720p", "480p", "Auto").forEach { q ->
                            val isSelected = settings.defaultQuality == q
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.primary else colors.surfaceVariant)
                                    .clickable { onUpdateDefaultQuality(q) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = q,
                                    color = if (isSelected) Color.Black else colors.textSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = colors.glassBorder
                    )

                    // Speed
                    Text(
                        text = AppStrings.playbackSpeedTitle,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            val isSelected = settings.defaultSpeed == spd
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.primary else colors.surfaceVariant)
                                    .clickable { onUpdateDefaultSpeed(spd) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${spd}x",
                                    color = if (isSelected) Color.Black else colors.textSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Divider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = colors.glassBorder
                )

                // App info & cache
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ANIWERTI 2.5.0",
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                    TextButton(onClick = onClearCache) {
                        Text(
                            text = AppStrings.clearCache,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
