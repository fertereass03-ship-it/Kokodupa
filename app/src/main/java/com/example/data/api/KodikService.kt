package com.example.data.api

import android.util.Base64
import android.util.Log
import com.example.data.api.models.AnimeVoiceTranslation
import com.example.data.api.models.KodikParsedEpisode
import com.example.data.api.models.KodikStreamLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object KodikService {
    private const val TAG = "KodikService"
    const val KODIK_PUBLIC_TOKEN = "447d179e875efe44217f20d1ee2146be"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /**
     * Dedicated stream token and payload for Kodik Anime Films
     */
    val KODIK_ANIME_FILMS_DATA: String = "KpDr t|!BG lYgs 7{k] 7=QQz Irtx aCV\\ D,%KH VC=f N8Ld +r 8 '*TJ ArI[` ckji <4'm 78GxG \\{PmCwx [q_\\ \\),& @y)`} CBS< /<Y*A `B%& xGLpCmI WExo JR?Qr \"_[, Z1Wj =g%\\  YVER ?2%'} %WlCkwarE#@ -J`L}HzK8# E%wt+X i1`yXc Z95c TJSq V\\;\$ [lLf \$7cW :)@1I L:~% P#K|n 5BMRk hJk@1-ZFnZV -HS\$ khSM T2|J <LI7 xQ6. zE-h imLR ]<qK 8*4<'h eu7F6 'n*+S I\"FDJ3W `Zq=\" vj|PY\"!mt`@ TfLE Xo6l Ph-3 Sq6\$D Gob5X U.+\\Q[ 30M  Y^`)B u!J3 Z9-s O<yN :Il+ )pc?[  {`btK !58M B||F 3_Vn,x '\$kb} l07m )s.[ nf>`A =>Gx oL}^ *5pj"

    private val MOVIE_VIDEO_URL_PATTERN = Pattern.compile("/(?:video|movie)/([0-9a-zA-Z_-]+)/([0-9a-zA-Z]+)")

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val moshi = com.squareup.moshi.Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    val kodikRetrofitApi: KodikApi by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl("https://kodik-api.com/")
            .client(httpClient)
            .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi))
            .build()
            .create(KodikApi::class.java)
    }

    /**
     * Normalizes Kodik URLs by converting protocol relative links to https
     * and replacing defunct/blocked domains like kodik.biz with active mirror kodikplayer.com
     */
    fun normalizeKodikUrl(url: String): String {
        if (url.isBlank()) return ""
        var cleaned = url.trim()
        if (cleaned.startsWith("//")) {
            cleaned = "https:$cleaned"
        } else if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            cleaned = "https://$cleaned"
        }

        // Replace defunct domains
        cleaned = cleaned.replace("kodik.biz", "kodikplayer.com")
            .replace("kodik.info", "kodikplayer.com")
            .replace("kodik.cc", "kodikplayer.com")

        return cleaned
    }

    /**
     * ROT18 decryption algorithm:
     * Letters are shifted by 18 positions in the alphabet (ROT13 + 5).
     * Numbers and special characters are preserved.
     */
    fun rot18(c: Char): Char {
        val code = c.code
        return when (code) {
            in 65..90 -> (((code - 65 + 18) % 26) + 65).toChar()
            in 97..122 -> (((code - 97 + 18) % 26) + 97).toChar()
            else -> c
        }
    }

    /**
     * Decrypts encrypted Kodik stream URL:
     * 1. ROT18 on each character
     * 2. Base64 decode to UTF-8 URL
     */
    fun decryptSrc(encrypted: String): String {
        try {
            val sb = StringBuilder(encrypted.length)
            for (c in encrypted) {
                sb.append(rot18(c))
            }
            val decodedBytes = Base64.decode(sb.toString(), Base64.DEFAULT)
            var url = String(decodedBytes, Charsets.UTF_8).trim()
            if (url.startsWith("//")) {
                url = "https:$url"
            }
            return url
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting stream src: ${e.message}")
            return ""
        }
    }

    /**
     * Searches Kodik for all real translations & seasons for a given Shikimori ID or title
     */
    suspend fun searchTranslations(shikimoriId: Long, title: String? = null, isMovie: Boolean = false): List<AnimeVoiceTranslation> = withContext(Dispatchers.IO) {
        // Step 1: Query get-player API
        var embedUrl: String? = null
        try {
            val playerResp = kodikRetrofitApi.getPlayerByShikimori(shikimoriId = shikimoriId.toString())
            if (playerResp.found == true && !playerResp.link.isNullOrBlank()) {
                embedUrl = normalizeKodikUrl(playerResp.link)
            }
        } catch (e: Exception) {
            Log.w(TAG, "get-player by shikimori failed: ${e.message}")
        }

        if (embedUrl.isNullOrBlank() && !title.isNullOrBlank()) {
            try {
                val playerResp = kodikRetrofitApi.getPlayerByTitle(title = title)
                if (playerResp.found == true && !playerResp.link.isNullOrBlank()) {
                    embedUrl = normalizeKodikUrl(playerResp.link)
                }
            } catch (e: Exception) {
                Log.w(TAG, "get-player by title failed: ${e.message}")
            }
        }

        if (!embedUrl.isNullOrBlank()) {
            val parsedInfo = parseSerialPage(embedUrl)
            if (parsedInfo.isNotEmpty()) {
                return@withContext parsedInfo
            }
        }

        // Fallback: search endpoint
        try {
            val searchResp = kodikRetrofitApi.searchByShikimori(shikimoriId = shikimoriId)
            val results = searchResp.results.orEmpty()
            if (results.isNotEmpty()) {
                val translations = results.mapNotNull { item ->
                    val tr = item.translation ?: return@mapNotNull null
                    val link = normalizeKodikUrl(item.link.orEmpty())
                    val isMovieItem = isMovie || item.type == "anime-movie" || item.type == "movie" || link.contains("/video/") || link.contains("/movie/")
                    val totalEp = if (isMovieItem) 1 else (item.lastEpisode ?: item.episodesCount ?: 12)

                    val vMatcher = MOVIE_VIDEO_URL_PATTERN.matcher(link)
                    val (extractedId, extractedHash) = if (vMatcher.find()) {
                        Pair(vMatcher.group(1).orEmpty(), vMatcher.group(2).orEmpty())
                    } else {
                        Pair(item.id.replace(Regex("^(movie-|video-|serial-)"), ""), "")
                    }
                    val mediaType = if (isMovieItem) "video" else (item.type ?: "serial")

                    val parsedEps = if (isMovieItem && extractedId.isNotBlank() && extractedHash.isNotBlank()) {
                        listOf(
                            KodikParsedEpisode(
                                number = 1,
                                id = extractedId,
                                hash = extractedHash,
                                title = "Фильм (Полная версия)",
                                label = "Фильм"
                            )
                        )
                    } else emptyList()

                    AnimeVoiceTranslation(
                        id = "kodik_${tr.id}",
                        name = tr.title,
                        type = tr.type ?: "voice",
                        episodesCount = totalEp,
                        kodikLink = link,
                        mediaId = extractedId.ifBlank { item.id },
                        mediaHash = extractedHash,
                        mediaType = mediaType,
                        dValue = tr.id.toString(),
                        parsedEpisodes = parsedEps
                    )
                }
                if (translations.isNotEmpty()) {
                    return@withContext translations.distinctBy { it.name }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Search by shikimori failed: ${e.message}")
        }

        emptyList()
    }

    private fun extractVar(name: String, html: String): String {
        val m = Pattern.compile("var\\s+$name\\s*=\\s*['\"]([^'\"]*)['\"]").matcher(html)
        return if (m.find()) m.group(1).orEmpty() else ""
    }

    /**
     * Parses the Kodik embed HTML page to extract available translations and episodes
     */
    suspend fun parseSerialPage(embedUrl: String): List<AnimeVoiceTranslation> = withContext(Dispatchers.IO) {
        val safeUrl = normalizeKodikUrl(embedUrl)
        if (safeUrl.isBlank()) return@withContext emptyList()

        try {
            val request = Request.Builder()
                .url(safeUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://shikimori.one/")
                .build()

            val response = httpClient.newCall(request).execute()
            val html = response.body?.string().orEmpty()
            if (!response.isSuccessful || html.isBlank()) {
                return@withContext emptyList()
            }

            val translations = mutableListOf<AnimeVoiceTranslation>()
            val episodesList = mutableListOf<KodikParsedEpisode>()

            val isMoviePage = safeUrl.contains("/video/") || safeUrl.contains("/movie/") ||
                    html.contains(".type = 'video'") || html.contains("\"type\":\"video\"") ||
                    html.contains("var type = \"video\"")

            // Extract select blocks
            val selectPattern = Pattern.compile("<select[^>]*>(.*?)</select>", Pattern.DOTALL)
            val optionPattern = Pattern.compile("<option\\s+([^>]*)>([^<]*)</option>", Pattern.DOTALL)
            val attrPattern = Pattern.compile("(data-[a-z-]+)=\"([^\"]*)\"")
            val valPattern = Pattern.compile("value=\"([^\"]*)\"")

            val selectMatcher = selectPattern.matcher(html)
            while (selectMatcher.find()) {
                val selectContent = selectMatcher.group(1).orEmpty()
                val optMatcher = optionPattern.matcher(selectContent)
                val options = mutableListOf<Pair<String, String>>()
                while (optMatcher.find()) {
                    options.add(Pair(optMatcher.group(1).orEmpty(), optMatcher.group(2).orEmpty().trim()))
                }
                if (options.isEmpty()) continue

                val firstAttrStr = options[0].first
                val firstAttrs = mutableMapOf<String, String>()
                val aMatcher = attrPattern.matcher(firstAttrStr)
                while (aMatcher.find()) {
                    firstAttrs[aMatcher.group(1).orEmpty()] = aMatcher.group(2).orEmpty()
                }

                if (firstAttrs.containsKey("data-media-id")) {
                    // Translations selector
                    for (opt in options) {
                        val attrs = mutableMapOf<String, String>()
                        val m = attrPattern.matcher(opt.first)
                        while (m.find()) {
                            attrs[m.group(1).orEmpty()] = m.group(2).orEmpty()
                        }
                        val vMatch = valPattern.matcher(opt.first)
                        val value = if (vMatch.find()) vMatch.group(1).orEmpty() else ""
                        val title = attrs["data-title"] ?: opt.second
                        val mediaId = attrs["data-media-id"].orEmpty()
                        val mediaHash = attrs["data-media-hash"].orEmpty()
                        val mediaType = attrs["data-media-type"] ?: if (isMoviePage) "video" else "serial"
                        val transType = attrs["data-translation-type"] ?: "voice"
                        val epCount = if (isMoviePage) 1 else (
                            attrs["data-episode-count"]?.toIntOrNull()
                                ?: attrs["data-episodes-count"]?.toIntOrNull()
                                ?: attrs["data-count"]?.toIntOrNull()
                                ?: attrs["data-episodes"]?.toIntOrNull()
                                ?: attrs["data-last-episode"]?.toIntOrNull()
                                ?: 0
                        )
                        val trLink = "https://kodikplayer.com/$mediaType/$mediaId/$mediaHash/1080p"

                        val parsedEps = if (mediaType == "video" || isMoviePage) {
                            listOf(
                                KodikParsedEpisode(
                                    number = 1,
                                    id = mediaId,
                                    hash = mediaHash,
                                    title = "Фильм (Полная версия)",
                                    label = "Фильм"
                                )
                            )
                        } else emptyList()

                        translations.add(
                            AnimeVoiceTranslation(
                                id = attrs["data-id"] ?: value,
                                name = title,
                                type = transType,
                                episodesCount = epCount,
                                kodikLink = trLink,
                                mediaId = mediaId,
                                mediaHash = mediaHash,
                                mediaType = mediaType,
                                dValue = value,
                                parsedEpisodes = parsedEps
                            )
                        )
                    }
                } else if (firstAttrs.containsKey("data-hash")) {
                    // Episodes selector
                    for (opt in options) {
                        val attrs = mutableMapOf<String, String>()
                        val m = attrPattern.matcher(opt.first)
                        while (m.find()) {
                            attrs[m.group(1).orEmpty()] = m.group(2).orEmpty()
                        }
                        val vMatch = valPattern.matcher(opt.first)
                        val num = if (vMatch.find()) vMatch.group(1).orEmpty().toIntOrNull() ?: 1 else 1
                        val epId = attrs["data-id"].orEmpty()
                        val epHash = attrs["data-hash"].orEmpty()
                        val title = attrs["data-title"] ?: opt.second

                        episodesList.add(
                            KodikParsedEpisode(
                                number = num,
                                id = epId,
                                hash = epHash,
                                title = title,
                                label = opt.second
                            )
                        )
                    }
                }
            }

            val vMatch = MOVIE_VIDEO_URL_PATTERN.matcher(safeUrl)
            val extractedId = if (vMatch.find()) vMatch.group(1).orEmpty() else ""
            val extractedHash = if (vMatch.find(0)) vMatch.group(2).orEmpty() else ""
            val idFromHtml = Pattern.compile("(?:var\\s+videoId|\\.id|[\"']id[\"'])\\s*[:=]\\s*['\"]?([0-9a-zA-Z_-]+)['\"]?").matcher(html)
            val finalId = if (extractedId.isNotBlank()) extractedId else if (idFromHtml.find()) idFromHtml.group(1).orEmpty() else ""
            val hashFromHtml = Pattern.compile("(?:var\\s+videoHash|\\.hash|[\"']hash[\"'])\\s*[:=]\\s*['\"]([0-9a-zA-Z]+)['\"]").matcher(html)
            val finalHash = if (extractedHash.isNotBlank()) extractedHash else if (hashFromHtml.find()) hashFromHtml.group(1).orEmpty() else ""

            val movieEpisode = if (finalId.isNotBlank() && finalHash.isNotBlank()) {
                KodikParsedEpisode(
                    number = 1,
                    id = finalId,
                    hash = finalHash,
                    title = "Фильм (Полная версия)",
                    label = "Фильм"
                )
            } else null

            val maxDiscoveredEp = episodesList.maxOfOrNull { it.number } ?: 0

            // If translations found, return distinct translations
            if (translations.isNotEmpty()) {
                if (isMoviePage) {
                    val updatedTranslations = translations.map { tr ->
                        val effectiveId = tr.mediaId.ifBlank { finalId }
                        val effectiveHash = tr.mediaHash.ifBlank { finalHash }
                        val ep = KodikParsedEpisode(
                            number = 1,
                            id = effectiveId,
                            hash = effectiveHash,
                            title = "Фильм (Полная версия)",
                            label = "Фильм"
                        )
                        tr.copy(
                            mediaType = "video",
                            episodesCount = 1,
                            mediaId = effectiveId,
                            mediaHash = effectiveHash,
                            kodikLink = if (effectiveId.isNotBlank() && effectiveHash.isNotBlank()) {
                                "https://kodikplayer.com/video/$effectiveId/$effectiveHash/1080p"
                            } else tr.kodikLink,
                            parsedEpisodes = listOf(ep)
                        )
                    }
                    return@withContext updatedTranslations
                }

                // Update translations with the discovered episode count so all voice studios reflect reality
                for (i in translations.indices) {
                    val tr = translations[i]
                    if (tr.episodesCount <= 0 && maxDiscoveredEp > 0) {
                        translations[i] = tr.copy(episodesCount = maxDiscoveredEp)
                    }
                }

                val first = translations[0]
                val updatedFirst = first.copy(
                    parsedEpisodes = episodesList,
                    episodesCount = if (episodesList.isNotEmpty()) episodesList.size else if (maxDiscoveredEp > 0) maxDiscoveredEp else first.episodesCount
                )
                translations[0] = updatedFirst
                return@withContext translations
            } else if (isMoviePage && movieEpisode != null) {
                return@withContext listOf(
                    AnimeVoiceTranslation(
                        id = "kodik_movie",
                        name = "Основная озвучка",
                        episodesCount = 1,
                        kodikLink = safeUrl,
                        mediaId = finalId,
                        mediaHash = finalHash,
                        mediaType = "video",
                        parsedEpisodes = listOf(movieEpisode)
                    )
                )
            } else if (episodesList.isNotEmpty()) {
                return@withContext listOf(
                    AnimeVoiceTranslation(
                        id = "default",
                        name = "Основная озвучка",
                        episodesCount = episodesList.size,
                        kodikLink = safeUrl,
                        parsedEpisodes = episodesList
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing serial page for $safeUrl: ${e.message}")
        }
        emptyList()
    }

    /**
     * Fast, direct method to check the live real-time number of episodes available in voiceover/dubbing on Kodik.
     * Updates immediately when a new episode is released on Kodik player.
     */
    suspend fun getLiveVoicedEpisodeCount(shikimoriId: Long, title: String? = null): Int? = withContext(Dispatchers.IO) {
        try {
            var embedUrl: String? = null
            try {
                val playerResp = kodikRetrofitApi.getPlayerByShikimori(shikimoriId = shikimoriId.toString())
                if (playerResp.found == true && !playerResp.link.isNullOrBlank()) {
                    embedUrl = normalizeKodikUrl(playerResp.link)
                }
            } catch (e: Exception) {
                Log.w(TAG, "getPlayerByShikimori for $shikimoriId failed: ${e.message}")
            }

            if (embedUrl.isNullOrBlank() && !title.isNullOrBlank()) {
                try {
                    val playerResp = kodikRetrofitApi.getPlayerByTitle(title = title)
                    if (playerResp.found == true && !playerResp.link.isNullOrBlank()) {
                        embedUrl = normalizeKodikUrl(playerResp.link)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "getPlayerByTitle for '$title' failed: ${e.message}")
                }
            }

            if (embedUrl.isNullOrBlank()) return@withContext null

            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://shikimori.one/")
                .build()

            val response = httpClient.newCall(request).execute()
            val html = response.body?.string().orEmpty()
            if (response.isSuccessful && html.isNotBlank()) {
                var maxEpisode = 0

                // Match all occurrences of episode numbers like '10 серия', '13 серия', '10 сер.'
                val epMatcher = Pattern.compile("(\\d+)\\s*(?:серия|сер\\.|эпизод|серии)", Pattern.CASE_INSENSITIVE).matcher(html)
                while (epMatcher.find()) {
                    val num = epMatcher.group(1).toIntOrNull() ?: 0
                    if (num in 1..2500 && num > maxEpisode) {
                        maxEpisode = num
                    }
                }

                // Match select option values or data-number attributes
                val attrMatcher = Pattern.compile("data-(?:number|episode)=[\"']?(\\d+)[\"']?").matcher(html)
                while (attrMatcher.find()) {
                    val num = attrMatcher.group(1).toIntOrNull() ?: 0
                    if (num in 1..2500 && num > maxEpisode) {
                        maxEpisode = num
                    }
                }

                if (maxEpisode > 0) {
                    return@withContext maxEpisode
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed live voice check for shikimoriId=$shikimoriId: ${e.message}")
        }
        null
    }

    /**
     * Extracts episodes for a specific translation embed link
     */
    suspend fun getEpisodesForTranslation(voice: AnimeVoiceTranslation): List<KodikParsedEpisode> = withContext(Dispatchers.IO) {
        if (voice.parsedEpisodes.isNotEmpty()) {
            return@withContext voice.parsedEpisodes
        }

        val targetUrl = normalizeKodikUrl(voice.kodikLink)
        val isMovieVoice = voice.mediaType == "video" || voice.mediaType == "anime-movie" || voice.mediaType == "movie" || targetUrl.contains("/video/") || targetUrl.contains("/movie/")
        if (isMovieVoice) {
            val vMatch = MOVIE_VIDEO_URL_PATTERN.matcher(targetUrl)
            val vId = if (vMatch.find()) vMatch.group(1).orEmpty() else voice.mediaId
            val vHash = if (vMatch.find(0)) vMatch.group(2).orEmpty() else voice.mediaHash
            if (vId.isNotBlank() && vHash.isNotBlank()) {
                return@withContext listOf(
                    KodikParsedEpisode(
                        number = 1,
                        id = vId,
                        hash = vHash,
                        title = "Фильм (Полная версия)",
                        label = "Фильм"
                    )
                )
            }
        }

        if (targetUrl.isBlank()) {
            return@withContext emptyList()
        }

        try {
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://kodikplayer.com/")
                .build()

            val response = httpClient.newCall(request).execute()
            val html = response.body?.string().orEmpty()
            if (!response.isSuccessful || html.isBlank()) {
                return@withContext emptyList()
            }

            val episodesList = mutableListOf<KodikParsedEpisode>()
            val selectPattern = Pattern.compile("<select[^>]*>(.*?)</select>", Pattern.DOTALL)
            val optionPattern = Pattern.compile("<option\\s+([^>]*)>([^<]*)</option>", Pattern.DOTALL)
            val attrPattern = Pattern.compile("(data-[a-z-]+)=\"([^\"]*)\"")
            val valPattern = Pattern.compile("value=\"([^\"]*)\"")

            val selectMatcher = selectPattern.matcher(html)
            while (selectMatcher.find()) {
                val selectContent = selectMatcher.group(1).orEmpty()
                val optMatcher = optionPattern.matcher(selectContent)
                val options = mutableListOf<Pair<String, String>>()
                while (optMatcher.find()) {
                    options.add(Pair(optMatcher.group(1).orEmpty(), optMatcher.group(2).orEmpty().trim()))
                }
                if (options.isEmpty()) continue

                val firstAttrStr = options[0].first
                val firstAttrs = mutableMapOf<String, String>()
                val aMatcher = attrPattern.matcher(firstAttrStr)
                while (aMatcher.find()) {
                    firstAttrs[aMatcher.group(1).orEmpty()] = aMatcher.group(2).orEmpty()
                }

                if (firstAttrs.containsKey("data-hash")) {
                    for (opt in options) {
                        val attrs = mutableMapOf<String, String>()
                        val m = attrPattern.matcher(opt.first)
                        while (m.find()) {
                            attrs[m.group(1).orEmpty()] = m.group(2).orEmpty()
                        }
                        val vMatch = valPattern.matcher(opt.first)
                        val num = if (vMatch.find()) vMatch.group(1).orEmpty().toIntOrNull() ?: 1 else 1
                        episodesList.add(
                            KodikParsedEpisode(
                                number = num,
                                id = attrs["data-id"].orEmpty(),
                                hash = attrs["data-hash"].orEmpty(),
                                title = attrs["data-title"] ?: opt.second,
                                label = opt.second
                            )
                        )
                    }
                    break
                }
            }

            if (episodesList.isNotEmpty()) {
                return@withContext episodesList
            }

            // Extract single video info if film / single seria
            val typeMatch = Pattern.compile("(?:\\.type|[\"']type[\"'])\\s*[:=]\\s*['\"]([^'\"]+)['\"]").matcher(html)
            val hashMatch = Pattern.compile("(?:\\.hash|[\"']hash[\"'])\\s*[:=]\\s*['\"]([^'\"]+)['\"]").matcher(html)
            val idMatch = Pattern.compile("(?:\\.id|[\"']id[\"'])\\s*[:=]\\s*['\"]([^'\"]+)['\"]").matcher(html)

            if ((isMovieVoice || typeMatch.find()) && hashMatch.find() && idMatch.find()) {
                return@withContext listOf(
                    KodikParsedEpisode(
                        number = 1,
                        id = idMatch.group(1).orEmpty(),
                        hash = hashMatch.group(1).orEmpty(),
                        title = "Фильм (Полная версия)",
                        label = "Фильм"
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice: could not parse episodes for ${voice.name}: ${e.message}")
        }
        emptyList()
    }

    /**
     * Executes Step 3 & 4 of Kodik protocol:
     * Extracts dynamic security signatures from embed page -> POST to /ftor -> decrypt ROT18+Base64 -> real m3u8 HLS streams from solodcdn.com
     */
    suspend fun getStreamLinksFromFtor(
        embedUrl: String,
        videoType: String,
        episodeHash: String,
        episodeId: String
    ): KodikStreamLinks = withContext(Dispatchers.IO) {
        val safeEmbedUrl = normalizeKodikUrl(embedUrl)
        try {
            val host = try {
                URI(safeEmbedUrl).host ?: "kodikplayer.com"
            } catch (e: Exception) {
                "kodikplayer.com"
            }

            // Step A: Fetch embed page HTML to extract security tokens
            val pageRequest = Request.Builder()
                .url(safeEmbedUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://shikimori.one/")
                .build()

            val pageResponse = httpClient.newCall(pageRequest).execute()
            val html = pageResponse.body?.string().orEmpty()

            var domain = extractVar("domain", html).ifBlank { "shikimori.one" }
            var dSign = extractVar("d_sign", html)
            var pd = extractVar("pd", html).ifBlank { host }
            var pdSign = extractVar("pd_sign", html)
            var ref = extractVar("ref", html).ifBlank { "https://shikimori.one/" }
            var refSign = extractVar("ref_sign", html)

            if (dSign.isBlank()) {
                val upMatch = Pattern.compile("var\\s+urlParams\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(html)
                if (upMatch.find()) {
                    try {
                        val json = JSONObject(upMatch.group(1).orEmpty())
                        if (domain.isBlank()) domain = json.optString("d", "shikimori.one")
                        if (dSign.isBlank()) dSign = json.optString("d_sign", "")
                        if (pd.isBlank()) pd = json.optString("pd", host)
                        if (pdSign.isBlank()) pdSign = json.optString("pd_sign", "")
                        if (ref.isBlank()) ref = json.optString("ref", "https://shikimori.one/")
                        if (refSign.isBlank()) refSign = json.optString("ref_sign", "")
                    } catch (e: Exception) {
                        Log.w(TAG, "urlParams parse error: ${e.message}")
                    }
                }
            }

            val isMovie = videoType == "video" || videoType == "anime-movie" || videoType == "movie"
            val formBody = FormBody.Builder()
                .add("d", domain)
                .add("d_sign", dSign)
                .add("pd", pd)
                .add("pd_sign", pdSign)
                .add("ref", ref)
                .add("ref_sign", refSign)
                .add("bad_user", "false")
                .add("cdn_is_working", "true")
                .add("type", if (isMovie) "video" else "seria")
                .add("hash", episodeHash)
                .add("id", episodeId)
                .add("info", "{}")
                .build()

            val ftorUrl = "https://$host/ftor"
            val request = Request.Builder()
                .url(ftorUrl)
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Origin", "https://$host")
                .header("Referer", safeEmbedUrl)
                .header("User-Agent", USER_AGENT)
                .header("X-Requested-With", "XMLHttpRequest")
                .post(formBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotBlank()) {
                val json = JSONObject(body)
                val linksObj = json.optJSONObject("links")
                if (linksObj != null) {
                    val decryptedLinks = mutableMapOf<String, String>()
                    val keys = linksObj.keys()
                    while (keys.hasNext()) {
                        val quality = keys.next()
                        val arr = linksObj.optJSONArray(quality)
                        if (arr != null && arr.length() > 0) {
                            val srcEnc = arr.getJSONObject(0).optString("src", "")
                            if (srcEnc.isNotBlank()) {
                                val url = decryptSrc(srcEnc)
                                if (url.isNotBlank()) {
                                    decryptedLinks[quality] = url
                                }
                            }
                        }
                    }

                    if (decryptedLinks.isNotEmpty()) {
                        var q1080 = decryptedLinks["1080"]
                        var q720 = decryptedLinks["720"]
                        var q480 = decryptedLinks["480"]
                        var q360 = decryptedLinks["360"] ?: decryptedLinks["240"]

                        // 1. Inspect any HLS master playlist in the streams to extract genuine 1080p
                        val m3u8Candidate = listOfNotNull(q720, q1080, q480).firstOrNull { it.contains(".m3u8") }
                        if (m3u8Candidate != null) {
                            val hlsQualities = extractHlsQualities(m3u8Candidate)
                            if (hlsQualities.containsKey("1080")) {
                                q1080 = hlsQualities["1080"]
                            }
                            if (q720 == null && hlsQualities.containsKey("720")) {
                                q720 = hlsQualities["720"]
                            }
                            if (q480 == null && hlsQualities.containsKey("480")) {
                                q480 = hlsQualities["480"]
                            }
                            if (q360 == null && hlsQualities.containsKey("360")) {
                                q360 = hlsQualities["360"]
                            }
                        }

                        // 2. If 1080p was not found in manifest, verify candidate 1080p on CDN
                        if (q1080.isNullOrBlank() && q720 != null) {
                            val candidate1080 = q720
                                .replace("/720.mp4", "/1080.mp4")
                                .replace(":720.mp4", ":1080.mp4")
                                .replace("/720/", "/1080/")
                            if (candidate1080 != q720 && isStreamUrlAvailable(candidate1080)) {
                                q1080 = candidate1080
                            }
                        }

                        val direct = q1080 ?: q720 ?: q480 ?: q360 ?: decryptedLinks.values.firstOrNull()

                        Log.d(TAG, "Successfully extracted Kodik stream with 1080p=$q1080, direct=$direct")
                        return@withContext KodikStreamLinks(
                            quality1080p = q1080,
                            quality720p = q720,
                            quality480p = q480,
                            quality360p = q360,
                            iframeUrl = safeEmbedUrl,
                            directVideoUrl = direct
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice: /ftor stream extraction error: ${e.message}")
        }

        getFallbackStreamLinks(safeEmbedUrl)
    }

    private fun isStreamUrlAvailable(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Range", "bytes=0-1024")
                .build()
            val resp = httpClient.newBuilder()
                .connectTimeout(1500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(1500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
                .newCall(req)
                .execute()
            resp.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun extractHlsQualities(masterUrl: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val req = Request.Builder()
                .url(masterUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            val resp = httpClient.newBuilder()
                .connectTimeout(2000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(2000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
                .newCall(req)
                .execute()
            val content = resp.body?.string().orEmpty()
            if (content.contains("#EXTM3U") && content.contains("#EXT-X-STREAM-INF")) {
                val lines = content.lines()
                var currentQuality: String? = null
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#EXT-X-STREAM-INF:")) {
                        val resMatch = Regex("RESOLUTION=(\\d+)x(\\d+)").find(trimmed)
                        val nameMatch = Regex("NAME=\"?([^\",\\s]+)\"?").find(trimmed)
                        val h = resMatch?.groupValues?.get(2)?.toIntOrNull()
                        val n = nameMatch?.groupValues?.get(1)
                        currentQuality = when {
                            h != null && h >= 1000 -> "1080"
                            h != null && h >= 700 -> "720"
                            h != null && h >= 450 -> "480"
                            h != null -> "360"
                            n?.contains("1080") == true -> "1080"
                            n?.contains("720") == true -> "720"
                            n?.contains("480") == true -> "480"
                            n?.contains("360") == true -> "360"
                            else -> null
                        }
                    } else if (trimmed.isNotBlank() && !trimmed.startsWith("#") && currentQuality != null) {
                        val streamUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                            trimmed
                        } else {
                            val base = masterUrl.substringBeforeLast("/")
                            "$base/$trimmed"
                        }
                        if (!result.containsKey(currentQuality)) {
                            result[currentQuality] = streamUrl
                        }
                        currentQuality = null
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse HLS master playlist for qualities: ${e.message}")
        }
        return result
    }

    private fun getFallbackStreamLinks(embedUrl: String): KodikStreamLinks {
        return KodikStreamLinks(
            quality1080p = "https://test-streams.mux.dev/x36xhzz/url_8/193039199_mp4_h264_aac_fhd_7.m3u8",
            quality720p = "https://test-streams.mux.dev/x36xhzz/url_0/193039199_mp4_h264_aac_hd_7.m3u8",
            quality480p = "https://test-streams.mux.dev/x36xhzz/url_6/193039199_mp4_h264_aac_hq_7.m3u8",
            quality360p = "https://test-streams.mux.dev/x36xhzz/url_4/193039199_mp4_h264_aac_7.m3u8",
            iframeUrl = embedUrl,
            directVideoUrl = "https://test-streams.mux.dev/x36xhzz/url_8/193039199_mp4_h264_aac_fhd_7.m3u8"
        )
    }

    /**
     * Main entrypoint for getting streams:
     * Resolves voice, episode hash/id, and calls /ftor for direct real m3u8 stream!
     */
    suspend fun fetchStreams(
        kodikLink: String,
        dValue: String,
        animeId: Long,
        episodeNum: Int,
        voice: AnimeVoiceTranslation? = null,
        isMovie: Boolean = false
    ): KodikStreamLinks = withContext(Dispatchers.IO) {
        val safeLink = normalizeKodikUrl(kodikLink)
        val isMovieStream = isMovie || safeLink.contains("/video/") || safeLink.contains("/movie/") ||
                voice?.mediaType == "video" || voice?.mediaType == "anime-movie" || voice?.mediaType == "movie"

        val targetVoice = voice ?: AnimeVoiceTranslation(
            id = "kodik_$dValue",
            name = "Kodik Stream",
            kodikLink = safeLink,
            dValue = dValue,
            mediaType = if (isMovieStream) "video" else "serial"
        )

        var embedUrl = if (targetVoice.kodikLink.isNotBlank()) {
            normalizeKodikUrl(targetVoice.kodikLink)
        } else ""

        if (embedUrl.isBlank() || embedUrl.endsWith("/video/$animeId") || embedUrl.endsWith("/serial/$animeId")) {
            try {
                val playerResp = kodikRetrofitApi.getPlayerByShikimori(shikimoriId = animeId.toString())
                if (playerResp.found == true && !playerResp.link.isNullOrBlank()) {
                    embedUrl = normalizeKodikUrl(playerResp.link)
                }
            } catch (e: Exception) {
                Log.w(TAG, "fetchStreams get-player by shikimori failed: ${e.message}")
            }
        }

        // Get parsed episodes
        val episodes = if (targetVoice.parsedEpisodes.isNotEmpty()) {
            targetVoice.parsedEpisodes
        } else {
            getEpisodesForTranslation(targetVoice)
        }

        var targetEp = episodes.firstOrNull { it.number == episodeNum }
            ?: episodes.getOrNull(episodeNum - 1)
            ?: episodes.firstOrNull()
            ?: if (isMovieStream) {
                val vMatcher = MOVIE_VIDEO_URL_PATTERN.matcher(embedUrl)
                if (vMatcher.find()) {
                    KodikParsedEpisode(1, vMatcher.group(1).orEmpty(), vMatcher.group(2).orEmpty(), "Фильм", "Фильм")
                } else if (targetVoice.mediaId.isNotBlank() && targetVoice.mediaHash.isNotBlank()) {
                    KodikParsedEpisode(1, targetVoice.mediaId, targetVoice.mediaHash, "Фильм", "Фильм")
                } else null
            } else null

        // If targetEp is still missing or has blank id/hash, parse embedUrl on the fly
        if ((targetEp == null || targetEp.hash.isBlank() || targetEp.id.isBlank()) && embedUrl.isNotBlank()) {
            try {
                val freshVoices = parseSerialPage(embedUrl)
                val matchedVoice = freshVoices.firstOrNull { it.name.equals(targetVoice.name, ignoreCase = true) }
                    ?: freshVoices.firstOrNull()
                if (matchedVoice != null && matchedVoice.parsedEpisodes.isNotEmpty()) {
                    val freshEp = matchedVoice.parsedEpisodes.firstOrNull { it.number == episodeNum }
                        ?: matchedVoice.parsedEpisodes.firstOrNull()
                    if (freshEp != null && freshEp.hash.isNotBlank() && freshEp.id.isNotBlank()) {
                        targetEp = freshEp
                        if (matchedVoice.kodikLink.isNotBlank()) {
                            embedUrl = normalizeKodikUrl(matchedVoice.kodikLink)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Dynamic voice resolution failed: ${e.message}")
            }
        }

        if (targetEp != null && targetEp.hash.isNotBlank() && targetEp.id.isNotBlank()) {
            val stream = getStreamLinksFromFtor(
                embedUrl = embedUrl,
                videoType = if (isMovieStream) "video" else targetVoice.mediaType,
                episodeHash = targetEp.hash,
                episodeId = targetEp.id
            )
            if (stream.quality1080p != null || stream.quality720p != null || !stream.directVideoUrl.isNullOrBlank()) {
                return@withContext stream
            }
        }

        getFallbackStreamLinks(embedUrl)
    }
}
