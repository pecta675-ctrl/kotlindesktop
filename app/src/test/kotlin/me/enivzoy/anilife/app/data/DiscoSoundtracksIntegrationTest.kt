package me.enivzoy.anilife.app.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import me.enivzoy.anilife.mpv.EndReason
import me.enivzoy.anilife.mpv.Event
import me.enivzoy.anilife.mpv.Mpv
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscoSoundtracksIntegrationTest {
    @TempDir
    lateinit var root: Path

    @Test
    fun everyRequestedYoutubeAudioCanBeDownloadedAndProbed() {
        val downloader = System.getenv("ANILIFE_TEST_DISCO_YTDLP")
        val tools = System.getenv("ANILIFE_TEST_FFMPEG_DIR")
        assumeTrue(!downloader.isNullOrBlank() && !tools.isNullOrBlank(), "Opt-in YouTube download test")
        val suffix = if (System.getProperty("os.name").startsWith("Windows")) ".exe" else ""
        val ffmpeg = Path.of(tools!!, "ffmpeg$suffix").toFile()
        val ffprobe = Path.of(tools, "ffprobe$suffix").toString()
        for (track in discoTracks.drop(1)) {
            val output = root.resolve("${track.videoId}.mp3").toFile()
            capture(discoDownloadCommand(Path.of(downloader!!).toFile(), ffmpeg, output, track), 120)
            assertTrue(output.isFile && output.length() > 0)
            val duration = capture(listOf(ffprobe, "-v", "error", "-show_entries", "format=duration", "-of", "default=nw=1:nk=1", output.absolutePath)).trim().toDouble()
            assertTrue(duration > track.startSeconds, "Track ${track.videoId} must continue past its start offset")
            println("Verified disco track ${track.videoId}: ${duration}s, start=${track.startSeconds}s")
        }
    }

    @Test
    fun realMpvStartsAt50SecondsAndEmitsEofRatherThanLooping() = runBlocking {
        val library = System.getenv("ANILIFE_TEST_MPV_LIBRARY")
        val tools = System.getenv("ANILIFE_TEST_FFMPEG_DIR")
        assumeTrue(!library.isNullOrBlank() && !tools.isNullOrBlank(), "Opt-in silent mpv test")
        val suffix = if (System.getProperty("os.name").startsWith("Windows")) ".exe" else ""
        val input = root.resolve("offset-test.mp3")
        capture(listOf(Path.of(tools!!, "ffmpeg$suffix").toString(), "-v", "error", "-y", "-f", "lavfi", "-i", "sine=frequency=440:duration=53", input.toString()))
        val mpv = Mpv(libraryPath = library, initialOptions = discoPlayerOptions(discoTracks.single { it.startSeconds == 50 }) + listOf("ao" to "null", "end" to "51.5"))
        try {
            val loaded = CompletableDeferred<Unit>()
            val ended = CompletableDeferred<Event.EndFile>()
            mpv.startEvents { event ->
                if (event is Event.FileLoaded) loaded.complete(Unit)
                if (event is Event.EndFile) ended.complete(event)
            }
            mpv.loadfile(input.toString())
            withTimeout(5_000) { loaded.await() }
            delay(100)
            val position = mpv.getDoubleProperty("time-pos")
            assertTrue(position != null && position >= 49.9, "Expected 50s start, got $position")
            assertEquals(true, mpv.getFlagProperty("pause"))
            mpv.play()
            delay(150)
            mpv.pause()
            val pausedAt = mpv.getDoubleProperty("time-pos")!!
            delay(100)
            assertEquals(true, mpv.getFlagProperty("pause"))
            assertTrue(kotlin.math.abs(mpv.getDoubleProperty("time-pos")!! - pausedAt) < 0.1)
            mpv.play()
            assertEquals(EndReason.EOF, withTimeout(5_000) { ended.await() }.reason)
        } finally {
            mpv.shutdown()
        }
    }

    private fun capture(command: List<String>, timeout: Long = 30): String {
        val log = root.resolve("command.log").toFile()
        val process = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log).start()
        try {
            assertTrue(process.waitFor(timeout, TimeUnit.SECONDS), "Command timed out")
            val output = log.readText()
            assertEquals(0, process.exitValue(), output.takeLast(2000))
            return output
        } finally {
            if (process.isAlive) {
                process.destroyForcibly()
                process.waitFor(10, TimeUnit.SECONDS)
            }
        }
    }
}
