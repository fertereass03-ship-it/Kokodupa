package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.GradientOrangeYellow
import com.example.ui.theme.PrimaryYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AvatarManager
import com.example.util.AvatarTransform
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun AvatarCropFitDialog(
    mediaPath: String,
    onDismiss: () -> Unit,
    onConfirm: (finalAvatarUrl: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val cleanPath = remember(mediaPath) { AvatarManager.getCleanAvatarUrl(mediaPath) }
    val isVideo = remember(cleanPath) { AvatarManager.isVideoAvatar(cleanPath) }
    val isGif = remember(cleanPath) { AvatarManager.isGifAvatar(cleanPath) }

    val initialTransform = remember(mediaPath) { AvatarManager.parseTransform(mediaPath) }

    var scale by remember { mutableFloatStateOf(initialTransform.scale.coerceIn(1.0f, 4.0f)) }
    var panXRatio by remember { mutableFloatStateOf(initialTransform.panXRatio) }
    var panYRatio by remember { mutableFloatStateOf(initialTransform.panYRatio) }
    var isProcessing by remember { mutableStateOf(false) }

    val viewportSizeDp = 280.dp
    val density = LocalDensity.current
    val viewportSizePx = with(density) { viewportSizeDp.toPx() }

    // ExoPlayer for video avatars
    val exoPlayer = remember(cleanPath, isVideo) {
        if (isVideo && cleanPath.isNotBlank()) {
            ExoPlayer.Builder(context).build().apply {
                val uri = if (cleanPath.startsWith("/") || cleanPath.startsWith("file://")) {
                    Uri.fromFile(File(cleanPath.removePrefix("file://")))
                } else {
                    Uri.parse(cleanPath)
                }
                setMediaItem(MediaItem.fromUri(uri))
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f
                videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                prepare()
                playWhenReady = true
            }
        } else null
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    fun applyAvatar() {
        if (isProcessing) return
        isProcessing = true
        coroutineScope.launch {
            try {
                if (!isVideo && !isGif) {
                    // For static images, perform high-quality square bitmap cropping
                    val cropResult = AvatarManager.cropImageToSquare(
                        context = context,
                        imagePath = cleanPath,
                        scale = scale,
                        panXRatio = panXRatio,
                        panYRatio = panYRatio
                    )
                    isProcessing = false
                    cropResult.onSuccess { croppedPath ->
                        onConfirm(croppedPath)
                    }.onFailure {
                        // Fallback to URL with transform params
                        val fallback = AvatarManager.buildAvatarUrlWithTransform(
                            cleanPath,
                            AvatarTransform(scale, panXRatio, panYRatio)
                        )
                        onConfirm(fallback)
                    }
                } else {
                    // For GIF and Video, encode transform parameters to preserve all animated frames
                    isProcessing = false
                    val finalUrl = AvatarManager.buildAvatarUrlWithTransform(
                        cleanPath,
                        AvatarTransform(scale, panXRatio, panYRatio)
                    )
                    onConfirm(finalUrl)
                }
            } catch (e: Exception) {
                isProcessing = false
                Toast.makeText(context, "Помилка збереження: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isProcessing) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isProcessing,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF50E0E12))
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { if (!isProcessing) onDismiss() },
                        modifier = Modifier.testTag("avatar_fit_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрити",
                            tint = TextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Підгонка аватарки",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val badgeIcon = when {
                                isVideo -> Icons.Default.Movie
                                isGif -> Icons.Default.Crop
                                else -> Icons.Default.Photo
                            }
                            val badgeText = when {
                                isVideo -> "Відео (до 5 сек)"
                                isGif -> "GIF анімація"
                                else -> "Фотографія"
                            }
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = null,
                                tint = PrimaryYellow,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = badgeText,
                                color = PrimaryYellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Placeholder to balance top row
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Subtitle Instruction
                Text(
                    text = "Перетягуйте та змінюйте масштаб, щоб аватарка ідеально вписалася в колечко",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Interactive Crop Viewport Box
                Box(
                    modifier = Modifier
                        .size(viewportSizeDp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .pointerInput(viewportSizePx) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (!isProcessing) {
                                    scale = (scale * zoom).coerceIn(1.0f, 4.0f)
                                    val maxPanRatio = (scale - 0.7f) * 0.75f
                                    panXRatio = (panXRatio + pan.x / viewportSizePx).coerceIn(-maxPanRatio, maxPanRatio)
                                    panYRatio = (panYRatio + pan.y / viewportSizePx).coerceIn(-maxPanRatio, maxPanRatio)
                                }
                            }
                        }
                        .testTag("avatar_crop_viewport"),
                    contentAlignment = Alignment.Center
                ) {
                    // Underneath Media Layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = panXRatio * viewportSizePx
                                translationY = panYRatio * viewportSizePx
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isVideo && cleanPath.isNotBlank() && exoPlayer != null) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val model = AvatarManager.getAvatarModel(cleanPath)
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(model)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Аватарка для підгонки",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Circular Mask Overlay with Visibility Ring
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = (size.minDimension / 2f) - 10.dp.toPx()

                        // Darkened outer cutout mask
                        val maskPath = Path().apply {
                            addRect(Rect(0f, 0f, size.width, size.height))
                            addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
                            fillType = PathFillType.EvenOdd
                        }
                        drawPath(maskPath, color = Color(0xCC000000))

                        // Outer subtle glow
                        drawCircle(
                            color = Color(0x33FFD54F),
                            radius = radius + 2.5.dp.toPx(),
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Main Bright Circular Ring (Колечко видимості)
                        drawCircle(
                            color = PrimaryYellow,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Subtle center guide crosshair
                        val crosshairLen = 8.dp.toPx()
                        drawLine(
                            color = Color(0x55FFFFFF),
                            start = Offset(center.x - crosshairLen, center.y),
                            end = Offset(center.x + crosshairLen, center.y),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color(0x55FFFFFF),
                            start = Offset(center.x, center.y - crosshairLen),
                            end = Offset(center.x, center.y + crosshairLen),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                // Controls Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Zoom Slider Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    scale = (scale - 0.2f).coerceAtLeast(1.0f)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Зменшити",
                                    tint = PrimaryYellow
                                )
                            }

                            Slider(
                                value = scale,
                                onValueChange = { scale = it },
                                valueRange = 1.0f..4.0f,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("avatar_zoom_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = PrimaryYellow,
                                    activeTrackColor = PrimaryYellow,
                                    inactiveTrackColor = Color(0x33FFFFFF)
                                )
                            )

                            IconButton(
                                onClick = {
                                    scale = (scale + 0.2f).coerceAtMost(4.0f)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Збільшити",
                                    tint = PrimaryYellow
                                )
                            }

                            // Zoom level display badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x33FFC400),
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text(
                                    text = "${((scale * 10).roundToInt() / 10f)}x",
                                    color = PrimaryYellow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Center / Reset Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Масштаб та положення",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )

                            TextButton(
                                onClick = {
                                    scale = 1.0f
                                    panXRatio = 0f
                                    panYRatio = 0f
                                },
                                modifier = Modifier.testTag("avatar_reset_center_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CenterFocusStrong,
                                    contentDescription = null,
                                    tint = PrimaryYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "По центру",
                                    color = PrimaryYellow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Action Buttons Row (Скасувати & Застосувати)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (!isProcessing) onDismiss() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("avatar_fit_cancel_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Text(
                            text = "Скасувати",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = { applyAvatar() },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("avatar_fit_apply_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryYellow,
                            contentColor = Color.Black
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Обробка...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Застосувати",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
