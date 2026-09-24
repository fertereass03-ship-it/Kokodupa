package com.example.ui.screens.player

import android.app.Application
import android.util.Log
import kotlin.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.api.models.AnimeVoiceTranslation
import com.example.data.api.models.KodikStreamLinks
import com.example.data.db.PlayerSettingsEntity
import com.example.data.repository.AnimeRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val animeTitle: String = "",
    val episodeNumber: Int = 1,
    val voiceName: String = "AniLibria",
    val availableVoices: List<AnimeVoiceTranslation> = emptyList(),
    val availableQualities: List<String> = listOf("1080p", "720p", "480p", "360p"),
    val selectedQuality: String = "1080p",
    val playbackSpeed: Float = 1.0f,
    val areControlsVisible: Boolean = true,
    val resizeMode: Int = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
    val isMovie: Boolean = false,
    val isQualityDialogVisible: Boolean = false,
    val isSpeedDialogVisible: Boolean = false,
    val isVoiceDialogVisible: Boolean = false,
    val isSettingsDialogVisible: Boolean = false,
    val isResizeModeDialogVisible: Boolean = false,
    val settings: PlayerSettingsEntity = PlayerSettingsEntity(),
    val errorMessage: String? = null
)

@OptIn(UnstableApi::class)
class PlayerViewModel(
    application: Application,
    private val animeId: Long,
    initialEpisodeNum: Int,
    initialVoiceName: String
) : AndroidViewModel(application) {
    private val TAG = "PlayerViewModel"
    private val animeRepository = AnimeRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            episodeNumber = initialEpisodeNum,
            voiceName = initialVoiceName
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    var exoPlayer: ExoPlayer? = null
        private set

    private var progressTrackingJob: Job? = null
    private var controlsHideJob: Job? = null
    private var streamLinks: KodikStreamLinks? = null
    private var animePosterUrl: String = ""
    private var totalEpisodesCount: Int = 12

    init {
        initPlayer(application)
        loadSettingsAndStreams()
    }

    private fun initPlayer(context: Application) {
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0")
            .setDefaultRequestProperties(mapOf("Referer" to "https://kodikplayer.com/"))
            .setAllowCrossProtocolRedirects(true)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpDataSourceFactory)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            setEnableDecoderFallback(true)
        }

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }
                        Player.STATE_READY -> {
                            val dur = if (duration > 0) duration else _uiState.value.durationMs
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                durationMs = dur,
                                bufferedPositionMs = bufferedPosition.coerceAtLeast(0L),
                                errorMessage = null
                            )
                        }
                        Player.STATE_ENDED -> {
                            _uiState.value = _uiState.value.copy(isPlaying = false)
                            // Auto advance to next episode if available
                            playNextEpisode()
                        }
                        Player.STATE_IDLE -> Unit
                    }
                }

                override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                    if (duration > 0) {
                        _uiState.value = _uiState.value.copy(durationMs = duration)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e(TAG, "Playback error: ${error.errorCodeName} - ${error.message}")
                    // Attempt retry with streamLinks direct/fallback without changing user's selectedQuality in UI
                    val fallback = streamLinks?.quality1080p ?: streamLinks?.directVideoUrl ?: streamLinks?.quality720p
                    if (fallback != null && exoPlayer != null) {
                        val p = exoPlayer!!
                        val cur = p.currentPosition
                        p.setMediaItem(MediaItem.fromUri(fallback))
                        p.prepare()
                        if (cur > 0) p.seekTo(cur)
                        p.playWhenReady = true
                        return
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isPlaying = false,
                        errorMessage = "Ошибка воспроизведения: ${error.message ?: "Поток недоступен"}"
                    )
                }
            })
        }
        exoPlayer = player
        startProgressTracker()
    }

    private fun loadSettingsAndStreams() {
        viewModelScope.launch {
            val settings = settingsRepository.getPlayerSettingsDirect()
            val anime = animeRepository.getAnimeDetails(animeId)
            val isMovie = anime.kind == "movie" || (anime.episodes ?: 0) == 1
            animePosterUrl = AnimeRepository.resolveImageUrl(
                anime.image?.original ?: anime.image?.preview,
                animeId = anime.id,
                animeName = anime.name
            )
            totalEpisodesCount = if (isMovie) 1 else (anime.episodes ?: 12)

            _uiState.value = _uiState.value.copy(
                settings = settings,
                animeTitle = anime.russian?.takeIf { it.isNotBlank() } ?: anime.name,
                playbackSpeed = settings.defaultSpeed,
                selectedQuality = settings.defaultQuality,
                isMovie = isMovie
            )

            loadEpisodeStream(_uiState.value.episodeNumber)
        }
    }

    fun loadEpisodeStream(episodeNum: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, episodeNumber = episodeNum)
            try {
                val isMovie = _uiState.value.isMovie
                val allVoices = animeRepository.getVoiceTranslations(animeId, _uiState.value.animeTitle, isMovie = isMovie)
                _uiState.value = _uiState.value.copy(availableVoices = allVoices)
                val targetVoice = allVoices.firstOrNull { it.name == _uiState.value.voiceName }
                    ?: allVoices.firstOrNull()
                    ?: AnimeVoiceTranslation(
                        id = _uiState.value.voiceName.lowercase().replace(" ", "_"),
                        name = _uiState.value.voiceName,
                        kodikLink = if (isMovie) "https://kodikplayer.com/video/$animeId" else "https://kodikplayer.com/serial/$animeId?d=ep$episodeNum",
                        mediaType = if (isMovie) "video" else "serial"
                    )

                val links = animeRepository.getStreamLinks(animeId, targetVoice, episodeNum, isMovie = isMovie)
                streamLinks = links

                val targetQuality = _uiState.value.selectedQuality
                val streamUrl = links.getStreamForQuality(targetQuality)
                    ?: links.quality1080p
                    ?: links.quality720p
                    ?: links.quality480p
                    ?: links.quality360p
                    ?: links.directVideoUrl

                if (!streamUrl.isNullOrBlank()) {
                    exoPlayer?.let { player ->
                        val (maxW, maxH, minW, minH) = when (targetQuality) {
                            "1080p" -> listOf(1920, 1080, 1280, 720)
                            "720p" -> listOf(1280, 720, 854, 480)
                            "480p" -> listOf(854, 480, 640, 360)
                            "360p" -> listOf(640, 360, 0, 0)
                            else -> listOf(1920, 1080, 0, 0)
                        }
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setMaxVideoSize(maxW, maxH)
                            .setMinVideoSize(minW, minH)
                            .build()

                        player.setMediaItem(MediaItem.fromUri(streamUrl))
                        player.playbackParameters = PlaybackParameters(_uiState.value.playbackSpeed)
                        player.prepare()

                        // Resume saved progress if available
                        val history = animeRepository.getHistoryDirect(animeId)
                        if (history != null && history.episodeNumber == episodeNum && history.positionMs > 0) {
                            player.seekTo(history.positionMs)
                        }
                        player.playWhenReady = true
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        availableQualities = links.getAvailableQualities().ifEmpty { listOf("1080p", "720p", "480p", "360p") }
                    )
                    saveWatchProgress()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = if (isMovie) "Поток фильма временно недоступен для озвучки \"${targetVoice.name}\". Попробуйте выбрать другую озвучку." else "Поток серии $episodeNum временно недоступен для озвучки \"${targetVoice.name}\"."
                    )
                }
                resetControlsTimer()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Ошибка загрузки видео"
                )
            }
        }
    }

    private fun startProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = viewModelScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = if (player.duration > 0) player.duration else _uiState.value.durationMs
                    val buf = player.bufferedPosition.coerceAtLeast(0L)
                    _uiState.value = _uiState.value.copy(
                        currentPositionMs = pos,
                        durationMs = dur,
                        bufferedPositionMs = buf
                    )
                    // Auto-save history every 5 seconds
                    if (dur > 0 && pos > 1000) {
                        saveWatchProgress()
                    }
                }
                delay(300)
            }
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
            resetControlsTimer()
        }
    }

    fun pausePlayback() {
        try {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                }
            }
        } catch (_: Exception) {}
    }

    fun seekRelative(seconds: Int) {
        exoPlayer?.let { player ->
            val effectiveDuration = if (player.duration > 0) {
                player.duration
            } else if (_uiState.value.durationMs > 0) {
                _uiState.value.durationMs
            } else {
                Long.MAX_VALUE
            }
            val current = if (player.currentPosition >= 0) player.currentPosition else _uiState.value.currentPositionMs
            val target = (current + (seconds * 1000L)).coerceIn(0L, effectiveDuration)
            player.seekTo(target)
            _uiState.value = _uiState.value.copy(currentPositionMs = target)
            resetControlsTimer()
        }
    }

    fun seekToPosition(positionMs: Long) {
        exoPlayer?.let { player ->
            val effectiveDuration = if (player.duration > 0) {
                player.duration
            } else if (_uiState.value.durationMs > 0) {
                _uiState.value.durationMs
            } else {
                Long.MAX_VALUE
            }
            val target = positionMs.coerceIn(0L, effectiveDuration)
            player.seekTo(target)
            _uiState.value = _uiState.value.copy(currentPositionMs = target)
            resetControlsTimer()
        }
    }

    fun skipOpening() {
        val step = _uiState.value.settings.seekStepSeconds
        seekRelative(step)
    }

    fun playNextEpisode() {
        val next = _uiState.value.episodeNumber + 1
        if (next <= totalEpisodesCount) {
            loadEpisodeStream(next)
        }
    }

    fun playPreviousEpisode() {
        val prev = _uiState.value.episodeNumber - 1
        if (prev >= 1) {
            loadEpisodeStream(prev)
        }
    }

    fun setQuality(quality: String) {
        _uiState.value = _uiState.value.copy(selectedQuality = quality, isQualityDialogVisible = false)
        val stream = streamLinks?.getStreamForQuality(quality) ?: return
        exoPlayer?.let { player ->
            val curPos = player.currentPosition
            val isPlaying = player.isPlaying

            val (maxW, maxH, minW, minH) = when (quality) {
                "1080p" -> listOf(1920, 1080, 1280, 720)
                "720p" -> listOf(1280, 720, 854, 480)
                "480p" -> listOf(854, 480, 640, 360)
                "360p" -> listOf(640, 360, 0, 0)
                else -> listOf(1920, 1080, 0, 0)
            }
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(maxW, maxH)
                .setMinVideoSize(minW, minH)
                .build()

            player.setMediaItem(MediaItem.fromUri(stream))
            player.prepare()
            player.seekTo(curPos)
            if (isPlaying) {
                player.play()
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed, isSpeedDialogVisible = false)
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    fun toggleControlsVisibility() {
        val nextVisible = !_uiState.value.areControlsVisible
        _uiState.value = _uiState.value.copy(areControlsVisible = nextVisible)
        if (nextVisible) {
            resetControlsTimer()
        }
    }

    fun showQualityDialog() {
        _uiState.value = _uiState.value.copy(isQualityDialogVisible = true)
    }

    fun hideQualityDialog() {
        _uiState.value = _uiState.value.copy(isQualityDialogVisible = false)
    }

    fun showSpeedDialog() {
        _uiState.value = _uiState.value.copy(isSpeedDialogVisible = true)
    }

    fun hideSpeedDialog() {
        _uiState.value = _uiState.value.copy(isSpeedDialogVisible = false)
    }

    fun showVoiceDialog() {
        _uiState.value = _uiState.value.copy(isVoiceDialogVisible = true)
    }

    fun hideVoiceDialog() {
        _uiState.value = _uiState.value.copy(isVoiceDialogVisible = false)
    }

    fun selectVoice(voice: AnimeVoiceTranslation) {
        _uiState.value = _uiState.value.copy(
            voiceName = voice.name,
            isVoiceDialogVisible = false
        )
        loadEpisodeStream(_uiState.value.episodeNumber)
    }

    fun showResizeModeDialog() {
        _uiState.value = _uiState.value.copy(isResizeModeDialogVisible = true)
    }

    fun hideResizeModeDialog() {
        _uiState.value = _uiState.value.copy(isResizeModeDialogVisible = false)
    }

    fun setResizeMode(mode: Int) {
        _uiState.value = _uiState.value.copy(resizeMode = mode, isResizeModeDialogVisible = false)
    }

    fun showSettingsDialog() {
        _uiState.value = _uiState.value.copy(isSettingsDialogVisible = true)
    }

    fun hideSettingsDialog() {
        _uiState.value = _uiState.value.copy(isSettingsDialogVisible = false)
    }

    fun updateSettings(newSettings: PlayerSettingsEntity) {
        _uiState.value = _uiState.value.copy(settings = newSettings, isSettingsDialogVisible = false)
        viewModelScope.launch {
            settingsRepository.updateSettings(newSettings)
        }
    }

    private fun resetControlsTimer() {
        controlsHideJob?.cancel()
        controlsHideJob = viewModelScope.launch {
            delay(3500)
            if (_uiState.value.isPlaying) {
                _uiState.value = _uiState.value.copy(areControlsVisible = false)
            }
        }
    }

    private fun saveWatchProgress() {
        viewModelScope.launch {
            val state = _uiState.value
            animeRepository.saveWatchProgress(
                animeId = animeId,
                name = state.animeTitle,
                russianName = state.animeTitle,
                posterUrl = animePosterUrl,
                episodeNumber = state.episodeNumber,
                episodeTitle = "Серия ${state.episodeNumber}",
                voiceName = state.voiceName,
                streamUrl = "",
                positionMs = state.currentPositionMs,
                durationMs = state.durationMs,
                quality = state.selectedQuality
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveWatchProgress()
        progressTrackingJob?.cancel()
        controlsHideJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}
