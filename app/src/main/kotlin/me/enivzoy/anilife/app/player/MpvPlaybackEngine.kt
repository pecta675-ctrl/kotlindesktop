package me.enivzoy.anilife.app.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import me.enivzoy.anilife.mpv.EndReason
import me.enivzoy.anilife.mpv.Event
import me.enivzoy.anilife.mpv.Format
import me.enivzoy.anilife.mpv.Mpv
import me.enivzoy.anilife.mpv.MpvError
import me.enivzoy.anilife.mpv.PropertyValue
import me.enivzoy.anilife.mpv.defaultPlayerOptions
import me.enivzoy.anilife.app.data.AnimeSubtitleSource
import me.enivzoy.anilife.app.data.AudioLanguage
import me.enivzoy.anilife.app.data.DiscordPlaybackInfo
import me.enivzoy.anilife.app.data.PlayerAction
import me.enivzoy.anilife.app.data.Presence
import me.enivzoy.anilife.app.data.SillyMode
import me.enivzoy.anilife.app.data.SubtitleAppearance
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.ImageInfo
import java.io.File
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import io.github.oshai.kotlinlogging.KotlinLogging
import me.enivzoy.anilife.core.diagnostics.ActionBreadcrumbs

private val logger = KotlinLogging.logger {}

data class PlayerSubtitleTrack(
    val id: Int,
    val label: String,
    val language: String?,
    val external: Boolean,
    val selected: Boolean,
)

/**
 * Picks the caption track to start with. mpv only auto-selects a track when its own `slang`
 * list matches, so a stream that only carries, say, Japanese captions would otherwise start
 * with the captions switched off even though the viewer asked for them.
 */
internal fun chooseSubtitleTrack(
    tracks: List<PlayerSubtitleTrack>,
    language: String,
): PlayerSubtitleTrack? {
    if (tracks.isEmpty()) return null
    val wanted = language.trim().lowercase().takeIf { it.isNotEmpty() && it != "none" }
        ?: return tracks.first()
    return tracks.firstOrNull { track -> subtitleLanguageMatches(track.language, wanted) }
        ?: tracks.first()
}

internal fun subtitleLanguageMatches(code: String?, wanted: String): Boolean {
    val actual = code?.trim()?.lowercase()?.substringBefore('-')?.takeIf(String::isNotBlank) ?: return false
    val target = wanted.trim().lowercase().substringBefore('-')
    return actual == target || actual.take(2) == target.take(2)
}

internal fun hwdecFor(gpuAcceleration: String): String = if (gpuAcceleration == "auto") "auto-copy-safe" else "no"

internal fun withHwdec(options: List<Pair<String, String>>, hwdec: String): List<Pair<String, String>> =
    options.filterNot { it.first == "hwdec" } + ("hwdec" to hwdec)

internal val lightweightBufferKeys = setOf(
    "cache-secs",
    "demuxer-max-bytes",
    "demuxer-max-back-bytes",
    "demuxer-lavf-o",
    "stream-lavf-o",
)

internal fun bufferedPlayerOptions(experimental: Boolean): List<Pair<String, String>> =
    if (experimental) {
        defaultPlayerOptions()
    } else {
        defaultPlayerOptions().filterNot { (name, _) -> name in lightweightBufferKeys } + ("cache" to "no")
    }

/**
 * Drives libmpv for the player screen.
 *
 * mpv decodes into a software render target (see [Mpv.SoftwareVideoSurface]); every decoded
 * frame is turned into an [ImageBitmap] so Compose can draw it under the controls. Transport
 * state comes back through mpv property observation, so the seek bar, play/pause icon and end
 * time follow the real playback instead of a simulated clock.
 */
