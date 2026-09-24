package com.example.data.api

import com.example.data.api.models.ScheduleItem
import com.example.data.api.models.ShikimoriAnimeDetailDto
import com.example.data.api.models.ShikimoriAnimeDto
import com.example.data.repository.AnimeScheduleData
import com.example.data.settings.AppSettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class AnimeEpisodeInfo(
    val airedEpisodes: Int?,
    val totalEpisodes: Int?,
    val isOngoing: Boolean,
    val isAnons: Boolean,
    val formattedText: String
)

object AnimeEpisodeHelper {

    // Curated accurate aired/total voiced episodes baseline for popular ongoings
    private val KNOWN_ONGOING_EPISODES: Map<Long, Pair<Int, Int>> = mapOf(
        59193L to Pair(13, 14), // Реинкарнация безработного 3 (13 из 14 сер.)
        58567L to Pair(13, 13), // Поднятие уровня в одиночку 2 (13 из 13 сер.)
        59978L to Pair(10, 10), // Провожающая в последний путь Фрирен 2 (10 сер. в озвучке)
        60058L to Pair(11, 12), // Ребёнок идола 3 (11 из 12 сер.)
        55825L to Pair(12, 13), // Адский рай 2 (12 из 13 сер.)
        49233L to Pair(12, 12), // Военная хроника маленькой девочки 2 (12 из 12 сер.)
        57658L to Pair(12, 12), // Магическая битва: Смертельная миграция
        51553L to Pair(12, 12), // Ателье колдовских колпаков
        56009L to Pair(12, 12), // Приговорённый быть героем
        60601L to Pair(12, 12), // Перерождение в аристократа со способностью анализа 3
        59787L to Pair(12, 12), // Военная хроника Ромелии
        63098L to Pair(12, 12), // Псайрен (Инкогнито)
        63409L to Pair(12, 12), // Я ведьма, которую возлюбленный попросил создать любовное зелье
        63712L to Pair(12, 12), // Я перевоплотился в гоблина, вопросы есть?
        61316L to Pair(12, 24), // Невероятные приключения ДжоДжо: Гонка «Стальной шар»
        63240L to Pair(16, 52), // Путешествие к бессмертию 5
        61214L to Pair(12, 12), // Шероховатый мир: Перерождение
        37096L to Pair(248, 250), // Игры и драконы
        21L to Pair(1123, 1123)   // Ван-Пис
    )

    // In-memory cache for dynamic voice/dubbing episode counts discovered in real time from Kodik
    private val dynamicVoiceEpisodesCache = ConcurrentHashMap<Long, Int>()

    // Concurrency control to throttle background queries
    private val lastCheckedTimestamps = ConcurrentHashMap<Long, Long>()

    // Reactive StateFlow broadcasting live episode info updates across all screens
    private val _liveEpisodesFlow = MutableStateFlow<Map<Long, AnimeEpisodeInfo>>(emptyMap())
    val liveEpisodesFlow: StateFlow<Map<Long, AnimeEpisodeInfo>> = _liveEpisodesFlow.asStateFlow()

    fun recordVoiceEpisodes(animeId: Long, maxEpisodes: Int) {
        if (maxEpisodes > 0) {
            val current = dynamicVoiceEpisodesCache[animeId] ?: 0
            if (maxEpisodes > current) {
                dynamicVoiceEpisodesCache[animeId] = maxEpisodes
            }
        }
    }

