package me.enivzoy.anilife.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpscaleEncodersTest {
    private val windows = upscaleEncoderCandidates("Windows 11")

    @Test
    fun windowsPrefersDedicatedGpuEncodersBeforeIntelAndSoftware() {
        assertEquals(listOf("h264_nvenc", "h264_amf", "h264_qsv", "libx264"), windows.map { it.codec })
        assertTrue(windows.dropLast(1).all { it.hardware })
        assertFalse(windows.last().hardware)
        assertTrue(windows.all { it.filter.contains("scale=-2:2160") && it.filter.contains("unsharp=") })
        assertEquals(listOf("-c:v", "h264_nvenc"), windows.first().arguments.take(2))
        assertTrue(windows.first().arguments.containsAll(listOf("p4", "constqp")))
        assertTrue(windows[1].arguments.containsAll(listOf("-quality", "speed", "-rc", "cqp")))
    }

    @Test
    fun linuxTriesEveryRenderNodeWithHardwareFrameUpload() {
        val candidates = upscaleEncoderCandidates("Linux", listOf("/dev/dri/renderD129", "/dev/dri/renderD128", "/dev/dri/renderD129"))
        val vaapi = candidates.filter { it.codec == "h264_vaapi" }
        assertEquals(2, vaapi.size)
        assertEquals(listOf("-vaapi_device", "/dev/dri/renderD128"), vaapi.first().inputArguments)
        assertTrue(vaapi.all { it.filter.endsWith(",format=nv12,hwupload") && it.pixelFormat == "vaapi" })
        assertEquals("libx264", candidates.last().codec)
    }

    @Test
    fun platformSpecificBackendsAreNotTriedOnOtherSystems() {
        val mac = upscaleEncoderCandidates("Mac OS X")
        assertEquals(listOf("h264_videotoolbox", "libx264"), mac.map { it.codec })
        assertTrue(mac.first().arguments.containsAll(listOf("-allow_sw", "0")))
        assertEquals(listOf("libx264"), upscaleEncoderCandidates("Unknown OS").map { it.codec })
        assertTrue(windows.none { it.codec == "h264_vaapi" || it.codec == "h264_videotoolbox" })
    }

    @Test
    fun availableEncodersAreReadWithoutMistakingAudioOrTheLegendForHardware() {
        val output = """
            Encoders:
             V..... = Video
             A..... = Audio
             V....D h264_nvenc NVIDIA NVENC H.264 encoder
             V....D h264_amf AMD AMF H.264 Encoder
             V..... h264_qsv H.264 (Intel Quick Sync Video acceleration)
             V....D libx264 libx264 H.264 / AVC
             A..... aac AAC
        """.trimIndent()
        assertEquals(setOf("h264_nvenc", "h264_amf", "h264_qsv", "libx264"), advertisedVideoEncoders(output))
        assertEquals(emptySet(), advertisedVideoEncoders(""))
    }

    @Test
    fun aSuccessfulNvidiaEncodeDoesNotTouchAmdIntelOrSoftware() = runBlocking {
        val probed = mutableListOf<String>()
        val selected = withUpscaleEncoderFallback(
            windows,
            probe = { probed += it.codec; true },
            encode = { it.codec },
            onUnavailable = { _, _ -> error("Unexpected fallback") },
        )
        assertEquals("h264_nvenc", selected)
        assertEquals(listOf("h264_nvenc"), probed)
    }

    @Test
    fun missingNvidiaDriversFallThroughToAmd() = runBlocking {
        val unavailable = mutableListOf<String>()
        val selected = withUpscaleEncoderFallback(
            windows,
            probe = { if (it.codec == "h264_nvenc") error("Cannot load nvcuda.dll") else true },
            encode = { it.codec },
            onUnavailable = { encoder, _ -> unavailable += encoder.codec },
        )
        assertEquals("h264_amf", selected)
        assertEquals(listOf("h264_nvenc"), unavailable)
    }

    @Test
    fun unavailableGpuDriversFallThroughToIntelAndSoftwareInOrder() = runBlocking {
        for (available in listOf("h264_qsv", "libx264")) {
            val unavailable = mutableListOf<String>()
            val selected = withUpscaleEncoderFallback(
                windows,
                probe = { it.codec == available },
                encode = { it.codec },
                onUnavailable = { encoder, _ -> unavailable += encoder.codec },
            )
            assertEquals(available, selected)
            assertEquals(windows.takeWhile { it.codec != available }.map { it.codec }, unavailable)
        }
    }

    @Test
    fun hardwareFailureDuringTheRealEncodeAlsoFallsBack() = runBlocking {
        val attempted = mutableListOf<String>()
        val selected = withUpscaleEncoderFallback(
            windows,
            probe = { true },
            encode = { encoder ->
                attempted += encoder.codec
                if (encoder.hardware) error("Driver failed after probe")
                encoder.codec
            },
            onUnavailable = { _, _ -> },
        )
        assertEquals("libx264", selected)
        assertEquals(windows.map { it.codec }, attempted)
    }

    @Test
    fun cancellationNeverStartsAnotherEncode() = runBlocking {
        var attempted = 0
        assertFailsWith<CancellationException> {
            withUpscaleEncoderFallback(
                windows,
                probe = { throw CancellationException("Cancelled during GPU check") },
                encode = { attempted++ },
                onUnavailable = { _, _ -> error("Cancellation swallowed") },
            )
        }
        assertFailsWith<CancellationException> {
            withUpscaleEncoderFallback(
                windows,
                probe = { true },
                encode = { attempted++; throw CancellationException("Cancelled during encode") },
                onUnavailable = { _, _ -> error("Cancellation swallowed") },
            )
        }
        assertEquals(1, attempted)
    }

    @Test
    fun softwareFailuresAreReportedRatherThanRetriedForever() = runBlocking {
        val error = assertFailsWith<IllegalStateException> {
            withUpscaleEncoderFallback(
                windows,
                probe = { false },
                encode = { error("Output disk full") },
                onUnavailable = { _, _ -> },
            )
        }
        assertEquals("Output disk full", error.message)
    }
}
