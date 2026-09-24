package com.example.data.api

import android.util.Log
import com.example.data.api.models.RelatedAnimeItem
import com.example.data.api.models.ShikimoriAnimeDetailDto
import com.example.data.api.models.ShikimoriGenreDto
import com.example.data.api.models.ShikimoriImageDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

@JsonClass(generateAdapter = true)
data class AnixartCategoryDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null
)

@JsonClass(generateAdapter = true)
data class AnixartRelatedRefDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "name_ru") val nameRu: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "image") val image: String? = null
)

@JsonClass(generateAdapter = true)
data class AnixartReleaseDto(
    @Json(name = "id") val id: Long,
    @Json(name = "title_ru") val titleRu: String? = null,
    @Json(name = "title_original") val titleOriginal: String? = null,
    @Json(name = "title_alt") val titleAlt: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "poster") val poster: String? = null,
    @Json(name = "year") val year: String? = null,
    @Json(name = "season") val season: Int? = null,
    @Json(name = "grade") val grade: Double? = null,
    @Json(name = "category") val category: AnixartCategoryDto? = null,
    @Json(name = "episodes_total") val episodesTotal: Int? = null,
    @Json(name = "episodes_released") val episodesReleased: Int? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "genres") val genres: String? = null,
    @Json(name = "screenshot_images") val screenshotImages: List<String>? = null,
    @Json(name = "related") val related: AnixartRelatedRefDto? = null,
    @Json(name = "related_releases") val relatedReleases: List<AnixartReleaseDto>? = null
)

@JsonClass(generateAdapter = true)
data class AnixartReleaseDetailResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "release") val release: AnixartReleaseDto? = null
)

@JsonClass(generateAdapter = true)
data class AnixartSearchResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "content") val content: List<AnixartReleaseDto>? = null,
    @Json(name = "releases") val releases: List<AnixartReleaseDto>? = null
)

@JsonClass(generateAdapter = true)
data class AnixartRelatedResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "content") val content: List<AnixartReleaseDto>? = null,
    @Json(name = "releases") val releases: List<AnixartReleaseDto>? = null
)

interface AnixartApi {
    @Headers("User-Agent: AnixartApp/8.2", "Content-Type: application/json")
    @POST("search/releases/{page}")
    suspend fun searchReleases(
        @Path("page") page: Int = 0,
        @Body body: Map<String, String>
    ): AnixartSearchResponse

    @Headers("User-Agent: AnixartApp/8.2")
    @GET("release/{id}")
    suspend fun getRelease(
        @Path("id") id: Long
    ): AnixartReleaseDetailResponse

    @Headers("User-Agent: AnixartApp/8.2")
    @GET("related/{relatedId}/{page}")
    suspend fun getRelated(
        @Path("relatedId") relatedId: Long,
        @Path("page") page: Int = 0
    ): AnixartRelatedResponse
}