    /**
     * Triggers a fast background update directly querying Kodik voiceovers.
     * Updates automatically and broadcasts real-time counts when new episodes release.
     */
    fun requestLiveUpdate(
        animeId: Long,
        name: String? = null,
        russian: String? = null,
        episodesTotal: Int? = null,
        episodesAired: Int? = null,
        status: String? = null
    ) {
        if (animeId <= 0) return
        val now = System.currentTimeMillis()
        val lastCheck = lastCheckedTimestamps[animeId] ?: 0L
        if (now - lastCheck < 60_000L) {
            // Already checked within last minute
            return
        }
        lastCheckedTimestamps[animeId] = now

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val titleCandidate = russian?.takeIf { it.isNotBlank() } ?: name
                val liveVoiced = KodikService.getLiveVoicedEpisodeCount(animeId, titleCandidate)
                if (liveVoiced != null && liveVoiced > 0) {
                    recordVoiceEpisodes(animeId, liveVoiced)
                    val info = getEpisodeInfo(
                        id = animeId,
                        episodes = episodesTotal,
                        episodesAired = liveVoiced,
                        status = status ?: "ongoing"
                    )
                    val currentMap = _liveEpisodesFlow.value.toMutableMap()
                    currentMap[animeId] = info
                    _liveEpisodesFlow.value = currentMap
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Resolves the best available episode info for a ShikimoriAnimeDto.
     */
    fun getEpisodeInfo(anime: ShikimoriAnimeDto, isUk: Boolean = AppSettingsManager.isUkrainian()): AnimeEpisodeInfo {
        return getEpisodeInfo(
            id = anime.id,
            episodes = anime.episodes,
            episodesAired = anime.episodesAired,
            status = anime.status,
            isUk = isUk
        )
    }

    /**
     * Resolves the best available episode info for a ShikimoriAnimeDetailDto.
     */
    fun getEpisodeInfo(anime: ShikimoriAnimeDetailDto, isUk: Boolean = AppSettingsManager.isUkrainian()): AnimeEpisodeInfo {
        return getEpisodeInfo(
            id = anime.id,
            episodes = anime.episodes,
            episodesAired = anime.episodesAired,
            status = anime.status,
            isUk = isUk
        )
    }

    /**
     * Core resolver using primitives.
     */
    fun getEpisodeInfo(
        id: Long,
        episodes: Int?,
        episodesAired: Int?,
        status: String?,
        isUk: Boolean = AppSettingsManager.isUkrainian()
    ): AnimeEpisodeInfo {
        val isAnonsStatus = status?.contains("anons", ignoreCase = true) == true
        val isOngoingStatus = status?.contains("ongoing", ignoreCase = true) == true

        // 1. Dynamic dubbing cache from Kodik player
        val cachedVoiceAired = dynamicVoiceEpisodesCache[id]

        // 2. Curated baseline for popular ongoings
        val curated = KNOWN_ONGOING_EPISODES[id]

        // 3. Schedule items
        val scheduleItem: ScheduleItem? = try {
            AnimeScheduleData.getRealShikimoriSchedule().firstOrNull { it.anime.id == id }
        } catch (_: Exception) {
            null
        }

        val scheduleAired: Int? = scheduleItem?.let { item ->
            val next = item.nextEpisode ?: 1
            (next - 1).coerceAtLeast(0)
        }
        val scheduleTotal: Int? = scheduleItem?.anime?.episodes?.takeIf { it > 0 }
            ?: scheduleAired?.let { aired -> if (aired > 12) (if (aired <= 14) 14 else 24) else 12 }

        // Resolve best aired episodes count: ALWAYS prioritize the real live count in voiceover
        val resolvedAired: Int? = when {
            cachedVoiceAired != null && cachedVoiceAired > 0 -> {
                val baseline = maxOf(episodesAired ?: 0, curated?.first ?: 0, scheduleAired ?: 0)
                maxOf(cachedVoiceAired, baseline)
            }
            episodesAired != null && episodesAired > 0 -> episodesAired
            curated != null && curated.first > 0 -> curated.first
            scheduleAired != null && scheduleAired > 0 -> scheduleAired
            else -> null
        }

        // Resolve best total episodes count
        val resolvedTotal: Int? = when {
            curated != null && curated.second > 0 -> {
                if (episodes != null && episodes > 0 && episodes >= (resolvedAired ?: 0)) episodes else curated.second
            }
            episodes != null && episodes > 0 -> episodes
            scheduleTotal != null && scheduleTotal > 0 -> scheduleTotal
            else -> null
        }

        // Determine if ongoing
        val isOngoing = !isAnonsStatus && (
            isOngoingStatus ||
            scheduleItem != null ||
            (resolvedAired != null && resolvedTotal != null && resolvedAired > 0 && resolvedAired < resolvedTotal) ||
            (resolvedAired != null && resolvedAired > 0 && resolvedTotal == null)
        )

        val formattedText = formatEpisodeLabel(
            aired = resolvedAired,
            total = resolvedTotal,
            isOngoing = isOngoing,
            isAnons = isAnonsStatus,
            isUk = isUk
        )

        return AnimeEpisodeInfo(
            airedEpisodes = resolvedAired,
            totalEpisodes = resolvedTotal,
            isOngoing = isOngoing,
            isAnons = isAnonsStatus,
            formattedText = formattedText
        )
    }

    /**
     * Formats the episode text label.
     * Ongoing examples:
     * - "13 из 14 сер." (RU) / "13 з 14 сер." (UK)
     * - "10 из 12 сер." (RU) / "10 з 12 сер." (UK)
     * Released / all out examples:
     * - "12 сер." (RU/UK)
     */
    fun formatEpisodeLabel(
        aired: Int?,
        total: Int?,
        isOngoing: Boolean,
        isAnons: Boolean,
        isUk: Boolean
    ): String {
        return when {
            isAnons -> {
                val base = "Анонс"
                if (total != null && total > 0) "$base ($total сер.)" else base
            }
            isOngoing -> {
                when {
                    aired != null && total != null && total > 0 -> {
                        if (aired < total) {
                            if (isUk) "$aired з $total сер." else "$aired из $total сер."
                        } else {
                            "$total сер."
                        }
                    }
                    aired != null && aired > 0 -> {
                        if (isUk) "Вийшло $aired сер." else "Вышло $aired сер."
                    }
                    total != null && total > 0 -> {
                        if (isUk) "Онґоінґ ($total сер.)" else "Онгоинг ($total сер.)"
                    }
                    else -> {
                        if (isUk) "Онґоінґ" else "Онгоинг"
                    }
                }
            }
            total != null && total > 0 -> {
                "$total сер."
            }
            aired != null && aired > 0 -> {
                "$aired сер."
            }
            else -> ""
        }
    }
}
