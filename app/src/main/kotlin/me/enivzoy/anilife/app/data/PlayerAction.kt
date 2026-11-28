package me.enivzoy.anilife.app.data

import androidx.compose.ui.input.key.Key

enum class PlayerAction(val label: String) {
    PLAY_PAUSE("Play / Pause"),
    SEEK_FORWARD_5("Seek forward 5 seconds"),
    SEEK_BACKWARD_5("Seek backward 5 seconds"),
    SEEK_FORWARD_10("Seek forward 10 seconds"),
    SEEK_BACKWARD_10("Seek backward 10 seconds"),
    VOLUME_UP("Volume up"),
    VOLUME_DOWN("Volume down"),
    MUTE("Mute"),
    FULLSCREEN("Fullscreen"),
    SUBTITLE_DELAY_LATER("Subtitle delay later"),
    SUBTITLE_DELAY_EARLIER("Subtitle delay earlier"),
    SPEED_UP("Speed up"),
    SPEED_DOWN("Speed down"),
    TOGGLE_PIP("Picture in picture"),
    FRAME_STEP_FORWARD("Next frame"),
    FRAME_STEP_BACKWARD("Previous frame"),
    TAKE_SCREENSHOT("Save screenshot"),
    ;
}

val DefaultPlayerKeyBindings: Map<PlayerAction, Key> = linkedMapOf(
    PlayerAction.PLAY_PAUSE to Key.Spacebar,
    PlayerAction.SEEK_FORWARD_5 to Key.DirectionRight,
    PlayerAction.SEEK_BACKWARD_5 to Key.DirectionLeft,
    PlayerAction.SEEK_FORWARD_10 to Key.L,
    PlayerAction.SEEK_BACKWARD_10 to Key.J,
    PlayerAction.VOLUME_UP to Key.DirectionUp,
    PlayerAction.VOLUME_DOWN to Key.DirectionDown,
    PlayerAction.MUTE to Key.M,
    PlayerAction.FULLSCREEN to Key.F,
    PlayerAction.SUBTITLE_DELAY_LATER to Key.X,
    PlayerAction.SUBTITLE_DELAY_EARLIER to Key.Z,
    PlayerAction.SPEED_UP to Key.RightBracket,
    PlayerAction.SPEED_DOWN to Key.LeftBracket,
    PlayerAction.TOGGLE_PIP to Key.I,
    PlayerAction.FRAME_STEP_FORWARD to Key.D,
    PlayerAction.FRAME_STEP_BACKWARD to Key.A,
    PlayerAction.TAKE_SCREENSHOT to Key.S,
)

private val DefaultPlayerKeyAliases: Map<Key, PlayerAction> = mapOf(
    Key.K to PlayerAction.PLAY_PAUSE,
)

private val KnownPlayerKeyCodes: Set<Long> by lazy {
    Key.Companion::class.java.declaredMethods
        .filter { method -> method.returnType == java.lang.Long.TYPE && method.parameterCount == 0 }
        .mapNotNull { method -> runCatching { method.invoke(Key.Companion) as Long }.getOrNull() }
        .toSet()
}

private val PlayerKeyLabels: Map<Key, String> = mapOf(
    Key.Spacebar to "Space",
    Key.DirectionLeft to "Left",
    Key.DirectionRight to "Right",
    Key.DirectionUp to "Up",
    Key.DirectionDown to "Down",
    Key.LeftBracket to "[",
    Key.RightBracket to "]",
    Key.Escape to "Esc",
    Key.Enter to "Enter",
    Key.Tab to "Tab",
    Key.Backspace to "Backspace",
)

private fun Long.isBindablePlayerKeyCode(): Boolean = this != Key.Unknown.keyCode && this in KnownPlayerKeyCodes

private fun Key.isBindablePlayerKey(): Boolean = keyCode.isBindablePlayerKeyCode()

fun Map<PlayerAction, Key>.withPlayerDefaults(): Map<PlayerAction, Key> {
    val normalized = linkedMapOf<PlayerAction, Key>()
    PlayerAction.entries.forEach { action ->
        val candidate = this[action]?.takeIf { it.isBindablePlayerKey() } ?: DefaultPlayerKeyBindings.getValue(action)
        if (normalized.values.none { it == candidate }) normalized[action] = candidate
    }
    return normalized
}

fun Map<PlayerAction, Key>.playerActionFor(key: Key): PlayerAction? {
    val bindings = withPlayerDefaults()
    bindings.firstNotNullOfOrNull { (action, bound) -> action.takeIf { bound == key } }?.let { return it }
    val alias = DefaultPlayerKeyAliases[key] ?: return null
    return alias.takeIf { bindings[it] == DefaultPlayerKeyBindings[it] && key !in bindings.values }
}

fun serializePlayerKeyBindings(bindings: Map<PlayerAction, Key>): String =
    PlayerAction.entries.joinToString(",") { action ->
        val key = bindings[action] ?: DefaultPlayerKeyBindings.getValue(action)
        "${action.name}:${key.keyCode}"
    }

fun parsePlayerKeyBindings(value: String?): Map<PlayerAction, Key> {
    if (value.isNullOrBlank()) return DefaultPlayerKeyBindings
    val parsed = linkedMapOf<PlayerAction, Key>()
    value.splitToSequence(',').forEach { entry ->
        val parts = entry.split(':', limit = 2)
        if (parts.size != 2) return@forEach
        val action = PlayerAction.entries.firstOrNull { it.name.equals(parts[0].trim(), ignoreCase = true) }
            ?: return@forEach
        val code = parts[1].trim().toLongOrNull()?.takeIf(Long::isBindablePlayerKeyCode) ?: return@forEach
        parsed[action] = Key(code)
    }
    return parsed.withPlayerDefaults()
}

data class PlayerKeyRebind(
    val bindings: Map<PlayerAction, Key>,
    val conflictWith: PlayerAction?,
)

fun rebindPlayerKey(
    bindings: Map<PlayerAction, Key>,
    action: PlayerAction,
    key: Key,
): PlayerKeyRebind {
    if (!key.isBindablePlayerKey()) return PlayerKeyRebind(bindings.withPlayerDefaults(), null)
    val updated = LinkedHashMap(bindings.withPlayerDefaults())
    val conflict = updated.entries.firstOrNull { (other, bound) -> other != action && bound == key }?.key
    if (conflict != null) updated[conflict] = updated.getValue(action)
    updated[action] = key
    return PlayerKeyRebind(updated.withPlayerDefaults(), conflict)
}

fun playerKeyLabel(key: Key): String = when {
    !key.isBindablePlayerKey() -> "Unassigned"
    key in PlayerKeyLabels -> PlayerKeyLabels.getValue(key)
    else -> key.toString().removePrefix("Key.").ifBlank { "Unknown" }
}
