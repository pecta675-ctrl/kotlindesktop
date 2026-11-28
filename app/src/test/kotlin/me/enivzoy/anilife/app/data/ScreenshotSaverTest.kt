package me.enivzoy.anilife.app.data

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenshotSaverTest {
    @Test
    fun pngHeightReadsTheImageHeightNotTheWidth() {
        val file = Files.createTempFile("anilife-shot", ".png")
        try {
            val header = byteArrayOf(
                137.toByte(), 80, 78, 71, 13, 10, 26, 10,
                0, 0, 0, 13, 73, 72, 68, 82,
                0, 0, 1, 64, 0, 0, 0, 180.toByte(),
            )
            Files.write(file, header)
            assertEquals(180, ScreenshotSaver.pngHeight(file))
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun upscaleFactorMatchesTheRequestedResolution() {
        assertEquals(0, upscaleFactor(2160, 2160))
        assertEquals(0, upscaleFactor(2160, 0))
        assertEquals(2, upscaleFactor(1080, 2160))
        assertEquals(4, upscaleFactor(1080, 4320))
        assertEquals(3, upscaleFactor(852, 2160))
    }
}