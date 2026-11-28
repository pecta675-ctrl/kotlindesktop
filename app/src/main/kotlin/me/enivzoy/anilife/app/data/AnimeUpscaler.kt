package me.enivzoy.anilife.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Comparator
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import io.github.oshai.kotlinlogging.KotlinLogging

private const val MODEL = "realesr-animevideov3"
private const val TARGET_HEIGHT = 2160
private const val CHUNK_SECONDS = 60.0
private val upscaleLogger = KotlinLogging.logger {}
private const val LINUX_ARCHIVE = "https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/realesrgan-ncnn-vulkan-20220424-ubuntu.zip"
private const val MAC_ARCHIVE = "https://github.com/xinntao/Real-ESRGAN/releases/download/v0.2.5.0/realesrgan-ncnn-vulkan-20220424-macos.zip"

class AnimeUpscaler(private val allowHardwareEncoding: Boolean = true) {
    suspend fun upscale(
        episode: LocalEpisode,
        mode: UpscaleMode = UpscaleMode.AIQuality,
        onProgress: (UpscaleProgress) -> Unit,
    ): Path =
        withContext(Dispatchers.IO) {
            var lastFraction = 0f
            var lastStage: UpscaleStage? = null
            var lastLog = 0L
            var completedFrames = 0L
            var totalFrames = 0L
            var speed: Double? = null
            fun report(stage: UpscaleStage, fraction: Float, detail: String, idle: Long = 0) {
                lastFraction = maxOf(lastFraction, fraction.coerceIn(0f, 1f))
                val update = UpscaleProgress(lastFraction, stage, detail, completedFrames, totalFrames, speed, idle)
                onProgress(update)
                val now = System.nanoTime()
                if (stage != lastStage || now - lastLog >= TimeUnit.SECONDS.toNanos(30)) {
                    upscaleLogger.info {
                        "upscale ${formatUpscalePercent(lastFraction)} stage=${stage.label} " +
                            "frames=$completedFrames/$totalFrames fps=$speed idleSeconds=$idle $detail"
                    }
                    lastLog = now
                    lastStage = stage
                }
            }
            report(UpscaleStage.Preparing, 0f, "Reading source video")
            val input = episode.path
            require(Files.isRegularFile(input)) { "Episode file not found: ${input}" }
            val ffmpeg = findTool("ffmpeg")
                ?: error("ffmpeg is missing from AniLife's managed runtime.")
            val ffprobe = findTool("ffprobe")
                ?: error("ffprobe is missing from AniLife's managed runtime.")
            val source = probe(ffprobe, input)
            require(source.duration > 0) { "Couldn't read the episode duration." }
            if (mode == UpscaleMode.Fast4K) {
                return@withContext fastUpscale(ffmpeg, ffprobe, input, source, onProgress)
            }
            val scale = (TARGET_HEIGHT.toDouble() / source.height).roundToInt().coerceIn(2, 4)
            totalFrames = (source.duration * source.rate).roundToLong()
            val sourceLabel = "${source.width}×${source.height} → 2160p"
            upscaleLogger.info { "upscale source=${input.fileName} $sourceLabel rate=${source.rate} duration=${source.duration} model=$MODEL-x$scale" }
            val upscaler = findUpscaler({ report(UpscaleStage.Preparing, it, "Preparing AI model · $sourceLabel") }, scale)
            val chunkFrames = (CHUNK_SECONDS * source.rate).roundToInt().coerceAtLeast(1)
            val chunks = ceil(source.duration * source.rate / chunkFrames).toInt().coerceAtLeast(1)
            val temporary = Files.createTempDirectory("anilife-upscale-")
            upscaleLogger.info { "upscale workspace=$temporary chunks=$chunks" }
            try {
                val parts = mutableListOf<Path>()
                var inferenceNanos = 0L
                repeat(chunks) { index ->
                    checkActive()
                    val part = temporary.resolve("part%05d.mkv".format(Locale.ROOT, index))
                    val chunk = Files.createDirectory(temporary.resolve("chunk%05d".format(Locale.ROOT, index)))
                    try {
                        val frames = Files.createDirectory(chunk.resolve("frames"))
                        val upscaled = Files.createDirectory(chunk.resolve("upscaled"))
                        val start = index.toDouble() * chunkFrames / source.rate
                        val chunkLabel = "Part ${index + 1}/$chunks · $sourceLabel"
                        val extractedFrames = AtomicLong()
                        report(UpscaleStage.Extracting, upscaleChunkProgress(index, chunks, 0.0, 0.0, 0.0), chunkLabel)
                        run(
                            listOf(
                                ffmpeg, "-v", "error", "-y", "-progress", "pipe:1", "-nostats",
                                "-ss", "%.3f".format(Locale.ROOT, start), "-i", input.toString(),
                                "-frames:v", chunkFrames.toString(),
                                "-qscale:v", "1", "-qmin", "1", "-qmax", "1",
                                frames.resolve("frame%08d.png").toString(),
                            ),
                            onLine = { line -> line.removePrefix("frame=").trim().toLongOrNull()?.let(extractedFrames::set) },
                        ) { idle ->
                            report(
                                UpscaleStage.Extracting,
                                upscaleChunkProgress(index, chunks, extractedFrames.get().toDouble() / chunkFrames, 0.0, 0.0),
                                chunkLabel,
                                idle,
                            )
                        }
                        checkActive()
                        val total = countFrames(frames)
                        require(total > 0) { "ffmpeg didn't extract any frames." }
                        val previousFrames = completedFrames
                        val inferenceStarted = System.nanoTime()
                        report(UpscaleStage.Upscaling, upscaleChunkProgress(index, chunks, 1.0, 0.0, 0.0), chunkLabel)
                        run(
                            listOf(
                                upscaler, "-i", frames.toString(), "-o", upscaled.toString(),
                                "-n", MODEL, "-s", scale.toString(), "-f", "png",
                            ),
                            workingDirectory = File(upscaler).absoluteFile.parentFile,
                        ) { idle ->
                            val done = countFrames(upscaled).coerceAtMost(total)
                            completedFrames = previousFrames + done
                            val elapsed = (inferenceNanos + System.nanoTime() - inferenceStarted) / 1_000_000_000.0
                            speed = if (completedFrames > 0 && elapsed > 0) completedFrames / elapsed else null
                            report(
                                UpscaleStage.Upscaling,
                                upscaleChunkProgress(index, chunks, 1.0, done.toDouble() / total, 0.0),
                                "$chunkLabel · $done/$total frames in this part",
                                idle,
                            )
                        }
                        inferenceNanos += System.nanoTime() - inferenceStarted
                        checkActive()
                        val written = countFrames(upscaled)
                        require(written == total) { "Upscaled $written of $total frames in part ${index + 1}." }
                        completedFrames = previousFrames + written
                        val encodedFrames = AtomicLong()
                        report(UpscaleStage.Encoding, upscaleChunkProgress(index, chunks, 1.0, 1.0, 0.0), chunkLabel)
                        run(
                            listOf(
                                ffmpeg, "-v", "error", "-y", "-progress", "pipe:1", "-nostats", "-framerate", source.rate.toString(),
                                "-i", upscaled.resolve("frame%08d.png").toString(),
                                "-vf", "scale=-2:$TARGET_HEIGHT:flags=lanczos",
                                "-c:v", "libx264", "-crf", "18", "-pix_fmt", "yuv420p",
                                part.toString(),
                            ),
                            onLine = { line -> line.removePrefix("frame=").trim().toLongOrNull()?.let(encodedFrames::set) },
                        ) { idle ->
                            report(
                                UpscaleStage.Encoding,
                                upscaleChunkProgress(index, chunks, 1.0, 1.0, encodedFrames.get().toDouble() / total),
                                chunkLabel,
                                idle,
                            )
                        }
                        parts.add(part)
                    } finally {
                        deleteRecursively(chunk)
                    }
                }
                checkActive()
                report(UpscaleStage.Joining, 0.98f, "Joining $chunks video parts")
                val output = outputPath(input)
                val video = temporary.resolve("video.mkv")
                val listing = temporary.resolve("parts.txt")
                Files.writeString(listing, parts.joinToString("\n") { "file '${it.fileName}'" })
                run(
                    listOf(ffmpeg, "-v", "error", "-y", "-f", "concat", "-safe", "0", "-i", listing.fileName.toString(), "-c", "copy", video.fileName.toString()),
                    workingDirectory = temporary.toFile(),
                ) { idle -> report(UpscaleStage.Joining, 0.98f, "Joining $chunks video parts", idle) }
                report(UpscaleStage.Audio, 0.99f, "Copying original audio into the 4K video")
                run(
                    listOf(
                        ffmpeg, "-v", "error", "-y",
                        "-i", video.fileName.toString(), "-i", input.toString(),
                        "-map", "0:v:0", "-map", "1:a?", "-c", "copy", output.toAbsolutePath().toString(),
                    ),
                    workingDirectory = temporary.toFile(),
                ) { idle -> report(UpscaleStage.Audio, 0.99f, "Copying original audio into the 4K video", idle) }
                checkActive()
                val result = probe(ffprobe, output)
                require(result.height == TARGET_HEIGHT && Files.size(output) > 0) { "The upscaler didn't produce a valid 2160p video." }
                totalFrames = completedFrames
                report(UpscaleStage.Completed, 1f, "4K video saved")
                output
            } catch (cancelled: CancellationException) {
                upscaleLogger.info { "upscale cancelled stage=$lastStage frames=$completedFrames/$totalFrames" }
                throw cancelled
            } catch (failure: Throwable) {
                upscaleLogger.error(failure) { "upscale failed stage=$lastStage frames=$completedFrames/$totalFrames" }
                throw failure
            } finally {
                deleteRecursively(temporary)
            }
        }

