package com.example.data.api

import android.util.Log
import com.example.data.api.models.ShikimoriAnimeDetailDto
import com.example.data.api.models.ShikimoriAnimeDto
import com.example.data.api.models.ShikimoriGenreDto
import com.example.data.api.models.ShikimoriImageDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class YaniAnimeItem(
    val animeId: Long,
    val title: String,
    val year: Int?,
    val posterUrl: String,
    val description: String,
    val shikimoriId: Long,
    val myanimelistId: Long,
    val kinopoiskId: Long,
    val score: String,
    val type: String?,
    val episodesCount: Int?,
    val episodesAired: Int?,
    val isAnons: Boolean = false
)

object YaniCatalogService {
    private const val TAG = "YaniCatalogService"
    private const val CATALOG_URL = "https://api.yani.tv/anime?limit=100&offset=0"
    private const val FALLBACK_CATALOG_URL = "https://api.yani.tv/anime/catalog"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Cache of Yani items
    private var cachedYaniItems: List<YaniAnimeItem>? = null
    // Fast lookup maps
    private val yaniByShikimoriId = mutableMapOf<Long, YaniAnimeItem>()
    private val yaniByAnimeId = mutableMapOf<Long, YaniAnimeItem>()
    private val yaniByTitle = mutableMapOf<String, YaniAnimeItem>()

