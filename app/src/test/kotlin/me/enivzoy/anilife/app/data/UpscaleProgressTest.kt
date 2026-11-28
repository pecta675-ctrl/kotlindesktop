package me.enivzoy.anilife.app.data

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UpscaleProgressTest {
    @Test
    fun percentagesUseThreeSignificantFiguresWithoutScientificNotation() {
        val cases = mapOf(
            0f to "0.00%",
            0.000001234f to "0.000123%",
            0.001234f to "0.123%",
            0.01234f to "1.23%",
            0.06f to "6.00%",
            0.060123f to "6.01%",
            0.069f to "6.90%",
            0.12345f to "12.3%",
            0.999f to "99.9%",
            1f to "100%",
        )
        cases.forEach { (input, expected) -> assertEquals(expected, formatUpscalePercent(input)) }
    }

    @Test
    fun unfinishedOrInvalidProgressCannotDisplayCompletion() {
        assertEquals("99.9%", formatUpscalePercent(0.999999f))
        assertEquals("0.00%", formatUpscalePercent(Float.NaN))
        assertEquals("0.00%", formatUpscalePercent(Float.POSITIVE_INFINITY))
        assertEquals("0.00%", formatUpscalePercent(-1f))
        assertEquals("100%", formatUpscalePercent(2f))
    }

    @Test
    fun formattingIsIndependentOfTheSystemLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("6.01%", formatUpscalePercent(0.060123f))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun everyStageAndChunkMovesForwardAndLeavesRoomForFinalMuxing() {
        val updates = buildList {
            add(0f)
            add(0.05f)
            repeat(24) { chunk ->
                for (frame in 0..100) add(upscaleChunkProgress(chunk, 24, frame / 100.0, 0.0, 0.0))
                for (frame in 0..100) add(upscaleChunkProgress(chunk, 24, 1.0, frame / 100.0, 0.0))
                for (frame in 0..100) add(upscaleChunkProgress(chunk, 24, 1.0, 1.0, frame / 100.0))
            }
            add(0.98f)
            add(0.99f)
        }
        assertTrue(updates.zipWithNext().all { (before, after) -> after >= before })
        assertTrue(updates.all { it < 1f })
        assertEquals(0.98f, upscaleChunkProgress(23, 24, 1.0, 1.0, 1.0))
    }

    @Test
    fun smallFrameIncrementsAreVisibleWithoutWaitingForAWholePercent() {
        val before = upscaleChunkProgress(0, 24, 1.0, 400.0 / 1439, 0.0)
        val after = upscaleChunkProgress(0, 24, 1.0, 410.0 / 1439, 0.0)
        assertTrue(before < after)
        assertTrue(formatUpscalePercent(before) != formatUpscalePercent(after))
    }

    @Test
    fun progressRejectsInvalidChunkNumbersAndBoundsFrameRatios() {
        assertFailsWith<IllegalArgumentException> { upscaleChunkProgress(0, 0, 0.0, 0.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { upscaleChunkProgress(1, 1, 0.0, 0.0, 0.0) }
        assertEquals(0.05f, upscaleChunkProgress(0, 1, -1.0, -1.0, -1.0))
        assertEquals(0.98f, upscaleChunkProgress(0, 1, 2.0, 2.0, 2.0))
    }
}
