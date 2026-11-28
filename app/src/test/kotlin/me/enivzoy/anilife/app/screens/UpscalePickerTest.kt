package me.enivzoy.anilife.app.screens

import me.enivzoy.anilife.app.data.LocalEpisode
import me.enivzoy.anilife.core.model.AnimeCard
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpscalePickerTest {
    private val horimiya = AnimeCard(124080, "Horimiya", cover = "https://example.test/horimiya.jpg")
    private val jjk = AnimeCard(113415, "JUJUTSU KAISEN")

    private fun entry(anime: AnimeCard, number: Double, audio: String = "sub") =
        LocalEpisode(anime, number, audio, Path.of("${anime.id}-E$number-$audio.mkv"), 100)

    @Test
    fun animeAreSortedByTitleAndEachCoverContainsOnlyItsOwnDownloads() {
        val first = entry(horimiya, 1.0)
        val third = entry(horimiya, 3.0)
        val other = entry(jjk, 1.0)
        val groups = groupUpscaleEpisodes(listOf(other, third, first))
        assertEquals(listOf("Horimiya", "JUJUTSU KAISEN"), groups.map { it.anime.title })
        assertEquals(listOf(first, third), groups.first().episodes)
        assertEquals(listOf(other), groups.last().episodes)
    }

    @Test
    fun episodesSortNumericallyAndKeepAudioAnd4kVariantsSeparate() {
        val entries = listOf(entry(horimiya, 10.0), entry(horimiya, 2.0), entry(horimiya, 1.0, "dub"), entry(horimiya, 1.0, "4k-fast"))
        val grouped = groupUpscaleEpisodes(entries).single().episodes
        assertEquals(listOf(1.0, 1.0, 2.0, 10.0), grouped.map { it.episode })
        assertEquals(4, grouped.map(::upscaleKey).distinct().size)
        assertEquals("Dub", upscaleVariant(grouped[0]))
        assertEquals("Fast 4K copy", upscaleVariant(grouped[1]))
    }

    @Test
    fun sameTitleWithDifferentSeriesIdsDoesNotMixSeasons() {
        val groups = groupUpscaleEpisodes(listOf(entry(horimiya, 1.0), entry(horimiya.copy(id = 2), 1.0)))
        assertEquals(2, groups.size)
    }

    @Test
    fun titleVariantsWithTheSameIdShareOneCoverAndPreferAvailableArtwork() {
        val groups = groupUpscaleEpisodes(listOf(entry(horimiya.copy(title = "Hori-san", cover = null), 1.0), entry(horimiya, 2.0)))
        assertEquals(1, groups.size)
        assertEquals(horimiya.cover, groups.single().anime.cover)
        assertEquals(2, groups.single().episodes.size)
    }

    @Test
    fun selectionsSurviveSwitchingAnimeAndClearingOneShowLeavesTheOtherSelected() {
        val hori = entry(horimiya, 1.0)
        val kaisen = entry(jjk, 1.0)
        val entries = listOf(hori, kaisen)
        val both = setOf(upscaleKey(hori), upscaleKey(kaisen))
        assertEquals(entries, selectedUpscaleEpisodes(entries, both))
        val clearedShow = both - groupUpscaleEpisodes(entries).first().episodes.map(::upscaleKey).toSet()
        assertEquals(listOf(kaisen), selectedUpscaleEpisodes(entries, clearedShow))
        assertTrue(selectedUpscaleEpisodes(entries, emptySet()).isEmpty())
    }

    @Test
    fun duplicateFilesAndRemovedSelectionsAreNeverQueuedTwice() {
        val one = entry(horimiya, 1.0)
        val deleted = entry(horimiya, 2.0)
        assertEquals(listOf(one), groupUpscaleEpisodes(listOf(one, one)).single().episodes)
        assertEquals(listOf(one), selectedUpscaleEpisodes(listOf(one, one), setOf(upscaleKey(one), upscaleKey(deleted))))
        assertTrue(groupUpscaleEpisodes(emptyList()).isEmpty())
    }
}
