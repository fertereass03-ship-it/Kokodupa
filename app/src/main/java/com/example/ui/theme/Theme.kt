package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.settings.AppThemeMode

data class AppCustomColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val glassBackground: Color,
    val glassBorder: Color,
    val isLight: Boolean
)

val LocalAppColors = staticCompositionLocalOf {
    AppCustomColors(
        background = BackgroundDark,
        surface = SurfaceCard,
        surfaceVariant = SurfaceVariantDark,
        primary = PrimaryYellow,
        secondary = AccentOrange,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textMuted = TextMuted,
        glassBackground = GlassBackground,
        glassBorder = GlassBorder,
        isLight = false
    )
}

private val OriginalDarkColors = darkColorScheme(
    primary = PrimaryYellow,
    onPrimary = BackgroundDark,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = PrimaryYellow,
    secondary = AccentOrange,
    onSecondary = BackgroundDark,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = AccentOrange,
    tertiary = PrimaryYellow,
    onTertiary = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = ErrorRed,
    onError = TextPrimary
)

private val DeepDarkColors = darkColorScheme(
    primary = DeepNightPrimary,
    onPrimary = DeepNightBackground,
    primaryContainer = DeepNightSurfaceVariant,
    onPrimaryContainer = DeepNightPrimary,
    secondary = AccentOrange,
    onSecondary = DeepNightBackground,
    secondaryContainer = DeepNightSurfaceVariant,
    onSecondaryContainer = AccentOrange,
    tertiary = DeepNightPrimary,
    onTertiary = DeepNightBackground,
    background = DeepNightBackground,
    onBackground = TextPrimary,
    surface = DeepNightSurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = DeepNightSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = ErrorRed,
    onError = TextPrimary
)

private val LightColors = lightColorScheme(
    primary = LightPrimaryYellow,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = LightAccentOrange,
    secondary = LightAccentOrange,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightAccentOrange,
    tertiary = LightPrimaryYellow,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurfaceCard,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightGlassBorder,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.ORIGINAL_DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> LightColors
        AppThemeMode.DARK -> DeepDarkColors
        AppThemeMode.ORIGINAL_DARK -> OriginalDarkColors
    }

    val customColors = when (themeMode) {
        AppThemeMode.LIGHT -> AppCustomColors(
            background = LightBackground,
            surface = LightSurfaceCard,
            surfaceVariant = LightSurfaceVariant,
            primary = LightPrimaryYellow,
            secondary = LightAccentOrange,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textMuted = LightTextMuted,
            glassBackground = LightGlassBackground,
            glassBorder = LightGlassBorder,
            isLight = true
        )
        AppThemeMode.DARK -> AppCustomColors(
            background = DeepNightBackground,
            surface = DeepNightSurfaceCard,
            surfaceVariant = DeepNightSurfaceVariant,
            primary = DeepNightPrimary,
            secondary = AccentOrange,
            textPrimary = TextPrimary,
            textSecondary = TextSecondary,
            textMuted = TextMuted,
            glassBackground = Color(0xEE0D0D0D),
            glassBorder = Color(0x33FFC400),
            isLight = false
        )
        AppThemeMode.ORIGINAL_DARK -> AppCustomColors(
            background = BackgroundDark,
            surface = SurfaceCard,
            surfaceVariant = SurfaceVariantDark,
            primary = PrimaryYellow,
            secondary = AccentOrange,
            textPrimary = TextPrimary,
            textSecondary = TextSecondary,
            textMuted = TextMuted,
            glassBackground = GlassBackground,
            glassBorder = GlassBorder,
            isLight = false
        )
    }

    CompositionLocalProvider(LocalAppColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
