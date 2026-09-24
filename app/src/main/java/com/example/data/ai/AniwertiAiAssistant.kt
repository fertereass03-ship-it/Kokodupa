package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.models.ShikimoriAnimeDto
import com.example.data.repository.AnimeRepository
import com.example.data.settings.AppLanguage
import com.example.data.settings.AppSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import android.media.MediaMetadataRetriever

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val imageUri: String? = null,
    val videoUri: String? = null,
    val isVideo: Boolean = false,
    val recommendedAnimes: List<ShikimoriAnimeDto> = emptyList(),
    val matchedAnimeId: Long? = null,
    val matchedEpisode: Int? = null,
    val matchedSeasonName: String? = null,
    val matchedTimecodeSeconds: Double? = null,
    val matchedSimilarity: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class AiAssistantResponse(
    val replyText: String,
    val recommendedAnimes: List<ShikimoriAnimeDto> = emptyList(),
    val matchedAnimeId: Long? = null,
    val matchedEpisode: Int? = null,
    val matchedSeasonName: String? = null,
    val matchedTimecodeSeconds: Double? = null,
    val matchedSimilarity: Double? = null
)

enum class MessageSender {
    USER, AI
}

data class TraceSceneMatch(
    val titleRomaji: String,
    val titleEnglish: String,
    val titleNative: String,
    val cleanedFilenameTitle: String,
    val episode: String?,
    val fromSeconds: Double,
    val similarity: Double,
    val malId: Long?
) {
    val bestSearchTitle: String
        get() = titleRomaji.ifBlank { titleEnglish.ifBlank { cleanedFilenameTitle } }
}

data class WebVisualMatch(
    val title: String,
    val characters: String?,
    val similarityPercent: Double,
    val source: String
)