class MpvPlaybackEngine(
    url: String,
    headers: Map<String, String> = emptyMap(),
    fallbackDurationSeconds: Double = 0.0,
    serverSubtitles: List<AnimeSubtitleSource> = emptyList(),
    private val startPositionSeconds: Double = 0.0,
    private val subtitlesByDefault: Boolean = true,
    private val subtitleLanguage: String = "en",
    subtitleAppearance: SubtitleAppearance = SubtitleAppearance(),
    private val renderWidth: Int = 1280,
    private val renderHeight: Int = 720,
    private val renderFormat: String = "bgra",
private val playbackBufferSize: String = "auto",
    private val gpuAcceleration: String = "off",
    private val experimentalBuffering: Boolean = true,
    private val preferredAudioLanguage: AudioLanguage? = null,
    /** Series metadata needed for Rich Presence; playback state is supplied by this engine. */
    private var presenceInfo: DiscordPlaybackInfo? = null,
    private val onPresence: (Presence?) -> Unit = {},
) : AutoCloseable {

    var positionSeconds by mutableStateOf(0.0)
        private set
    var durationSeconds by mutableStateOf(fallbackDurationSeconds)
        private set
    var isPlaying by mutableStateOf(true)
        private set
    var videoFrame by mutableStateOf<ImageBitmap?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var hasEnded by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var subtitleTracks by mutableStateOf<List<PlayerSubtitleTrack>>(emptyList())
        private set
    var audioTracks by mutableStateOf<List<PlayerAudioTrack>>(emptyList())
        private set
    var switchError by mutableStateOf<String?>(null)
        private set
    var isMuted by mutableStateOf(false)
        private set
    var subtitleDelaySeconds by mutableStateOf(0.0)
        private set

    val currentUrl: String get() = sourceUrl
    // the source mpv actually opened; may lag currentUrl while a switch is loading
    val currentLoadedUrl: String? get() = loadedUrl

    // embedded track first because a source label can lag the real stream
    val currentAudioLanguage: AudioLanguage?
        get() = audioTracks.firstOrNull { it.selected }?.language?.takeIf(AudioLanguage::known)
            ?: sourceAudioLanguage
            ?: audioTracks.firstOrNull { it.selected }?.language

    private val closed = AtomicBoolean(false)
    @Volatile private var player: Mpv? = null
    @Volatile private var surface: Mpv.SoftwareVideoSurface? = null
    @Volatile private var worker: Thread? = null
    @Volatile private var sourceUrl = url
    @Volatile private var sourceHeaders = headers
    @Volatile private var sourceSubtitles = serverSubtitles
    @Volatile private var sourceAudioLanguage = preferredAudioLanguage
    @Volatile private var loadedUrl: String? = null
    @Volatile private var lastGoodSource: LoadedSource? = null
    @Volatile private var pendingRestore: PlaybackRestore? = null
    @Volatile private var pendingSwitchLabel: String? = null
    @Volatile private var rollingBack = false
    private data class LoadedSource(
        val url: String,
        val headers: Map<String, String>,
        val subtitles: List<AnimeSubtitleSource>,
        val language: AudioLanguage?,
    )
    @Volatile private var activeSubtitleAppearance = subtitleAppearance
    @Volatile private var lastPresenceReportAt = 0L
    @Volatile private var lastFrameAt = 0L
    @Volatile private var state = "idle"
    private val positionClock = PositionClock()

    fun start() {
        if (worker != null || closed.get()) return
        SillyMode.pause()
        transition("loading")
        worker = thread(name = "AniLife-playback", isDaemon = true) { runPlayback() }
    }

    private fun transition(next: String, detail: String = "") {
        if (state == next && detail.isBlank()) return
        state = next
        logger.info { if (detail.isBlank()) "mpv state=$next" else "mpv state=$next $detail" }
    }

    private fun runPlayback() {
        try {
            val hwdec = hwdecFor(gpuAcceleration)
            val mpv = createMpvWithFallback(sourceHeaders, hwdec)
            player = mpv
            applySubtitleAppearanceTo(mpv, activeSubtitleAppearance)
            mpv.observeDouble("time-pos")
            mpv.observeDouble("duration")
            mpv.observeFlag("pause")
            mpv.observeFlag("mute")
            mpv.observeProperty("aid", Format.STRING)
            mpv.observeProperty("sid", Format.STRING)
            mpv.startEvents(::onEvent)
            val target = mpv.createSoftwareVideoSurface(renderWidth, renderHeight, renderFormat)
            surface = target
            mpv.loadfile(sourceUrl)

            while (!closed.get()) {
                if (target.needsRender()) {
                    // Limit frame rate to ~30fps to reduce memory pressure from ImageBitmap allocation.
                    val now = System.currentTimeMillis()
                    if (now - lastFrameAt < 33L) {
                        Thread.sleep(33L - (now - lastFrameAt))
                        continue
                    }
                    lastFrameAt = now
                    val bytes = target.render() ?: continue
                    videoFrame = SkiaImage.makeRaster(
                        ImageInfo(renderWidth, renderHeight, ColorType.BGRA_8888, ColorAlphaType.OPAQUE),
                        bytes,
                        renderWidth * 4,
                    ).toComposeImageBitmap()
                } else {
                    Thread.sleep(8L)
                }
            }
        } catch (failure: Throwable) {
            error = failure.message?.takeIf(String::isNotBlank)
                ?: failure::class.simpleName
                ?: "Playback failed"
            val code = (failure as? MpvError)?.code
            transition("error", listOfNotNull(code?.let { "code=$it" }, error).joinToString(" ").ifBlank { "playback failed" })
            logger.error(failure) { "mpv playback failed code=$code" }
            isLoading = false
        } finally {
            release()
        }
    }

    private fun onEvent(event: Event) {
        when (event) {
            is Event.PropertyChange -> when (event.name) {
                "time-pos" -> (event.value as? PropertyValue.DoubleValue)?.value?.let {
                    val position = it.coerceAtLeast(0.0)
                    if (positionClock.offer(position)) positionSeconds = position
                    reportPresence()
                }
                "duration" -> (event.value as? PropertyValue.DoubleValue)?.value?.let {
                    if (it.isFinite() && it > 0.0) {
                        durationSeconds = it
                        reportPresence(force = true)
                    }
                }
                "pause" -> (event.value as? PropertyValue.Flag)?.value?.let {
                    positionClock.flush()
                    isPlaying = !it
                    transition(if (it) "idle" else "playing")
                    SillyMode.paused(it)
                    reportPresence(force = true)
                }
                "mute" -> (event.value as? PropertyValue.Flag)?.value?.let { isMuted = it }
                "aid", "sid" -> refreshTracks()
            }
            Event.FileLoaded -> {
                val firstLoad = lastGoodSource == null
                positionClock.reset()
                isLoading = false
                transition("playing")
                ActionBreadcrumbs.record("Started playback")
                player?.getFlagProperty("pause")?.let { isPlaying = !it }
                player?.getDoubleProperty("duration")
                    ?.takeIf { it.isFinite() && it > 0.0 }
                    ?.let { durationSeconds = it }
                sourceSubtitles.distinctBy(AnimeSubtitleSource::url).forEach { subtitle ->
                    runCatching { player?.addSubtitle(subtitle.url, subtitle.label, subtitle.language) }
                }
                refreshTracks()
                player?.let(::applyDefaultSubtitlesInternal)
                lastGoodSource = LoadedSource(sourceUrl, sourceHeaders, sourceSubtitles, sourceAudioLanguage)
                loadedUrl = sourceUrl
                pendingSwitchLabel = null
                rollingBack = false
                pendingRestore?.let { restore ->
                    player?.let { mpv -> applyRestore(mpv, restore) }
                    pendingRestore = null
                }
                // Resume where the episode was left off, a few seconds back so the viewer hears the
                // line before the one they missed.
                if (firstLoad && startPositionSeconds > 0.0) {
                    val target = startPositionSeconds
                    positionSeconds = target
                    runCatching { player?.seekTo(target) }
                }
                refreshTracks()
                reportPresence(force = true)
            }
            is Event.EndFile -> {
                positionClock.reset()
                if (event.reason == EndReason.EOF) {
                    isLoading = false
                    isPlaying = false
                    positionSeconds = durationSeconds
                    hasEnded = true
                    transition("idle")
                } else if (event.reason == EndReason.ERROR) {
                    if (rollBackAfterFailedSwitch()) {
                        isLoading = true
                    } else {
                        isLoading = false
                        error = event.error ?: "mpv could not play this stream"
                        val code = event.code
                        logger.error { "mpv end-file error code=$code error=$error" }
                        transition("error", "code=$code $error")
                    }
                } else {
                    isLoading = false
                }
                if (!isLoading) onPresence(null)
            }
            is Event.Log -> {
                val level = event.level
                val msg = event.text
                if (level == "error" || level == "fatal") logger.error { "mpv[$level]: $msg" }
                else if (level == "warn") logger.warn { "mpv[$level]: $msg" }
            }
            else -> Unit
        }
    }

    /** Keeps presence metadata current when the episode changes without swapping the stream. */
    fun setPresenceInfo(info: DiscordPlaybackInfo?) {
        presenceInfo = info
        reportPresence(force = true)
    }

    /** Feeds the Discord worker a playback-state snapshot; caller throttles position-only drift. */
    private fun reportPresence(force: Boolean = false) {
        val info = presenceInfo ?: return
        val now = System.currentTimeMillis()
        if (!force && now - lastPresenceReportAt < 1_000L) return
        lastPresenceReportAt = now
        onPresence(
            Presence(
                animeId = info.animeId,
                title = info.title,
                episode = info.episode,
                episodeTitle = info.episodeTitle,
                total = info.total,
                cover = info.cover,
                position = positionSeconds,
                duration = durationSeconds,
                paused = !isPlaying,
                embedded = info.embedded,
            ),
        )
    }

    fun togglePause() {
        val playing = !isPlaying
        isPlaying = playing
        player?.setPaused(!playing)
        reportPresence(force = true)
    }

    fun runPlayerAction(action: PlayerAction) {
        val mpv = player ?: return
        when (action) {
            PlayerAction.PLAY_PAUSE -> mpv.command("cycle", "pause")
            PlayerAction.SEEK_FORWARD_5 -> mpv.command("seek", "5")
            PlayerAction.SEEK_BACKWARD_5 -> mpv.command("seek", "-5")
            PlayerAction.SEEK_FORWARD_10 -> mpv.command("seek", "10")
            PlayerAction.SEEK_BACKWARD_10 -> mpv.command("seek", "-10")
            PlayerAction.VOLUME_UP -> mpv.command("add", "volume", "5")
            PlayerAction.VOLUME_DOWN -> mpv.command("add", "volume", "-5")
            PlayerAction.MUTE -> mpv.command("cycle", "mute")
            PlayerAction.FULLSCREEN -> Unit
            PlayerAction.TOGGLE_PIP -> Unit
            PlayerAction.SUBTITLE_DELAY_LATER -> {
                mpv.command("add", "sub-delay", "0.1")
                subtitleDelaySeconds += 0.1
            }
            PlayerAction.SUBTITLE_DELAY_EARLIER -> {
                mpv.command("add", "sub-delay", "-0.1")
                subtitleDelaySeconds -= 0.1
            }
            PlayerAction.SPEED_UP -> mpv.command("add", "speed", "0.1")
            PlayerAction.SPEED_DOWN -> mpv.command("add", "speed", "-0.1")
            PlayerAction.FRAME_STEP_FORWARD -> stepFrame(mpv, 1)
            PlayerAction.FRAME_STEP_BACKWARD -> stepFrame(mpv, -1)
            PlayerAction.TAKE_SCREENSHOT -> Unit
        }
    }

    private fun stepFrame(mpv: Mpv, direction: Int) {
        mpv.command("set", "pause", "yes")
        mpv.command(if (direction > 0) "frame-step" else "frame-back-step")
    }

    fun screenshotTo(path: Path): Boolean {
        val mpv = player ?: return false
        return runCatching { mpv.screenshotToFile(path.toString()) }.isSuccess
    }

    fun seekTo(seconds: Double) {
        val target = if (durationSeconds > 0.0) {
            seconds.coerceIn(0.0, durationSeconds)
        } else {
            seconds.coerceAtLeast(0.0)
        }
        positionSeconds = target
        player?.seekTo(target)
        reportPresence(force = true)
    }

    fun seekRelative(deltaSeconds: Double) {
        player?.seekRelative(deltaSeconds)
        positionSeconds = (positionSeconds + deltaSeconds).coerceAtLeast(0.0)
        reportPresence(force = true)
    }

    fun selectSubtitleTrack(id: Int?) {
        player?.setPropertyValue("sid", id?.toString() ?: "no")
        if (id != null) player?.setPropertyValue("sub-visibility", "yes")
        refreshTracks()
    }

    fun selectAudioTrack(id: Int?) {
        player?.setPropertyValue("aid", id?.toString() ?: "no")
        switchError = null
        refreshTracks()
    }

    // loadfile keeps volume, speed and mute alive; a failed load falls back to the last good stream
    fun switchSource(
        url: String,
        headers: Map<String, String>,
        subtitles: List<AnimeSubtitleSource>,
        audioLanguage: AudioLanguage?,
        label: String? = null,
    ) {
        if (url.isBlank() || url == sourceUrl) return
        synchronized(this) {
            // An unlabelled dub keeps the stream's own default track instead of guessing.
            val wantedAudio = when {
                audioLanguage == null -> currentAudioLanguage
                audioLanguage.known -> audioLanguage
                else -> null
            }
            pendingRestore = PlaybackRestore(
                positionSeconds = positionSeconds,
                // a switch before the first successful load should still autoplay
                paused = !isPlaying && lastGoodSource != null,
                subtitlePreference = subtitlePreference(),
                audioLanguage = wantedAudio,
            )
            pendingSwitchLabel = label
            sourceUrl = url
            sourceHeaders = headers
            sourceSubtitles = subtitles
            sourceAudioLanguage = audioLanguage
            switchError = null
            rollingBack = false
            error = null
            hasEnded = false
            loadedUrl = null
            val mpv = player
            if (mpv != null) {
                applyHeaders(mpv, headers)
                runCatching { mpv.command("sub-remove") }
                isLoading = true
                transition("loading", "switch")
                mpv.loadfile(url)
            }
        }
    }

    /**
     * Applies the "show captions by default" and "preferred caption language" settings to the
     * current file. Called when the file finishes loading and whenever those settings change
     * during playback.
     */
    fun applyDefaultSubtitles() {
        val mpv = player ?: return
        refreshTracks()
        applyDefaultSubtitlesInternal(mpv)
    }

    private fun applyDefaultSubtitlesInternal(mpv: Mpv) {
        if (!subtitlesByDefault) {
            runCatching { mpv.setPropertyValue("sid", "no") }
            return
        }
        if (subtitleTracks.any(PlayerSubtitleTrack::selected)) return
        val track = chooseSubtitleTrack(subtitleTracks, subtitleLanguage) ?: return
        runCatching { mpv.setPropertyValue("sid", track.id.toString()) }
    }

    fun addSubtitle(source: AnimeSubtitleSource) {
        player?.addSubtitle(source.url, source.label, source.language, select = true)
        refreshTracks()
    }

    fun setVolume(volume: Int) {
        player?.setVolume(volume)
    }

    fun setSpeed(speed: Double) {
        player?.setSpeed(speed)
    }

    fun setMute(muted: Boolean) {
        isMuted = muted
        player?.setPropertyValue("mute", if (muted) "yes" else "no")
    }

    /** Shifts the subtitles earlier (negative) or later (positive), in seconds. */
    fun shiftSubtitles(seconds: Double) {
        subtitleDelaySeconds = seconds
        player?.setPropertyValue("sub-delay", seconds.toString())
    }

    fun setContentScale(scale: String) {
        player?.setPropertyValue("keepaspect", if (scale == "fill") "no" else "yes")
        player?.setPropertyValue("panscan", if (scale == "crop") "1" else "0")
    }

    fun applySubtitleAppearance(appearance: SubtitleAppearance) {
        activeSubtitleAppearance = appearance
        player?.let { applySubtitleAppearanceTo(it, appearance) }
    }

    private fun applySubtitleAppearanceTo(mpv: Mpv, appearance: SubtitleAppearance) {
        subtitleAppearanceOptions(appearance)
            .filterNot { it.first == "sub-fonts-dir" }
            .forEach { (name, value) -> mpv.setPropertyValue(name, value) }
    }

    private fun applyRestore(mpv: Mpv, restore: PlaybackRestore) {
        restore.audioLanguage?.let { wanted ->
            chooseAudioTrack(audioTracks, wanted)?.let { track ->
                runCatching { mpv.setPropertyValue("aid", track.id.toString()) }
            }
        }
        when (val preference = restore.subtitlePreference) {
            null -> Unit
            "off" -> runCatching { mpv.setPropertyValue("sid", "no") }
            else -> findSubtitleTrack(preference)?.let { track ->
                runCatching {
                    mpv.setPropertyValue("sid", track.id.toString())
                    mpv.setPropertyValue("sub-visibility", "yes")
                }
            }
        }
        if (restore.positionSeconds > 0.0) {
            val target = restorePosition(restore.positionSeconds, durationSeconds)
            positionSeconds = target
            runCatching { mpv.seekTo(target) }
        }
        if (restore.paused) {
            runCatching { mpv.setPaused(true) }
            isPlaying = false
        }
    }

    private fun findSubtitleTrack(language: String): PlayerSubtitleTrack? =
        subtitleTracks.firstOrNull { it.selected && subtitleLanguageMatches(it.language, language) }
            ?: subtitleTracks.firstOrNull { subtitleLanguageMatches(it.language, language) }

    private fun subtitlePreference(): String? = when {
        subtitleTracks.isEmpty() -> null
        subtitleTracks.none(PlayerSubtitleTrack::selected) -> "off"
        else -> subtitleTracks.first(PlayerSubtitleTrack::selected).language
    }

    private fun rollBackAfterFailedSwitch(): Boolean {
        val good = lastGoodSource ?: return false
        val mpv = player ?: return false
        val restore = pendingRestore
        if (restore == null || rollingBack || good.url == sourceUrl) return false
        val failedLabel = pendingSwitchLabel
        rollingBack = true
        switchError = if (failedLabel != null) {
            "Couldn't switch to $failedLabel. Keeping the previous stream."
        } else {
            "Couldn't switch the audio. Keeping the previous stream."
        }
        sourceUrl = good.url
        sourceHeaders = good.headers
        sourceSubtitles = good.subtitles
        sourceAudioLanguage = good.language
        pendingRestore = restore
        applyHeaders(mpv, good.headers)
        runCatching { mpv.command("sub-remove") }
        error = null
        loadedUrl = null
        transition("loading", "rollback")
        mpv.loadfile(good.url)
        return true
    }

    private fun applyHeaders(mpv: Mpv, headers: Map<String, String>) {
        runCatching { mpv.setPropertyValue("http-header-fields", httpHeaderFields(headers)) }
    }

    private fun refreshTracks() {
        val text = runCatching { player?.getStringProperty("track-list") }.getOrNull() ?: return
        subtitleTracks = parseSubtitleTracks(text)
        audioTracks = parseAudioTracks(text)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        onPresence(null)
        worker?.let { runCatching { it.join(3_000L) } }
        release()
    }

    private fun release() {
        transition("idle")
        surface?.let { runCatching { it.close() } }
        surface = null
        player?.let { runCatching { it.shutdown() } }
        player = null
    }

    private fun playbackOptions(headers: Map<String, String>, hwdec: String): List<Pair<String, String>> {
        val options = bufferedPlayerOptions(experimentalBuffering)
        return withHwdec(options, hwdec).toMutableList().apply {
            // Captions are drawn into the rendered frame, so they follow these options too.
            add("sub-visibility" to "yes")
            addAll(subtitleAppearanceOptions(activeSubtitleAppearance))
            add("sub-ass-override" to "no")
            subtitleLanguage.trim().lowercase()
                .takeIf { it.isNotEmpty() && it != "none" }
                ?.let { add("slang" to it) }
            val fields = httpHeaderFields(headers)
            if (fields.isNotEmpty()) add("http-header-fields" to fields)
        }
    }

    private fun createMpvWithFallback(headers: Map<String, String>, hwdec: String): Mpv {
        try {
            return Mpv(initialOptions = playbackOptions(headers, hwdec))
        } catch (e: Exception) {
            if (hwdec != "no") {
                System.err.println("mpv[warn]: GPU decoding was disabled for this video: ${e.message}. Falling back to CPU decoding.")
                return Mpv(initialOptions = playbackOptions(headers, "no"))
            }
            throw e
        }
    }
}

