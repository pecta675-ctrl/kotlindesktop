package me.enivzoy.anilife.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.enivzoy.anilife.mpv.Mpv
import me.enivzoy.anilife.mpv.Event
import me.enivzoy.anilife.mpv.EndReason
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.coroutines.coroutineContext
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

object SillyMode {
    private const val YT_DLP_RELEASE = "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private class Session {
        var job: Job? = null
        var process: Process? = null
        var player: Mpv? = null
    }
    private var session: Session? = null
    private var videoPlaying = false
    private var audioAllowed = true
    private var resumeJob: Job? = null

    fun start() = synchronized(lock) {
        if (session != null) return@synchronized
        val current = Session()
        session = current
        current.job = scope.launch {
            try {
                play(current)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                logger.warn(failure) { "silly mode: no soundtrack today. the disco dances on." }
            } finally {
                synchronized(lock) { if (session === current) session = null }
            }
        }
    }

    fun stop() = synchronized(lock) {
        resumeJob?.cancel()
        resumeJob = null
        val current = session ?: return@synchronized
        session = null
        runCatching { current.player?.pause() }
        current.job?.cancel()
        current.process?.descendants()?.use { children -> children.forEach { it.destroyForcibly() } }
        current.process?.destroyForcibly()
    }

    fun pause() = synchronized(lock) {
        videoPlaying = true
        audioAllowed = false
        resumeJob?.cancel()
        resumeJob = null
        if (session?.player != null) logger.info { "silly audio paused for video" }
        runCatching { session?.player?.setPaused(true) }
    }

    fun resume() = synchronized(lock) {
        videoPlaying = false
        audioAllowed = true
        resumeJob?.cancel()
        resumeJob = null
        if (session?.player != null) logger.info { "silly audio resumed" }
        runCatching { session?.player?.play() }
    }

    fun paused(paused: Boolean) = synchronized(lock) {
        if (!videoPlaying) return@synchronized
        resumeJob?.cancel()
        resumeJob = null
        if (!paused) {
            audioAllowed = false
            runCatching { session?.player?.setPaused(true) }
            return@synchronized
        }
        val current = session
        resumeJob = scope.launch {
            delay(5_000)
            synchronized(lock) {
                if (videoPlaying && session === current) {
                    audioAllowed = true
                    logger.info { "silly audio back after pause" }
                    runCatching { current?.player?.play() }
                }
            }
        }
    }

    private suspend fun play(current: Session) {
        val appData = System.getenv("APPDATA")?.takeIf(String::isNotBlank)?.let(::File)
            ?: File(System.getProperty("user.home"))
        val root = File(appData, "AniLife")
        val available = discoTracks.toMutableList()
        while (available.isNotEmpty()) {
            coroutineContext.ensureActive()
            val track = pickDiscoTrack(available)
            val file = discoTrackFile(root, track)
            try {
                if (!file.isFile || file.length() == 0L) {
                    val ytDlp = ensureYtDlp(File(root, "bin"))
                    coroutineContext.ensureActive()
                    downloadTrack(current, ytDlp, file, track)
                }
                coroutineContext.ensureActive()
                val mpv = Mpv(initialOptions = discoPlayerOptions(track))
                try {
                    val ended = CompletableDeferred<Event.EndFile>()
                    mpv.startEvents { event ->
                        if (event is Event.EndFile) ended.complete(event)
                    }
                    synchronized(lock) {
                        if (session !== current) throw CancellationException("Disco mode stopped")
                        current.player = mpv
                        mpv.loadfile(file.absolutePath)
                        if (audioAllowed) mpv.play()
                    }
                    logger.info { "silly audio playing track=${track.videoId} start=${track.startSeconds}s choices=${available.size}" }
                    val event = ended.await()
                    check(event.reason == EndReason.EOF) { event.error ?: "Soundtrack stopped: ${event.reason}" }
                } finally {
                    synchronized(lock) { if (current.player === mpv) current.player = null }
                    withContext(NonCancellable + Dispatchers.IO) { runCatching { mpv.shutdown() } }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                available.remove(track)
                logger.warn(failure) { "silly audio unavailable track=${track.videoId}; trying remaining tracks" }
            }
        }
        logger.warn { "silly mode: none of the soundtracks could play. the disco dances on." }
    }

    private suspend fun downloadTrack(current: Session, ytDlp: File, target: File, track: DiscoTrack) {
        target.parentFile.mkdirs()
        val temporary = Files.createTempDirectory(target.parentFile.toPath(), "download-").toFile()
        try {
            val file = File(temporary, target.name)
            val directory = System.getProperty("AniLife.ffmpeg.dir")?.let(::File) ?: File(dependencyDirectory(), "ffmpeg")
            val ffmpeg = File(directory, if (System.getProperty("os.name").startsWith("Windows", true)) "ffmpeg.exe" else "ffmpeg")
            val command = discoDownloadCommand(ytDlp, ffmpeg.takeIf(File::isFile), file, track)
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            val output = StringBuilder()
            try {
                synchronized(lock) {
                    if (session !== current) throw CancellationException("Disco mode stopped")
                    current.process = process
                }
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        output.append(line.takeLast(2_000)).append('\n')
                        if (output.length > 8_000) output.delete(0, output.length - 4_000)
                    }
                }
                val code = process.waitFor()
                coroutineContext.ensureActive()
                check(code == 0 && file.isFile && file.length() > 0L) { "YouTube audio download failed (exit $code): ${output.takeLast(500)}" }
                Files.move(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } finally {
                withContext(NonCancellable + Dispatchers.IO) {
                    if (process.isAlive) {
                        process.destroyForcibly()
                        process.waitFor()
                    }
                }
                synchronized(lock) { if (current.process === process) current.process = null }
            }
        } finally {
            temporary.deleteRecursively()
        }
    }

    private fun ensureYtDlp(directory: File): File {
        val exe = File(directory, "yt-dlp.exe")
        if (exe.isFile && exe.length() > 0L) return exe
        directory.mkdirs()

        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
        val release = client.send(
            HttpRequest.newBuilder(URI.create(YT_DLP_RELEASE))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "AniLife")
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        ).body()

        val assets = Json.parseToJsonElement(release).jsonObject["assets"]?.jsonArray
            ?: error("yt-dlp release lookup failed")
        fun asset(name: String) = assets.map { it.jsonObject }
            .firstOrNull { it["name"]?.jsonPrimitive?.content == name }

        val binary = asset("yt-dlp.exe") ?: error("yt-dlp.exe missing from release")
        val url = binary["browser_download_url"]?.jsonPrimitive?.content ?: error("yt-dlp url missing")
        val expected = binary["digest"]?.jsonPrimitive?.content
            ?.takeIf(String::isNotBlank)
            ?.removePrefix("sha256:")
            ?: sha256FromSums(client, asset("SHA2-256SUMS"))

        val bytes = client.send(
            HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(3))
                .header("User-Agent", "AniLife")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofByteArray(),
        ).body()
        val actual = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        if (!actual.equals(expected, ignoreCase = true)) error("yt-dlp checksum mismatch")
        exe.writeBytes(bytes)
        return exe
    }

    private fun sha256FromSums(client: HttpClient, sums: JsonObject?): String {
        val url = sums?.get("browser_download_url")?.jsonPrimitive?.content
            ?: error("yt-dlp checksum unavailable")
        val text = client.send(
            HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "AniLife")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        ).body()
        return text.lineSequence()
            .map { it.trim().split(Regex("\\s+")) }
            .firstOrNull { it.size >= 2 && it.last() == "yt-dlp.exe" }
            ?.first()
            ?: error("yt-dlp checksum missing from SHA2-256SUMS")
    }
}
