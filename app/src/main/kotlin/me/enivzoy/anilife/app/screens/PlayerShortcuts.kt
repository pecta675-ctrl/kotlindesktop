package me.enivzoy.anilife.app.screens

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import me.enivzoy.anilife.app.data.PlayerAction
import me.enivzoy.anilife.app.data.playerActionFor

internal data class PlayerShortcuts(
    val onTogglePause: () -> Unit,
    val onSeekRelative: (Double) -> Unit,
    val onVolumeChange: (Int) -> Unit,
    val onToggleMute: () -> Unit,
    val onToggleFullscreen: () -> Unit,
    val onTogglePip: () -> Unit,
    val onSpeedChange: (Float) -> Unit,
    val onSubtitleDelay: (Double) -> Unit,
    val onFrameStep: (Int) -> Unit,
    val onScreenshot: () -> Unit,
)

internal fun applyPlayerAction(action: PlayerAction, shortcuts: PlayerShortcuts) {
    when (action) {
        PlayerAction.PLAY_PAUSE -> shortcuts.onTogglePause()
        PlayerAction.SEEK_FORWARD_5 -> shortcuts.onSeekRelative(5.0)
        PlayerAction.SEEK_BACKWARD_5 -> shortcuts.onSeekRelative(-5.0)
        PlayerAction.SEEK_FORWARD_10 -> shortcuts.onSeekRelative(10.0)
        PlayerAction.SEEK_BACKWARD_10 -> shortcuts.onSeekRelative(-10.0)
        PlayerAction.VOLUME_UP -> shortcuts.onVolumeChange(5)
        PlayerAction.VOLUME_DOWN -> shortcuts.onVolumeChange(-5)
        PlayerAction.MUTE -> shortcuts.onToggleMute()
        PlayerAction.FULLSCREEN -> shortcuts.onToggleFullscreen()
        PlayerAction.TOGGLE_PIP -> shortcuts.onTogglePip()
        PlayerAction.SUBTITLE_DELAY_LATER -> shortcuts.onSubtitleDelay(0.1)
        PlayerAction.SUBTITLE_DELAY_EARLIER -> shortcuts.onSubtitleDelay(-0.1)
        PlayerAction.SPEED_UP -> shortcuts.onSpeedChange(0.1f)
        PlayerAction.SPEED_DOWN -> shortcuts.onSpeedChange(-0.1f)
        PlayerAction.FRAME_STEP_FORWARD -> shortcuts.onFrameStep(1)
        PlayerAction.FRAME_STEP_BACKWARD -> shortcuts.onFrameStep(-1)
        PlayerAction.TAKE_SCREENSHOT -> shortcuts.onScreenshot()
    }
}

internal fun handlePlayerShortcut(
    event: KeyEvent,
    shortcuts: PlayerShortcuts,
    bindings: Map<PlayerAction, Key>,
    textInputFocused: Boolean = false,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    if (textInputFocused) return false
    if (event.isCtrlPressed || event.isMetaPressed || event.isAltPressed) return false
    val action = bindings.playerActionFor(event.key) ?: return false
    applyPlayerAction(action, shortcuts)
    return true
}
