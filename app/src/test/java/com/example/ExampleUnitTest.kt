package com.example

import com.example.data.api.AnimeEpisodeHelper
import com.example.data.api.models.ShikimoriAnimeDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testMushokuTenseiOngoingEpisodeDisplay() {
        val dto = ShikimoriAnimeDto(
            id = 59193,
            name = "Mushoku Tensei III: Isekai Ittara Honki Dasu",
            russian = "Реинкарнация безработного 3",
            image = null,
            url = "/animes/59193",
            kind = "tv",
            score = "9.10",
            status = "ongoing",
            episodes = 14,
            episodesAired = 13,
            airedOn = "2026-04-05",
            releasedOn = null
        )

        val infoRu = AnimeEpisodeHelper.getEpisodeInfo(dto, isUk = false)
        assertTrue(infoRu.isOngoing)
        assertEquals(13, infoRu.airedEpisodes)
        assertEquals(14, infoRu.totalEpisodes)
        assertEquals("13 из 14 сер.", infoRu.formattedText)

        val infoUk = AnimeEpisodeHelper.getEpisodeInfo(dto, isUk = true)
        assertEquals("13 з 14 сер.", infoUk.formattedText)
    }

    @Test
    fun testSoloLevelingOngoingEpisodeDisplay() {
        val dto = ShikimoriAnimeDto(
            id = 58567,
            name = "Ore dake Level Up na Ken Season 2",
            russian = "Поднятие уровня в одиночку 2",
            image = null,
            url = "/animes/58567",
            kind = "tv",
            score = "8.80",
            status = "ongoing",
            episodes = 12,
            episodesAired = 10,
            airedOn = "2025-01-06",
            releasedOn = null
        )

        val infoRu = AnimeEpisodeHelper.getEpisodeInfo(dto, isUk = false)
        assertTrue(infoRu.isOngoing)
        assertEquals("10 из 12 сер.", infoRu.formattedText)
    }

    @Test
    fun testFormatEpisodeLabelCases() {
        // Ongoing 13 out of 14
        val ru13 = AnimeEpisodeHelper.formatEpisodeLabel(aired = 13, total = 14, isOngoing = true, isAnons = false, isUk = false)
        assertEquals("13 из 14 сер.", ru13)

        val uk13 = AnimeEpisodeHelper.formatEpisodeLabel(aired = 13, total = 14, isOngoing = true, isAnons = false, isUk = true)
        assertEquals("13 з 14 сер.", uk13)

        // Released 12 episodes
        val rel12 = AnimeEpisodeHelper.formatEpisodeLabel(aired = 12, total = 12, isOngoing = false, isAnons = false, isUk = false)
        assertEquals("12 сер.", rel12)
    }

    @Test
    fun testAnixartRelatedReleasesIntegration() = kotlinx.coroutines.runBlocking {
        // Test fetching Demon Slayer related releases from Anixart
        val related = com.example.data.api.AnixartService.getRelatedReleases("Клинок, рассекающий демонов", 38000L)
        println("Fetched ${related.size} related releases from Anixart")
        assertTrue(related.isNotEmpty())

        val first = related.first()
        assertTrue(first.russianName.isNotBlank())
        assertTrue(first.posterUrl.isNotBlank())
        assertTrue(first.year.isNotBlank())
        assertTrue(first.score.isNotBlank())

        // Check chronological sorting
        for (i in 0 until related.size - 1) {
            assertTrue(related[i].yearInt <= related[i + 1].yearInt)
        }
    }
}
