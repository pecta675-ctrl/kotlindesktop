package me.enivzoy.anilife.app.data

import me.enivzoy.anilife.core.model.AnimeCard
import me.enivzoy.anilife.core.model.AnimeDetail
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

const val UPSCALED_AUDIO = "4k"

fun LocalEpisode.isUpscaled(): Boolean =
    audio.equals(UPSCALED_AUDIO, ignoreCase = true) ||
        path.fileName?.toString()?.contains("_4k", ignoreCase = true) == true

fun AnimeCard.toDetail(): AnimeDetail = AnimeDetail(
    id = id,
    malId = malId,
    title = title,
    titleRomaji = titleRomaji,
    titleEnglish = titleEnglish,
    titleNative = titleNative,
    cover = cover,
    color = color,
    banner = banner,
    format = format,
    status = status,
    episodes = episodes,
    score = score,
    year = year,
    season = season,
    genres = genres,
    nextAiring = nextAiring,
    isAdult = isAdult,
    synonyms = aliases,
)

fun LocalEpisode.matchesAnime(animeId: Long, title: String?): Boolean {
    if (animeId > 0 && anime.id == animeId) return true
    val wanted = title?.let(::normalizedTitle)?.takeIf(String::isNotBlank) ?: return false
    val candidates = listOfNotNull(anime.title, anime.titleRomaji, anime.titleEnglish, anime.titleNative) + anime.aliases
    return candidates.any { normalizedTitle(it) == wanted }
}

fun normalizedTitle(value: String): String = value.lowercase()
    .map { if (it.isLetterOrDigit()) it else ' ' }
    .joinToString("")
    .trim()
    .replace(Regex("\\s+"), " ")

object LocalPlayback {
    fun entriesFor(
        entries: List<LocalEpisode>,
        animeId: Long,
        title: String?,
    ): List<LocalEpisode> = entries.filter { it.matchesAnime(animeId, title) }

    fun hasUpscaled(
        entries: List<LocalEpisode>,
        animeId: Long,
        title: String?,
    ): Boolean = entriesFor(entries, animeId, title).any(LocalEpisode::isUpscaled)

    fun availableEpisodes(
        entries: List<LocalEpisode>,
        animeId: Long,
        title: String?,
    ): List<Double> = entriesFor(entries, animeId, title)
        .map(LocalEpisode::episode)
        .filter { it.isFinite() && it > 0.0 }
        .distinct()
        .sorted()

    fun find(
        entries: List<LocalEpisode>,
        animeId: Long,
        title: String?,
        episode: Double,
        audio: String?,
        requireFile: Boolean = true,
    ): LocalEpisode? = ranked(
        entriesFor(entries, animeId, title).filter { it.episode == episode },
        audio,
    ).firstOrNull { !requireFile || Files.isRegularFile(it.path) }

    fun pick(
        entries: List<LocalEpisode>,
        animeId: Long,
        title: String?,
        episode: Double?,
        audio: String?,
        requireFile: Boolean = true,
    ): LocalEpisode? {
        val candidates = entriesFor(entries, animeId, title)
            .filter { it.episode.isFinite() && it.episode > 0.0 }
            .filter { !requireFile || Files.isRegularFile(it.path) }
        if (candidates.isEmpty()) return null
        val numbers = candidates.map(LocalEpisode::episode).distinct().sorted()
        val chosen = when {
            episode == null -> numbers.first()
            episode in numbers -> episode
            else -> numbers.firstOrNull { it >= episode } ?: numbers.last()
        }
        return ranked(candidates.filter { it.episode == chosen }, audio).firstOrNull()
    }

    fun seriesEpisodes(
        entries: List<LocalEpisode>,
        animeId: Long,
        title: String?,
        totalEpisodes: Int?,
    ): List<Double> {
        val local = availableEpisodes(entries, animeId, title)
        val listed = if (totalEpisodes != null && totalEpisodes > 0) {
            (1..totalEpisodes).map { it.toDouble() }
        } else {
            emptyList()
        }
        val beyond = if (listed.isEmpty()) {
            val highest = local.maxOrNull()?.toInt()?.takeIf { it > 0 }
            if (highest != null) (1..highest).map { it.toDouble() } + (highest + 1.0) else emptyList()
        } else {
            emptyList()
        }
        return (listed + local + beyond)
            .filter { it.isFinite() && it > 0.0 }
            .distinct()
            .sorted()
    }

    fun pathFromLocation(location: String): Path? = runCatching {
        val trimmed = location.trim()
        if (trimmed.startsWith("file:", ignoreCase = true)) {
            Path.of(URI(trimmed))
        } else {
            Path.of(trimmed)
        }
    }.getOrNull()

    fun matchesLocation(entry: LocalEpisode, location: String): Boolean {
        val path = pathFromLocation(location) ?: return false
        return runCatching {
            path.toAbsolutePath().normalize() == entry.path.toAbsolutePath().normalize()
        }.getOrDefault(false)
    }

    private fun ranked(entries: List<LocalEpisode>, audio: String?): List<LocalEpisode> =
        entries.sortedWith(
            compareBy(
                { if (it.isUpscaled()) 0 else 1 },
                { if (audio != null && it.audio.equals(audio, ignoreCase = true)) 0 else 1 },
                { it.path.fileName?.toString()?.lowercase().orEmpty() },
            ),
        )
}
