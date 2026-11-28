package me.enivzoy.anilife.app.screens

import androidx.compose.ui.input.key.Key
import me.enivzoy.anilife.app.data.DefaultPlayerKeyBindings
import me.enivzoy.anilife.app.data.PlayerAction
import me.enivzoy.anilife.app.data.playerActionFor
import me.enivzoy.anilife.app.data.rebindPlayerKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerShortcutsTest {
    private class Recorder {
        var pauses = 0
        val seeks = mutableListOf<Double>()
        val volumes = mutableListOf<Int>()
        var mutes = 0
        var fullscreens = 0
        var pips = 0
        val speeds = mutableListOf<Float>()
        val delays = mutableListOf<Double>()
        val steps = mutableListOf<Int>()
        var screenshots = 0
    }

    private fun shortcuts(recorded: Recorder) = PlayerShortcuts(
        onTogglePause = { recorded.pauses += 1 },
        onSeekRelative = { recorded.seeks += it },
        onVolumeChange = { recorded.volumes += it },
        onToggleMute = { recorded.mutes += 1 },
        onToggleFullscreen = { recorded.fullscreens += 1 },
        onTogglePip = { recorded.pips += 1 },
        onSpeedChange = { recorded.speeds += it },
        onSubtitleDelay = { recorded.delays += it },
        onFrameStep = { recorded.steps += it },
        onScreenshot = { recorded.screenshots += 1 },
    )

    private fun press(recorded: Recorder, key: Key, bindings: Map<PlayerAction, Key> = DefaultPlayerKeyBindings) {
        bindings.playerActionFor(key)?.let { applyPlayerAction(it, shortcuts(recorded)) }
    }

    @Test
    fun spaceAndKPlayAndPause() {
        val recorded = Recorder()

        press(recorded, Key.Spacebar)
        press(recorded, Key.K)

        assertEquals(2, recorded.pauses)
    }

    @Test
    fun theArrowsSeekAndChangeTheVolume() {
        val recorded = Recorder()

        press(recorded, Key.DirectionLeft)
        press(recorded, Key.DirectionRight)
        press(recorded, Key.J)
        press(recorded, Key.L)
        press(recorded, Key.DirectionUp)
        press(recorded, Key.DirectionDown)

        assertEquals(listOf(-5.0, 5.0, -10.0, 10.0), recorded.seeks)
        assertEquals(listOf(5, -5), recorded.volumes)
    }

    @Test
    fun theOtherKeysDoWhatTheySay() {
        val recorded = Recorder()

        press(recorded, Key.M)
        press(recorded, Key.F)
        press(recorded, Key.Z)
        press(recorded, Key.X)
        press(recorded, Key.LeftBracket)
        press(recorded, Key.RightBracket)
        press(recorded, Key.I)

        assertEquals(1, recorded.mutes)
        assertEquals(1, recorded.fullscreens)
        assertEquals(1, recorded.pips)
        assertEquals(listOf(-0.1, 0.1), recorded.delays)
        assertEquals(listOf(-0.1f, 0.1f), recorded.speeds)
    }

    @Test
    fun frameSteppingAndScreenshotsHaveDefaultKeys() {
        val recorded = Recorder()

        press(recorded, Key.D)
        press(recorded, Key.A)
        press(recorded, Key.S)

        assertEquals(listOf(1, -1), recorded.steps)
        assertEquals(1, recorded.screenshots)
    }

    @Test
    fun aCustomBindingReplacesTheDefaultAndSwapsConflicts() {
        val recorded = Recorder()
        val rebound = rebindPlayerKey(DefaultPlayerKeyBindings, PlayerAction.MUTE, Key.M)
        assertEquals(PlayerAction.MUTE, rebound.bindings.playerActionFor(Key.M))

        val swapped = rebindPlayerKey(rebound.bindings, PlayerAction.FULLSCREEN, Key.M)
        assertEquals(PlayerAction.FULLSCREEN, swapped.bindings.playerActionFor(Key.M))
        assertEquals(PlayerAction.MUTE, swapped.conflictWith)

        press(recorded, Key.M, swapped.bindings)
        assertEquals(1, recorded.fullscreens)
    }

    @Test
    fun anUnknownKeyIsNotAShortcut() {
        assertNull(DefaultPlayerKeyBindings.playerActionFor(Key.Q))
    }
}
