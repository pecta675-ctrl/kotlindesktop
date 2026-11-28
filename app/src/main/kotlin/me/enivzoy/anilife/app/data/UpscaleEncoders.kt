package me.enivzoy.anilife.app.data

import kotlinx.coroutines.CancellationException

internal data class UpscaleEncoder(
    val name: String,
    val codec: String,
    val options: List<String>,
    val pixelFormat: String = "nv12",
    val inputArguments: List<String> = emptyList(),
    val filterSuffix: String = "",
    val hardware: Boolean = true,
) {
    val arguments: List<String> get() = listOf("-c:v", codec) + options
    val filter: String get() = "scale=-2:2160:flags=lanczos,unsharp=5:5:0.35:3:3:0$filterSuffix"
}

internal val softwareUpscaleEncoder = UpscaleEncoder(
    "Software (veryfast)", "libx264", listOf("-preset", "veryfast", "-crf", "20"), "yuv420p", hardware = false,
)

internal fun upscaleEncoderCandidates(osName: String, renderDevices: List<String> = emptyList()): List<UpscaleEncoder> {
    val windows = osName.startsWith("Windows", true)
    val linux = osName.startsWith("Linux", true)
    return buildList {
        if (windows || linux) {
            add(UpscaleEncoder("NVIDIA NVENC", "h264_nvenc", listOf("-preset", "p4", "-rc", "constqp", "-qp", "20")))
            add(UpscaleEncoder("AMD AMF", "h264_amf", listOf("-quality", "speed", "-rc", "cqp", "-qp_i", "20", "-qp_p", "22")))
            add(UpscaleEncoder("Intel QSV", "h264_qsv", listOf("-preset", "veryfast", "-global_quality", "22")))
        }
        if (linux) {
            renderDevices.distinct().sorted().forEach { device ->
                add(
                    UpscaleEncoder(
                        "AMD / Intel VA-API ($device)", "h264_vaapi", listOf("-qp", "20"), "vaapi",
                        inputArguments = listOf("-vaapi_device", device), filterSuffix = ",format=nv12,hwupload",
                    ),
                )
            }
        }
        if (osName.startsWith("Mac", true)) {
            add(UpscaleEncoder("Apple VideoToolbox", "h264_videotoolbox", listOf("-allow_sw", "0", "-b:v", "20M")))
        }
        add(softwareUpscaleEncoder)
    }
}

internal fun advertisedVideoEncoders(output: String): Set<String> = output.lineSequence().mapNotNull { line ->
    val parts = line.trim().split(Regex("\\s+"), limit = 3)
    parts.getOrNull(1)?.takeIf { parts[0].length == 6 && parts[0].startsWith('V') && it != "=" }
}.toSet()

internal suspend fun <T> withUpscaleEncoderFallback(
    candidates: List<UpscaleEncoder>,
    probe: suspend (UpscaleEncoder) -> Boolean,
    encode: suspend (UpscaleEncoder) -> T,
    onUnavailable: (UpscaleEncoder, Exception?) -> Unit,
): T {
    for (encoder in candidates) {
        try {
            if (encoder.hardware && !probe(encoder)) {
                onUnavailable(encoder, null)
                continue
            }
            return encode(encoder)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (!encoder.hardware) throw failure
            onUnavailable(encoder, failure)
        }
    }
    error("No video encoder is available. Check AniLife's FFmpeg runtime.")
}
