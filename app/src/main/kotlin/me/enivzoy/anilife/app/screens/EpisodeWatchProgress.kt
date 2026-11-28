package me.enivzoy.anilife.app.screens

import me.enivzoy.anilife.app.data.ContinueProgress

data class EpisodeWatchProgress(
	val fraction: Float,
	val watched: Boolean,
)

internal fun episodeWatchProgress(
	episode: Double,
	aniListWatchedEpisodes: Int,
	localProgress: ContinueProgress?,
): EpisodeWatchProgress? {
	if (!episode.isFinite() || episode <= 0.0) return null
	if (episode <= aniListWatchedEpisodes.coerceAtLeast(0)) {
		return EpisodeWatchProgress(fraction = 1f, watched = true)
	}

	val progress = localProgress ?: return null
	if (!progress.episode.isFinite() || progress.episode <= 0.0) return null
	if (episode < progress.episode) return EpisodeWatchProgress(fraction = 1f, watched = true)
	if (
		episode != progress.episode ||
		!progress.position.isFinite() ||
		!progress.duration.isFinite() ||
		progress.duration <= 0.0
	) return null

	val fraction = (progress.position / progress.duration).toFloat().coerceIn(0f, 1f)
	if (!fraction.isFinite() || fraction <= 0f) return null
	return EpisodeWatchProgress(fraction = fraction, watched = fraction >= 0.9f)
}
