package me.enivzoy.anilife.app.data

import java.io.File
import kotlin.random.Random

internal data class DiscoTrack(val videoId: String, val startSeconds: Int = 0) {
    val url: String get() = "https://www.youtube.com/watch?v=$videoId"
}

internal val discoTracks = listOf(
    DiscoTrack("mvkcyB_lve0"),
    DiscoTrack("OkBxj51gjUk"),
    DiscoTrack("6D9TR7W9z9I"),
    DiscoTrack("kcu4RTdAft4"),
    DiscoTrack("Ub0wWUq6M4A", startSeconds = 50),
    DiscoTrack("x706w5NCnG4"),
)

internal fun pickDiscoTrack(tracks: List<DiscoTrack> = discoTracks, random: Random = Random.Default): DiscoTrack =
    tracks[random.nextInt(tracks.size)]

internal fun discoTrackFile(root: File, track: DiscoTrack): File {
    val legacy = File(root, "buckshot_ost.mp3")
    return if (track.videoId == "mvkcyB_lve0" && legacy.isFile && legacy.length() > 0) legacy
    else File(root, "disco/${track.videoId}.mp3")
}

internal fun discoPlayerOptions(track: DiscoTrack): List<Pair<String, String>> = listOf(
    "vo" to "null",
    "video" to "no",
    "pause" to "yes",
    "start" to track.startSeconds.toString(),
    "volume" to "100",
    "audio-exclusive" to "no",
    "terminal" to "no",
    "loop-file" to "no",
)

internal fun discoDownloadCommand(ytDlp: File, ffmpeg: File?, output: File, track: DiscoTrack): List<String> = buildList {
    add(ytDlp.absolutePath)
    addAll(listOf("-x", "--audio-format", "mp3", "--no-playlist", "--no-warnings", "--no-progress", "--socket-timeout", "20", "--retries", "2"))
    if (ffmpeg != null) addAll(listOf("--ffmpeg-location", ffmpeg.absolutePath))
    addAll(listOf("-o", output.absolutePath, track.url))
}