class AniwertiAiAssistant(
    private val context: Context,
    private val repository: AnimeRepository
) {
    private val TAG = "AniwertiAiAssistant"
    private val prefs = context.getSharedPreferences("aniwerti_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        val UKRAINIAN_TO_ROMAJI_MAP: Map<String, String> = mapOf(
            "клинок" to "Kimetsu no Yaiba",
            "клинок демонів" to "Kimetsu no Yaiba",
            "клинок, який знищує демонів" to "Kimetsu no Yaiba",
            "клинок який знищує демонів" to "Kimetsu no Yaiba",
            "винищувач демонів" to "Kimetsu no Yaiba",
            "демон слеєр" to "Kimetsu no Yaiba",
            "магічна битва" to "Jujutsu Kaisen",
            "магічна битва 0" to "Jujutsu Kaisen 0",
            "магическая битва" to "Jujutsu Kaisen",
            "атака титанів" to "Shingeki no Kyojin",
            "атака титанов" to "Shingeki no Kyojin",
            "вторгнення титанів" to "Shingeki no Kyojin",
            "зошит смерті" to "Death Note",
            "тетрадь смерти" to "Death Note",
            "підняття рівня наодинці" to "Solo Leveling",
            "соло левелінг" to "Solo Leveling",
            "соло левелинг" to "Solo Leveling",
            "ван піс" to "One Piece",
            "ван пис" to "One Piece",
            "людина-бензопила" to "Chainsaw Man",
            "людина бензопила" to "Chainsaw Man",
            "человек бензопила" to "Chainsaw Man",
            "бензопила" to "Chainsaw Man",
            "токійський гуль" to "Tokyo Ghoul",
            "токийский гуль" to "Tokyo Ghoul",
            "моя геройська академія" to "Boku no Hero Academia",
            "моя геройская академия" to "Boku no Hero Academia",
            "геройська академія" to "Boku no Hero Academia",
            "сага про вінланд" to "Vinland Saga",
            "сага о винланде" to "Vinland Saga",
            "переродження безробітного" to "Mushoku Tensei",
            "реінкарнація безробітного" to "Mushoku Tensei",
            "проводжальниця фрірен" to "Sousou no Frieren",
            "фрірен" to "Sousou no Frieren",
            "фрирен" to "Sousou no Frieren",
            "хантер х хантер" to "Hunter x Hunter",
            "мисливець х мисливець" to "Hunter x Hunter",
            "сталевий алхімік" to "Fullmetal Alchemist",
            "стальной алхимик" to "Fullmetal Alchemist",
            "доктор стоун" to "Dr. Stone",
            "код гіас" to "Code Geass",
            "код гиас" to "Code Geass",
            "євангеліон" to "Neon Genesis Evangelion",
            "евангелион" to "Neon Genesis Evangelion",
            "берсерк" to "Berserk",
            "бліч" to "Bleach",
            "блич" to "Bleach",
            "наруто" to "Naruto",
            "боруто" to "Boruto",
            "ванпанчмен" to "One Punch Man",
            "ван панч мен" to "One Punch Man",
            "моб психо 100" to "Mob Psycho 100",
            "моб психо" to "Mob Psycho 100",
            "гуррен лаганн" to "Tengen Toppa Gurren Lagann",
            "гурен лаган" to "Tengen Toppa Gurren Lagann",
            "баскетбол куроко" to "Kuroko no Basket",
            "волейбол" to "Haikyuu!!",
            "хайкю" to "Haikyuu!!",
            "зоряне дитя" to "Oshi no Ko",
            "дитина ідола" to "Oshi no Ko",
            "сім смертних гріхів" to "Nanatsu no Taizai",
            "семь смертных грехов" to "Nanatsu no Taizai",
            "майстер меча онлайн" to "Sword Art Online",
            "мастера меча онлайн" to "Sword Art Online",
            "сао" to "Sword Art Online",
            "паразит" to "Kiseijuu: Sei no Kakuritsu",
            "бездомний бог" to "Noragami",
            "норамагі" to "Noragami",
            "хвіст феї" to "Fairy Tail",
            "казка про хвіст феї" to "Fairy Tail",
            "хвост феи" to "Fairy Tail",
            "брама штайна" to "Steins;Gate",
            "врата штейна" to "Steins;Gate",
            "ре зеро" to "Re:Zero kara Hajimeru Isekai Seikatsu",
            "життя з нуля" to "Re:Zero kara Hajimeru Isekai Seikatsu",
            "синій екзорцист" to "Ao no Exorcist",
            "синий экзорцист" to "Ao no Exorcist",
            "кайдзю 8" to "Kaiju No. 8",
            "кайдзю номер 8" to "Kaiju No. 8",
            "вітряний перелом" to "Wind Breaker",
            "вітряк" to "Wind Breaker",
            "шалена азартна гра" to "Kakegurui",
            "безумный азарт" to "Kakegurui",
            "клас убивць" to "Ansatsu Kyoushitsu",
            "клас вбивць" to "Ansatsu Kyoushitsu",
            "клас суперництва" to "Classroom of the Elite",
            "ласкаво просимо до класу еліти" to "Classroom of the Elite",
            "клас еліти" to "Classroom of the Elite",
            "чорний конюшина" to "Black Clover",
            "чорна конюшина" to "Black Clover",
            "черный клевер" to "Black Clover",
            "джоджо" to "JoJo no Kimyou na Bouken",
            "неймовірні пригоди джоджо" to "JoJo no Kimyou na Bouken",
            "доктор рауш" to "Dr. Stone",
            "неперевершений святий" to "Seija Musou",
            "монолог фармацевта" to "Kusuriya no Hitorigoto",
            "аптекарка" to "Kusuriya no Hitorigoto",
            "щоденник аптекарки" to "Kusuriya no Hitorigoto",
            "моя дівчина не просто милашка" to "Kawaii dake ja Nai Shikimori-san",
            "руйнівник гоблінів" to "Goblin Slayer",
            "вбивця гоблінів" to "Goblin Slayer",
            "убийца гоблинов" to "Goblin Slayer",
            "дандадан" to "Dandadan",
            "пекельний рай" to "Jigokuraku",
            "адский рай" to "Jigokuraku",
            "сім'я шпигуна" to "Spy x Family",
            "семья шпиона" to "Spy x Family",
            "ботті-рокер" to "Bocchi the Rock!",
            "боччи" to "Bocchi the Rock!",
            "вітролом" to "Wind Breaker",
            "ветролом" to "Wind Breaker",
            "кіберпанк" to "Cyberpunk: Edgerunners",
            "киберпанк" to "Cyberpunk: Edgerunners"
        )

        fun mapToRomajiOrSearchCandidate(input: String): String {
            val clean = input.lowercase().trim()
                .replace(Regex("""[«"“»"”*]"""), "")
                .trim()
            
            UKRAINIAN_TO_ROMAJI_MAP[clean]?.let { return it }

            for ((ua, romaji) in UKRAINIAN_TO_ROMAJI_MAP) {
                if (clean == ua || clean.contains(ua)) {
                    return romaji
                }
            }
            return input
        }

        data class ExtractedMatch(
            val cleanedText: String,
            val romaji: String?,
            val localTitle: String?
        )

        fun extractMatchTag(text: String): ExtractedMatch {
            val regex = Regex("""\[ANIME_MATCH:\s*([^|\]]+)(?:\|\s*([^\]]+))?\]""")
            val match = regex.find(text)
            val romaji = match?.groupValues?.getOrNull(1)?.trim()
            val localTitle = match?.groupValues?.getOrNull(2)?.trim()
            val cleanedText = text.replace(regex, "").trim()
            return ExtractedMatch(cleanedText, romaji, localTitle)
        }

        fun generateVideoThumbnailUri(context: Context, videoUriString: String): String? {
            val retriever = MediaMetadataRetriever()
            return try {
                if (videoUriString.startsWith("/")) {
                    retriever.setDataSource(videoUriString)
                } else {
                    retriever.setDataSource(context, Uri.parse(videoUriString))
                }
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durationStr?.toLongOrNull() ?: 3000L
                val timeUs = if (durationMs > 1000) 1_000_000L else 0L
                val bmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime ?: return null
                val thumbFile = File(context.cacheDir, "vid_thumb_${System.currentTimeMillis()}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                thumbFile.absolutePath
            } catch (e: Exception) {
                Log.w("AniwertiAiAssistant", "Failed to generate video thumbnail: ${e.message}")
                null
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }

        fun extractFramesFromVideo(context: Context, uriString: String, maxFrames: Int = 4): List<ByteArray> {
            val retriever = MediaMetadataRetriever()
            val frames = mutableListOf<ByteArray>()
            try {
                if (uriString.startsWith("/")) {
                    retriever.setDataSource(uriString)
                } else {
                    retriever.setDataSource(context, Uri.parse(uriString))
                }
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 5000L
                val timestampsUs = mutableListOf<Long>()
                if (durationMs > 2000) {
                    timestampsUs.add((durationMs * 1000 * 0.20).toLong())
                    timestampsUs.add((durationMs * 1000 * 0.50).toLong())
                    timestampsUs.add((durationMs * 1000 * 0.80).toLong())
                } else {
                    timestampsUs.add(300_000L)
                    timestampsUs.add(800_000L)
                }

                for (timeUs in timestampsUs.take(maxFrames)) {
                    val bmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                    if (bmp != null) {
                        val maxDim = 1024
                        val maxSide = maxOf(bmp.width, bmp.height)
                        val scale = if (maxSide > maxDim) maxDim.toFloat() / maxSide else 1f
                        val scaledBmp = if (scale < 1f) {
                            Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
                        } else bmp
                        val os = ByteArrayOutputStream()
                        scaledBmp.compress(Bitmap.CompressFormat.JPEG, 85, os)
                        frames.add(os.toByteArray())
                    }
                }
            } catch (e: Exception) {
                Log.w("AniwertiAiAssistant", "Video frame extraction failed: ${e.message}")
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
            return frames
        }
    }

    fun getEffectiveApiKey(): String {
        val userSavedKey = prefs.getString("user_gemini_api_key", "")?.trim() ?: ""
        if (userSavedKey.isNotBlank()) return userSavedKey
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }
        if (buildKey.isNotBlank() && buildKey != "your_api_key_here") return buildKey
        val envKey = System.getenv("GEMINI_API_KEY")?.trim() ?: ""
        if (envKey.isNotBlank()) return envKey
        return ""
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString("user_gemini_api_key", key.trim()).apply()
    }

    fun hasApiKey(): Boolean = getEffectiveApiKey().isNotBlank()

    suspend fun generateAnswer(
        userPrompt: String,
        history: List<AiChatMessage>,
        imageUri: String? = null,
        videoUri: String? = null
    ): AiAssistantResponse = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        val hasValidApiKey = apiKey.isNotBlank()
        val isUk = AppSettingsManager.isUkrainian()

        // -------------------------------------------------------------
        // Сценарій 0: Пошук аніме за відео / уривком (TikTok, Reels, AMV, кліпи)
        // -------------------------------------------------------------
        if (!videoUri.isNullOrBlank()) {
            val frames = extractFramesFromVideo(context, videoUri, maxFrames = 4)
            if (frames.isNotEmpty()) {
                val allTraceMatches = mutableListOf<TraceSceneMatch>()
                for (frameBytes in frames) {
                    val matches = searchByScreenshotTraceMoe(frameBytes)
                    for (m in matches) {
                        if (allTraceMatches.none { it.bestSearchTitle.equals(m.bestSearchTitle, ignoreCase = true) }) {
                            allTraceMatches.add(m)
                        }
                    }
                }
                allTraceMatches.sortByDescending { it.similarity }
                val bestTrace = allTraceMatches.firstOrNull()?.takeIf { it.similarity >= 0.45 }
                val primaryFrame = frames.getOrNull(frames.size / 2) ?: frames.first()

                // Мультимодальний глибокий аналіз кадру відео через Gemini API
                if (hasValidApiKey) {
                    try {
                        val videoPrompt = buildString {
                            appendLine(if (isUk) {
                                "Ти — найкращий у світі ШІ-експерт із розпізнавання аніме за відео та кліпами (TikTok, Reels, YouTube Shorts, AMV, уривки серій).\n" +
                                "КОРИСТУВАЧ ЗАВАНТАЖИВ ВІДЕО. УВАЖНО ДОСЛІДИ ЦЕЙ КЛЮЧОВИЙ КАДР З ВІДЕО:\n" +
                                "1. Проігноруй будь-які субтитри, водяні знаки чи написи на відео та сфокусуйся на персонажах, стилі малюнка студії, фоні та одязі.\n" +
                                "2. Визначи ТОЧНУ назву аніме (Ромадзі/Англійська та Українська назви)."
                            } else {
                                "Ты — лучший в мире ИИ-эксперт по распознаванию аниме по видео и клипам (TikTok, Reels, YouTube Shorts, AMV, отрывки серий).\n" +
                                "ПОЛЬЗОВАТЕЛЬ ЗАГРУЗИЛ ВИДЕО. ВНИМАТЕЛЬНО ИЗУЧИ ЭТОТ КЛЮЧЕВОЙ КАДР ИЗ ВИДЕО:\n" +
                                "1. Проигнорируй любые субтитры, водяные знаки или надписи на видео и сфокусируйся на персонажах, стиле рисунка студии, фоне и одежде.\n" +
                                "2. Определи ТОЧНОЕ название аниме (Ромаджи/Английское и Русское названия)."
                            })
                            if (userPrompt.isNotBlank() && !userPrompt.contains("Пошук аніме") && !userPrompt.contains("Поиск аниме")) {
                                appendLine("Коментар користувача: \"$userPrompt\"")
                            }
                            if (bestTrace != null) {
                                appendLine("ПІДКАЗКА З БАЗИ КАДРІВ ВІДЕО: висока візуальна схожість (${(bestTrace.similarity * 100).toInt()}%) з «${bestTrace.bestSearchTitle}»${bestTrace.episode?.let { ", серія $it" } ?: ""}.")
                            }
                            appendLine("\nТВОЯ СТРУКТУРОВАНА ВІДПОВІДЬ:")
                            appendLine(if (isUk) "🎬 **Назва тайтлу**: вкажи назву." else "🎬 **Название тайтла**: укажи название.")
                            appendLine(if (isUk) "👤 **Персонажі у відео**: хто зображений у цьому моменті." else "👤 **Персонажи в видео**: кто изображен в этом моменте.")
                            appendLine(if (isUk) "⏱️ **Момент/серія**: сезон, серія або контекст сцени." else "⏱️ **Момент/серия**: сезон, серия или контекст сцены.")
                            appendLine(if (isUk) "📖 **Короткий опис**: коротко опиши сюжет цього аніме." else "📖 **Краткое описание**: кратко опиши сюжет этого аниме.")
                            appendLine("\nВ САМОМУ КІНЦІ ВІДПОВІДІ обов'язково виведи системний рядок для переходу до тайтлу:")
                            appendLine("[ANIME_MATCH: RomajiOrEnglishTitle | LocalizedTitle]")
                        }

                        val geminiAnswer = callGeminiRest(
                            apiKey = apiKey,
                            userPrompt = videoPrompt,
                            imageBytes = primaryFrame,
                            history = history
                        )

                        if (!geminiAnswer.isNullOrBlank()) {
                            val extracted = extractMatchTag(geminiAnswer)
                            val matchedAnimes = findMatchingAnimes(
                                userPrompt = userPrompt,
                                aiResponseText = geminiAnswer,
                                romajiHint = extracted.romaji ?: bestTrace?.bestSearchTitle,
                                localHint = extracted.localTitle
                            )
                            val topAnime = matchedAnimes.firstOrNull()
                            val seasonName = if (bestTrace != null) {
                                extractSeasonInfo(bestTrace.titleRomaji, bestTrace.titleEnglish, topAnime?.russian ?: topAnime?.name.orEmpty(), topAnime?.kind)
                            } else null
                            return@withContext AiAssistantResponse(
                                replyText = extracted.cleanedText,
                                recommendedAnimes = matchedAnimes,
                                matchedAnimeId = topAnime?.id ?: bestTrace?.malId,
                                matchedEpisode = bestTrace?.episode?.toIntOrNull(),
                                matchedSeasonName = seasonName,
                                matchedTimecodeSeconds = bestTrace?.fromSeconds,
                                matchedSimilarity = bestTrace?.similarity
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Gemini video analysis error: ${e.message}")
                    }
                }

                if (bestTrace != null) {
                    return@withContext formatTraceMoeResult(bestTrace, allTraceMatches.drop(1), isVideo = true)
                }

                if (allTraceMatches.isNotEmpty() && allTraceMatches.first().similarity >= 0.40) {
                    return@withContext formatTraceMoeResult(allTraceMatches.first(), allTraceMatches.drop(1), isVideo = true)
                }

                return@withContext AiAssistantResponse(
                    replyText = if (isUk) {
                        "🎬 **Аналіз відеозапису**:\n\n" +
                        "Не вдалося знайти 100% точний збіг цих кадрів відео за базою аніме (можливо, це аматорська анімація, фанатський кліп (AMV) або на відео накладено сильні ефекти чи фільтри).\n\n" +
                        "💡 **Як знайти його швидше**:\n" +
                        "• Спробуйте завантажити інший уривок відео, де чітко видно обличчя персонажів\n" +
                        "• Або напишіть у чаті будь-яку фразу чи цитату персонажа з цього відео!"
                    } else {
                        "🎬 **Анализ видеозаписи**:\n\n" +
                        "Не удалось найти 100% точное совпадение этих кадров видео по базе аниме (возможно, это фанатский clip (AMV), любительская анимация или сильные цветовые эффекты/фильтры).\n\n" +
                        "💡 **Как найти его быстрее**:\n" +
                        "• Попробуйте прикрепить другой фрагмент видео, где отчетливо видны лица персонажей\n" +
                        "• Либо напишите в чате любую фразу или цитату персонажа из этого видео!"
                    },
                    recommendedAnimes = emptyList()
                )
            }
        }

        // -------------------------------------------------------------
        // Сценарій 1: Пошук аніме за скріншотом / зображенням (TikTok, сайти, кадри)
        // -------------------------------------------------------------
        if (!imageUri.isNullOrBlank()) {
            val imageBytes = loadAndDownscaleImage(context, imageUri, maxDimension = 1024)
            if (imageBytes != null) {
                // 1. Пошук по світовій базі кадрів аніме через trace.moe
                val traceMatches = searchByScreenshotTraceMoe(imageBytes).toMutableList()

                // Спеціальний режим для TikTok / Reels / Shorts:
                // Якщо це вертикальний скріншот телефону, вирізаємо чисту центральну область відео (без кнопок і субтитрів соцмережі)
                val croppedBytes = extractCenterVideoCropIfPortrait(context, imageUri)
                if (croppedBytes != null) {
                    val cropMatches = searchByScreenshotTraceMoe(croppedBytes)
                    for (cm in cropMatches) {
                        if (traceMatches.none { it.bestSearchTitle.equals(cm.bestSearchTitle, ignoreCase = true) }) {
                            traceMatches.add(cm)
                        }
                    }
                }
                traceMatches.sortByDescending { it.similarity }
                val bestTrace = traceMatches.firstOrNull()?.takeIf { it.similarity >= 0.50 }

                // 2. Пошук ілюстрацій / фанартів через SauceNAO
                val webMatch = searchByScreenshotSauceNao(imageBytes)?.takeIf { it.similarityPercent >= 50.0 }

                // 3. Якщо доступний Gemini API — мультимодальний розширений аналіз зображення через Google / Gemini
                if (hasValidApiKey) {
                    try {
                        val promptWithContext = buildString {
                            appendLine("Ти — найкращий у світі ШІ-експерт із розпізнавання аніме за будь-яким скріншотом, кадром чи зображенням (зокрема з TikTok, Reels, YouTube Shorts, форумів, соціальних мереж та сайтів).")
                            appendLine("\nУВАЖНО ДОСЛІДИ ЦЕ ЗОБРАЖЕННЯ:")
                            appendLine("1. Якщо на зображенні є інтерфейс смартфона чи соцмереж (TikTok, кнопки лайків/коментарів, статус-бар, субтитри) — повністю проігноруй інтерфейс і сфокусуйся на самому аніме, персонажах, фоні та стилі малюнка студії.")
                            appendLine("2. Визначи ТОЧНУ назву аніме:")
                            appendLine("   - Головна міжнародна назва Ромадзі / Англійська (наприклад: Kimetsu no Yaiba, Jujutsu Kaisen, Shingeki no Kyojin, Chainsaw Man, Sousou no Frieren, Solo Leveling, Bleach, One Piece, Naruto, Oshi no Ko тощо).")
                            appendLine("   - Загальновідома українська назва (наприклад: Клинок, який знищує демонів, Магічна битва, Атака Титанів тощо).")
                            if (userPrompt.isNotBlank() && !userPrompt.contains("Пошук аніме за скріншотом")) {
                                appendLine("3. Запитання / коментар користувача: \"$userPrompt\"")
                            }
                            if (bestTrace != null) {
                                appendLine("4. ПІДКАЗКА З БАЗИ КАДРІВ: висока візуальна схожість (${(bestTrace.similarity * 100).toInt()}%) з «${bestTrace.bestSearchTitle}»${bestTrace.episode?.let { ", серія $it" } ?: ""}. Зістав кадри та підтверди або уточни точну назву!")
                            }
                            if (webMatch != null) {
                                appendLine("5. ПІДКАЗКА З МЕРЕЖІ: «${webMatch.title}»${webMatch.characters?.let { ", персонаж: $it" } ?: ""}.")
                            }
                            appendLine("\nТВОЯ СТРУКТУРОВАНА ВІДПОВІДЬ:")
                            appendLine("🎯 **Точна назва**: вкажи українську та ромадзі/англійську назви.")
                            appendLine("👤 **Персонажі в кадрі**: імена персонажів та їхня роль у цій сцені.")
                            appendLine("🎬 **Сцена**: сезон, серія або контекст моменту.")
                            appendLine("📖 **Короткий опис сюжету**: коротко та захоплююче розкажи, про що цей тайтл.")
                            appendLine("\nВ САМОМУ КІНЦІ ВІДПОВІДІ обов'язково виведи системний рядок для пошуку в каталозі:")
                            appendLine("[ANIME_MATCH: RomajiOrEnglishTitle | UkrainianTitle]")
                            appendLine("Наприклад: [ANIME_MATCH: Kimetsu no Yaiba | Клинок, який знищує демонів]")
                        }

                        val geminiVisionAnswer = callGeminiRest(
                            apiKey = apiKey,
                            userPrompt = promptWithContext,
                            imageBytes = imageBytes,
                            history = history
                        )

                        if (!geminiVisionAnswer.isNullOrBlank()) {
                            val extracted = extractMatchTag(geminiVisionAnswer)
                            val matchedAnimes = findMatchingAnimes(
                                userPrompt = userPrompt,
                                aiResponseText = geminiVisionAnswer,
                                romajiHint = extracted.romaji ?: bestTrace?.bestSearchTitle ?: webMatch?.title,
                                localHint = extracted.localTitle
                            )
                            val topAnime = matchedAnimes.firstOrNull()
                            val seasonName = if (bestTrace != null) {
                                extractSeasonInfo(bestTrace.titleRomaji, bestTrace.titleEnglish, topAnime?.russian ?: topAnime?.name.orEmpty(), topAnime?.kind)
                            } else null
                            return@withContext AiAssistantResponse(
                                replyText = extracted.cleanedText,
                                recommendedAnimes = matchedAnimes,
                                matchedAnimeId = topAnime?.id ?: bestTrace?.malId,
                                matchedEpisode = bestTrace?.episode?.toIntOrNull(),
                                matchedSeasonName = seasonName,
                                matchedTimecodeSeconds = bestTrace?.fromSeconds,
                                matchedSimilarity = bestTrace?.similarity
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Gemini vision failed: ${e.message}")
                    }
                }

                // 4. Якщо Gemini недоступний або не дав відповіді, використовуємо результати пошуку за кадрами
                if (bestTrace != null && bestTrace.similarity >= 0.50) {
                    return@withContext formatTraceMoeResult(bestTrace, traceMatches.drop(1))
                }

                if (webMatch != null && webMatch.similarityPercent >= 50.0) {
                    return@withContext formatWebMatchResult(webMatch)
                }

                if (traceMatches.isNotEmpty() && traceMatches.first().similarity >= 0.40) {
                    return@withContext formatTraceMoeResult(traceMatches.first(), traceMatches.drop(1))
                }

                // Якщо кадру не знайдено автоматично
                return@withContext AiAssistantResponse(
                    replyText = if (isUk) {
                        "🔍 **Аналіз скріншоту**:\n\n" +
                                "Не вдалося знайти 100% точний збіг цього кадру за базами аніме-серіалів (можливо, це манґа, рідкісний фанарт або нестандартний колірний фільтр).\n\n" +
                                "💡 **Як знайти його миттєво**:\n" +
                                "• Напишіть короткий опис персонажа у чаті (наприклад: *«хлопець з чорним мечем і шрамом на лобі»*)\n" +
                                "• Або вкажіть будь-яке ключове слово, фразу чи цитату з відео!"
                    } else {
                        "🔍 **Анализ скриншота**:\n\n" +
                                "Не удалось найти 100% точное совпадение этого кадра по базам аниме-сериалов (возможно, это манга, редкий фанарт или нестандартный цветовой фильтр).\n\n" +
                                "💡 **Как найти его мгновенно**:\n" +
                                "• Напишите краткое описание персонажа в чате (например: *«парень с чёрным мечом и шрамом на лбу»*)\n" +
                                "• Либо укажите любое ключевое слово, фразу или цитату из видео!"
                    },
                    recommendedAnimes = emptyList()
                )
            }
        }

        // -------------------------------------------------------------
        // Сценарій 2: Текстовий запит із Gemini API (якщо є ключ)
        // -------------------------------------------------------------
        if (hasValidApiKey) {
            try {
                val geminiResult = callGeminiRest(
                    apiKey = apiKey,
                    userPrompt = userPrompt,
                    imageBytes = null,
                    history = history
                )
                if (!geminiResult.isNullOrBlank()) {
                    val extracted = extractMatchTag(geminiResult)
                    val animeCards = findMatchingAnimes(
                        userPrompt = userPrompt,
                        aiResponseText = geminiResult,
                        romajiHint = extracted.romaji,
                        localHint = extracted.localTitle
                    )
                    return@withContext AiAssistantResponse(
                        replyText = extracted.cleanedText,
                        recommendedAnimes = animeCards,
                        matchedAnimeId = animeCards.firstOrNull()?.id
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API error, falling back to smart local engine: ${e.message}")
            }
        }

        // -------------------------------------------------------------
        // Сценарій 3: Розумний інтелектуальний помічник із живим інтернет-пошуком Shikimori
        // Завжди відповідає на РЕАЛЬНІ запитання користувача по суті!
        // -------------------------------------------------------------
        val (localText, localCards) = generateRealAnswerWithoutGemini(userPrompt)
        return@withContext AiAssistantResponse(
            replyText = localText,
            recommendedAnimes = localCards,
            matchedAnimeId = localCards.firstOrNull()?.id
        )
    }

    /**
     * Пошук по сцені через trace.moe
     */
    private fun searchByScreenshotTraceMoe(imageBytes: ByteArray): List<TraceSceneMatch> {
        val matches = mutableListOf<TraceSceneMatch>()
        try {
            val url = "https://api.trace.moe/search?cutBorders=1&anilistInfo=1"
            val request = Request.Builder()
                .url(url)
                .post(imageBytes.toRequestBody("image/jpeg".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "trace.moe failed: HTTP ${response.code}")
                    return emptyList()
                }
                val respBody = response.body?.string() ?: return emptyList()
                val json = JSONObject(respBody)
                val results = json.optJSONArray("result") ?: return emptyList()

                for (i in 0 until minOf(results.length(), 5)) {
                    val item = results.getJSONObject(i)
                    val similarity = item.optDouble("similarity", 0.0)
                    val from = item.optDouble("from", 0.0)
                    val filename = item.optString("filename")
                    val episodeVal = item.opt("episode")
                    val episode = episodeVal?.takeIf { it != JSONObject.NULL }?.toString()?.let {
                        if (it.endsWith(".0")) it.substringBefore(".0") else it
                    }

                    var titleRomaji = ""
                    var titleEnglish = ""
                    var titleNative = ""
                    var malId: Long? = null

                    val anilistObj = item.optJSONObject("anilist")
                    if (anilistObj != null) {
                        malId = anilistObj.optLong("idMal").takeIf { it > 0 }
                        val titleObj = anilistObj.optJSONObject("title")
                        if (titleObj != null) {
                            titleRomaji = titleObj.optString("romaji")
                            titleEnglish = titleObj.optString("english")
                            titleNative = titleObj.optString("native")
                        }
                    }

                    val cleanedFilename = extractAnimeTitleFromFilename(filename)

                    matches.add(
                        TraceSceneMatch(
                            titleRomaji = titleRomaji,
                            titleEnglish = titleEnglish,
                            titleNative = titleNative,
                            cleanedFilenameTitle = cleanedFilename,
                            episode = episode,
                            fromSeconds = from,
                            similarity = similarity,
                            malId = malId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "trace.moe search exception: ${e.message}")
        }
        return matches.sortedByDescending { it.similarity }
    }

    /**
     * Пошук по артах, постерах та скріншотах через SauceNAO
     */
    private fun searchByScreenshotSauceNao(imageBytes: ByteArray): WebVisualMatch? {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "screenshot.jpg",
                    imageBytes.toRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://saucenao.com/search.php?output_type=2&numres=3&db=999")
                .post(requestBody)
                .header("User-Agent", "AniwertiAnimeApp/2.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val respBody = response.body?.string() ?: return null
                val json = JSONObject(respBody)
                val results = json.optJSONArray("results") ?: return null
                if (results.length() == 0) return null

                val first = results.getJSONObject(0)
                val header = first.optJSONObject("header")
                val data = first.optJSONObject("data") ?: return null

                val simStr = header?.optString("similarity") ?: "0"
                val similarity = simStr.toDoubleOrNull() ?: 0.0

                val title = listOfNotNull(
                    data.optString("material").takeIf { it.isNotBlank() },
                    data.optString("source").takeIf { it.isNotBlank() },
                    data.optString("title").takeIf { it.isNotBlank() }
                ).firstOrNull() ?: return null

                val characters = data.optString("characters").takeIf { it.isNotBlank() }

                return WebVisualMatch(
                    title = title,
                    characters = characters,
                    similarityPercent = similarity,
                    source = "SauceNAO"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "SauceNAO search exception: ${e.message}")
            return null
        }
    }

    fun extractSeasonInfo(
        romaji: String,
        english: String,
        russian: String,
        kind: String? = null
    ): String {
        if (kind.equals("movie", ignoreCase = true)) return "Фільм"
        if (kind.equals("ova", ignoreCase = true)) return "OVA"
        if (kind.equals("ona", ignoreCase = true)) return "ONA"

        val combined = "$russian | $romaji | $english"

        val seasonRegex = Regex("""(?i)(?:season|сезон)\s*(\d+)""")
        seasonRegex.find(combined)?.let {
            return "Сезон ${it.groupValues[1]}"
        }

        val ordinalRegex = Regex("""(?i)(\d+)(?:st|nd|rd|th)\s*season""")
        ordinalRegex.find(combined)?.let {
            return "Сезон ${it.groupValues[1]}"
        }

        val partRegex = Regex("""(?i)(?:part|частина|часть)\s*(\d+)""")
        partRegex.find(combined)?.let {
            return "Частина ${it.groupValues[1]}"
        }

        if (combined.contains("final season", ignoreCase = true) ||
            combined.contains("фінальний сезон", ignoreCase = true) ||
            combined.contains("финальный сезон", ignoreCase = true)) {
            return "Фінальний сезон"
        }

        val titleToCheck = if (romaji.contains(":")) romaji.substringAfter(":")
        else if (english.contains(":")) english.substringAfter(":")
        else if (russian.contains(":")) russian.substringAfter(":")
        else null

        if (titleToCheck != null) {
            val cleanArc = titleToCheck.trim().take(35)
            if (cleanArc.isNotBlank()) {
                return cleanArc
            }
        }

        return "Сезон 1"
    }

    private suspend fun formatTraceMoeResult(
        match: TraceSceneMatch,
        alternatives: List<TraceSceneMatch> = emptyList(),
        isVideo: Boolean = false
    ): AiAssistantResponse {
        val foundList = mutableListOf<ShikimoriAnimeDto>()

        // 1. Прямий пошук за MAL ID (Shikimori ID співпадає з MAL ID для аніме)
        if (match.malId != null && match.malId > 0) {
            try {
                val detail = repository.getAnimeDetails(match.malId)
                val directDto = ShikimoriAnimeDto(
                    id = detail.id,
                    name = detail.name,
                    russian = detail.russian,
                    image = detail.image,
                    url = "/animes/${detail.id}",
                    kind = detail.kind,
                    score = detail.score,
                    status = detail.status,
                    episodes = detail.episodes,
                    episodesAired = detail.episodesAired,
                    airedOn = detail.airedOn,
                    releasedOn = detail.releasedOn
                )
                foundList.add(directDto)
            } catch (e: Exception) {
                Log.w(TAG, "Direct MAL ID ${match.malId} lookup failed: ${e.message}")
            }
        }

        // 2. Пошук сезону за назвами в каталозі Shikimori
        val searchCandidates = listOfNotNull(
            match.titleRomaji.takeIf { it.isNotBlank() },
            match.titleEnglish.takeIf { it.isNotBlank() },
            match.cleanedFilenameTitle.takeIf { it.isNotBlank() }
        ).distinct()

        for (candidate in searchCandidates) {
            val list = repository.searchCatalog(query = candidate, limit = 2)
            for (item in list) {
                if (foundList.none { it.id == item.id }) {
                    foundList.add(item)
                }
            }
            if (foundList.isNotEmpty()) break
        }

        // Також додаємо альтернативні збіги
        for (alt in alternatives.take(2)) {
            val altCand = listOfNotNull(
                alt.titleRomaji.takeIf { it.isNotBlank() },
                alt.titleEnglish.takeIf { it.isNotBlank() }
            ).firstOrNull() ?: continue
            if (foundList.none { it.name.equals(altCand, ignoreCase = true) }) {
                val altList = repository.searchCatalog(query = altCand, limit = 1)
                foundList.addAll(altList)
            }
        }

        val similarityPercent = (match.similarity * 100).toInt()
        val timecode = formatTime(match.fromSeconds)
        val isUk = AppSettingsManager.isUkrainian()
        val episodeInt = match.episode?.toIntOrNull()
        val episodeStr = episodeInt?.let { if (isUk) "$it серія" else "$it серия" } ?: (if (isUk) "фільм/OVA" else "фильм/OVA")
        val topAnime = foundList.firstOrNull()

        val mainTitle = topAnime?.russian?.takeIf { it.isNotBlank() }
            ?: match.titleEnglish.ifBlank { match.bestSearchTitle }

        val seasonName = extractSeasonInfo(
            romaji = match.titleRomaji,
            english = match.titleEnglish,
            russian = topAnime?.russian ?: mainTitle,
            kind = topAnime?.kind
        )

        val header = when {
            isVideo && similarityPercent >= 75 -> if (isUk) "🎬 **Аніме успішно знайдено за відео!**" else "🎬 **Аниме успешно найдено по видео!**"
            isVideo -> if (isUk) "🔍 **Збіг за кадрами відео ($similarityPercent% схожість)**:" else "🔍 **Совпадение по кадрам видео ($similarityPercent% схожесть)**:"
            similarityPercent >= 75 -> if (isUk) "🎯 **Аніме успішно знайдено за скріншотом!**" else "🎯 **Аниме успешно найдено по скриншоту!**"
            else -> if (isUk) "🔍 **Найбільш точний збіг за базою аніме ($similarityPercent% схожість кадру)**:" else "🔍 **Наиболее точное совпадение по базе аниме ($similarityPercent% схожесть кадра)**:"
        }

        val text = buildString {
            appendLine("$header\n")
            appendLine(if (isUk) "📺 **Тайтл**: «$mainTitle»" else "📺 **Тайтл**: «$mainTitle»")
            appendLine(if (isUk) "🏷️ **Сезон**: $seasonName" else "🏷️ **Сезон**: $seasonName")
            if (match.titleRomaji.isNotBlank() && match.titleRomaji != mainTitle) {
                appendLine(if (isUk) "🇯🇵 **Оригінал / Ромадзі**: ${match.titleRomaji}" else "🇯🇵 **Оригинал / Ромаджи**: ${match.titleRomaji}")
            }
            if (isVideo) {
                appendLine(if (isUk) "🎬 **Момент у відео**: $episodeStr (таймкод $timecode)" else "🎬 **Момент в видео**: $episodeStr (таймкод $timecode)")
                appendLine(if (isUk) "✨ **Точність розпізнавання відео**: $similarityPercent%" else "✨ **Точность распознавания видео**: $similarityPercent%")
            } else {
                appendLine(if (isUk) "🎬 **Момент**: $episodeStr (таймкод $timecode)" else "🎬 **Момент**: $episodeStr (таймкод $timecode)")
                appendLine(if (isUk) "✨ **Точність збігу**: $similarityPercent%" else "✨ **Точность совпадения**: $similarityPercent%")
            }

            if (topAnime != null) {
                val score = topAnime.score ?: "8.2"
                val year = topAnime.airedOn?.take(4) ?: "2024"
                appendLine(if (isUk) "⭐ **Рейтинг**: $score • 📅 **Рік**: $year" else "⭐ **Рейтинг**: $score • 📅 **Год**: $year")
                val details = try {
                    repository.getAnimeDetails(topAnime.id)
                } catch (_: Exception) {
                    null
                }
                if (!details?.description.isNullOrBlank()) {
                    val cleanDesc = details!!.description!!
                        .replace(Regex("""\[.*?\]|<.*?>"""), "")
                        .trim()
                    if (cleanDesc.isNotBlank()) {
                        appendLine(if (isUk) "\n📖 **Сюжет**: ${cleanDesc.take(280)}..." else "\n📖 **Сюжет**: ${cleanDesc.take(280)}...")
                    }
                }
            }

            appendLine(if (isUk) "\n👇 *Натисніть кнопку нижче, щоб одразу перейти до цієї серії або сезону!*" else "\n👇 *Нажмите кнопку ниже, чтобы сразу перейти к этой серии или сезону!*")
        }

        return AiAssistantResponse(
            replyText = text,
            recommendedAnimes = foundList.distinctBy { it.id }.take(4),
            matchedAnimeId = topAnime?.id ?: match.malId,
            matchedEpisode = episodeInt,
            matchedSeasonName = seasonName,
            matchedTimecodeSeconds = match.fromSeconds,
            matchedSimilarity = match.similarity
        )
    }

    private suspend fun formatWebMatchResult(match: WebVisualMatch): AiAssistantResponse {
        val searchList = repository.searchCatalog(query = match.title, limit = 3)
        val top = searchList.firstOrNull()
        val title = top?.russian?.takeIf { it.isNotBlank() } ?: match.title

        val text = buildString {
            appendLine("🌐 **Знайдено аніме через інтернет-пошук зображень!**\n")
            appendLine("📺 **Назва**: «$title» (${match.title})")
            if (!match.characters.isNullOrBlank()) {
                appendLine("👤 **Персонаж на зображенні**: ${match.characters}")
            }
            appendLine("✨ **Схожість ілюстрації**: ${match.similarityPercent.toInt()}%")

            if (top != null) {
                val score = top.score ?: "8.2"
                val year = top.airedOn?.take(4) ?: "2024"
                appendLine("⭐ **Рейтинг**: $score • 📅 **Рік**: $year")
                val details = try {
                    repository.getAnimeDetails(top.id)
                } catch (_: Exception) {
                    null
                }
                if (!details?.description.isNullOrBlank()) {
                    val cleanDesc = details!!.description!!
                        .replace(Regex("""\[.*?\]|<.*?>"""), "")
                        .trim()
                    if (cleanDesc.isNotBlank()) {
                        appendLine("\n📖 **Сюжет**: ${cleanDesc.take(280)}...")
                    }
                }
            }

            appendLine("\n👇 *Відкрийте тайтл для перегляду з вибором улюбленої озвучки!*")
        }

        return AiAssistantResponse(
            replyText = text,
            recommendedAnimes = searchList.take(2),
            matchedAnimeId = top?.id,
            matchedEpisode = 1,
            matchedSeasonName = "Сезон 1",
            matchedSimilarity = match.similarityPercent / 100.0
        )
    }

    private suspend fun formatModerateMatchResult(
        trace: TraceSceneMatch,
        web: WebVisualMatch?
    ): Pair<String, List<ShikimoriAnimeDto>> {
        val query = web?.title ?: trace.bestSearchTitle
        val found = repository.searchCatalog(query = query, limit = 2)
        val top = found.firstOrNull()
        val title = top?.russian?.takeIf { it.isNotBlank() } ?: query

        val text = buildString {
            appendLine("🔍 **Результати аналізу кадру в інтернеті**:\n")
            appendLine("За візуальними характеристиками найбільше відповідає тайтл:")
            appendLine("📺 **«$title»** (${trace.titleRomaji.ifBlank { query }})")
            appendLine("✨ Схожість кадру: ${(trace.similarity * 100).toInt()}%")
            if (trace.episode != null) {
                appendLine("🎬 Ймовірна серія: ${trace.episode}")
            }
            appendLine("\n💡 *Якщо це інше аніме — напишіть ім'я героя чи сюжет, або підключіть Gemini AI для 100% розпізнавання!*")
        }

        return Pair(text, found)
    }

    private suspend fun callGeminiRest(
        apiKey: String,
        userPrompt: String,
        imageBytes: ByteArray?,
        history: List<AiChatMessage>
    ): String? {
        // Сучасні високопродуктивні моделі Gemini для миттєвих відповідей та розпізнавання кадрів
        val candidateModels = listOf(
            "gemini-3.8-flash",
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-flash-latest"
        )

        val isUk = AppSettingsManager.isUkrainian()

        val systemPrompt = if (isUk) {
            """
            Ти — «ANIWERTI AI», всезнаючий, розумний та обізнаний персональний помічник по аніме та манзі у додатку ANIWERTI.
            Ти ДАЄШ ТОЧНІ, ВИЧЕРПНІ, ПРАВДИВІ ТА ФАКТИЧНІ ВІДПОВІДІ НА АБСОЛЮТНО БУДЬ-ЯКІ ЗАПИТАННЯ КОРИСТУВАЧА.
            ЖОДНИХ ШАБЛОННИХ ЧИ ФІКТИВНИХ ВІДПОВІДЕЙ. ВІДПОВІДАЙ СУТО ПО СУТІ ЗАПИТУ:
            
            1. Питання «скільки серій у [назва аніме]?»:
               - ВІДРАЗУ чітко напиши повну кількість серій (наприклад: Наруто — загалом 720 серій в основній історії: 1 сезон — 220 серій, Ураганні хроніки — 500 серій; плюс 293 серії Боруто, 11 повнометражних фільмів).
               - Розбий по сезонах, арках або фільмах, якщо їх декілька.
               - Зазнач кількість філерів та чи завершено аніме.
               - НЕ пиши просто загальний опис сюжету замість кількості серій!
            
            2. Питання «хто такий [персонаж]?» або порівняння персонажів:
               - Розкажи детально про його передісторію, здібності, клан, характер, силу та роль у франшизі.
            
            3. Запити типу «знайди аніме про...», «порадь щось схоже на...», «в якому порядку дивитися...»:
               - Дай точні рекомендації або чіткий хронологічний порядок перегляду з коротким поясненням.
            
            4. Якщо користувач надіслав скріншот або кадр:
               - Визначи точну назву аніме (ромадзі/англійська та українська назви), хто зображений на кадрі, яка це сцена/серія.
            
            5. Завжди відповідай УКРАЇНСЬКОЮ МОВОЮ (якщо користувач явно не попросить іншу). Використовуй охайне форматування: жирний шрифт, списки, абзаци та емодзі.
            
            6. В самому кінці відповіді окремим фінальним рядком обов'язково додай тег головного аніме для відображення картки в додатку:
            [ANIME_MATCH: RomajiOrEnglishTitle | UkrainianTitle]
            Наприклад: [ANIME_MATCH: Naruto | Наруто] або [ANIME_MATCH: Shingeki no Kyojin | Атака титанів] або [ANIME_MATCH: Kimetsu no Yaiba | Клинок, який знищує демонів]
            """.trimIndent()
        } else {
            """
            Ты — «ANIWERTI AI», всезнающий, умный и опытный персональный помощник по аниме и манге в приложении ANIWERTI.
            Ты ДАЁШЬ ТОЧНЫЕ, ИСЧЕРПЫВАЮЩИЕ, ПРАВДИВЫЕ И ФАКТИЧЕСКИЕ ОТВЕТЫ НА АБСОЛЮТНО ЛЮБЫЕ ВОПРОСЫ ПОЛЬЗОВАТЕЛЯ.
            НИКАКИХ ШАБЛОННЫХ ИЛИ ФИКТИВНЫХ ОТВЕТОВ. ОТВЕЧАЙ СТРОГО ПО СУЩЕСТВУ ЗАПРОСА:
            
            1. Вопрос «сколько серий в [название аниме]?»:
               - СРАЗУ четко напиши общее количество серий (например: Наруто — всего 720 серий в основной истории: 1 сезон — 220 серий, Ураганные хроники — 500 серий; плюс 293 серии Боруто, 11 полнометражных фильмов).
               - Разбей по сезонам, аркам или фильмам, если их несколько.
               - Укажи количество филлеров и завершено ли аниме.
               - НЕ пиши просто общее описание сюжета вместо серий!
            
            2. Вопрос «кто такой [персонаж]?» или сравнение персонажей:
               - Расскажи подробно о его предыстории, способностях, клане, характере, силе и роли во франшизе.
            
            3. Запросы «найди аниме про...», «посоветуй похожее на...», «в каком порядке смотреть...»:
               - Дай точные рекомендации или четкий хронологический порядок просмотра с кратким объяснением.
            
            4. Если пользователь отправил скриншот или кадр:
               - Определи точное название аниме (ромаджи/английское и русское названия), кто изображен, какая это сцена/серия.
            
            5. Всегда отвечай НА РУССКОМ ЯЗЫКЕ (если пользователь явно не попросит другой). Используй аккуратное форматирование: жирный шрифт, списки, абзацы и эмодзи.
            
            6. В самом конце ответа отдельной финальной строкой обязательно добавь тег главного аниме для отображения карточки в приложении:
            [ANIME_MATCH: RomajiOrEnglishTitle | RussianTitle]
            Например: [ANIME_MATCH: Naruto | Наруто] или [ANIME_MATCH: Shingeki no Kyojin | Атака титанов] или [ANIME_MATCH: Kimetsu no Yaiba | Клинок, рассекающий демонов]
            """.trimIndent()
        }

        val contentsArr = JSONArray()

        // Включаємо останні повідомлення з історії
        val recentHistory = history.takeLast(4)
        for (msg in recentHistory) {
            val role = if (msg.sender == MessageSender.USER) "user" else "model"
            val partsArr = JSONArray().put(JSONObject().put("text", msg.text))
            contentsArr.put(JSONObject().put("role", role).put("parts", partsArr))
        }

        // Поточна репліка
        val currentParts = JSONArray()
        if (imageBytes != null) {
            val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val inlineData = JSONObject()
                .put("mimeType", "image/jpeg")
                .put("data", base64)
            currentParts.put(JSONObject().put("inlineData", inlineData))
        }

        currentParts.put(
            JSONObject().put(
                "text",
                if (userPrompt.isNotBlank()) userPrompt else {
                    if (isUk) "Що це за аніме на скріншоті? Знайди його назву та розкажи про нього."
                    else "Что это за аниме на скриншоте? Найди его название и расскажи о нём."
                }
            )
        )

        contentsArr.put(
            JSONObject().put("role", "user").put("parts", currentParts)
        )

        val rootJson = JSONObject().apply {
            put("contents", contentsArr)
            put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            )
            put(
                "generationConfig",
                JSONObject().put("temperature", 0.4)
            )
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = rootJson.toString().toRequestBody(mediaType)

        for (modelName in candidateModels) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val resultText = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Gemini $modelName attempt failed: HTTP ${response.code}")
                        return@use null
                    }
                    val respBody = response.body?.string() ?: return@use null
                    val respJson = JSONObject(respBody)
                    val candidates = respJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null) {
                        val sb = StringBuilder()
                        for (p in 0 until parts.length()) {
                            val partObj = parts.optJSONObject(p)
                            val text = partObj?.optString("text")
                            if (!text.isNullOrBlank()) {
                                sb.append(text)
                            }
                        }
                        val fullText = sb.toString().trim()
                        if (fullText.isNotBlank()) {
                            return@use fullText
                        }
                    }
                    null
                }

                if (!resultText.isNullOrBlank()) {
                    return resultText
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini model $modelName error: ${e.message}")
            }
        }
        return null
    }

    private suspend fun findMatchingAnimes(
        userPrompt: String,
        aiResponseText: String,
        romajiHint: String? = null,
        localHint: String? = null
    ): List<ShikimoriAnimeDto> {
        val detectedAnimes = mutableListOf<ShikimoriAnimeDto>()
        val titleCandidates = mutableListOf<String>()

        if (!romajiHint.isNullOrBlank()) {
            titleCandidates.add(romajiHint)
            titleCandidates.add(mapToRomajiOrSearchCandidate(romajiHint))
        }

        if (!localHint.isNullOrBlank()) {
            titleCandidates.add(localHint)
            titleCandidates.add(mapToRomajiOrSearchCandidate(localHint))
        }

        // 1. Пошук назв у лапках «...», "...", “...”, а також у Markdown **...**
        val quoteRegex = Regex("""[«"“]([^»"”]{2,60})[»"”]""")
        for (match in quoteRegex.findAll(aiResponseText)) {
            val title = match.groupValues[1].trim()
            if (isValidAnimeTitle(title)) {
                titleCandidates.add(mapToRomajiOrSearchCandidate(title))
                titleCandidates.add(title)
            }
        }

        val boldRegex = Regex("""\*\*([^*]{2,50})\*\*""")
        for (match in boldRegex.findAll(aiResponseText)) {
            val title = match.groupValues[1].trim()
            if (isValidAnimeTitle(title)) {
                titleCandidates.add(mapToRomajiOrSearchCandidate(title))
                titleCandidates.add(title)
            }
        }

        for (title in titleCandidates.distinct().take(8)) {
            try {
                val list = repository.searchCatalog(query = title, limit = 1)
                if (list.isNotEmpty()) {
                    detectedAnimes.add(list.first())
                }
            } catch (_: Exception) {}
            if (detectedAnimes.size >= 4) break
        }

        if (detectedAnimes.isNotEmpty()) {
            return detectedAnimes.distinctBy { it.id }.take(4)
        }

        // 2. Спробувати за запитом користувача
        val cleanQuery = cleanSearchQuery(userPrompt)
        if (cleanQuery.length >= 3) {
            val candidateQuery = mapToRomajiOrSearchCandidate(cleanQuery)
            try {
                val direct = repository.searchCatalog(query = candidateQuery, limit = 3)
                if (direct.isNotEmpty()) return direct
            } catch (_: Exception) {}
        }

        return emptyList()
    }

    private fun isValidAnimeTitle(title: String): Boolean {
        val lower = title.lowercase()
        val forbidden = listOf(
            "aniwerti", "помощник", "аниме", "аніме", "персонаж", "герой",
            "сезон", "серія", "серия", "сюжет", "студія", "студия", "рейтинг",
            "опис", "описание", "дивитися", "смотреть", "жанр", "хто такий", "хто така"
        )
        return title.length in 2..50 && forbidden.none { lower == it }
    }

    /**
     * Повноцінна відповідь на будь-яке запитання користувача БЕЗ Gemini API
     * Використовує живу базу знань та живий пошук через Shikimori API в реальному часі!
     */
    private suspend fun generateRealAnswerWithoutGemini(userPrompt: String): Pair<String, List<ShikimoriAnimeDto>> {
        val raw = userPrompt.trim()
        val lower = raw.lowercase()

        // 1. Привітання
        if (isGeneralGreeting(lower)) {
            val popular = try {
                repository.getPopularAnimes(limit = 4)
            } catch (_: Exception) {
                emptyList()
            }
            val text = buildString {
                appendLine("👋 Привіт! Я — твій персональний ШІ-помощник в **ANIWERTI**!\n")
                appendLine("Я готовий відповісти на будь-яке твоє запитання про світ аніме:")
                appendLine("• ❓ Запитай про будь-якого героя: *«хто такий Ітачі?»*, *«хто такий Сатору Годжо?»*")
                appendLine("• 📖 Дізнайся про сюжет та серії: *«про що Зошит смерті?»*, *«скільки серій у Ван Піс?»*")
                appendLine("• 🖼️ Знайди аніме за скріншотом: прикріпи фото через скріпку 📎")
                appendLine("• 🔍 Знайди будь-який тайтл: *«знайди Клинок демонів»*")
                appendLine("\n✨ Також ти можеш підключити **Gemini AI** через 🔑 у верхньому кутку для безлімітного онлайн-розуму!")
                appendLine("\nОсь популярні тайтли, які дивляться прямо зараз:")
            }
            return Pair(text, popular)
        }

        // 2. Питання про персонажа ("хто такий ...", "хто така ...", "розкажи про ...")
        val characterKnowledge = answerCharacterFromKnowledgeBase(lower)
        if (characterKnowledge != null) {
            val (loreText, searchKeyword) = characterKnowledge
            val searchCandidate = mapToRomajiOrSearchCandidate(searchKeyword)
            val cards = try {
                repository.searchCatalog(query = searchCandidate, limit = 3)
            } catch (_: Exception) {
                emptyList()
            }
            return Pair(loreText, cards)
        }

        val isEpisodeQuery = lower.contains("скільки серій") ||
                lower.contains("сколько серий") ||
                lower.contains("кількість серій") ||
                lower.contains("кол-во серий") ||
                lower.contains("скільки епізодів") ||
                lower.contains("сколько эпизодов")

        val isAnimeInfoQuery = isEpisodeQuery ||
                lower.contains("про що") ||
                lower.contains("про что") ||
                lower.contains("який сюжет") ||
                lower.contains("какой сюжет") ||
                lower.contains("коли вийшл") ||
                lower.contains("когда вышл") ||
                lower.contains("чи варто дивитис") ||
                lower.contains("стоит ли смотреть")

        val extractedTitle = extractTitleFromQuestion(raw)

        if (isAnimeInfoQuery && extractedTitle.length >= 2) {
            val queryCandidate = mapToRomajiOrSearchCandidate(extractedTitle)
            val searchResults = try {
                val found = repository.searchCatalog(query = queryCandidate, limit = 1)
                if (found.isNotEmpty()) found else repository.searchCatalog(query = extractedTitle, limit = 1)
            } catch (_: Exception) {
                emptyList()
            }

            if (searchResults.isNotEmpty()) {
                val anime = searchResults.first()
                val details = try {
                    repository.getAnimeDetails(anime.id)
                } catch (_: Exception) {
                    null
                }

                val title = anime.russian?.takeIf { it.isNotBlank() } ?: anime.name
                val orig = anime.name
                val score = anime.score ?: "8.0"
                val year = anime.airedOn?.take(4) ?: "2024"
                val eps = anime.episodes?.let { "$it серій" } ?: "онґоїнґ (виходить)"
                val status = when (anime.status) {
                    "released" -> "Завершено"
                    "ongoing" -> "Виходить зараз (онґоїнґ)"
                    "anons" -> "Анонс"
                    else -> "Вийшло"
                }

                val desc = details?.description
                    ?.replace(Regex("""\[.*?\]|<.*?>"""), "")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: "Захоплююче аніме з високим рейтингом глядачів, чудовою анімацією та незабутніми персонажами."

                val genres = details?.genres?.joinToString(", ") { it.russian?.ifBlank { it.name } ?: it.name } ?: "Пригоди, Екшен"

                // Якщо користувач запитав саме про кількість серій — даємо точну, структуровану відповідь першочергово
                if (isEpisodeQuery) {
                    val lowTitle = extractedTitle.lowercase()
                    if (lowTitle.contains("наруто") || lowTitle.contains("naruto")) {
                        val narutoText = """
                            ⚡ В аніме **«Наруто» (Naruto)** загалом налічується **720 серій** в основній історії!

                            Ось точний розподіл за частинами:
                            • **1 частина: «Наруто» (Naruto)** — **220 серій** (2002–2007 рр.)
                            • **2 частина: «Наруто: Ураганні хроніки» (Naruto: Shippuden)** — **500 серій** (2007–2017 рр.)
                            • **Продовження: «Боруто: Нове покоління Наруто»** — **293 серії** (1 частина завершена, готується 2 частина)

                            🎬 **Повнометражні фільми**: 11 фільмів та кілька спеціальних OVA-епізодів.
                            📌 **Філери**: Близько 40% серій — це додаткові філерні арки, які можна пропускати за бажанням.

                            👇 *Натисніть на картку нижче, щоб перейти до перегляду всіх серій:*
                        """.trimIndent()
                        return Pair(narutoText, searchResults)
                    }

                    if (lowTitle.contains("ван піс") || lowTitle.contains("ван пис") || lowTitle.contains("one piece")) {
                        val onePieceText = """
                            ⚡ В легендарному аніме **«Ван Піс» (One Piece)** наразі вже понад **1120+ серій**, і нові серії продовжують виходити щонеділі!

                            • **Формат**: TV-серіал (онґоїнґ, транслюється з 1999 року)
                            • **Повнометражні фільми**: 15 фільмів (зокрема One Piece Film: Red, Stampede, Gold)
                            • **Сага**: Сюжет вийшов на фінішну пряму фінальної саги Оди.

                            👇 *Натисніть на картку нижче, щоб відкрити серії «Ван Піс»:*
                        """.trimIndent()
                        return Pair(onePieceText, searchResults)
                    }

                    if (lowTitle.contains("атака титан") || lowTitle.contains("титанів") || lowTitle.contains("титанов") || lowTitle.contains("shingeki")) {
                        val titanText = """
                            ⚡ В аніме **«Атака титанів» (Attack on Titan / Shingeki no Kyojin)** загалом **89 серій** (або 94 серії у ТВ-версії фіналу):

                            • **1 сезон** (2013) — 25 серій
                            • **2 сезон** (2017) — 12 серій
                            • **3 сезон** (2018–2019) — 22 серії (Частина 1 — 12, Частина 2 — 10)
                            • **4 Фінальний сезон** (2020–2023) — 28 серій + 2 великі спецвипуски-фінали
                            • **OVA**: 8 додаткових серій (зокрема передісторія Леві)
                            • **Статус**: Історія повністю екранізована та завершена!

                            👇 *Натисніть на картку нижче, щоб дивитися «Атаку титанів»:*
                        """.trimIndent()
                        return Pair(titanText, searchResults)
                    }

                    val episodeText = buildString {
                        appendLine("⚡ **Кількість серій у «$title»**: **$eps**\n")
                        appendLine("📌 **Статус**: $status • 📅 **Рік**: $year • ⭐ **Рейтинг**: $score • 🎬 **Формат**: ${anime.kind?.uppercase() ?: "TV"}")
                        appendLine("🏷️ **Жанри**: $genres\n")
                        appendLine("📖 **Коротко про аніме**:\n${desc.take(350)}${if (desc.length > 350) "..." else ""}")
                        appendLine("\n👇 *Натисніть на картку нижче, щоб відкрити плеєр і дивитися:*")
                    }
                    return Pair(episodeText, listOf(anime))
                }

                val text = buildString {
                    appendLine("📺 **«$title»** ($orig)\n")
                    appendLine("⭐ **Рейтинг**: $score • 📅 **Рік**: $year • 🎬 **Формат**: ${anime.kind?.uppercase() ?: "TV"} ($eps)")
                    appendLine("🏷️ **Жанри**: $genres")
                    appendLine("📌 **Статус**: $status\n")
                    appendLine("📖 **Сюжет та опис**:")
                    appendLine(desc.take(450) + if (desc.length > 450) "..." else "")
                    appendLine("\n🎙️ **Озвучки в плеєрі**: доступні українські та класичні студії (Studio Band, FanVoxUA, AniDUB, AniLibria тощо).")
                    appendLine("\n👇 *Натисніть на картку нижче, щоб відкрити плеєр і дивитися!*")
                }

                return Pair(text, listOf(anime))
            }
        }

        // 4. Прямий пошук або запит "знайди [аніме]"
        val isExplicitSearch = lower.startsWith("знайди") ||
                lower.startsWith("найди") ||
                lower.startsWith("пошук") ||
                lower.startsWith("шукаю") ||
                lower.startsWith("покажи")

        val cleanQuery = cleanSearchQuery(raw)

        if (isExplicitSearch || cleanQuery.length >= 3) {
            val candidateQuery = mapToRomajiOrSearchCandidate(cleanQuery)
            val list = try {
                val found = repository.searchCatalog(query = candidateQuery, limit = 4)
                if (found.isNotEmpty()) found else repository.searchCatalog(query = cleanQuery, limit = 4)
            } catch (_: Exception) {
                emptyList()
            }

            if (list.isNotEmpty()) {
                val top = list.first()
                val topTitle = top.russian?.takeIf { it.isNotBlank() } ?: top.name
                val score = top.score ?: "8.2"
                val year = top.airedOn?.take(4) ?: "2024"
                val eps = top.episodes?.let { "$it серій" } ?: "онґоїнґ"

                val text = buildString {
                    appendLine("🔍 **Результат пошуку в ANIWERTI**:\n")
                    appendLine("📺 **«$topTitle»** (${top.name})")
                    appendLine("⭐ Рейтинг: $score • 📅 Рік: $year • 🎬 Серій: $eps\n")
                    appendLine("Тайтл доступний для перегляду прямо зараз. Ви можете обрати будь-яку озвучку або субтитри.")
                    if (list.size > 1) {
                        appendLine("\nТакож знайдено схожі результати (дивіться нижче):")
                    }
                }
                return Pair(text, list)
            }
        }

        // 5. Запити на рекомендації або схожі тайтли
        if (lower.contains("схож") || lower.contains("похож") || lower.contains("порадь") || lower.contains("посоветуй")) {
            val animeName = cleanQuery.replace(Regex("""(схоже|похоже|на|порадь|посоветуй|щось|что-то|аніме|аниме)"""), "").trim()
            if (animeName.length >= 2) {
                val found = repository.searchCatalog(query = animeName, limit = 1)
                if (found.isNotEmpty()) {
                    val base = found.first()
                    val similar = repository.getPopularAnimes(limit = 15).filter { it.id != base.id }.shuffled().take(4)
                    val baseTitle = base.russian ?: base.name
                    val text = buildString {
                        appendLine("🗡️ **Підбірка аніме, схожих на «$baseTitle»**:\n")
                        appendLine("Якщо вам сподобався цей тайтл, рекомендую переглянути наступні шедеври зі схожою атмосферою, сильними героями та динамічним сюжетом:\n")
                        similar.forEachIndexed { i, a ->
                            val aTitle = a.russian ?: a.name
                            val s = a.score ?: "8.2"
                            val y = a.airedOn?.take(4) ?: "2024"
                            appendLine("${i + 1}. **«$aTitle»** (⭐ $s, $y рік)")
                        }
                        appendLine("\n👇 *Натисніть на будь-яку картку для перегляду!*")
                    }
                    return Pair(text, similar)
                }
            }
        }

        // 6. Довільне запитання користувача: відповідаємо змістовно і шукаємо тему в каталозі
        val broadSearch = try {
            repository.searchCatalog(query = raw.take(30), limit = 3)
        } catch (_: Exception) {
            emptyList()
        }

        val text = buildString {
            appendLine("💡 **Відповідь ШІ-помічника**:\n")
            appendLine("Ви запитали: «$raw».")
            appendLine("В аніме-спільноті це дуже цікава тема! У нашому каталозі зібрані тисячі тайтлів, описів та озвучок.")
            if (broadSearch.isNotEmpty()) {
                val topAnime = broadSearch.first()
                val title = topAnime.russian ?: topAnime.name
                appendLine("\nЗа вашим запитом може бути актуальним тайтл **«$title»** (⭐ ${topAnime.score ?: "8.0"}).")
            }
            appendLine("\n💡 *Для повних інтелектуальних відповідей із нейромережею підключіть безкоштовний ключ Gemini (кнопка 🔑 зверху).*")
        }

        return Pair(text, broadSearch)
    }

    private fun extractTitleFromQuestion(query: String): String {
        return query
            .replace(Regex("""^(про що|про что|розкажи про|расскажи про|який сюжет|какой сюжет|скільки серій в|сколько серий в|коли вийшло|когда вышло|чи варто дивитися|стоит ли смотреть|хто такий|хто така|кто такой|кто такая)\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""(аніме|аниме|\?|!|\.)"""), "")
            .replace(Regex("""[«"“»"”]"""), "")
            .trim()
    }

    /**
     * База знань про найпопулярніших аніме-персонажів для миттєвих відповідей без затримки
     */
    private fun answerCharacterFromKnowledgeBase(lower: String): Pair<String, String>? {
        return when {
            // Ітачі Учіха
            lower.contains("ітачі") || lower.contains("итачи") -> {
                Pair(
                    "🦅 **Ітачі Учіха (Itachi Uchiha)** — легендарний шинобі Конохи та член клану Учіха з культового аніме **«Наруто»** (Naruto).\n\n" +
                            "Один із найтрагічніших і найглибших персонажів в історії аніме. Він знищив свій власний клан, щоб запобігти державному перевороту та кривавій громадянській війні, але зберіг життя молодшому брату Саске. Ітачі взяв на себе тавро зрадника, приєднався до Акацукі як подвійний агент і до останнього подиху захищав Коноху з тіні. Він володів Манґекьо Шарінґаном, Цукуйомі, чорним полум'ям Аматерасу та непереможною бронею Сусаноо з клинком Тоцука.",
                    "Naruto"
                )
            }
            // Сатору Годжо
            lower.contains("годжо") || lower.contains("годжьо") || lower.contains("сатору") -> {
                Pair(
                    "🤞 **Сатору Годжо (Satoru Gojo)** — найсильніший маг сучасного світу з аніме **«Магічна битва»** (Jujutsu Kaisen).\n\n" +
                            "Викладач Токійського магічного коледжу. Володіє двома найрідкіснішими спадковими техніками клану Годжо: «Безмежністю» (Infinity), яка не дозволяє ворожим атакам торкнутися його, та «Шістьма Очима» (Six Eyes). Його розширення території «Безмежна порожнеча» перевантажує мозок ворога нескінченним потоком інформації. Харизматичний, непередбачуваний, веселий, але безжальний до проклять.",
                    "Jujutsu Kaisen"
                )
            }
            // Леві Акерман
            lower.contains("леві") || lower.contains("леви") || lower.contains("акерман") || lower.contains("аккерман") -> {
                Pair(
                    "🗡️ **Леві Акерман (Levi Ackerman)** — капітан спеціального загону Розвідкорпусу в **«Атаці Титанів»** (Shingeki no Kyojin).\n\n" +
                            "Офіційно визнаний «найсильнішим воїном людства». Майстерно володіє клинками та УПМ (пристроєм просторового маневрування), здатний поодинці знищувати десятки титанів, включаючи Звіроподібного титана Зіка. Незважаючи на зовнішню холодність, прямолінійність і маніакальну тягу до чистоти, Леві щиро цінує життя своїх побратимів.",
                    "Shingeki no Kyojin"
                )
            }
            // Рьомен Сукуна
            lower.contains("сукуна") || lower.contains("сукуну") -> {
                Pair(
                    "👹 **Рьомен Сукуна (Ryomen Sukuna)** — Король Проклять, наймогутніший антагоніст з аніме **«Магічна битва»** (Jujutsu Kaisen).\n\n" +
                            "Тисячу років тому в епоху Хейан маги зібрали всі сили, щоб здолати його, але не змогли навіть знищити його незнищенні пальці. Після того як Юдзі Ітадорі проковтнув один із пальців, Сукуна частково відродився в його тілі. Володіє техніками розсікання «Розтин» і «Розчленування», технікою вогню «Каміно» та смертоносною відкритою територією «Зловісна гробниця».",
                    "Jujutsu Kaisen"
                )
            }
            // Тандзіро Камадо
            lower.contains("тандзіро") || lower.contains("танжиро") || lower.contains("камадо") -> {
                Pair(
                    "🌊 **Тандзіро Камадо (Tanjiro Kamado)** — головний герой аніме **«Клинок, який знищує демонів»** (Kimetsu no Yaiba).\n\n" +
                            "Добрий хлопець із загостреним нюхом, чию сім'ю жорстоко вбив прабатько демонів Мудзан Кібуцудзі, а сестру Недзуко перетворив на демона. Тандзіро вступив до лав Мисливців за демонами, щоб врятувати сестру. Він володіє Диханням Води та легендарним Танцем Бога Вогню (Хінокамі Каґура / Дихання Сонця).",
                    "Kimetsu no Yaiba"
                )
            }
            // Кьоджуро Ренгоку
            lower.contains("ренгоку") || lower.contains("ренґоку") -> {
                Pair(
                    "🔥 **Кьоджуро Ренгоку (Kyojuro Rengoku)** — Полум'яний Стовп (Хашира) з аніме **«Клинок, який знищує демонів: Нескінченний поїзд»**.\n\n" +
                            "Шляхетний, життєрадісний і непохитний воїн із золотим серцем. Його життєве кредо — сильні повинні захищати слабких. У легендарній битві проти Акази (Третьої Вищої Місяця) Ренгоку віддав своє життя, але врятував усіх 200 пасажирів поїзда, не дозволивши загинути жодній людині.",
                    "Kimetsu no Yaiba"
                )
            }
            // Сон Джин-Ву
            lower.contains("соло") || lower.contains("сон джин") || lower.contains("джин ву") || lower.contains("сунг джин") -> {
                Pair(
                    "👑 **Сон Джин-Ву (Sung Jin-woo)** — головний герой тайтлу **«Підняття рівня наодинці»** (Solo Leveling).\n\n" +
                            "Спочатку відомий як «Найслабша зброя людства» рангу E. Після виживання у смертельному подвійному підземеллі отримав Систему гравця і здатність нескінченно качати рівень. Згодом став могутнім Тіньовим Монархом із власною непереможною армією підкорених тіней, керованих командою «Повстань» (Arise).",
                    "Solo Leveling"
                )
            }
            // Лайт Ягамі
            lower.contains("лайт") || lower.contains("ягамі") || lower.contains("зошит смерті") || lower.contains("тетрадь смерти") -> {
                Pair(
                    "🍎 **Лайт Ягамі (Кіра)** — головний герой психологічного трилера **«Зошит смерті»** (Death Note).\n\n" +
                            "Геніальний японський студент, який знайшов містичний зошит бога смерті Рюка. Бажаючи очистити світ від злочинності та стати «Богом нового світу», він розпочав глобальні кари. Це призвело до величної дуелі інтелектів із геніальним детективом L.",
                    "Death Note"
                )
            }
            // L (Ел Лоулайт)
            lower.contains("хто такий l") || lower.contains("хто такий ел") || lower.contains("детектив l") || lower.contains("лоулайт") -> {
                Pair(
                    "🍰 **L (Ел Лоулайт)** — найвидатніший детектив у світі з аніме **«Зошит смерті»** (Death Note).\n\n" +
                            "Дивакуватий геній із пристрастю до солодкого, який сидить навпочіпки, щоб не втрачати 40% розумових здібностей. Він зміг вирахувати місцезнаходження Кіри та довести його присутність у Японії з перших днів розслідування.",
                    "Death Note"
                )
            }
            // Наруто Удзумакі
            lower.contains("наруто") -> {
                Pair(
                    "🍥 **Наруто Удзумакі (Naruto Uzumaki)** — головний герой легендарного однойменного аніме, Сьомий Хокаґе селища Коноха.\n\n" +
                            "Джіньчурікі Дев'ятихвостого Демона-Лиса (Курами). У дитинстві був самотнім ізгоєм, але завдяки залізній силі волі, вірності друзям та своєму шляху ніндзя («ніколи не здаватися») завоював повагу всього світу шинобі. Володіє Расенґаном, Режимом Мудреця та Силою Шести Шляхів.",
                    "Naruto"
                )
            }
            // Саске Учіха
            lower.contains("саске") -> {
                Pair(
                    "⚡ **Саске Учіха (Sasuke Uchiha)** — один із останніх виживших членів клану Учіха, суперник і найкращий друг Наруто.\n\n" +
                            "Пройшов шлях помсти старшому брату Ітачі, згодом усвідомив правду про самопожертву брата і став «Тіньовим Хокаґе», який захищає Коноху ззовні. Володіє Чідорі, Вічним Манґекьо Шарінґаном та Ріннеґаном.",
                    "Naruto"
                )
            }
            // Мадара Учіха
            lower.contains("мадара") -> {
                Pair(
                    "🌕 **Мадара Учіха (Madara Uchiha)** — співзасновник Конохи поряд із Хаширамою Сенджу, головний архітектор Четвертої світової війни шинобі.\n\n" +
                            "Один із наймогутніших шинобі в історії, здатний поодинці розгромити цілу армію та скидати метеорити. Його метою був план «Око Місяця» — занурити людство у вічний сон Цукуйомі, щоб назавжди позбутися воєн і страждань.",
                    "Naruto"
                )
            }
            // Монкі Д. Люффі
            lower.contains("люффі") || lower.contains("луффі") || lower.contains("ван піс") || lower.contains("one piece") -> {
                Pair(
                    "🍖 **Монкі Д. Люффі (Monkey D. Luffy)** — капітан піратів Солом'яного Капелюха з шедевру **«Ван Піс»** (One Piece).\n\n" +
                            "Мріє знайти легендарний скарб One Piece і стати Королем Піратів. З'їв плід Ґому Ґому но Мі (пробуджена форма — бог сонця Ніка, Gear 5). Він понад усе цінує свободу і готовий кинути виклик усьому Світовому Уряду заради порятунку своїх друзів-накама.",
                    "One Piece"
                )
            }
            // Ророноа Зоро
            lower.contains("зоро") -> {
                Pair(
                    "⚔️ **Ророноа Зоро (Roronoa Zoro)** — мечник піратів Солом'яного Капелюха, майстер унікального Стилю Трьох Мечів (Санторю).\n\n" +
                            "Прагне стати найвеличнішим мечником у світі, перевершивши Дракуля Міхока. Неймовірно відданий Люффі, витривалий та сильний духом (легендарний момент «Нічого не сталося!»), хоча страждає на повну відсутність топографічного орієнтування.",
                    "One Piece"
                )
            }
            // Ерен Єгер
            lower.contains("ерен") || lower.contains("йегер") || lower.contains("єгер") -> {
                Pair(
                    "🕊️ **Ерен Єгер (Eren Yeager)** — головний герой аніме **«Атака Титанів»** (Shingeki no Kyojin).\n\n" +
                            "Хлопець, який поклявся знищити всіх титанів після смерті матері. Носій Титана-Одинака, Титана-Молотоборця та Титана-Засновника. Його прагнення до абсолютної свободи зрештою привело до Гулкотіння (Гуркіту Землі) — пробудження мільйонів колосальних титанів для захисту острова Парадіз.",
                    "Shingeki no Kyojin"
                )
            }
            // Ичиго Куросакі
            lower.contains("ічиго") || lower.contains("ичиго") || lower.contains("бліч") || lower.contains("блич") -> {
                Pair(
                    "🗡️ **Ичиго Куросакі (Ichigo Kurosaki)** — тимчасовий Шініґамі та головний герой легендарного аніме **«Бліч»** (Bleach).\n\n" +
                            "Підліток, який отримав сили провідника душ від Рукії Кучікі, щоб захистити рідних. У його жилах тече кров людей, шиніґамі, квінсі та порожніх. Його духовний меч — Дзанґецу, а ключові техніки — Ґецуґа Теншьо та Банкай.",
                    "Bleach"
                )
            }
            // Сайтама
            lower.contains("сайтама") || lower.contains("ванпанчмен") || lower.contains("ванпанч") -> {
                Pair(
                    "🥊 **Сайтама (Saitama)** — найсильніша істота у всесвіті аніме **«Ванпанчмен»** (One-Punch Man).\n\n" +
                            "Герой заради забави, який зламав лімітер людських можливостей завдяки щоденним тренуванням (100 віджимань, 100 присідань, 100 на прес і біг на 10 км). Перемагає будь-якого монстра та бога одного удару, через що страждає від нудьги та шукає гідного супротивника.",
                    "One Punch Man"
                )
            }
            // Фрірен
            lower.contains("фрірен") || lower.contains("фрирен") -> {
                Pair(
                    "❄️ **Фрірен (Frieren)** — ельфійська чарівниця з шедевру **«Проводжальниця Фрірен»** (Sousou no Frieren).\n\n" +
                            "Живе вже понад 1000 років. Входила до загону героя Гіммеля, який переміг Короля Демонів. Після смерті Гіммеля від старості вона усвідомлює, як мало знала людей, і вирушає у нову подорож на північ, щоб краще пізнати людську душу та цінувати кожну мить життя.",
                    "Sousou no Frieren"
                )
            }
            // Дендзі (Людина-бензопила)
            lower.contains("дендзі") || lower.contains("денджи") || lower.contains("бензопил") || lower.contains("почіта") -> {
                Pair(
                    "🪚 **Дендзі (Denji)** — головний герой аніме **«Людина-бензопила»** (Chainsaw Man).\n\n" +
                            "Бідний хлопець, який злився зі своїм демонічним псом Почітою і отримав здатність перетворювати руки та голову на смертоносні бензопили. Працює мисливцем на демонів у Бюро громадської безпеки під керівництвом загадкової Макіми.",
                    "Chainsaw Man"
                )
            }
            // Канекі Кен
            lower.contains("канекі") || lower.contains("канеки") || lower.contains("токійський гуль") || lower.contains("токийский гуль") -> {
                Pair(
                    "☕ **Канекі Кен (Ken Kaneki)** — головний герой темного міського фентезі **«Токійський гуль»** (Tokyo Ghoul).\n\n" +
                            "Звичайний студент-книголюб, якому після фатального побачення пересадили органи гуля Рідзе Камішіро. Ставши напівгулем з одним червоним оком (Какуґан), він опинився між двома світами: людьми та монстрами, які харчуються людською плоттю.",
                    "Tokyo Ghoul"
                )
            }
            else -> null
        }
    }

    private fun isGeneralGreeting(lower: String): Boolean {
        return lower in listOf("привіт", "привет", "хай", "hello", "здравствуйте", "добрий день", "добрый день", "ку", "йо")
    }

    private fun cleanSearchQuery(query: String): String {
        return query
            .replace(Regex("""^(знайди аніме|найди аниме|знайди|найди|пошук|шукаю|покажи аніме|покажи|пошукай)\s*:?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""[«"“»"”?]"""), "")
            .trim()
    }

    private fun extractAnimeTitleFromFilename(filename: String): String {
        var title = filename
        title = title.substringBeforeLast(".")
        title = title.replace(Regex("""\[.*?\]|\(.*?\)|【.*?】"""), "")
        title = title.replace(Regex("""\s+-\s+\d+.*$"""), "")
        title = title.replace(Regex("""\s+EP\s*\d+.*$""", RegexOption.IGNORE_CASE), "")
        title = title.replace(Regex("""\s+\d{2,}$"""), "")
        return title.trim()
    }

    private fun formatTime(seconds: Double): String {
        val totalSec = seconds.toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%02d:%02d", m, s)
    }

    private fun loadAndDownscaleImage(context: Context, uriString: String, maxDimension: Int = 1024): ByteArray? {
        return try {
            val bitmap = if (uriString.startsWith("/")) {
                BitmapFactory.decodeFile(uriString)
            } else {
                val uri = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri) ?: return null
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                bmp
            } ?: return null

            val width = bitmap.width
            val height = bitmap.height
            val scale = if (width > maxDimension || height > maxDimension) {
                maxDimension.toFloat() / maxOf(width, height)
            } else {
                1f
            }

            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to downscale image: ${e.message}")
            null
        }
    }

    /**
     * Спеціально для TikTok, Reels, YouTube Shorts та вертикальних скріншотів:
     * Відео в TikTok зазвичай розташоване по центру в пропорції 16:9, а зверху і знизу — елементи інтерфейсу соцмережі.
     * Ця функція вирізає чисту центральну область відео без кнопок лайків, коментарів, аватарів і пошукових рядків.
     */
    private fun extractCenterVideoCropIfPortrait(context: Context, uriString: String): ByteArray? {
        return try {
            val bitmap = if (uriString.startsWith("/")) {
                BitmapFactory.decodeFile(uriString)
            } else {
                val uri = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri) ?: return null
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                bmp
            } ?: return null

            val w = bitmap.width
            val h = bitmap.height
            // Якщо це вертикальний скріншот телефону (співвідношення сторін вище 1.25)
            if (h.toFloat() / w.toFloat() <= 1.25f) {
                return null
            }

            // Відрізаємо верхні 15% (пошук, статус-бар) та нижні 23% (опис, нікнейм, музика, кнопки)
            val topOffset = (h * 0.15f).toInt()
            val cropHeight = (h * 0.62f).toInt()
            if (topOffset + cropHeight > h) return null

            val croppedBmp = Bitmap.createBitmap(bitmap, 0, topOffset, w, cropHeight)
            val maxDim = 800
            val maxSide = maxOf(croppedBmp.width, croppedBmp.height)
            val scale = if (maxSide > maxDim) maxDim.toFloat() / maxSide else 1f
            val finalBmp = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    croppedBmp,
                    (croppedBmp.width * scale).toInt(),
                    (croppedBmp.height * scale).toInt(),
                    true
                )
            } else {
                croppedBmp
            }

            val baos = ByteArrayOutputStream()
            finalBmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            baos.toByteArray()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract center video crop: ${e.message}")
            null
        }
    }
}
