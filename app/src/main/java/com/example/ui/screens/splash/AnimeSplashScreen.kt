package com.example.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.settings.AppSettingsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Minimalist, elegant launch screen inspired by Anixart:
 * - Clean dark background with subtle warm radial glow
 * - Centered squircle logo fading in with smooth scale
 * - Bold modern "ANIWERTI" typography
 * - Smooth 3-dot loading indicator
 * - Fast & responsive (1.3s duration, instant click-to-skip)
 */
@Composable
fun AnimeSplashScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasFinished by remember { mutableStateOf(false) }
    val safeFinish: () -> Unit = {
        if (!hasFinished) {
            hasFinished = true
            onFinish()
        }
    }

    val isUk = AppSettingsManager.isUkrainian()

    // Smooth entry animations (fade & scale)
    val logoScale = remember { Animatable(0.82f) }
    val logoAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }

    // Breathing pulse for logo
    val infiniteTransition = rememberInfiniteTransition(label = "anixartPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )

    // Animated loading dots
    val dotPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotPhase"
    )

    LaunchedEffect(Unit) {
        // Step 1: Smooth fade & scale-in of logo
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        }

        // Step 2: Content text fade-in
        delay(250)
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )

        // Wait a short comfortable duration (total ~1350ms), then finish smoothly
        delay(750)
        safeFinish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D12))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { safeFinish() }
            .testTag("anime_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // --- Beautiful Multi-layered Ambient Background Lighting ---
        // 1. Wide soft ambient aura that gently pulses
        Box(
            modifier = Modifier
                .size(460.dp)
                .scale(pulseScale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x2EFFB300),
                            Color(0x1AFF8F00),
                            Color(0x0AFF6D00),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Focused warm golden core light behind the emblem
        Box(
            modifier = Modifier
                .size(240.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x4DFFD54F),
                            Color(0x26FF9800),
                            Color.Transparent
                        )
                    )
                )
        )

        // Center Brand Column (Anixart Style)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 40.dp)
        ) {
            // App Logo Squircle filled edge-to-edge with the official app launcher icon
            Box(
                modifier = Modifier
                    .scale(logoScale.value * pulseScale)
                    .alpha(logoAlpha.value)
                    .size(112.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color(0x66FFB300),
                        spotColor = Color(0x88FF8F00)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF14151D)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_aniwerti_app_icon),
                    contentDescription = "ANIWERTI App Icon",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App Name (Bold, clean, golden gradient)
            Text(
                text = "ANIWERTI",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                color = Color.White,
                modifier = Modifier.alpha(contentAlpha.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Minimalist subtitle
            Text(
                text = if (isUk) "Дивись аніме онлайн" else "Смотри аниме онлайн",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF9E9E9E),
                letterSpacing = 0.5.sp,
                modifier = Modifier.alpha(contentAlpha.value)
            )
        }

        // Bottom clean 3-dot pulse loader (Anixart signature minimalist loading)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..2) {
                    val dotAlpha = when {
                        dotPhase.toInt() % 3 == i -> 1f
                        (dotPhase.toInt() + 1) % 3 == i -> 0.5f
                        else -> 0.25f
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(dotAlpha)
                            .background(Color(0xFFFFC400), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "v2.6",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF555768)
            )
        }
    }
}