    private suspend fun fastUpscale(
        ffmpeg: String,
        ffprobe: String,
        input: Path,
        source: Source,
        onProgress: (UpscaleProgress) -> Unit,
    ): Path {
        val total = (source.duration * source.rate).roundToLong().coerceAtLeast(1)
        val frames = AtomicLong()
        val timeMicros = AtomicLong()
        val sourceLabel = "${source.width}×${source.height} → 2160p"
        var lastFraction = 0f
        var lastStage: UpscaleStage? = null
        var lastLog = 0L
        var started = System.nanoTime()
        fun report(stage: UpscaleStage, fraction: Float, detail: String, idle: Long = 0) {
            lastFraction = maxOf(lastFraction, fraction)
            val elapsed = (System.nanoTime() - started) / 1_000_000_000.0
            val done = frames.get()
            val speed = if (elapsed > 0 && done > 0) done / elapsed else null
            onProgress(UpscaleProgress(lastFraction, stage, detail, done, if (stage == UpscaleStage.Completed) done else total, speed, idle))
            if (lastStage != stage || System.nanoTime() - lastLog >= TimeUnit.SECONDS.toNanos(30)) {
                upscaleLogger.info { "fast 4K ${formatUpscalePercent(lastFraction)} stage=$stage frames=$done/$total fps=$speed idleSeconds=$idle $detail" }
                lastStage = stage
                lastLog = System.nanoTime()
            }
        }
        report(UpscaleStage.Preparing, 0.01f, "Checking available NVIDIA / AMD / Intel encoders · $sourceLabel")
        val encoders = if (allowHardwareEncoding) fastEncoders(ffmpeg) else listOf(softwareUpscaleEncoder)
        val output = outputPath(input, fast = true)
        return withUpscaleEncoderFallback(
            candidates = encoders,
            probe = { encoder ->
                report(UpscaleStage.Preparing, 0.02f, "Testing ${encoder.name} at 2160p · $sourceLabel")
                testEncoder(ffmpeg, ffprobe, source, encoder)
            },
            encode = { encoder ->
                frames.set(0)
                timeMicros.set(0)
                started = System.nanoTime()
                upscaleLogger.info { "fast 4K source=${input.fileName} encoder=${encoder.name} output=${output.fileName}" }
                try {
                    val detail = "Fast 4K · ${encoder.name} · $sourceLabel · resizing + light sharpening (not AI)"
                    report(UpscaleStage.Encoding, 0.05f, detail)
                    run(
                        listOf(ffmpeg, "-v", "error", "-y", "-progress", "pipe:1", "-nostats") + encoder.inputArguments + listOf(
                            "-i", input.toString(), "-map", "0:v:0", "-map", "0:a?",
                            "-vf", encoder.filter, "-pix_fmt", encoder.pixelFormat,
                        ) + encoder.arguments + listOf("-c:a", "copy", output.toAbsolutePath().toString()),
                        onLine = { line ->
                            if (line.startsWith("frame=")) line.substringAfter('=').trim().toLongOrNull()?.let(frames::set)
                            if (line.startsWith("out_time_us=")) line.substringAfter('=').trim().toLongOrNull()?.let(timeMicros::set)
                        },
                    ) { idle ->
                        val fraction = (timeMicros.get() / (source.duration * 1_000_000)).coerceIn(0.0, 1.0)
                        report(UpscaleStage.Encoding, (0.05 + 0.94 * fraction).toFloat(), detail, idle)
                    }
                    checkActive()
                    report(UpscaleStage.Encoding, 0.99f, "Verifying 2160p output · ${encoder.name}")
                    val result = probe(ffprobe, output)
                    require(result.height == TARGET_HEIGHT && Files.size(output) > 0) { "Fast 4K didn't produce a valid 2160p video." }
                    report(UpscaleStage.Completed, 1f, "Fast 4K copy saved · ${encoder.name} · original kept")
                    output
                } catch (failure: Throwable) {
                    Files.deleteIfExists(output)
                    if (failure is CancellationException) upscaleLogger.info { "fast 4K cancelled frames=${frames.get()}/$total" }
                    else upscaleLogger.warn(failure) { "fast 4K attempt failed encoder=${encoder.name} frames=${frames.get()}/$total" }
                    throw failure
                }
            },
            onUnavailable = { encoder, failure ->
                upscaleLogger.warn { "fast 4K ${encoder.name} unavailable; trying next encoder: ${failure?.message ?: "2160p test failed or timed out"}" }
                val restarting = lastStage == UpscaleStage.Encoding
                if (restarting) {
                    lastFraction = 0f
                    frames.set(0)
                    timeMicros.set(0)
                }
                report(
                    UpscaleStage.Preparing,
                    if (restarting) 0.01f else lastFraction,
                    if (restarting) "${encoder.name} failed; restarting with another encoder"
                    else "${encoder.name} unavailable; trying another encoder",
                )
            },
        )
    }

