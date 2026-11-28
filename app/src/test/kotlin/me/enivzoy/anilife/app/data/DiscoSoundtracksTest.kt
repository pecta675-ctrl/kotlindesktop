package me.enivzoy.anilife.app.data

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscoSoundtracksTest {
    @TempDir
    lateinit var root: Path

    @Test
    fun originalAndAllFiveRequestedTracksAppearExactlyOnce() {
        assertEquals(
            listOf("mvkcyB_lve0", "OkBxj51gjUk", "6D9TR7W9z9I", "kcu4RTdAft4", "Ub0wWUq6M4A", "x706w5NCnG4"),
            discoTracks.map { it.videoId },
        )
        assertEquals(6, discoTracks.distinct().size)
        assertTrue(discoTracks.all { it.url == "https://www.youtube.com/watch?v=${it.videoId}" })
    }

    @Test
    fun everyTrackGetsExactlyOneRandomSlotWithNoCacheWeightingOrRepeatExclusion() {
        var slot = 0
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = error("Unexpected random primitive")
            override fun nextInt(until: Int): Int {
                assertEquals(6, until)
                return slot++ % until
            }
        }
        val choices = (1..600).map { pickDiscoTrack(random = random) }.groupingBy { it.videoId }.eachCount()
        assertEquals(discoTracks.associate { it.videoId to 100 }, choices)
        val repeat = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }
        assertEquals(pickDiscoTrack(random = repeat), pickDiscoTrack(random = repeat))
    }

    @Test
    fun onlyTheFourthRequestedTrackStartsAt50SecondsAndNoTrackLoopsForever() {
        for (track in discoTracks) {
            val expected = if (track.videoId == "Ub0wWUq6M4A") 50 else 0
            assertEquals(expected, track.startSeconds)
            val options = discoPlayerOptions(track).toMap()
            assertEquals(expected.toString(), options["start"])
            assertEquals("no", options["loop-file"])
            assertEquals("yes", options["pause"])
            assertEquals("no", options["video"])
            assertEquals("100", options["volume"])
        }
    }

    @Test
    fun eachTrackHasAnIndependentCacheAndTheExistingSoundtrackIsReused() {
        val legacy = Files.writeString(root.resolve("buckshot_ost.mp3"), "existing audio").toFile()
        assertEquals(legacy, discoTrackFile(root.toFile(), discoTracks.first()))
        val paths = discoTracks.map { discoTrackFile(root.toFile(), it).absolutePath }
        assertEquals(6, paths.distinct().size)
        assertTrue(paths.drop(1).all { Path.of(it).parent == root.resolve("disco") })
        Files.writeString(legacy.toPath(), "")
        assertEquals(root.resolve("disco/mvkcyB_lve0.mp3").toFile(), discoTrackFile(root.toFile(), discoTracks.first()))
    }

    @Test
    fun commandsDownloadOnlyTheChosenAudioUsingSafeSeparateArguments() {
        val executable = root.resolve("tools with spaces/yt-dlp.exe").toFile()
        val ffmpeg = root.resolve("ffmpeg tools/ffmpeg.exe").toFile()
        val output = root.resolve("music with spaces/track.mp3").toFile()
        val track = discoTracks[4]
        val command = discoDownloadCommand(executable, ffmpeg, output, track)
        assertEquals(executable.absolutePath, command.first())
        assertEquals(track.url, command.last())
        assertEquals(ffmpeg.absolutePath, command[command.indexOf("--ffmpeg-location") + 1])
        assertEquals(output.absolutePath, command[command.indexOf("-o") + 1])
        assertTrue(command.containsAll(listOf("-x", "--no-playlist", "--no-progress")))
        assertFalse(discoDownloadCommand(executable, null, output, track).contains("--ffmpeg-location"))
    }

    @Test
    fun unavailableTracksCanBeExcludedWithoutPreventingPlaybackOfOthers() {
        val remaining = discoTracks.drop(2)
        val random = Random(42)
        val picks = (1..100).map { pickDiscoTrack(remaining, random) }
        assertTrue(picks.all { it in remaining })
        assertEquals(remaining.toSet(), picks.toSet())
    }
}
