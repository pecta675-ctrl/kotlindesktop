package me.enivzoy.anilife.app.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import me.enivzoy.anilife.app.data.AnimeEpisode
import me.enivzoy.anilife.app.data.ContinueProgress

class EpisodeSectionTest {
    @Test
    fun splitsLongSeriesIntoHundredEpisodeRanges() {
        val episodes = (1..201).map { AnimeEpisode(it.toDouble(), null) }

        val ranges = episodeRanges(episodes)

        assertEquals(listOf(100, 100, 1), ranges.map { it.size })
        assertEquals("1–100", episodeRangeLabel(ranges[0]))
        assertEquals("201–201", episodeRangeLabel(ranges[2]))
    }

    @Test
    fun labelsFractionalEpisodeRanges() {
        val episodes = listOf(AnimeEpisode(1.0, null), AnimeEpisode(1.5, null))

        assertEquals("1–1.5", episodeRangeLabel(episodes))
    }

    @Test
    fun rejectsInvalidRangeSizesAndEmptyLabels() {
        assertFailsWith<IllegalArgumentException> { episodeRanges(emptyList(), 0) }
        assertFailsWith<IllegalArgumentException> { episodeRangeLabel(emptyList()) }
    }

    @Test
    fun marksEpisodesFromAniListWatchedCountAsComplete() {
        assertEquals(
            EpisodeWatchProgress(fraction = 1f, watched = true),
            episodeWatchProgress(3.0, aniListWatchedEpisodes = 3, localProgress = null),
        )
        assertNull(episodeWatchProgress(4.0, aniListWatchedEpisodes = 3, localProgress = null))
    }

    @Test
    fun showsSavedPlaybackFractionAndInfersEarlierEpisodesWatched() {
        val local = ContinueProgress(episode = 4.0, position = 45.0, duration = 100.0)

        assertEquals(
            EpisodeWatchProgress(fraction = 0.45f, watched = false),
            episodeWatchProgress(4.0, aniListWatchedEpisodes = 2, localProgress = local),
        )
        assertEquals(
            EpisodeWatchProgress(fraction = 1f, watched = true),
            episodeWatchProgress(3.0, aniListWatchedEpisodes = 2, localProgress = local),
        )
        assertNull(episodeWatchProgress(5.0, aniListWatchedEpisodes = 2, localProgress = local))
    }

    @Test
    fun marksLocallySavedEpisodeWatchedAtNinetyPercent() {
        assertEquals(
            EpisodeWatchProgress(fraction = 0.9f, watched = true),
            episodeWatchProgress(
                2.0,
                aniListWatchedEpisodes = 0,
                localProgress = ContinueProgress(episode = 2.0, position = 90.0, duration = 100.0),
            ),
        )
    }

    @Test
    fun ignoresInvalidOrEmptyLocalPlaybackProgress() {
        assertNull(
            episodeWatchProgress(
                1.0,
                aniListWatchedEpisodes = -1,
                localProgress = ContinueProgress(episode = 1.0, position = 0.0, duration = 100.0),
            ),
        )
        assertNull(
            episodeWatchProgress(
                1.0,
                aniListWatchedEpisodes = 0,
                localProgress = ContinueProgress(episode = 1.0, position = 10.0, duration = 0.0),
            ),
        )
        assertNull(
            episodeWatchProgress(
                1.0,
                aniListWatchedEpisodes = 0,
                localProgress = ContinueProgress(episode = 1.0, position = Double.NaN, duration = 100.0),
            ),
        )
        assertNull(episodeWatchProgress(0.0, aniListWatchedEpisodes = 0, localProgress = null))
    }
}
