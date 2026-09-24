package com.example.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShikimoriAnimeDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "russian") val russian: String?,
    @Json(name = "image") val image: ShikimoriImageDto?,
    @Json(name = "url") val url: String?,
    @Json(name = "kind") val kind: String?,
    @Json(name = "score") val score: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "episodes") val episodes: Int?,
    @Json(name = "episodes_aired") val episodesAired: Int?,
    @Json(name = "aired_on") val airedOn: String?,
    @Json(name = "released_on") val releasedOn: String?
)

@JsonClass(generateAdapter = true)
data class ShikimoriImageDto(
    @Json(name = "original") val original: String? = null,
    @Json(name = "preview") val preview: String? = null,
    @Json(name = "x96") val x96: String? = null,
    @Json(name = "x48") val x48: String? = null
)

@JsonClass(generateAdapter = true)
data class ShikimoriGenreDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "russian") val russian: String?,
    @Json(name = "kind") val kind: String?,
    @Json(name = "entry_type") val entryType: String?
)

@JsonClass(generateAdapter = true)
data class ShikimoriStudioDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "filtered_name") val filteredName: String?,
    @Json(name = "real") val real: Boolean?,
    @Json(name = "image") val image: String?
)

@JsonClass(generateAdapter = true)
data class ShikimoriAnimeDetailDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "russian") val russian: String?,
    @Json(name = "image") val image: ShikimoriImageDto?,
    @Json(name = "kind") val kind: String?,
    @Json(name = "score") val score: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "episodes") val episodes: Int?,
    @Json(name = "episodes_aired") val episodesAired: Int?,
    @Json(name = "aired_on") val airedOn: String?,
    @Json(name = "released_on") val releasedOn: String?,
    @Json(name = "rating") val rating: String?,
    @Json(name = "english") val english: List<String?>?,
    @Json(name = "japanese") val japanese: List<String?>?,
    @Json(name = "synonyms") val synonyms: List<String?>?,
    @Json(name = "duration") val duration: Int?,
    @Json(name = "description") val description: String?,
    @Json(name = "description_html") val descriptionHtml: String?,
    @Json(name = "franchise") val franchise: String?,
    @Json(name = "genres") val genres: List<ShikimoriGenreDto>?,
    @Json(name = "studios") val studios: List<ShikimoriStudioDto>?
)

@JsonClass(generateAdapter = true)
data class ShikimoriScreenshotDto(
    @Json(name = "original") val original: String?,
    @Json(name = "preview") val preview: String?
)

@JsonClass(generateAdapter = true)
data class ShikimoriMangaDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String?,
    @Json(name = "russian") val russian: String?
)

@JsonClass(generateAdapter = true)
data class ShikimoriRelatedDto(
    @Json(name = "relation") val relation: String?,
    @Json(name = "relation_russian") val relationRussian: String?,
    @Json(name = "anime") val anime: ShikimoriAnimeDto?,
    @Json(name = "manga") val manga: ShikimoriMangaDto? = null
)

@JsonClass(generateAdapter = true)
data class ShikimoriFranchiseDto(
    @Json(name = "links") val links: List<ShikimoriFranchiseLinkDto>? = null,
    @Json(name = "nodes") val nodes: List<ShikimoriFranchiseNodeDto>? = null
)

@JsonClass(generateAdapter = true)
data class ShikimoriFranchiseLinkDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "source_id") val sourceId: Long? = null,
    @Json(name = "target_id") val targetId: Long? = null,
    @Json(name = "relation") val relation: String? = null
)

@JsonClass(generateAdapter = true)
data class ShikimoriFranchiseNodeDto(
    @Json(name = "id") val id: Long,
    @Json(name = "date") val date: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "year") val year: Int? = null,
    @Json(name = "kind") val kind: String? = null,
    @Json(name = "weight") val weight: Int? = null
)

data class RelatedAnimeItem(
    val id: Long,
    val name: String,
    val russianName: String,
    val posterUrl: String,
    val relation: String,
    val relationRussian: String,
    val year: String,
    val yearInt: Int,
    val score: String,
    val kind: String,
    val episodes: Int,
    val isCurrent: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ShikimoriCalendarDto(
    @Json(name = "next_episode") val nextEpisode: Int? = null,
    @Json(name = "next_episode_at") val nextEpisodeAt: String? = null,
    @Json(name = "duration") val duration: Int? = null,
    @Json(name = "anime") val anime: ShikimoriAnimeDto
)

data class ScheduleItem(
    val anime: ShikimoriAnimeDto,
    val nextEpisode: Int?,
    val nextEpisodeAt: String?,
    val formattedTime: String,
    val dayOfWeek: Int, // 1 (Mon) to 7 (Sun)
    val dayName: String
)

