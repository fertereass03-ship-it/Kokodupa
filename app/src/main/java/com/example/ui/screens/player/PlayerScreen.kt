package com.example.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.OptIn
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.data.api.models.AnimeVoiceTranslation
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.GradientDarkOverlay
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val toggleOrientation = {
        val activity = context as? Activity
        if (activity != null) {
            if (isLandscape) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
    }

    val handleBack = {
        val activity = context as? Activity
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onBackClick()
        }
    }

    // Intercept back button: exit landscape first, or exit player if portrait
    BackHandler(enabled = true) {
        handleBack()
    }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableFloatStateOf(0f) }

    // Keep screen on and hide system bars (immersive mode - time, battery, nav bars disappear)
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            viewModel.pausePlayback()
            val act = context as? Activity
            act?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (window != null) {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // --- 1. Video Surface (AndroidView with ExoPlayer using TextureView) ---
        AndroidView(
            factory = { ctx ->
                (android.view.LayoutInflater.from(ctx).inflate(
                    com.example.R.layout.item_player_view,
                    null,
                    false
                ) as PlayerView).apply {
                    player = viewModel.exoPlayer
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    resizeMode = state.resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = viewModel.exoPlayer
                if (playerView.resizeMode != state.resizeMode) {
                    playerView.resizeMode = state.resizeMode
                }
            },
            onRelease = { playerView ->
                playerView.player = null
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.toggleControlsVisibility()
                }
        )

        // Loading Spinner
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = PrimaryYellow,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Error Message Overlay with Retry
        if (state.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE0D0D0D))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Предупреждение",
                        tint = AccentOrange,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = state.errorMessage ?: "",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.loadEpisodeStream(state.episodeNumber) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryYellow,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Повторить", fontWeight = FontWeight.Bold)
                        }
                        if (state.availableVoices.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.showVoiceDialog() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SurfaceCard,
                                    contentColor = PrimaryYellow
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Озвучка", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        OutlinedButton(
                            onClick = handleBack,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                        ) {
                            Text("Назад", color = TextPrimary)
                        }
                    }
                }
            }
        }

        // --- 2. Custom Video Controls Overlay ---
        AnimatedVisibility(
            visible = state.areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x77000000))
            ) {
                // Top Header Overlay - Higher position, unified row, constrained voice label
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xF0000000), Color(0x99000000), Color.Transparent)
                            )
                        )
                        .padding(top = 8.dp, start = 12.dp, end = 12.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back button
                        IconButton(
                            onClick = handleBack,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0x66000000), CircleShape)
                                .testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Title & Episode Info + Constrained Voice Badge
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = state.animeTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (state.isMovie) "Фильм" else "Серия ${state.episodeNumber}",
                                    color = PrimaryYellow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                // Voice translation badge - strictly limited width (max 115dp) so it never blows up
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0x33FFFFFF),
                                    modifier = Modifier
                                        .widthIn(max = 115.dp)
                                        .clickable { viewModel.showVoiceDialog() }
                                        .testTag("player_voice_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RecordVoiceOver,
                                            contentDescription = "Озвучка",
                                            tint = PrimaryYellow,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = state.voiceName,
                                            color = TextPrimary,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Higher up controls: Quality, Speed, Aspect Ratio & Settings
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Quality Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x66000000))
                                    .border(1.dp, PrimaryYellow.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.showQualityDialog() }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                                    .testTag("player_quality_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = "Качество",
                                        tint = PrimaryYellow,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = state.selectedQuality,
                                        color = PrimaryYellow,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Speed Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x66000000))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.showSpeedDialog() }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                                    .testTag("player_speed_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = "Скорость",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${state.playbackSpeed}x",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Settings Button
                            IconButton(
                                onClick = { viewModel.showSettingsDialog() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0x66000000), CircleShape)
                                    .testTag("player_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Настройки",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Center Playback Action Buttons
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!state.isMovie) {
                        // Previous Episode
                        IconButton(
                            onClick = { viewModel.playPreviousEpisode() },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0x66000000), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Предыдущая серия",
                                tint = TextPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Rewind 10s
                    IconButton(
                        onClick = { viewModel.seekRelative(-10) },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0x66000000), CircleShape)
                            .testTag("player_rewind_10")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "-10 сек",
                            tint = TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play/Pause Big Center Button
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(AccentOrange, PrimaryYellow)
                                )
                            )
                            .clickable { viewModel.togglePlayPause() }
                            .testTag("player_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Пауза" else "Воспроизведение",
                            tint = Color.Black,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = { viewModel.seekRelative(10) },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0x66000000), CircleShape)
                            .testTag("player_forward_10")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "+10 сек",
                            tint = TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    if (!state.isMovie) {
                        // Next Episode
                        IconButton(
                            onClick = { viewModel.playNextEpisode() },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0x66000000), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Следующая серия",
                                tint = TextPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Bottom Timeline & Skip Opening Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xDD000000))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    // Skip Opening Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xAA000000))
                                .border(1.dp, PrimaryYellow, RoundedCornerShape(8.dp))
                                .clickable { viewModel.skipOpening() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("skip_opening_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = null,
                                    tint = PrimaryYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+${state.settings.seekStepSeconds}с Пропуск",
                                    color = PrimaryYellow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Floating seek time indicator when dragging
                    if (isDraggingSlider) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xEE1A1A1A),
                                border = BorderStroke(1.dp, PrimaryYellow)
                            ) {
                                Text(
                                    text = formatTime(dragPositionMs.toLong()),
                                    color = PrimaryYellow,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Scrubber Live Slider with buttery smooth dragging, tap-to-seek and rotate/fullscreen button
                    val totalDuration = if (state.durationMs > 0) state.durationMs.toFloat() else 1f
                    val currentPosition = if (isDraggingSlider) {
                        dragPositionMs.coerceIn(0f, totalDuration)
                    } else {
                        state.currentPositionMs.toFloat().coerceIn(0f, totalDuration)
                    }
                    val currentDisplayMs = if (isDraggingSlider) dragPositionMs.toLong() else state.currentPositionMs

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = currentPosition,
                            onValueChange = { pos ->
                                isDraggingSlider = true
                                dragPositionMs = pos
                            },
                            onValueChangeFinished = {
                                viewModel.seekToPosition(dragPositionMs.toLong())
                                isDraggingSlider = false
                            },
                            valueRange = 0f..totalDuration.coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryYellow,
                                activeTrackColor = PrimaryYellow,
                                inactiveTrackColor = Color(0x44FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("player_progress_slider")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Fullscreen / Rotate Screen button directly beside the slider
                        IconButton(
                            onClick = { toggleOrientation() },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0x66000000), CircleShape)
                                .testTag("player_fullscreen_rotate_button")
                        ) {
                            Icon(
                                imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isLandscape) "Згорнути в портретний режим" else "Повернути на весь екран",
                                tint = PrimaryYellow,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Time Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 44.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentDisplayMs),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatTime(state.durationMs),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // --- Quality Selection Dialog ---
    if (state.isQualityDialogVisible) {
        QualityBottomSheet(
            currentQuality = state.selectedQuality,
            availableQualities = state.availableQualities,
            onSelect = { viewModel.setQuality(it) },
            onDismiss = { viewModel.hideQualityDialog() }
        )
    }

    // --- Speed Selection Dialog ---
    if (state.isSpeedDialogVisible) {
        SpeedBottomSheet(
            currentSpeed = state.playbackSpeed,
            onSelect = { viewModel.setPlaybackSpeed(it) },
            onDismiss = { viewModel.hideSpeedDialog() }
        )
    }

    // --- Player Settings Dialog ---
    if (state.isSettingsDialogVisible) {
        PlayerSettingsBottomSheet(
            settings = state.settings,
            onSave = { viewModel.updateSettings(it) },
            onDismiss = { viewModel.hideSettingsDialog() }
        )
    }

    // --- Aspect Ratio / Scaling Dialog ---
    if (state.isResizeModeDialogVisible) {
        AspectRatioBottomSheet(
            currentMode = state.resizeMode,
            onSelect = { viewModel.setResizeMode(it) },
            onDismiss = { viewModel.hideResizeModeDialog() }
        )
    }

    // --- Voice Selection Dialog ---
    if (state.isVoiceDialogVisible) {
        VoiceBottomSheet(
            currentVoice = state.voiceName,
            availableVoices = state.availableVoices,
            onSelect = { viewModel.selectVoice(it) },
            onDismiss = { viewModel.hideVoiceDialog() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceBottomSheet(
    currentVoice: String,
    availableVoices: List<AnimeVoiceTranslation>,
    onSelect: (AnimeVoiceTranslation) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = SurfaceCard,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = PrimaryYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Выбор озвучки",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (availableVoices.isEmpty()) {
                Text(
                    text = "Список озвучек загружается...",
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                availableVoices.forEach { voice ->
                    val isSelected = voice.name.equals(currentVoice, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SurfaceVariantDark else Color.Transparent)
                            .clickable { onSelect(voice) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = voice.name,
                                color = if (isSelected) PrimaryYellow else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                            if (voice.episodesCount > 0) {
                                Text(
                                    text = "${voice.episodesCount} сер.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Выбрано",
                                tint = PrimaryYellow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualityBottomSheet(
    currentQuality: String,
    availableQualities: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = SurfaceCard,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.HighQuality,
                    contentDescription = null,
                    tint = PrimaryYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Качество видео",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            availableQualities.forEach { quality ->
                val isSelected = quality == currentQuality
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) SurfaceVariantDark else Color.Transparent)
                        .clickable { onSelect(quality) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quality,
                        color = if (isSelected) PrimaryYellow else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isSelected) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = PrimaryYellow)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedBottomSheet(
    currentSpeed: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = SurfaceCard,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = PrimaryYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Скорость воспроизведения",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            speeds.forEach { speed ->
                val isSelected = (currentSpeed - speed).let { Math.abs(it) < 0.01f }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) SurfaceVariantDark else Color.Transparent)
                        .clickable { onSelect(speed) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (speed == 1.0f) "1.0x (Обычная)" else "${speed}x",
                        color = if (isSelected) PrimaryYellow else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isSelected) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = PrimaryYellow)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSettingsBottomSheet(
    settings: com.example.data.db.PlayerSettingsEntity,
    onSave: (com.example.data.db.PlayerSettingsEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var autoSkip by remember { androidx.compose.runtime.mutableStateOf(settings.autoSkip) }
    var seekStep by remember { androidx.compose.runtime.mutableStateOf(settings.seekStepSeconds) }
    var hwAccel by remember { androidx.compose.runtime.mutableStateOf(settings.hardwareAcceleration) }

    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = SurfaceCard,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = PrimaryYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Настройки плеера",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Auto-skip opening switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Автопропуск опенинга", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Пропускать стандартные 90 секунд в начале серии", color = TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = autoSkip,
                    onCheckedChange = { autoSkip = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryYellow)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step options (60s, 85s, 90s)
            Text("Интервал быстрой перемотки опенинга", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 85, 90, 120).forEach { seconds ->
                    val isSelected = seekStep == seconds
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryYellow else SurfaceVariantDark)
                            .clickable { seekStep = seconds }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${seconds}с",
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            androidx.compose.material3.Button(
                onClick = {
                    onSave(settings.copy(autoSkip = autoSkip, seekStepSeconds = seekStep, hardwareAcceleration = hwAccel))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = PrimaryYellow,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Сохранить", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AspectRatioBottomSheet(
    currentMode: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val modes = listOf(
        Triple(
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
            "По размеру (Fit)",
            "Исходные пропорции видео без обрезки"
        ),
        Triple(
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            "На весь экран (Заполнение/Zoom)",
            "Заполняет весь экран телефона без черных полос"
        ),
        Triple(
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL,
            "Растянуть (Stretch)",
            "Растягивает изображение по ширине и высоте экрана"
        )
    )

    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = SurfaceCard,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = null,
                    tint = PrimaryYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Масштабирование видео",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            modes.forEach { (mode, title, desc) ->
                val isSelected = mode == currentMode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) SurfaceVariantDark else Color.Transparent)
                        .clickable { onSelect(mode) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = if (isSelected) PrimaryYellow else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    if (isSelected) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = PrimaryYellow)
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