object AnixartService {
    private const val TAG = "AnixartService"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val api: AnixartApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.anixart.tv/")
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AnixartApi::class.java)
    }

    // In-memory caches for fast retrieval
    private val releaseCache = ConcurrentHashMap<Long, AnixartReleaseDto>()
    private val franchiseCache = ConcurrentHashMap<Long, List<AnixartReleaseDto>>()
    private val titleFranchiseIdCache = ConcurrentHashMap<String, Long>()

    // Pre-seeded franchise mappings for instant lookup of top franchises
    private val KNOWN_FRANCHISE_IDS = mapOf(
        // Attack on Titan
        "атака титанов" to 546L,
        "shingeki no kyojin" to 546L,
        "attack on titan" to 546L,
        // Jujutsu Kaisen
        "магическая битва" to 2297L,
        "jujutsu kaisen" to 2297L,
        // Demon Slayer
        "клинок, рассекающий демонов" to 1033L,
        "клинок рассекающий демонов" to 1033L,
        "kimetsu no yaiba" to 1033L,
        "demon slayer" to 1033L,
        // Solo Leveling
        "поднятие уровня в одиночку" to 2614L,
        "solo leveling" to 2614L,
        "ore dake level up na ken" to 2614L,
        // Frieren
        "провожающая в последний путь фрирен" to 2588L,
        "фрирен" to 2588L,
        "sousou no frieren" to 2588L,
        // Bleach
        "блич" to 180L,
        "bleach" to 180L,
        // Re:Zero
        "re:zero" to 736L,
        "жизнь в альтернативном мире с нуля" to 736L,
        "re:zero. жизнь с нуля в альтернативном мире" to 736L,
        // Mushoku Tensei
        "реинкарнация безработного" to 2355L,
        "mushoku tensei" to 2355L,
        // Chainsaw Man
        "человек-бензопила" to 2601L,
        "человек бензопила" to 2601L,
        "chainsaw man" to 2601L,
        // Naruto
        "наруто" to 267L,
        "боруто" to 267L,
        "naruto" to 267L,
        // One Piece
        "ван-пис" to 593L,
        "ван пис" to 593L,
        "one piece" to 593L,
        // Tokyo Ghoul
        "токийский гуль" to 527L,
        "tokyo ghoul" to 527L,
        // Hunter x Hunter
        "хантер х хантер" to 447L,
        "охотник х охотник" to 447L,
        "hunter x hunter" to 447L,
        // Classroom of the Elite
        "добро пожаловать в класс превосходства" to 844L,
        "класс превосходства" to 844L,
        "classroom of the elite" to 844L,
        // Spy x Family
        "семья шпиона" to 2407L,
        "spy x family" to 2407L,
        // Death Note
        "тетрадь смерти" to 2538L,
        "death note" to 2538L,
        // Oshi no Ko
        "ребёнок идола" to 2552L,
        "звёздное дитя" to 2552L,
        "oshi no ko" to 2552L
    )

    fun getCachedRelease(id: Long): AnixartReleaseDto? = releaseCache[id]

    suspend fun getRelease(id: Long): AnixartReleaseDto? = withContext(Dispatchers.IO) {
        releaseCache[id]?.let { return@withContext it }
        try {
            val response = api.getRelease(id)
            val rel = response.release
            if (rel != null) {
                releaseCache[id] = rel
                return@withContext rel
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get Anixart release $id: ${e.message}")
        }
        null
    }

    suspend fun searchReleases(query: String): List<AnixartReleaseDto> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val cleanQuery = cleanSearchQuery(query)
        try {
            val response = api.searchReleases(0, mapOf("query" to cleanQuery))
            val items = response.content ?: response.releases ?: emptyList()
            items.forEach { releaseCache[it.id] = it }
            return@withContext items
        } catch (e: Exception) {
            Log.w(TAG, "Failed to search Anixart for '$cleanQuery': ${e.message}")
            emptyList()
        }
    }

    suspend fun getRelatedFranchise(relatedId: Long): List<AnixartReleaseDto> = withContext(Dispatchers.IO) {
        franchiseCache[relatedId]?.let { return@withContext it }
        try {
            val response = api.getRelated(relatedId, 0)
            val items = response.content ?: response.releases ?: emptyList()
            if (items.isNotEmpty()) {
                franchiseCache[relatedId] = items
                items.forEach { releaseCache[it.id] = it }
                return@withContext items
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch Anixart franchise $relatedId: ${e.message}")
        }
        emptyList()
    }

    /**
     * Primary entry point: retrieves real related releases from Anixart for the given anime
     */
    suspend fun getRelatedReleases(
        animeTitle: String?,
        currentAnimeId: Long
    ): List<RelatedAnimeItem> = withContext(Dispatchers.IO) {
        val title = animeTitle?.trim() ?: ""
        if (title.isBlank() && currentAnimeId <= 0) return@withContext emptyList()

        // 1. Resolve franchise ID via pre-seeded mapping or cache
        val normTitle = title.lowercase().trim()
        var franchiseId: Long? = KNOWN_FRANCHISE_IDS[normTitle]
            ?: titleFranchiseIdCache[normTitle]

        if (franchiseId == null) {
            for ((key, id) in KNOWN_FRANCHISE_IDS) {
                if (normTitle.contains(key) || key.contains(normTitle)) {
                    franchiseId = id
                    break
                }
            }
        }

        // 2. If not found, search Anixart by title to discover the franchise
        if (franchiseId == null && title.isNotBlank()) {
            val searchResults = searchReleases(title)
            if (searchResults.isNotEmpty()) {
                // Find best matching release
                val matched = searchResults.firstOrNull { r ->
                    val rTitle = r.titleRu?.lowercase() ?: ""
                    rTitle.contains(normTitle) || normTitle.contains(rTitle)
                } ?: searchResults.first()

                // Check if release already has related object
                franchiseId = matched.related?.id
                if (franchiseId == null) {
                    val full = getRelease(matched.id)
                    franchiseId = full?.related?.id
                }
                if (franchiseId != null) {
                    titleFranchiseIdCache[normTitle] = franchiseId
                }
            }
        }

        // 3. If franchiseId is available, load all franchise releases from Anixart!
        if (franchiseId != null) {
            val rawReleases = getRelatedFranchise(franchiseId)
            if (rawReleases.isNotEmpty()) {
                return@withContext mapAnixartReleasesToItems(rawReleases, title, currentAnimeId)
            }
        }

        emptyList()
    }

    private fun mapAnixartReleasesToItems(
        releases: List<AnixartReleaseDto>,
        currentTitle: String,
        currentAnimeId: Long
    ): List<RelatedAnimeItem> {
        val normCurrent = currentTitle.lowercase().trim()

        // Determine current year for relation heuristics
        val currentYear = releases.firstOrNull { r ->
            r.id == currentAnimeId || (r.titleRu?.lowercase()?.trim() == normCurrent)
        }?.year?.toIntOrNull() ?: 2024

        val items = releases.map { release ->
            val isCurrent = (release.id == currentAnimeId) ||
                    (normCurrent.isNotBlank() && release.titleRu?.lowercase()?.trim() == normCurrent)

            val rawPoster = release.image
                ?: if (!release.poster.isNullOrBlank()) "https://s.anixmirai.com/posters/${release.poster}.jpg" else ""

            val titleRu = release.titleRu?.takeIf { it.isNotBlank() }
                ?: release.titleOriginal
                ?: "Релиз"
            val titleOrig = release.titleOriginal?.takeIf { it.isNotBlank() } ?: titleRu

            val yearInt = release.year?.toIntOrNull() ?: 9999
            val displayYear = if (yearInt in 1950..2035) "$yearInt г." else "Год не указан"

            val categoryName = release.category?.name ?: "Сериал"
            val kindText = when (categoryName.lowercase()) {
                "сериал" -> "ТВ Сериал"
                "фильм" -> "Фильм"
                "спешл" -> "Спешл"
                "ova" -> "OVA"
                else -> categoryName
            }

            val relationRussian = determineRelationLabel(
                title = titleRu,
                category = categoryName,
                yearInt = yearInt,
                currentYear = currentYear,
                isCurrent = isCurrent
            )

            val scoreText = if (release.grade != null && release.grade > 0.0) {
                // Anixart grade is 0.0 - 5.0, convert to standard 10-point scale (e.g. 4.8 * 2 = 9.6)
                String.format(Locale.US, "%.1f", release.grade * 2.0).takeIf { it != "0.0" } ?: "8.5"
            } else "8.5"

            val episodesCount = release.episodesTotal
                ?: release.episodesReleased
                ?: 1

            RelatedAnimeItem(
                id = release.id,
                name = titleOrig,
                russianName = titleRu,
                posterUrl = rawPoster,
                relation = if (isCurrent) "current" else "franchise",
                relationRussian = relationRussian,
                year = displayYear,
                yearInt = yearInt,
                score = scoreText,
                kind = kindText,
                episodes = episodesCount,
                isCurrent = isCurrent
            )
        }

        // Sort chronologically by year, then by release ID
        return items.sortedWith(
            compareBy<RelatedAnimeItem> { it.yearInt }
                .thenBy { it.id }
        )
    }

    private fun determineRelationLabel(
        title: String,
        category: String,
        yearInt: Int,
        currentYear: Int,
        isCurrent: Boolean
    ): String {
        if (isCurrent) return "Текущий релиз"
        if (category.equals("Фильм", ignoreCase = true)) return "Фильм"
        if (category.equals("OVA", ignoreCase = true)) return "OVA"
        if (category.equals("Спешл", ignoreCase = true)) return "Спешл"

        val t = title.lowercase()
        if (t.contains("финал") || t.contains("заключительная")) return "Финал"
        if (t.contains(" 2") || t.contains(" 2:") || t.contains("2 сезон")) return "2 сезон"
        if (t.contains(" 3") || t.contains(" 3:") || t.contains("3 сезон")) return "3 сезон"
        if (t.contains(" 4") || t.contains(" 4:") || t.contains("4 сезон")) return "4 сезон"
        if (t.contains("рекап")) return "Рекап"

        return when {
            yearInt < currentYear -> "Предыстория"
            yearInt > currentYear -> "Продолжение"
            else -> "Сериал"
        }
    }

    private fun cleanSearchQuery(raw: String): String {
        return raw
            .replace(Regex("(?i)\\b(1|2|3|4|5)\\s*сезон\\b"), "")
            .replace(Regex("(?i)\\bсезон\\s*(1|2|3|4|5)\\b"), "")
            .replace(Regex("(?i)\\bчасть\\s*(1|2|3|4)\\b"), "")
            .replace(Regex("[.,:;!?'\"()\\[\\]{}]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    fun AnixartReleaseDto.toShikimoriAnimeDetailDto(): ShikimoriAnimeDetailDto {
        val gradeScore = if (grade != null && grade > 0.0) {
            String.format(Locale.US, "%.2f", grade * 2.0)
        } else "8.50"

        val kindCode = when (category?.name?.lowercase()) {
            "фильм" -> "movie"
            "ova" -> "ova"
            "спешл" -> "special"
            else -> "tv"
        }

        val posterUrl = image
            ?: if (!poster.isNullOrBlank()) "https://s.anixmirai.com/posters/$poster.jpg" else null

        val genresList = genres?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.map {
            ShikimoriGenreDto(id = 0L, name = it, russian = it, kind = "anime", entryType = "Anime")
        }

        val totalEp = episodesTotal ?: episodesReleased ?: 1
        val airedEp = episodesReleased ?: episodesTotal ?: 1

        return ShikimoriAnimeDetailDto(
            id = id,
            name = titleOriginal ?: titleRu ?: "",
            russian = titleRu ?: titleOriginal ?: "",
            image = ShikimoriImageDto(
                original = posterUrl,
                preview = posterUrl,
                x96 = posterUrl,
                x48 = posterUrl
            ),
            kind = kindCode,
            score = gradeScore,
            status = if (episodesReleased != null && episodesTotal != null && episodesReleased >= episodesTotal) "released" else "ongoing",
            episodes = totalEp,
            episodesAired = airedEp,
            airedOn = year,
            releasedOn = year,
            rating = "r_plus",
            english = listOfNotNull(titleOriginal),
            japanese = null,
            synonyms = listOfNotNull(titleAlt),
            duration = null,
            description = description ?: "Описание предоставлено каталогом Anixart.",
            descriptionHtml = null,
            franchise = null,
            genres = genresList,
            studios = null
        )
    }
}
