package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.PrimaryYellow
import com.example.util.AvatarManager
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun UserAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentDescription: String? = "Аватар пользователя",
    contentScale: ContentScale = ContentScale.Crop,
    showVideoBadge: Boolean = true
) {
    val context = LocalContext.current
    val cleanUrl = remember(avatarUrl) { AvatarManager.getCleanAvatarUrl(avatarUrl) }
    val transform = remember(avatarUrl) { AvatarManager.parseTransform(avatarUrl) }
    val isVideo = remember(avatarUrl) { AvatarManager.isVideoAvatar(avatarUrl) }

    Box(
        modifier = modifier.clip(shape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = transform.scale
                    scaleY = transform.scale
                    translationX = transform.panXRatio * size.width
                    translationY = transform.panYRatio * size.height
                },
            contentAlignment = Alignment.Center
        ) {
            if (isVideo && cleanUrl.isNotBlank()) {
                val exoPlayer = remember(cleanUrl) {
                    ExoPlayer.Builder(context).build().apply {
                        val uri = if (cleanUrl.startsWith("/") || cleanUrl.startsWith("file://")) {
                            Uri.fromFile(File(cleanUrl.removePrefix("file://")))
                        } else {
                            Uri.parse(cleanUrl)
                        }
                        setMediaItem(MediaItem.fromUri(uri))
                        repeatMode = Player.REPEAT_MODE_ALL
                        volume = 0f
                        videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                        prepare()
                        playWhenReady = true
                    }
                }

                DisposableEffect(exoPlayer) {
                    onDispose {
                        exoPlayer.release()
                    }
                }

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
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("video_avatar_player")
                )
            } else {
                val model = AvatarManager.getAvatarModel(avatarUrl)
                if (model is Int) {
                    Image(
                        painter = painterResource(id = model),
                        contentDescription = contentDescription,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("app_icon_avatar_image"),
                        contentScale = contentScale
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(model)
                            .crossfade(true)
                            .error(R.drawable.ic_aniwerti_app_icon)
                            .fallback(R.drawable.ic_aniwerti_app_icon)
                            .build(),
                        contentDescription = contentDescription,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("user_avatar_image"),
                        contentScale = contentScale
                    )
                }
            }
        }

        if (isVideo && showVideoBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC000000))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Видео-аватар",
                    tint = PrimaryYellow,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
