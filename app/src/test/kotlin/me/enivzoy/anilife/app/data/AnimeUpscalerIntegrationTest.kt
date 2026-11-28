package me.enivzoy.anilife.app.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import me.enivzoy.anilife.core.model.AnimeCard
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimeUpscalerIntegrationTest {
    @TempDir
    lateinit var root: Path

    @Test
    fun real360pClipProducesVerified2160pVideoWithAudio() = withRuntime { ffmpeg, ffprobe ->
        val input = root.resolve("360p sample.mkv")
        capture(
            listOf(
                ffmpeg, "-v", "error", "-y", "-f", "lavfi", "-i", "color=c=blue:s=640x360:r=2:d=1",
                "-f", "lavfi", "-i", "sine=frequency=440:duration=1", "-c:v", "libx264", "-c:a", "aac",
                "-shortest", input.toString(),
            ),
        )
        verifyUpscale(input, ffprobe)
    }

    @Test
    fun twoFramesFromADownloadedEpisodeCompleteAllStages() {
        val source = System.getenv("ANILIFE_TEST_UPSCALE_SOURCE")
        assumeTrue(!source.isNullOrBlank(), "Opt-in downloaded video sample")
        withRuntime { ffmpeg, ffprobe ->
            val input = root.resolve("downloaded sample.mkv")
            capture(
                listOf(
                    ffmpeg, "-v", "error", "-y", "-ss", "60", "-i", source!!, "-t", "0.084",
                    "-frames:v", "2", "-c:v", "libx264", "-c:a", "aac", input.toString(),
                ),
            )
            verifyUpscale(input, ffprobe)
        }
    }

    private fun verifyUpscale(input: Path, ffprobe: String) {
        val updates = mutableListOf<UpscaleProgress>()
        val inputSize = Files.size(input)
        val entry = LocalEpisode(AnimeCard(1, "Upscale sample"), 1.0, "sub", input, inputSize)
        val output = runBlocking {
            withTimeout(180_000) { AnimeUpscaler().upscale(entry, updates::add) }
        }
        assertEquals(inputSize, Files.size(input))
        assertTrue(Files.size(output) > 0)
        val streams = capture(
            listOf(ffprobe, "-v", "error", "-show_entries", "stream=codec_type,width,height", "-of", "default=nw=1", output.toString()),
        )
        assertTrue("height=2160" in streams, streams)
        assertTrue("codec_type=audio" in streams, streams)
        assertEquals(1f, updates.last().fraction)
        assertEquals(UpscaleStage.Completed, updates.last().stage)
        assertTrue(updates.zipWithNext().all { (a, b) -> a.fraction <= b.fraction })
        assertTrue(updates.dropLast(1).all { it.fraction < 1f })
        assertEquals(UpscaleStage.entries.toSet(), updates.map { it.stage }.toSet())
        assertTrue(updates.last().completedFrames > 0)
    }

    private fun withRuntime(block: (String, String) -> Unit) {
        val ffmpegDir = System.getenv("ANILIFE_TEST_FFMPEG_DIR")
        val upscalerDir = System.getenv("ANILIFE_TEST_UPSCALER_DIR")
        assumeTrue(!ffmpegDir.isNullOrBlank() && !upscalerDir.isNullOrBlank(), "Opt-in real Vulkan/ffmpeg integration test")
        val oldFfmpeg = System.getProperty("AniLife.ffmpeg.dir")
        val oldUpscaler = System.getProperty("AniLife.upscaler.dir")
        val oldLocale = Locale.getDefault()
        try {
            System.setProperty("AniLife.ffmpeg.dir", ffmpegDir!!)
            System.setProperty("AniLife.upscaler.dir", upscalerDir!!)
            Locale.setDefault(Locale.GERMANY)
            val suffix = if (System.getProperty("os.name").startsWith("Windows")) ".exe" else ""
            block(Path.of(ffmpegDir, "ffmpeg$suffix").toString(), Path.of(ffmpegDir, "ffprobe$suffix").toString())
        } finally {
            if (oldFfmpeg == null) System.clearProperty("AniLife.ffmpeg.dir") else System.setProperty("AniLife.ffmpeg.dir", oldFfmpeg)
            if (oldUpscaler == null) System.clearProperty("AniLife.upscaler.dir") else System.setProperty("AniLife.upscaler.dir", oldUpscaler)
            Locale.setDefault(oldLocale)
        }
    }

    private fun capture(command: List<String>): String {
        val log = root.resolve("command.log").toFile()
        val process = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log).start()
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Tool timed out: ${command.first()}")
            val output = log.readText()
            assertEquals(0, process.exitValue(), output)
            return output
        } finally {
            if (process.isAlive) {
                process.destroyForcibly()
                process.waitFor(10, TimeUnit.SECONDS)
            }
        }
    }
}
