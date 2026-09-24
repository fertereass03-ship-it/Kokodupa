package com.example.util

import java.util.regex.Pattern

object TextCleaner {

    private val HTML_TAG_REGEX = Pattern.compile("<[^>]+>")
    private val BBCODE_PAIR_REGEX = Pattern.compile("\\[([a-zA-Z0-9_-]+)(?:=[^\\]]+)?\\](.*?)\\[/\\1\\]", Pattern.DOTALL)
    private val BBCODE_SINGLE_REGEX = Pattern.compile("\\[/?[a-zA-Z0-9_-]+(?:=[^\\]]*)?\\]")
    private val CITATION_REGEX = Pattern.compile("\\[\\d+\\]")
    private val SPOILER_PIPE_REGEX = Pattern.compile("\\|\\|(.*?)\\|\\|", Pattern.DOTALL)
    private val SOURCE_FOOTER_REGEX = Pattern.compile("(?i)\\((?:source|written by).*?\\)", Pattern.DOTALL)
    private val MULTI_NEWLINES_REGEX = Pattern.compile("\\n{3,}")
    private val MULTI_SPACES_REGEX = Pattern.compile("[ \\t]{2,}")

    /**
     * Cleans raw anime description from Shikimori/APIs:
     * - Strips BBCode tags, character/anime IDs (e.g. [character=12345])
     * - Strips HTML tags and decodes HTML entities
     * - Removes weird citations, raw numbers and formatting leftovers
     * - Ensures Russian language fallback if the description is in English or empty
     */
    fun cleanAnimeDescription(
        rawText: String?,
        russianTitle: String? = null,
        origTitle: String? = null,
        genres: List<String>? = null
    ): String {
        val title = russianTitle?.takeIf { it.isNotBlank() } ?: origTitle ?: "Аниме"

        if (rawText.isNullOrBlank()) {
            return generateRussianSynopsis(title, genres)
        }

        var text: String = rawText

        // 1. Decode common HTML entities
        text = text
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&hellip;", "...")

        // 2. Remove HTML tags (<br>, <p>, etc.)
        text = HTML_TAG_REGEX.matcher(text).replaceAll(" ")

        // 3. Unpack BBCode pairs like [character=123]Имя[/character], [b]жирный[/b]
        // Run multiple passes for nested tags
        var previous = ""
        var iterations = 0
        while (text != previous && iterations < 5) {
            previous = text
            text = BBCODE_PAIR_REGEX.matcher(text).replaceAll("$2")
            iterations++
        }

        // 4. Remove orphan BBCode tags like [character=12345], [/character], [anime=123], [url=...]
        text = BBCODE_SINGLE_REGEX.matcher(text).replaceAll(" ")

        // 5. Remove citations like [1], [2]
        text = CITATION_REGEX.matcher(text).replaceAll("")

        // 6. Unwrap markdown spoiler double-pipes ||spoiler||
        text = SPOILER_PIPE_REGEX.matcher(text).replaceAll("$1")

        // 7. Remove footer notes like (Source: MAL News)
        text = SOURCE_FOOTER_REGEX.matcher(text).replaceAll("")

        // 8. Normalize spacing and linebreaks
        text = MULTI_SPACES_REGEX.matcher(text).replaceAll(" ")
        text = MULTI_NEWLINES_REGEX.matcher(text).replaceAll("\n\n")
        text = text.trim()

        // 9. Check language: if virtually no Russian letters exist (mostly English),
        // provide a clean, engaging Russian synopsis
        val cyrillicCount = text.count { (it in 'а'..'я') || (it in 'А'..'Я') || it == 'ё' || it == 'Ё' }
        val latinCount = text.count { (it in 'a'..'z') || (it in 'A'..'Z') }

        if (cyrillicCount < 12 && latinCount > 20) {
            return generateRussianSynopsis(title, genres)
        }

        if (text.isBlank() || text.length < 15) {
            return generateRussianSynopsis(title, genres)
        }

        return text
    }

    private fun generateRussianSynopsis(title: String, genres: List<String>?): String {
        val genresStr = if (!genres.isNullOrEmpty()) {
            " в жанре " + genres.take(3).joinToString(", ")
        } else {
            ""
        }
        return "Захватывающая история «$title»$genresStr. Главных героев ждут невероятные приключения, испытания дружбы и верности, а также захватывающие повороты сюжета. Смотрите все серии в отличном качестве (доступно много озвучек на выбор) в приложении ANIWERTI."
    }
}
