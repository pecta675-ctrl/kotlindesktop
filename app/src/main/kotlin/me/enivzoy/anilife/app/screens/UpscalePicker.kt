package me.enivzoy.anilife.app.screens

import me.enivzoy.anilife.app.data.LocalEpisode
import me.enivzoy.anilife.app.data.isUpscaled
import me.enivzoy.anilife.core.model.AnimeCard
import java.util.Locale

internal data class UpscaleAnimeGroup(val key: String, val anime: AnimeCard, val episodes: List<LocalEpisode>)

internal fun upscaleKey(entry: LocalEpisode): String = entry.path.toAbsolutePath().normalize().toString()

internal fun groupUpscaleEpisodes(episodes: List<LocalEpisode>): List<UpscaleAnimeGroup> = episodes
    .distinctBy(::upscaleKey)
    .groupBy { if (it.anime.id > 0) "id:${it.anime.id}" else "title:${it.anime.title.trim().lowercase(Locale.ROOT)}" }
    .map { (key, entries) ->
        val anime = entries.firstOrNull { !it.anime.cover.isNullOrBlank() }?.anime ?: entries.first().anime
        UpscaleAnimeGroup(key, anime, entries.sortedWith(compareBy({ it.episode }, { it.isUpscaled() }, { it.audio }, ::upscaleKey)))
    }
    .sortedWith(compareBy({ it.anime.title.lowercase(Locale.ROOT) }, { it.key }))

internal fun selectedUpscaleEpisodes(episodes: List<LocalEpisode>, keys: Set<String>): List<LocalEpisode> =
    episodes.distinctBy(::upscaleKey).filter { upscaleKey(it) in keys }

internal fun upscaleVariant(entry: LocalEpisode): String = when {
    entry.audio.equals("4k-fast", true) -> "Fast 4K copy"
    entry.isUpscaled() -> "AI / 4K copy"
    entry.audio.equals("dub", true) -> "Dub"
    entry.audio.equals("sub", true) -> "Sub"
    else -> entry.audio.ifBlank { "Original" }
}
