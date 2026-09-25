package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// --- ANIWERTI Original Color Palette (Тёмно-жёлто-оранжевый стиль) ---
val BackgroundDark = Color(0xFF0B0B0B)
val SurfaceCard = Color(0xFF151515)
val SurfaceVariantDark = Color(0xFF202020)
val PrimaryYellow = Color(0xFFFFC400)
val AccentOrange = Color(0xFFFF8A00)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A0A0)
val TextMuted = Color(0xFF6B6B6B)

// Glassmorphism & Accent Gradients
val GlassBackground = Color(0xDD151515)
val GlassBorder = Color(0x33FFC400)
val GlassSurface = Color(0x99202020)
val GradientOrangeYellow = listOf(AccentOrange, PrimaryYellow)
val GradientDarkOverlay = listOf(Color.Transparent, Color(0xAA0B0B0B), Color(0xFF0B0B0B))
val GradientCardOverlay = listOf(Color.Transparent, Color(0xDD151515))

val RatingGold = Color(0xFFFFD700)
val StatusOngoing = Color(0xFF00E676)
val StatusReleased = Color(0xFF2979FF)
val StatusAnons = Color(0xFFFF9100)
val ErrorRed = Color(0xFFFF5252)

// --- Light Theme Colors ---
val LightBackground = Color(0xFFF7F8FA)
val LightSurfaceCard = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEEEEE)
val LightTextPrimary = Color(0xFF1A1A1A)
val LightTextSecondary = Color(0xFF666666)
val LightTextMuted = Color(0xFF9E9E9E)
val LightPrimaryYellow = Color(0xFFF59E0B)
val LightAccentOrange = Color(0xFFEA580C)
val LightGlassBackground = Color(0xEEFFFFFF)
val LightGlassBorder = Color(0x33EA580C)

// --- Dark Purple Theme Colors (Тёмно-фиолетовый стиль) ---
val DarkPurpleBackground = Color(0xFF0E0919)
val DarkPurpleSurfaceCard = Color(0xFF171026)
val DarkPurpleSurfaceVariant = Color(0xFF231838)
val DarkPurplePrimary = Color(0xFFBB86FC)
val DarkPurpleSecondary = Color(0xFF9965F4)
val DarkPurpleGlassBackground = Color(0xEE140D24)
val DarkPurpleGlassBorder = Color(0x33BB86FC)

// Backward-compatible aliases
val DeepNightBackground = DarkPurpleBackground
val DeepNightSurfaceCard = DarkPurpleSurfaceCard
val DeepNightSurfaceVariant = DarkPurpleSurfaceVariant
val DeepNightPrimary = DarkPurplePrimary