internal fun httpHeaderFields(headers: Map<String, String>): String =
    headers.entries
        .filter { it.key.isNotBlank() && it.value.isNotBlank() }
        .joinToString(",") { (name, value) -> "$name: ${value.replace(',', ' ').replace('\n', ' ')}" }

internal fun subtitleAppearanceOptions(appearance: SubtitleAppearance): List<Pair<String, String>> {
    val position = appearance.verticalOffset.coerceIn(0f, 30f)
    return buildList {
        add("sub-font" to appearance.fontFamily)
        add("sub-scale" to appearance.scale.coerceIn(0.5f, 2f).toString())
        add("sub-color" to mpvSubtitleColor(appearance.textColor, appearance.textOpacity))
        add("sub-back-color" to mpvSubtitleColor(appearance.backgroundColor, appearance.backgroundOpacity))
        add("sub-border-color" to "#FF000000")
        add("sub-border-size" to if (appearance.edgeStyle == "outline") "2.5" else "0")
        add("sub-shadow-color" to "#CC000000")
        add("sub-shadow-offset" to if (appearance.edgeStyle == "shadow") "2" else "0")
        add("sub-pos" to if (appearance.verticalPosition == "top") position.toInt().toString() else (100f - position).toInt().toString())
        add("sub-align-x" to appearance.alignment)
        File(appearance.fontFile).takeIf { appearance.fontFile.isNotBlank() }?.parentFile
            ?.let { add("sub-fonts-dir" to it.absolutePath) }
    }
}

private fun mpvSubtitleColor(color: String, opacity: Float): String {
    val rgb = color.removePrefix("#").takeLast(6).uppercase()
    val alpha = (opacity.coerceIn(0f, 1f) * 255).toInt().toString(16).uppercase().padStart(2, '0')
    return "#$alpha$rgb"
}

