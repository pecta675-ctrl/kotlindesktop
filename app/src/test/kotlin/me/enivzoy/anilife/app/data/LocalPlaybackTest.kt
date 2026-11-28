package me.enivzoy.anilife.app.data

import me.enivzoy.anilife.app.screens.nextEpisodeNumber
import me.enivzoy.anilife.core.model.AnimeCard
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalPlaybackTest {
    private fun card(
        id: Long = 21,
        title: String = "Frieren",
        romaji: String? = "Sousou no Frieren",
        english: String? = "Frieren: Beyond Journey's End",
        aliases: List<String> = listOf("Frieren at the Funeral"),
    ) = AnimeCard(
        id = id,
        title = title,
        titleRomaji = romaji,
        titleEnglish = english,
        aliases = aliases,
    )

    private fun episode(
        root: Path,
        anime: AnimeCard,
        number: Double,
        audio: String = "sub",
        fileName: String = "${anime.title} - E${number.toInt()}.mkv",
    ): LocalEpisode {
        val path = Files.writeString(root.resolve(fileName), "media")
        return LocalEpisode(anime, number, audio, path, Files.size(path))
    }

    @Test
    fun findsTheRequestedEpisodeEvenWhenTheStoredTitleUsesAnotherVariant() {
        val root = Files.createTempDirectory("AniLife-playback-title")
        try {
            val anime = card()
            val entry = episode(root, anime, 3.0)

            assertEquals(entry, LocalPlayback.find(listOf(entry), 21, "Frieren at the Funeral", 3.0, "sub"))
            assertEquals(entry, LocalPlayback.find(listOf(entry), 21, "Sousou no Frieren", 3.0, "sub"))
            assertEquals(entry, LocalPlayback.find(listOf(entry), 21, "Unknown title", 3.0, "sub"))
            assertNull(LocalPlayback.find(listOf(entry), 21, "Frieren", 4.0, "sub"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun prefersTheUpscaledCopyOfTheEpisode() {
        val root = Files.createTempDirectory("AniLife-playback-4k")
        try {
            val anime = card()
            val original = episode(root, anime, 2.0)
            val upscaled = episode(root, anime, 2.0, audio = "4k", fileName = "Frieren - E2_4k.mkv")

            assertEquals(upscaled, LocalPlayback.find(listOf(original, upscaled), 21, "Frieren", 2.0, "sub"))
            assertTrue(LocalPlayback.hasUpscaled(listOf(original, upscaled), 21, "Frieren"))
            assertFalse(LocalPlayback.hasUpscaled(listOf(original), 21, "Frieren"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun prefersTheRequestedAudioVariantWhenNeitherCopyIsUpscaled() {
        val root = Files.createTempDirectory("AniLife-playback-audio")
        try {
            val anime = card()
            val sub = episode(root, anime, 1.0, audio = "sub", fileName = "Frieren - E1 sub.mkv")
            val dub = episode(root, anime, 1.0, audio = "dub", fileName = "Frieren - E1 dub.mkv")

            assertEquals(dub, LocalPlayback.find(listOf(sub, dub), 21, "Frieren", 1.0, "dub"))
            assertEquals(sub, LocalPlayback.find(listOf(sub, dub), 21, "Frieren", 1.0, "sub"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun episodesBeyondTheMetadataCountStayPlayableAndAutoplayFindsTheNextDownload() {
        val root = Files.createTempDirectory("AniLife-playback-next")
        try {
            val anime = card()
            val first = episode(root, anime, 12.0)
            val next = episode(root, anime, 13.0, audio = "4k", fileName = "Frieren - E13_4k.mkv")
            val entries = listOf(first, next)

            assertEquals(listOf(12.0, 13.0), LocalPlayback.availableEpisodes(entries, 21, "Frieren"))
            val series = LocalPlayback.seriesEpisodes(entries, 21, "Frieren", totalEpisodes = 12)
            assertEquals(13.0, series.max())
            assertEquals(13.0, nextEpisodeNumber(series, 12.0))
            assertEquals(next, LocalPlayback.find(entries, 21, "Frieren", 13.0, "sub"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun withoutAMetadataCountTheSeriesStillAdvancesOneEpisodeAtATime() {
        val root = Files.createTempDirectory("AniLife-playback-count")
        try {
            val anime = card()
            val third = episode(root, anime, 3.0)
            val series = LocalPlayback.seriesEpisodes(listOf(third), 21, "Frieren", totalEpisodes = null)

            assertEquals(listOf(1.0, 2.0, 3.0, 4.0), series)
            assertEquals(2.0, nextEpisodeNumber(series, 1.0))
            assertNull(LocalPlayback.find(listOf(third), 21, "Frieren", 2.0, "sub"))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun aMissingFileIsIgnoredUnlessTheCallerAllowsIt() {
        val root = Files.createTempDirectory("AniLife-playback-missing")
        try {
            val anime = card()
            val entry = episode(root, anime, 1.0)
            Files.delete(entry.path)

            assertNull(LocalPlayback.find(listOf(entry), 21, "Frieren", 1.0, "sub"))
            assertEquals(entry, LocalPlayback.find(listOf(entry), 21, "Frieren", 1.0, "sub", requireFile = false))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun upscaledCopiesAreRecognisedFromTheAudioLabelOrTheLegacyFileName() {
        val root = Files.createTempDirectory("AniLife-playback-label")
        try {
            val anime = card()
            val labelled = episode(root, anime, 1.0, audio = "4K", fileName = "Frieren - E1.mkv")
            val legacy = episode(root, anime, 2.0, audio = "sub", fileName = "Frieren - E2_4k.mkv")

            assertTrue(labelled.isUpscaled())
            assertTrue(legacy.isUpscaled())
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}