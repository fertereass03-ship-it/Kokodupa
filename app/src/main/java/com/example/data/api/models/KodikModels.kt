package com.example.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KodikStreamLinks(
    val quality1080p: String? = null,
    val quality720p: String? = null,
    val quality480p: String? = null,
    val quality360p: String? = null,
    val iframeUrl: String? = null,
    val directVideoUrl: String? = null
) {
    fun getAvailableQualities(): List<String> {
        return listOf("1080p", "720p", "480p", "360p")
    }

    fun getStreamForQuality(quality: String): String? {
        return when (quality) {
            "1080p" -> quality1080p ?: quality720p ?: quality480p ?: directVideoUrl
            "720p" -> quality720p ?: quality1080p ?: quality480p ?: directVideoUrl
            "480p" -> quality480p ?: quality720p ?: quality360p ?: directVideoUrl
            "360p" -> quality360p ?: quality480p ?: quality720p ?: directVideoUrl
            else -> quality1080p ?: quality720p ?: quality480p ?: quality360p ?: directVideoUrl
        }
    }
}

@JsonClass(generateAdapter = true)
data class KodikGetPlayerResponse(
    @Json(name = "found") val found: Boolean? = false,
    @Json(name = "allowed") val allowed: Int? = null,
    @Json(name = "quality") val quality: String? = null,
    @Json(name = "translation") val translation: String? = null,
    @Json(name = "link") val link: String? = null
)

@JsonClass(generateAdapter = true)
data class KodikSearchResponse(
    @Json(name = "time") val time: String? = null,
    @Json(name = "total") val total: Int? = null,
    @Json(name = "results") val results: List<KodikResultItem>? = null
)

@JsonClass(generateAdapter = true)
data class KodikResultItem(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "title_orig") val titleOrig: String? = null,
    @Json(name = "other_title") val otherTitle: String? = null,
    @Json(name = "translation") val translation: KodikTranslation? = null,
    @Json(name = "year") val year: Int? = null,
    @Json(name = "last_season") val lastSeason: Int? = null,
    @Json(name = "last_episode") val lastEpisode: Int? = null,
    @Json(name = "episodes_count") val episodesCount: Int? = null,
    @Json(name = "shikimori_id") val shikimoriId: String? = null,
    @Json(name = "seasons") val seasons: Map<String, KodikSeason>? = null
)

@JsonClass(generateAdapter = true)
data class KodikTranslation(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "type") val type: String? = null
)

@JsonClass(generateAdapter = true)
data class KodikSeason(
    @Json(name = "link") val link: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "episodes") val episodes: Map<String, String>? = null
)

data class KodikParsedEpisode(
    val number: Int,
    val id: String,
    val hash: String,
    val title: String,
    val label: String
)

data class AnimeVoiceTranslation(
    val id: String,
    val name: String,
    val type: String = "voice", // voice / sub
    val episodesCount: Int = 0,
    val kodikLink: String = "",
    val mediaId: String = "",
    val mediaHash: String = "",
    val mediaType: String = "serial",
    val dValue: String = "",
    val episodeUrls: Map<Int, String> = emptyMap(),
    val parsedEpisodes: List<KodikParsedEpisode> = emptyList()
)

data class AnimeEpisode(
    val number: Int,
    val title: String,
    val durationMin: Int = 24,
    val watchedPercent: Int = 0,
    val isWatched: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val kodikDirectLink: String? = null,
    val episodeId: String? = null,
    val episodeHash: String? = null
)
