package me.enivzoy.anilife.app.data

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class UpscaleMode(val label: String, val description: String, val audioTag: String) {
    Fast4K("Fast 4K", "Resizing + light sharpening, not AI. Auto NVIDIA / AMD / Intel encoding, with software fallback.", "4k-fast"),
    AIQuality("AI Quality", "Real-ESRGAN via Vulkan on compatible NVIDIA / AMD / Intel GPUs. Slow on integrated graphics.", UPSCALED_AUDIO),
}

enum class UpscaleStage(val label: String) {
    Preparing("Preparing"),
    Extracting("Extracting frames"),
    Upscaling("AI upscaling"),
    Encoding("Encoding video"),
    Joining("Joining video parts"),
    Audio("Restoring audio"),
    Completed("Completed"),
}

data class UpscaleProgress(
    val fraction: Float = 0f,
    val stage: UpscaleStage = UpscaleStage.Preparing,
    val detail: String = "Reading source video",
    val completedFrames: Long = 0,
    val totalFrames: Long = 0,
    val framesPerSecond: Double? = null,
    val secondsSinceOutput: Long = 0,
)

internal fun formatUpscalePercent(fraction: Float): String {
    val bounded = if (fraction.isFinite()) fraction.coerceIn(0f, 1f) else 0f
    if (bounded == 0f) return "0.00%"
    if (bounded == 1f) return "100%"
    val percent = BigDecimal.valueOf(bounded.toDouble() * 100)
        .round(MathContext(3, RoundingMode.HALF_UP))
        .min(BigDecimal("99.9"))
    return "${percent.toPlainString()}%"
}

internal fun upscaleChunkProgress(
    chunk: Int,
    chunks: Int,
    extracted: Double,
    upscaled: Double,
    encoded: Double,
): Float {
    require(chunks > 0 && chunk in 0 until chunks)
    val part = 0.02 * extracted.coerceIn(0.0, 1.0) +
        0.93 * upscaled.coerceIn(0.0, 1.0) + 0.05 * encoded.coerceIn(0.0, 1.0)
    return (0.05 + 0.93 * (chunk + part) / chunks).toFloat()
}