    private suspend fun fastEncoders(ffmpeg: String): List<UpscaleEncoder> {
        val devices = try {
            if (System.getProperty("os.name").startsWith("Linux", true) && Files.isDirectory(Path.of("/dev/dri"))) {
                Files.list(Path.of("/dev/dri")).use { paths ->
                    paths.filter { it.fileName.toString().matches(Regex("renderD\\d+")) }.map(Path::toString).toList()
                }
            } else emptyList()
        } catch (failure: Exception) {
            upscaleLogger.warn { "Couldn't enumerate VA-API devices: ${failure.message}" }
            emptyList()
        }
        val candidates = upscaleEncoderCandidates(System.getProperty("os.name"), devices)
        val advertised = try {
            withTimeoutOrNull(10_000) { advertisedVideoEncoders(capture(listOf(ffmpeg, "-hide_banner", "-encoders"))) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            upscaleLogger.warn { "Couldn't list FFmpeg encoders; checking candidates individually: ${failure.message}" }
            null
        }
        return candidates.filter { !it.hardware || advertised == null || it.codec in advertised }
    }

    private suspend fun testEncoder(ffmpeg: String, ffprobe: String, source: Source, encoder: UpscaleEncoder): Boolean {
        val output = Files.createTempFile("anilife-encoder-check-", ".mkv")
        val width = ((source.width.toDouble() * TARGET_HEIGHT / source.height) / 2).roundToInt() * 2
        return try {
            withTimeoutOrNull(15_000) {
                run(
                    listOf(ffmpeg, "-v", "error", "-y") + encoder.inputArguments + listOf(
                        "-f", "lavfi", "-i", "color=size=${width}x$TARGET_HEIGHT:rate=${source.rate}:duration=1",
                        "-frames:v", "2", "-vf", encoder.filter, "-pix_fmt", encoder.pixelFormat,
                    ) + encoder.arguments + listOf(output.toString()),
                )
                val result = probe(ffprobe, output)
                result.height == TARGET_HEIGHT && result.width == width && Files.size(output) > 0
            } ?: false
        } finally {
            Files.deleteIfExists(output)
        }
    }

    private suspend fun findUpscaler(onProgress: (Float) -> Unit, scale: Int): String {
        val configured = System.getProperty("AniLife.upscaler.dir")?.takeIf(String::isNotBlank)?.let { Path.of(it) }
        val directory = configured ?: dependencyDirectory().resolve("upscaler").toPath()
        Files.createDirectories(directory)
        val executable = if (isWindows) "$UPSCALER.exe" else UPSCALER
        val installed = directory.resolve(executable)
        val models = directory.resolve("models")
        val modelParam = models.resolve("$MODEL-x$scale.param")
        val modelWeights = models.resolve("$MODEL-x$scale.bin")
        if (Files.isRegularFile(installed) && Files.isRegularFile(modelParam) && Files.isRegularFile(modelWeights)) {
            return installed.toString()
        }

        val archiveUrl = when {
            isWindows -> UPSCALER_ARCHIVE
            isMac -> MAC_ARCHIVE
            else -> LINUX_ARCHIVE
        }
        val archive = Files.createTempFile("anilife-upscaler-", ".zip")
        try {
            download(archiveUrl, archive) { onProgress(it * 0.05f) }
            unzip(archive, directory)
        } finally {
            Files.deleteIfExists(archive)
        }
        require(Files.isRegularFile(installed)) { "The upscaler download didn't contain $executable." }
        installed.toFile().setExecutable(true, false)
        require(Files.isRegularFile(modelParam) && Files.isRegularFile(modelWeights)) {
            "The upscaler download didn't contain the $MODEL-x$scale model files."
        }
        return installed.toString()
    }

    private suspend fun download(url: String, target: Path, onProgress: (Float) -> Unit) {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "application/octet-stream")
        try {
            connection.inputStream.use { input ->
                Files.newOutputStream(target).use { output ->
                    val total = connection.contentLength.toLong()
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (true) {
                        checkActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0) onProgress((copied.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun unzip(archive: Path, root: Path) {
        val destination = root.toAbsolutePath().normalize()
        ZipInputStream(Files.newInputStream(archive)).use { zip ->
            while (true) {
                val entry: ZipEntry = zip.nextEntry ?: break
                val target = destination.resolve(entry.name).normalize()
                if (!target.startsWith(destination)) continue
                if (entry.isDirectory) {
                    Files.createDirectories(target)
                } else {
                    Files.createDirectories(target.parent)
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING)
                    target.toFile().setExecutable(true, false)
                }
                zip.closeEntry()
            }
        }
    }

    private suspend fun probe(ffprobe: String, input: Path): Source {
        val output = capture(
            listOf(
                ffprobe, "-v", "error", "-select_streams", "v:0",
                "-show_entries", "stream=r_frame_rate,avg_frame_rate,width,height:format=duration",
                "-of", "default=nw=1", input.toString(),
            ),
        )
        val fields = output.lineSequence().filter { '=' in it }.associate { line ->
            line.substringBefore('=').trim() to line.substringAfter('=').trim()
        }
        val rate = fields["avg_frame_rate"]?.let(::parseRate) ?: fields["r_frame_rate"]?.let(::parseRate)
            ?: error("Couldn't read the video frame rate.")
        val width = fields["width"]?.toIntOrNull()?.takeIf { it > 0 } ?: error("Couldn't read the video width.")
        val height = fields["height"]?.toIntOrNull()?.takeIf { it > 0 } ?: error("Couldn't read the video height.")
        return Source(rate, width, height, fields["duration"]?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0)
    }

    private suspend fun run(
        command: List<String>,
        workingDirectory: File? = null,
        onLine: (String) -> Unit = {},
        tick: (suspend (Long) -> Unit)? = null,
    ) {
        checkActive()
        val processBuilder = ProcessBuilder(command).redirectErrorStream(true)
        workingDirectory?.let(processBuilder::directory)
        val process = processBuilder.start()
        val log = StringBuilder()
        val lastOutput = AtomicLong(System.nanoTime())
        upscaleLogger.info { "upscale process started tool=${File(command.first()).name} pid=${process.pid()}" }
        val drain = Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    lastOutput.set(System.nanoTime())
                    onLine(line)
                    if (line.startsWith('[') && !line.contains("%")) upscaleLogger.info { "upscale device $line" }
                    synchronized(log) {
                        log.append(line.takeLast(8_192)).append('\n')
                        if (log.length > 32_768) log.delete(0, log.length - 16_384)
                    }
                }
            }
        }.apply { isDaemon = true; start() }
        try {
            while (process.isAlive) {
                checkActive()
                tick?.invoke(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - lastOutput.get()))
                delay(300)
            }
            val exit = process.waitFor()
            drain.join(2_000)
            checkActive()
            require(exit == 0) { "${File(command.first()).name} exited with code $exit: ${reason(log)}" }
            upscaleLogger.info { "upscale process finished tool=${File(command.first()).name} pid=${process.pid()} exit=$exit" }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                if (process.isAlive) {
                    process.destroyForcibly()
                    check(process.waitFor(10, TimeUnit.SECONDS)) { "Couldn't stop upscale process ${process.pid()}." }
                }
                drain.join(2_000)
            }
        }
    }

