package me.enivzoy.anilife.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import me.enivzoy.anilife.app.data.AnimeEpisode
import me.enivzoy.anilife.app.data.AnimeVideoProviders
import me.enivzoy.anilife.app.data.ContinueProgress
import me.enivzoy.anilife.app.data.LocalEpisode
import me.enivzoy.anilife.app.data.LocalPlayback
import me.enivzoy.anilife.app.data.isUpscaled
import me.enivzoy.anilife.app.ui.AniLifeColors
import me.enivzoy.anilife.app.ui.PageScrollbar
import me.enivzoy.anilife.app.ui.RemoteImage
import me.enivzoy.anilife.core.model.AnimeDetail

private const val EPISODES_PER_RANGE = 100

@Composable
internal fun EpisodeSection(
    detail: AnimeDetail,
    providers: AnimeVideoProviders,
    aniListWatchedEpisodes: Int = 0,
    continueProgress: ContinueProgress? = null,
    localEpisodes: List<LocalEpisode> = emptyList(),
    onWatchEpisode: (Double) -> Unit,
) {
    var episodes by remember(detail.id) { mutableStateOf<List<AnimeEpisode>>(emptyList()) }
    var loading by remember(detail.id) { mutableStateOf(true) }
    var error by remember(detail.id) { mutableStateOf<String?>(null) }
    var retry by remember(detail.id) { mutableIntStateOf(0) }

    LaunchedEffect(detail.id, retry) {
        loading = true
        error = null
        try {
            episodes = providers.episodes(detail.id, detail.episodes, detail.malId)
                .sortedBy(AnimeEpisode::number)
            if (episodes.isEmpty()) error = "No episode list is available for this title."
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message?.takeIf(String::isNotBlank) ?: "Couldn't load episodes."
        } finally {
            loading = false
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Episodes",
                color = AniLifeColors.Foreground,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            if (episodes.isNotEmpty()) {
                Text(
                    "${episodes.size} episodes",
                    color = AniLifeColors.MutedForeground,
                    fontSize = 12.sp,
                )
            }
        }

        when {
            loading -> Row(
                Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    color = AniLifeColors.Primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    "Finding episodes…",
                    color = AniLifeColors.MutedForeground,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }

            episodes.isEmpty() -> Row(
                Modifier.fillMaxWidth().padding(vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(error.orEmpty(), color = AniLifeColors.MutedForeground, fontSize = 13.sp)
                Spacer(Modifier.width(10.dp))
                EpisodeControl("Try again") { retry++ }
            }

            else -> EpisodePicker(
                episodes = episodes,
                fallbackArt = if (episodes.size == 1 || episodes.any { !it.thumbnail.isNullOrBlank() }) {
                    detail.banner ?: detail.cover
                } else {
                    null
                },
                aniListWatchedEpisodes = aniListWatchedEpisodes,
                continueProgress = continueProgress,
                localByEpisode = LocalPlayback.entriesFor(localEpisodes, detail.id, detail.title)
                    .groupBy(LocalEpisode::episode)
                    .mapValues { (_, list) ->
                        list.sortedBy { if (it.isUpscaled()) 0 else 1 }.first()
                    },
                onWatchEpisode = onWatchEpisode,
            )
        }
    }
}

@Composable
private fun EpisodePicker(
    episodes: List<AnimeEpisode>,
    fallbackArt: String?,
    aniListWatchedEpisodes: Int,
    continueProgress: ContinueProgress?,
    localByEpisode: Map<Double, LocalEpisode>,
    onWatchEpisode: (Double) -> Unit,
) {
    var newestFirst by remember { mutableStateOf(false) }
    var compact by remember { mutableStateOf(false) }
    var selectedRange by remember { mutableIntStateOf(0) }
    val ordered = remember(episodes, newestFirst) {
        if (newestFirst) episodes.asReversed() else episodes
    }
    val ranges = remember(ordered) { episodeRanges(ordered) }
    val rangeIndex = selectedRange.coerceIn(0, ranges.lastIndex)
    val visible = ranges[rangeIndex]

    if (ranges.size > 1) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(ranges, key = { _, range -> episodeRangeLabel(range) }) { index, range ->
                EpisodeControl(
                    text = episodeRangeLabel(range),
                    selected = rangeIndex == index,
                ) {
                    selectedRange = index
                }
            }
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EpisodeControl(if (newestFirst) "Newest first" else "Oldest first") {
            newestFirst = !newestFirst
            selectedRange = 0
        }
        Spacer(Modifier.width(8.dp))
        EpisodeControl(if (compact) "Episode cards" else "Compact grid") {
            compact = !compact
        }
    }

    val gridState = rememberLazyGridState()
    Box(Modifier.fillMaxWidth().heightIn(max = if (compact) 360.dp else 680.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (compact) 58.dp else 228.dp),
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 18.dp),
        ) {
            items(visible, key = AnimeEpisode::number) { episode ->
                val progress = episodeWatchProgress(episode.number, aniListWatchedEpisodes, continueProgress)
                val downloaded = localByEpisode[episode.number]
                if (compact) {
                    CompactEpisodeButton(episode, progress, downloaded, onWatchEpisode)
                } else {
                    EpisodeCard(episode, fallbackArt, progress, downloaded, onWatchEpisode)
                }
            }
        }
        PageScrollbar(gridState, Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun EpisodeCard(
    episode: AnimeEpisode,
    fallbackArt: String?,
    progress: EpisodeWatchProgress?,
    downloaded: LocalEpisode?,
    onClick: (Double) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(if (progress?.watched == true) AniLifeColors.Primary.copy(alpha = 0.06f) else Color.Transparent)
            .border(
                1.dp,
                if (progress?.watched == true) AniLifeColors.Primary.copy(alpha = 0.38f) else Color.Transparent,
                shape,
            )
            .clickable { onClick(episode.number) }
            .pointerHoverIcon(PointerIcon.Hand),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(shape)
                .background(AniLifeColors.CardHover),
        ) {
            RemoteImage(
                url = episode.thumbnail?.takeIf(String::isNotBlank) ?: fallbackArt,
                contentDescription = "Episode ${formatEpisodeNumber(episode.number)}",
                modifier = Modifier.fillMaxSize(),
                placeholderColor = AniLifeColors.CardHover,
            )
            Box(
                Modifier.align(Alignment.Center)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.58f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(25.dp),
                )
            }
            Text(
                formatEpisodeNumber(episode.number),
                Modifier.align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            if (progress?.watched == true) {
                Text(
                    "Watched",
                    Modifier.align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AniLifeColors.Primary.copy(alpha = 0.9f))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    color = AniLifeColors.PrimaryForeground,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (downloaded != null) {
                Text(
                    if (downloaded.isUpscaled()) "4K UPSCALED" else "DOWNLOADED",
                    Modifier.align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (downloaded.isUpscaled()) AniLifeColors.Gold else AniLifeColors.Primary)
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    color = if (downloaded.isUpscaled()) AniLifeColors.GoldForeground else AniLifeColors.PrimaryForeground,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            episode.title?.takeIf(String::isNotBlank) ?: "Episode ${formatEpisodeNumber(episode.number)}",
            color = AniLifeColors.Foreground,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        episode.airedOn?.takeIf(String::isNotBlank)?.let { airedOn ->
            Text(
                airedOn,
                Modifier.padding(bottom = 4.dp),
                color = AniLifeColors.MutedForeground,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                maxLines = 1,
            )
        }
        progress?.let { EpisodeProgressBar(it.fraction) }
    }
}

@Composable
private fun CompactEpisodeButton(
    episode: AnimeEpisode,
    progress: EpisodeWatchProgress?,
    downloaded: LocalEpisode?,
    onClick: (Double) -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(if (progress?.watched == true) AniLifeColors.Primary.copy(alpha = 0.10f) else AniLifeColors.Card)
            .border(1.dp, if (progress?.watched == true) AniLifeColors.Primary.copy(alpha = 0.38f) else AniLifeColors.Border, shape)
            .clickable { onClick(episode.number) }
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            formatEpisodeNumber(episode.number),
            color = AniLifeColors.Foreground,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (downloaded != null) {
            Text(
                if (downloaded.isUpscaled()) "4K" else "DL",
                color = if (downloaded.isUpscaled()) AniLifeColors.Gold else AniLifeColors.Primary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        progress?.let { EpisodeProgressBar(it.fraction, Modifier.padding(top = 6.dp)) }
    }
}

@Composable
internal fun EpisodeProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth()
            .heightIn(min = 3.dp)
            .clip(CircleShape)
            .background(AniLifeColors.Primary.copy(alpha = 0.18f)),
    ) {
        Box(
            Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f))
                .heightIn(min = 3.dp)
                .background(AniLifeColors.Primary),
        )
    }
}

@Composable
private fun EpisodeControl(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.clip(shape)
            .background(if (selected) AniLifeColors.CardHover else AniLifeColors.Card)
            .border(1.dp, if (selected) AniLifeColors.Primary.copy(alpha = 0.55f) else AniLifeColors.Border, shape)
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) AniLifeColors.Foreground else AniLifeColors.MutedForeground,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

internal fun episodeRanges(
    episodes: List<AnimeEpisode>,
    rangeSize: Int = EPISODES_PER_RANGE,
): List<List<AnimeEpisode>> {
    require(rangeSize > 0) { "Episode range size must be positive." }
    return episodes.chunked(rangeSize)
}

internal fun episodeRangeLabel(episodes: List<AnimeEpisode>): String {
    require(episodes.isNotEmpty()) { "An episode range must not be empty." }
    return "${formatEpisodeNumber(episodes.first().number)}–${formatEpisodeNumber(episodes.last().number)}"
}
