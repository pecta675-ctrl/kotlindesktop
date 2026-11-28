package me.enivzoy.anilife.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object ScreenshotSaver {
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS")

    fun screenshotsDirectory(): Path {
        val pictures = Path.of(System.getProperty("user.home"), "Pictures")
        return Files.createDirectories(pictures.resolve("AniLife"))
    }

    fun temporaryFile(): Path =
        Files.createDirectories(dependencyDirectory().toPath().resolve("screenshots"))
            .resolve("temp_raw.png")

    suspend fun finish(raw: Path, resolution: String, now: LocalDateTime = LocalDateTime.now()): Path =
        withContext(Dispatchers.IO) {
            val target = screenshotsDirectory().resolve("AniLife_${stamp.format(now)}.png")
            val wanted = when (resolution) {
                "4k" -> 2160
                "8k" -> 4320
                else -> 0
            }
            val scale = pngHeight(raw)?.let { upscaleFactor(it, wanted) } ?: 0
            val upscaler = if (scale > 0) upscalerPath() else null
            if (upscaler != null && upscale(upscaler, raw, target, scale)) {
                Files.deleteIfExists(raw)
            } else {
                Files.move(raw, target, StandardCopyOption.REPLACE_EXISTING)
            }
            target
        }

    private fun upscalerPath(): Path? =
        dependencyDirectory().toPath().resolve("upscaler")
            .resolve(if (isWindows) "realesrgan-ncnn-vulkan.exe" else "realesrgan-ncnn-vulkan")
            .takeIf(Files::isRegularFile)

    private fun upscale(tool: Path, raw: Path, target: Path, scale: Int): Boolean = runCatching {
        val process = ProcessBuilder(
            tool.toString(), "-i", raw.toString(), "-o", target.toString(),
            "-n", "realesr-animevideov3", "-s", scale.toString(), "-f", "png",
        ).directory(tool.parent.toFile()).redirectErrorStream(true).start()
        process.inputStream.use { it.readBytes() }
        process.waitFor(15, TimeUnit.MINUTES) && process.exitValue() == 0 && Files.isRegularFile(target)
    }.getOrDefault(false)

    internal fun pngHeight(file: Path): Int? = runCatching {
        val header = Files.newInputStream(file).use { it.readNBytes(24) }
        if (header.size < 24) null else ByteBuffer.wrap(header, 20, 4).int
    }.getOrNull()

    private val isWindows: Boolean get() = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
}

internal fun upscaleFactor(height: Int, wanted: Int): Int =
    if (wanted > 0 && height in 1 until wanted) (wanted.toDouble() / height).roundToInt().coerceIn(2, 4) else 0