    suspend fun getCatalog(): List<YaniAnimeItem> = withContext(Dispatchers.IO) {
        cachedYaniItems?.let { if (it.isNotEmpty()) return@withContext it }
        try {
            val request = Request.Builder()
                .url(CATALOG_URL)
                .header("User-Agent", "AniWerti/1.0")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string().orEmpty()
                val parsed = parseCatalogJson(jsonStr)
                if (parsed.isNotEmpty()) {
                    cachedYaniItems = parsed
                    populateLookupMaps(parsed)
                    return@withContext parsed
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load Yani catalog: ${e.message}")
        }

        // Try secondary catalog url
        try {
            val request = Request.Builder()
                .url(FALLBACK_CATALOG_URL)
                .header("User-Agent", "AniWerti/1.0")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string().orEmpty()
                val parsed = parseCatalogJson(jsonStr)
                if (parsed.isNotEmpty()) {
                    cachedYaniItems = parsed
                    populateLookupMaps(parsed)
                    return@withContext parsed
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load fallback catalog: ${e.message}")
        }

        // Fallback to embedded snapshot of the 24 items in case of offline/network issues
        val fallback = getFallbackYaniCatalog()
        cachedYaniItems = fallback
        populateLookupMaps(fallback)
        fallback
    }

    suspend fun precachePopularPosters(): Unit = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://api.yani.tv/anime?limit=100&offset=0&sort=views",
            "https://api.yani.tv/anime?limit=100&offset=100&sort=views",
            "https://api.yani.tv/anime?year=2026&limit=100",
            "https://api.yani.tv/anime?year=2025&limit=100",
            "https://api.yani.tv/anime?status=ongoing&limit=100",
            "https://api.yani.tv/anime?limit=100&offset=0&sort=rating",
            "https://api.yani.tv/anime?limit=100&offset=0&sort=year"
        )
        val allFetched = mutableListOf<YaniAnimeItem>()
        for (url in urls) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "AniWerti/1.0")
                    .header("Accept", "application/json")
                    .build()
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val jsonStr = resp.body?.string().orEmpty()
                    val parsed = parseCatalogJson(jsonStr)
                    if (parsed.isNotEmpty()) {
                        allFetched.addAll(parsed)
                        mergeIntoLookupMaps(parsed)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error precaching Yani batch $url: ${e.message}")
            }
        }
        if (allFetched.isNotEmpty()) {
            cachedYaniItems = allFetched
        }
    }

    private fun populateLookupMaps(items: List<YaniAnimeItem>) {
        mergeIntoLookupMaps(items)
    }

    private fun mergeIntoLookupMaps(items: List<YaniAnimeItem>) {
        synchronized(this) {
            for (item in items) {
                if (item.shikimoriId > 0) {
                    yaniByShikimoriId[item.shikimoriId] = item
                }
                yaniByAnimeId[item.animeId] = item
                val cleanTitle = normalizeTitle(item.title)
                if (cleanTitle.isNotBlank()) {
                    yaniByTitle[cleanTitle] = item
                }
            }
        }
    }

    private fun normalizeTitle(raw: String): String {
        return raw.lowercase()
            .replace(Regex("[^a-zа-я0-9]"), "")
            .trim()
    }

    fun findYaniItem(shikimoriId: Long?, title: String? = null): YaniAnimeItem? {
        if (shikimoriId != null && shikimoriId > 0) {
            yaniByShikimoriId[shikimoriId]?.let { return it }
        }
        if (title != null) {
            val norm = normalizeTitle(title)
            if (norm.isNotBlank()) {
                yaniByTitle[norm]?.let { return it }
                // Substring match
                for ((k, v) in yaniByTitle) {
                    if (k.contains(norm) || norm.contains(k)) {
                        return v
                    }
                }
            }
        }
        return null
    }

    fun getYaniPoster(shikimoriId: Long?, title: String? = null): String? {
        return findYaniItem(shikimoriId, title)?.posterUrl
    }

    suspend fun lookupYaniPoster(shikimoriId: Long?, title: String?): String? = withContext(Dispatchers.IO) {
        val existing = getYaniPoster(shikimoriId, title)
        if (!existing.isNullOrBlank()) return@withContext existing

        if (title.isNullOrBlank()) return@withContext null

        try {
            val queryClean = title.split(" / ").first().split(" - ").first().trim()
            val encodedQuery = java.net.URLEncoder.encode(queryClean, "UTF-8")
            val searchUrl = "https://api.yani.tv/anime?q=$encodedQuery"
            val req = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "AniWerti/1.0")
                .header("Accept", "application/json")
                .build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val jsonStr = resp.body?.string().orEmpty()
                val parsed = parseCatalogJson(jsonStr)
                if (parsed.isNotEmpty()) {
                    mergeIntoLookupMaps(parsed)
                    return@withContext getYaniPoster(shikimoriId, title) ?: parsed.firstOrNull()?.posterUrl
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed on-demand Yani lookup for '$title': ${e.message}")
        }
        return@withContext null
    }

    fun getYaniDescription(shikimoriId: Long?, title: String? = null): String? {
        return findYaniItem(shikimoriId, title)?.description?.takeIf { it.isNotBlank() }
    }

    private fun parseCatalogJson(jsonStr: String): List<YaniAnimeItem> {
        val list = mutableListOf<YaniAnimeItem>()
        try {
            val root = JSONObject(jsonStr)
            val responseObj = root.opt("response")
            val dataArray = when (responseObj) {
                is org.json.JSONArray -> responseObj
                is JSONObject -> responseObj.optJSONArray("data")
                else -> null
            } ?: return emptyList()

            for (i in 0 until dataArray.length()) {
                val obj = dataArray.getJSONObject(i)
                val animeId = obj.optLong("anime_id", 0L)
                val title = obj.optString("title", "").trim()
                val year = obj.optInt("year", 0).takeIf { it > 0 }
                val desc = obj.optString("description", "").trim()

                val posterObj = obj.optJSONObject("poster")
                var rawPoster = posterObj?.optString("fullsize")
                    ?.takeIf { it.isNotBlank() }
                    ?: posterObj?.optString("big")?.takeIf { it.isNotBlank() }
                    ?: posterObj?.optString("medium").orEmpty()

                if (rawPoster.startsWith("//")) {
                    rawPoster = "https:$rawPoster"
                }

                val remoteIds = obj.optJSONObject("remote_ids")
                val shikiId = remoteIds?.optLong("shikimori_id", 0L) ?: 0L
                val malId = remoteIds?.optLong("myanimelist_id", 0L) ?: 0L
                val kpId = remoteIds?.optLong("kp_id", 0L) ?: 0L

                val ratingObj = obj.optJSONObject("rating")
                val avgRating = ratingObj?.optDouble("average", 0.0) ?: 0.0
                val scoreStr = if (avgRating > 0.0) String.format(java.util.Locale.US, "%.1f", avgRating) else "8.8"

                val typeObj = obj.optJSONObject("type")
                val kind = typeObj?.optString("alias")

                val epObj = obj.optJSONObject("episodes")
                val epCount = epObj?.optInt("count", 0)?.takeIf { it > 0 }
                val epAired = epObj?.optInt("aired", 0)?.takeIf { it > 0 }

                val statusObj = obj.optJSONObject("anime_status")
                val statusAlias = statusObj?.optString("alias", "") ?: ""
                val statusTitle = statusObj?.optString("title", "") ?: ""
                val isAnonsYear = year != null && year > 2026
                val isAnonsDetected = isAnonsYear ||
                    statusAlias.contains("anons", ignoreCase = true) ||
                    statusAlias.contains("announced", ignoreCase = true) ||
                    statusTitle.contains("анонс", ignoreCase = true)

                if (animeId > 0 && title.isNotBlank() && rawPoster.isNotBlank()) {
                    list.add(
                        YaniAnimeItem(
                            animeId = animeId,
                            title = title,
                            year = year,
                            posterUrl = rawPoster,
                            description = desc,
                            shikimoriId = shikiId,
                            myanimelistId = malId,
                            kinopoiskId = kpId,
                            score = if (isAnonsDetected) "" else scoreStr,
                            type = kind,
                            episodesCount = epCount,
                            episodesAired = if (isAnonsDetected) 0 else epAired,
                            isAnons = isAnonsDetected
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Yani catalog json: ${e.message}")
        }
        return list
    }

    /**
     * Converts a Yani item to a ShikimoriAnimeDto for direct display in cards and catalogs
     */
    fun YaniAnimeItem.toShikimoriAnimeDto(): ShikimoriAnimeDto {
        // If shikimoriId is present, use it so player/episodes connect seamlessly.
        // If shikimoriId is 0 (like Avatar), use synthetic negative ID to avoid conflicts.
        val targetId = if (shikimoriId > 0) shikimoriId else animeId
        val img = ShikimoriImageDto(
            original = posterUrl,
            preview = posterUrl,
            x96 = posterUrl,
            x48 = posterUrl
        )
        val normalizedKind = when (type?.lowercase()) {
            "movie", "п/ф" -> "movie"
            "ona" -> "ona"
            "ova" -> "ova"
            "special" -> "special"
            else -> "tv"
        }
        val isAnonsItem = isAnons || (year != null && year > 2026)
        val resolvedStatus = if (isAnonsItem) "anons" else "released"
        val resolvedScore = if (isAnonsItem) null else score.takeIf { it.isNotBlank() }
        val resolvedEpisodes = if (isAnonsItem) episodesCount else (episodesCount ?: 12)
        val resolvedAired = if (isAnonsItem) 0 else (episodesAired ?: episodesCount ?: 12)

        return ShikimoriAnimeDto(
            id = targetId,
            name = title,
            russian = title,
            image = img,
            url = "/animes/$targetId",
            kind = normalizedKind,
            score = resolvedScore,
            status = resolvedStatus,
            episodes = resolvedEpisodes,
            episodesAired = resolvedAired,
            airedOn = year?.toString() ?: "2026",
            releasedOn = year?.toString() ?: "2026"
        )
    }

    /**
     * Converts a Yani item to a ShikimoriAnimeDetailDto
     */
    fun YaniAnimeItem.toShikimoriAnimeDetailDto(): ShikimoriAnimeDetailDto {
        val targetId = if (shikimoriId > 0) shikimoriId else animeId
        val img = ShikimoriImageDto(
            original = posterUrl,
            preview = posterUrl,
            x96 = posterUrl,
            x48 = posterUrl
        )
        val normalizedKind = when (type?.lowercase()) {
            "movie", "п/ф" -> "movie"
            "ona" -> "ona"
            "ova" -> "ova"
            "special" -> "special"
            else -> "tv"
        }
        val isAnonsItem = isAnons || (year != null && year > 2026)
        val resolvedStatus = if (isAnonsItem) "anons" else "released"
        val resolvedScore = if (isAnonsItem) null else score.takeIf { it.isNotBlank() }
        val resolvedEpisodes = if (isAnonsItem) episodesCount else (episodesCount ?: 12)
        val resolvedAired = if (isAnonsItem) 0 else (episodesAired ?: episodesCount ?: 12)

        return ShikimoriAnimeDetailDto(
            id = targetId,
            name = title,
            russian = title,
            image = img,
            kind = normalizedKind,
            score = resolvedScore,
            status = resolvedStatus,
            episodes = resolvedEpisodes,
            episodesAired = resolvedAired,
            airedOn = year?.toString() ?: "2026",
            releasedOn = year?.toString() ?: "2026",
            rating = "r",
            english = listOf(title),
            japanese = listOf(title),
            synonyms = listOf(title),
            duration = 24,
            description = description,
            descriptionHtml = description,
            franchise = null,
            genres = listOf(ShikimoriGenreDto(1L, "anime", "Аниме", "anime", "Anime")),
            studios = emptyList()
        )
    }

    private fun getFallbackYaniCatalog(): List<YaniAnimeItem> {
        return listOf(
            YaniAnimeItem(
                animeId = 10818,
                title = "Аватар: Легенда об Аанге",
                year = 2005,
                posterUrl = "https://static.yani.tv/posters/full/1636669570.jpg",
                description = "Мир разделён на четыре народа: Водные племена, Королевство земли, Воздушные кочевники и Страна огня. В этом мире часть людей из каждого народа обладает способностью к контролю своего элемента, такие люди именуют себя магами воды, земли, воздуха или огня. Только аватар является властелином всех четырёх стихий. Его роль заключается в поддержке баланса между народами и сохранении мирового порядка. Однажды предводитель Страны огня, Хозяин огня, развязал войну со всем миром с целью подчинить себе остальные народы. Ответственность за спасение мира легла на плечи 12-летнего мальчика, мага воздуха по имени Аанг, который узнал, что он аватар. Аанг отправляется в опасное путешествие вместе со своими отважными друзьями из Племени воды, Катарой и её братом Соккой. Ему предстоит овладеть всеми стихиями, пройти через множество испытаний, перебороть свои страхи и сразить Хозяина огня, чтобы остановить войну и восстановить равновесие в мире.",
                shikimoriId = 0,
                myanimelistId = 0,
                kinopoiskId = 401152,
                score = "9.4",
                type = "ona",
                episodesCount = 61,
                episodesAired = 61
            ),
            YaniAnimeItem(
                animeId = 24920,
                title = "Освободите эту ведьму",
                year = 2026,
                posterUrl = "https://static.yani.tv/posters/full/1636885510.jpg",
                description = "Современный человек Чэн Янь перерождается в четвёртом принце Королевства Грейкасл по имени Роланд Уимблдон. В этом мире ведьмы подвергаются гонениям и казням со стороны Церкви, считающей их порождением зла. В день казни Роланд спасает Анну — юную ведьму, способную манипулировать зелёным пламенем высокой температуры. Осознав, что способности ведьм подчиняются законам физики и могут быть применены в производстве, Роланд начинает технологическую революцию в пограничном городе. Он собирает вокруг себя других ведьм, создавая оружие нового поколения, паровые машины и укрепления, чтобы защитить город от демонических тварей и отстоять трон в жестокой борьбе за власть между наследниками.",
                shikimoriId = 62927,
                myanimelistId = 62927,
                kinopoiskId = 0,
                score = "9.3",
                type = "tv",
                episodesCount = 12,
                episodesAired = 12
            ),
            YaniAnimeItem(
                animeId = 1486,
                title = "Унесенные призраками",
                year = 2001,
                posterUrl = "https://static.yani.tv/posters/full/1636666502.jpg",
                description = "Девочка Тихиро вместе с родителями переезжают в свой новый дом. Сбившись с дороги, семья попадает в таинственный заброшенный городок, где на столах накрыты невероятные яства. Родители набрасываются на еду и превращаются в свиней, став пленниками коварной колдуньи Юбабы. Тихиро оказывается в волшебном мире духов и богов, где ей приходится устроиться на работу в купальни Юбабы, взять имя Сэн и преодолеть свои страхи, чтобы спасти родителей и вернуться в мир людей.",
                shikimoriId = 199,
                myanimelistId = 199,
                kinopoiskId = 370,
                score = "9.2",
                type = "movie",
                episodesCount = 1,
                episodesAired = 1
            ),
            YaniAnimeItem(
                animeId = 481,
                title = "Ходячий замок",
                year = 2004,
                posterUrl = "https://static.yani.tv/posters/full/1636891284.jpg",
                description = "18-летнюю Софи злая Ведьма Пустоши заточила в тело старухи. В поисках способа снять проклятие Софи покидает родной город и встречает удивительный ходячий замок таинственного чародея Хаула. Поселившись в замке в качестве уборщицы, Софи знакомится с огненным демоном Кальцифером и юным учеником Марклом, становясь частью невероятных приключений на фоне разгорающейся войны.",
                shikimoriId = 431,
                myanimelistId = 431,
                kinopoiskId = 49684,
                score = "9.2",
                type = "movie",
                episodesCount = 1,
                episodesAired = 1
            ),
            YaniAnimeItem(
                animeId = 1509,
                title = "Вайолет Эвергарден — Фильм",
                year = 2020,
                posterUrl = "https://static.yani.tv/posters/full/1636669301.jpg",
                description = "Вайолет продолжает помогать другим людям писать письма, выражая все чувства клиентов через слова. Работая в почтовой службе CH, она не теряет надежды, что майор Гилберт Бугенвиллея всё ещё жив. Однажды Вайолет получает письмо с загадочным адресом, которое возрождает её веру в то, что самое заветное желание может исполниться.",
                shikimoriId = 37987,
                myanimelistId = 37987,
                kinopoiskId = 1199636,
                score = "9.3",
                type = "movie",
                episodesCount = 1,
                episodesAired = 1
            ),
            YaniAnimeItem(
                animeId = 662,
                title = "Крутой учитель Онидзука",
                year = 1999,
                posterUrl = "https://static.yani.tv/posters/full/1636876707.jpg",
                description = "Бывший член нагоняющей на горожан ужас банды «Онибаку», байкер Эйкити Онидзука, неожиданно решает стать школьным учителем. Ему достаётся самый проблемный класс академии «Сэйрин», ученики которого ненавидят учителей и доводят их до нервного срыва. Своими нестандартными и дерзкими методами Онидзука начинает менять жизни подростков и учить их настоящей дружбе.",
                shikimoriId = 245,
                myanimelistId = 245,
                kinopoiskId = 408596,
                score = "9.2",
                type = "tv",
                episodesCount = 43,
                episodesAired = 43
            ),
            YaniAnimeItem(
                animeId = 468,
                title = "Хантер х Хантер (2011)",
                year = 2011,
                posterUrl = "https://static.yani.tv/posters/full/1636690825.jpg",
                description = "Охотник — это тот, кто путешествует по миру, выполняя опасные задания: от поиска сокровищ в неизведанных землях до поимки опаснейших преступников. Юный Гон Фрикс мечтает стать Охотником, чтобы найти своего отца Джина. В ходе сложнейшего экзамена Гон находит верных друзей: Курапику, Леорио и Киллуа, вместе с которыми вступает в мир опасных сражений и невероятных открытий.",
                shikimoriId = 11061,
                myanimelistId = 11061,
                kinopoiskId = 647602,
                score = "9.1",
                type = "tv",
                episodesCount = 148,
                episodesAired = 148
            ),
            YaniAnimeItem(
                animeId = 15109,
                title = "Необъятный океан 2",
                year = 2025,
                posterUrl = "https://static.yani.tv/posters/full/1636889109.jpg",
                description = "Прошло три месяца с тех пор, как Иори Китахара поселился над магазином снаряжения для дайвинга «Гранд Блю» своего дяди. Его жизнь наполнилась безумными вечеринками дайвинг-клуба «Peek a Boo», бесконечным весельем с друзьями и, конечно же, чарующей красотой подводных глубин. Новый семестр приносит ещё больше уморительных ситуаций, романтических недопониманий и незабываемых погружений в море.",
                shikimoriId = 59986,
                myanimelistId = 59986,
                kinopoiskId = 1112986,
                score = "9.1",
                type = "tv",
                episodesCount = 12,
                episodesAired = 12
            ),
            YaniAnimeItem(
                animeId = 610,
                title = "Код Гиасс: Восстание Лелуша 2",
                year = 2008,
                posterUrl = "https://static.yani.tv/posters/full/1636691210.jpg",
                description = "Прошел год после неудачного Чёрного Восстания. Лелуш Ламперуж живёт мирной жизнью студента академии Эшфорд со стёртыми воспоминаниями. Однако после внезапного нападения остатков Ордена Чёрных Рыцарей память возвращается к нему. Зеро возрождается вновь, чтобы завершить начатое и сокрушить Священную Британскую Империю раз и навсегда.",
                shikimoriId = 2904,
                myanimelistId = 2904,
                kinopoiskId = 408674,
                score = "9.1",
                type = "tv",
                episodesCount = 25,
                episodesAired = 25
            ),
            YaniAnimeItem(
                animeId = 1274,
                title = "Сага о Винланде",
                year = 2019,
                posterUrl = "https://static.yani.tv/posters/full/1636691037.jpg",
                description = "На протяжении тысячи лет викинги буйствовали в северных водах, заслужив себе звание самых жестоких и бесстрашных воинов. Юный Торфинн, сын величайшего воина Торса, становится свидетелем гибели отца от руки наёмника Аскеладда. Одержимый жаждой мести, Торфинн вступает в дружину убийцы своего отца, чтобы заслужить право на честную дуэль и отомстить за гибель родных.",
                shikimoriId = 37521,
                myanimelistId = 37521,
                kinopoiskId = 1274280,
                score = "9.1",
                type = "tv",
                episodesCount = 24,
                episodesAired = 24
            ),
            YaniAnimeItem(
                animeId = 12094,
                title = "Монолог фармацевта 2",
                year = 2025,
                posterUrl = "https://static.yani.tv/posters/full/1636792676.jpg",
                description = "Ещё недавно она была простой служанкой, а теперь стала одной из приближённых самого императора! Эксцентричная травница Маомао возвращается во Внутренний двор, где политические интриги, тайные заговоры и смертоносные яды становятся ещё опаснее. Вместе с евнухом Дзиньси ей предстоит распутать сложнейшие загадки императорского дворца.",
                shikimoriId = 58514,
                myanimelistId = 58514,
                kinopoiskId = 5258557,
                score = "9.1",
                type = "tv",
                episodesCount = 24,
                episodesAired = 24
            ),
            YaniAnimeItem(
                animeId = 463,
                title = "Гуррен-Лаганн",
                year = 2007,
                posterUrl = "https://static.yani.tv/posters/full/1636793979.jpg",
                description = "Симон и Камина живут в глубокой подземной деревне Дзиха. Камина верит, что на поверхности существует целый мир, и жаждет вырваться наружу. Однажды Симон находит странный бур и загадочное миниатюрное лицо — робота Лаганна. Пробив потолок деревни, друзья вырываются на залитую солнцем поверхность, где разворачивается эпохальная битва за свободу человечества против Спирального Короля.",
                shikimoriId = 2001,
                myanimelistId = 2001,
                kinopoiskId = 452973,
                score = "9.1",
                type = "tv",
                episodesCount = 27,
                episodesAired = 27
            ),
            YaniAnimeItem(
                animeId = 15269,
                title = "Звёздное дитя 3",
                year = 2026,
                posterUrl = "https://static.yani.tv/posters/full/1636881822.jpg",
                description = "Продолжение захватывающей драматической истории о темной изнанке японского шоу-бизнеса. Аквамарин Хосино продолжает своё расследование с целью найти биологического отца и отомстить за смерть матери, легендарного айдола Ай. Тем временем Руби и обновлённая группа B-Komachi стремятся покорить главную сцену страны.",
                shikimoriId = 60058,
                myanimelistId = 60058,
                kinopoiskId = 5308105,
                score = "9.1",
                type = "tv",
                episodesCount = 12,
                episodesAired = 12
            ),
            YaniAnimeItem(
                animeId = 70,
                title = "Берсерк",
                year = 1997,
                posterUrl = "https://static.yani.tv/posters/full/1636671122.jpg",
                description = "Гатс, могучий мечник с колоссальным мечом, странствует по истерзанному бесконечными войнами королевству Мидленд. Встретив Гриффита, амбициозного лидера отряда наёмников «Банда Ястреба», Гатс находит своё место среди боевых товарищей. Но неумолимая судьба и темные силы ведут отряд к роковому Затмению, которое изменит их мир навсегда.",
                shikimoriId = 33,
                myanimelistId = 33,
                kinopoiskId = 257376,
                score = "9.1",
                type = "tv",
                episodesCount = 25,
                episodesAired = 25
            ),
            YaniAnimeItem(
                animeId = 2230,
                title = "Необъятный океан",
                year = 2018,
                posterUrl = "https://static.yani.tv/posters/full/1636690961.jpg",
                description = "Поступив в университет прибрежного городка Идзу, Иори Китахара надеялся на красивую студенческую жизнь с прекрасными девушками. Но вместо этого он попадает в дайвинг-клуб «Peek a Boo», полный мускулистых парней, неукротимых алкогольных попоек и безумных выходок, где ему предстоит полюбить океан и научиться плавать.",
                shikimoriId = 37105,
                myanimelistId = 37105,
                kinopoiskId = 1112986,
                score = "9.1",
                type = "tv",
                episodesCount = 12,
                episodesAired = 12
            ),
            YaniAnimeItem(
                animeId = 1386,
                title = "Стальной Алхимик: Братство",
                year = 2009,
                posterUrl = "https://static.yani.tv/posters/full/1636691537.jpg",
                description = "Нарушив главное табу алхимии — попытку человеческой трансформации с целью вернуть погибшую мать — братья Эдвард и Альфонс Элрики платят страшную цену. Эдвард теряет руку и ногу, а Альфонс — всё своё тело, его душа оказывается привязана к стальным доспехам. Став государственным алхимиком, Эдвард вместе с братом отправляется на поиски легендарного Философского камня.",
                shikimoriId = 5114,
                myanimelistId = 5114,
                kinopoiskId = 452838,
                score = "9.1",
                type = "tv",
                episodesCount = 64,
                episodesAired = 64
            ),
            YaniAnimeItem(
                animeId = 10531,
                title = "Монолог фармацевта",
                year = 2023,
                posterUrl = "https://static.yani.tv/posters/full/1636668909.jpg",
                description = "Маомао вела спокойную жизнь травницы в квартале красных фонарей, пока её не похитили и не продали служанкой во Внутренний двор императорского дворца. Когда наследники императора заболевают таинственной хворью, Маомао благодаря своим глубоким знаниям ядов и медицины тайком спасает их жизни, привлекая внимание влиятельного евнуха Дзиньси.",
                shikimoriId = 54492,
                myanimelistId = 54492,
                kinopoiskId = 5258557,
                score = "9.0",
                type = "tv",
                episodesCount = 24,
                episodesAired = 24
            ),
            YaniAnimeItem(
                animeId = 608,
                title = "Код Гиасс: Восстание Лелуша",
                year = 2006,
                posterUrl = "https://static.yani.tv/posters/full/1636691507.jpg",
                description = "Япония захвачена Священной Британской Империей и лишена имени, став «Зоной 11». Опальный британский принц Лелуш случайно оказывается в эпицентре стычки террористов и военных и встречает загадочную бессмертную девушку C.C., которая наделяет его абсолютной силой подчинения — Гиассом. Лелуш надевает маску Зеро и начинает грандиозную войну за освобождение страны.",
                shikimoriId = 1575,
                myanimelistId = 1575,
                kinopoiskId = 408674,
                score = "9.0",
                type = "tv",
                episodesCount = 25,
                episodesAired = 25
            ),
            YaniAnimeItem(
                animeId = 2020,
                title = "Форма голоса",
                year = 2016,
                posterUrl = "https://static.yani.tv/posters/full/1531695789.jpg",
                description = "В начальной школе Сёя Исида вместе с одноклассниками травил глухую девочку Сёко Нисимию, пока та не была вынуждена перевестись в другую школу. Вся вина за травлю пала на Исиду, и он сам стал изгоем. Годы спустя, терзаемый чувством вины, повзрослевший Сёя решает искупить свои прошлые ошибки, выучить язык жестов и попросить прощения у Сёко.",
                shikimoriId = 28851,
                myanimelistId = 28851,
                kinopoiskId = 963343,
                score = "9.0",
                type = "movie",
                episodesCount = 1,
                episodesAired = 1
            ),
            YaniAnimeItem(
                animeId = 11541,
                title = "Человек-бензопила: История Резе",
                year = 2025,
                posterUrl = "https://static.yani.tv/posters/full/1636888945.jpg",
                description = "Полнометражное продолжение приключений Дэндзи. В дождливый день в телефонной будке Дэндзи встречает очаровательную девушку Резе, работающую в местном кафе. Дэндзи влюбляется, надеясь на простое человеческое счастье, однако истинные мотивы Резе скрывают смертельную опасность для Человека-бензопилы.",
                shikimoriId = 57555,
                myanimelistId = 57555,
                kinopoiskId = 5430477,
                score = "9.0",
                type = "movie",
                episodesCount = 1,
                episodesAired = 1
            ),
            YaniAnimeItem(
                animeId = 10661,
                title = "Провожающая в последний путь Фрирен",
                year = 2023,
                posterUrl = "https://static.yani.tv/posters/full/1636668991.jpg",
                description = "Отряд героев под предводительством Химмеля победил Короля демонов и принёс мир. Для эльфийки-волшебницы Фрирен десятилетний поход был лишь мгновением в её тысячелетней жизни. Спустя полвека Химмель умирает от старости, и Фрирен с болью осознаёт, как мало она знала о друге. Она отправляется в новое путешествие по землям, чтобы лучше понять человеческое сердце.",
                shikimoriId = 52991,
                myanimelistId = 52991,
                kinopoiskId = 5401195,
                score = "9.0",
                type = "tv",
                episodesCount = 28,
                episodesAired = 28
            ),
            YaniAnimeItem(
                animeId = 13270,
                title = "Клинок, рассекающий демонов: Бесконечный замок",
                year = 2025,
                posterUrl = "https://static.yani.tv/posters/full/1636807160.jpg",
                description = "Финальная битва человечества против прародителя демонов Мудзана Кибуцудзи! После жестоких тренировок со Столпами Тандзиро Камадо и все истребители демонов оказываются втянуты в пространственный лабиринт Бесконечного замка, где им предстоит сойтись в смертельной схватке с сильнейшими Высшими Лунами.",
                shikimoriId = 59192,
                myanimelistId = 59192,
                kinopoiskId = 7436042,
                score = "9.1",
                type = "movie",
                episodesCount = 1,
                episodesAired = 1
            ),
            YaniAnimeItem(
                animeId = 1620,
                title = "Врата Штейна",
                year = 2011,
                posterUrl = "https://static.yani.tv/posters/full/1636691327.jpg",
                description = "В районе Акихабара самопровозглашенный «безумный ученый» Ринтаро Окабэ случайно модифицирует микроволновку так, что она способна отправлять текстовые сообщения в прошлое. Эксперименты с временными линиями привлекают внимание тайной организации SERN, и Окабэ оказывается в кошмарной паутине временных петель, пытаясь спасти своих друзей.",
                shikimoriId = 9253,
                myanimelistId = 9253,
                kinopoiskId = 586251,
                score = "9.0",
                type = "tv",
                episodesCount = 24,
                episodesAired = 24
            ),
            YaniAnimeItem(
                animeId = 19654,
                title = "Доктор Стоун: Научное будущее. Часть 2",
                year = 2025,
                posterUrl = "https://static.yani.tv/posters/full/1636857037.jpg",
                description = "Кульминация научно-приключенческой саги! Сэнку Исигами и Царство Науки завершают постройку космического корабля для полёта на Луну, чтобы встретиться лицом к лицу с Почемучкой — источником зелёного луча, обратившего человечество в камень тысячи лет назад.",
                shikimoriId = 61322,
                myanimelistId = 61322,
                kinopoiskId = 1249511,
                score = "9.1",
                type = "tv",
                episodesCount = 12,
                episodesAired = 12
            )
        )
    }
}