    private suspend fun capture(command: List<String>): String {
        val output = StringBuilder()
        run(command, onLine = { line -> output.append(line).append('\n') })
        return output.toString()
    }

    private fun reason(log: StringBuilder): String =
        synchronized(log) { log.lineSequence().lastOrNull { it.isNotBlank() }.orEmpty() }.ifBlank { "no output" }

    private suspend fun checkActive() {
        if (!coroutineContext.isActive) throw CancellationException("upscale cancelled")
    }

    private fun findTool(name: String): String? {
        val executable = if (isWindows) "$name.exe" else name
        val property = if (name == UPSCALER) "AniLife.upscaler.dir" else "AniLife.ffmpeg.dir"
        val managedDirectory = System.getProperty(property)
            ?: dependencyDirectory().resolve(if (name == UPSCALER) "upscaler" else "ffmpeg").path
        val directories = if (isWindows) {
            listOf(managedDirectory)
        } else {
            listOf(managedDirectory) + System.getenv("PATH")?.split(File.pathSeparator).orEmpty() +
                listOf("/usr/bin", "/usr/local/bin")
        }
        return directories.asSequence()
            .map { File(it, executable) }
            .firstOrNull { it.isFile && it.canExecute() }
            ?.absolutePath
    }

    private fun countFrames(directory: Path): Long =
        Files.list(directory).use { entries ->
            entries.filter { it.fileName.toString().endsWith(".png", ignoreCase = true) }.count()
        }

    private fun outputPath(input: Path, fast: Boolean = false): Path {
        val name = input.fileName.toString()
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ".mp4"
        val parent = input.parent ?: Path.of(".")
        val suffix = if (fast) "_fast_4k" else "_4k"
        var candidate = parent.resolve("$base$suffix$extension")
        var index = 2
        while (Files.exists(candidate)) {
            candidate = parent.resolve("${base}${suffix}_$index$extension")
            index++
        }
        return candidate
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).use { entries ->
            entries.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    private data class Source(val rate: Double, val width: Int, val height: Int, val duration: Double)

    private companion object {
        const val UPSCALER = "realesrgan-ncnn-vulkan"
        val isWindows: Boolean get() = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
        val isMac: Boolean get() = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)

        fun parseRate(value: String): Double? {
            val parts = value.trim().split('/')
            val numerator = parts.firstOrNull()?.toDoubleOrNull() ?: return null
            val denominator = parts.getOrNull(1)?.toDoubleOrNull() ?: 1.0
            return (numerator / denominator).takeIf { it.isFinite() && it > 0 }
        }
    }
}
