package me.enivzoy.anilife.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import me.enivzoy.anilife.app.data.AniListRepository
import me.enivzoy.anilife.app.data.DirectMediaDownloader
import me.enivzoy.anilife.app.data.AppSettings
import me.enivzoy.anilife.app.data.AniListSync
import me.enivzoy.anilife.app.data.AnimeVideoProviders
import me.enivzoy.anilife.app.data.ContinueStoreInstance
import me.enivzoy.anilife.app.data.AnimeVideoSource
import me.enivzoy.anilife.app.data.MediaAutoDetector
import me.enivzoy.anilife.app.data.LocalEpisode
import me.enivzoy.anilife.app.data.isUpscaled
import me.enivzoy.anilife.app.data.matchesAnime
import me.enivzoy.anilife.app.ui.AniLifeColors
import me.enivzoy.anilife.app.ui.AudioType
import me.enivzoy.anilife.app.ui.AnimePosterCard
import me.enivzoy.anilife.app.ui.AnimeRow
import me.enivzoy.anilife.app.ui.Chip
import me.enivzoy.anilife.app.ui.ErrorPanel
import me.enivzoy.anilife.app.ui.GlassIconButton
import me.enivzoy.anilife.app.ui.LoadState
import me.enivzoy.anilife.app.ui.LocalGlassBackdrop
import me.enivzoy.anilife.app.ui.PageScrollbar
import me.enivzoy.anilife.app.ui.PosterSkeleton
import me.enivzoy.anilife.app.ui.appEnter
import me.enivzoy.anilife.app.ui.RemoteImage
import me.enivzoy.anilife.app.ui.Resolution
import me.enivzoy.anilife.app.ui.Shimmer
import me.enivzoy.anilife.app.ui.cardMeta
import me.enivzoy.anilife.app.ui.loadState
import me.enivzoy.anilife.app.ui.parseAnimeColor
import me.enivzoy.anilife.core.model.AnimeDetail
import me.enivzoy.anilife.core.model.countdown
import me.enivzoy.anilife.core.model.formatLabel
import me.enivzoy.anilife.core.model.seasonLabel
import me.enivzoy.anilife.core.model.statusLabel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException

 



@Composable
fun DetailScreen(
    repository: AniListRepository,
    providers: AnimeVideoProviders,
    id: Long,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onWatch: (AnimeDetail, Double?) -> Unit,
    queueManager: me.enivzoy.anilife.app.data.DownloadQueueManager? = null,
    settings: AppSettings? = null,
) {
    var attempt by remember(id) { mutableIntStateOf(0) }
    var state by remember(id) { mutableStateOf<LoadState<AnimeDetail>>(LoadState.Loading) }
    var showDownloadDialog by remember(id) { mutableStateOf(false) }
    var loadedDetail by remember(id) { mutableStateOf<AnimeDetail?>(null) }
    var downloadNotice by remember(id) { mutableStateOf<Pair<String, String>?>(null) }
    val library by repository.libraryEntries.collectAsState()
    val localEpisodes by repository.localEpisodes.collectAsState()
    val aniListUser by AniListSync.user.collectAsState()
    val aniListEntries by AniListSync.entries.collectAsState()
    val continueEntries by ContinueStoreInstance.entries.collectAsState()
    val scope = rememberCoroutineScope()
    var aniListActionError by remember(id) { mutableStateOf<String?>(null) }
    var aniListActionPending by remember(id) { mutableStateOf(false) }
    var showEntryEditor by remember(id) { mutableStateOf(false) }

    LaunchedEffect(id, attempt) {
        state = LoadState.Loading
        val result = loadState { repository.detail(id) }
        state = result
        if (result is LoadState.Ready) loadedDetail = result.value
    }

    val glassSource = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(glassSource)) {
        when (val current = state) {
            LoadState.Loading -> DetailSkeleton()
            is LoadState.Failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ErrorPanel(current.message, onRetry = { attempt++ })
            }
            is LoadState.Ready -> {
                val remoteEntry = aniListEntries.firstOrNull { it.anime.id == current.value.id }
                DetailContent(
                    detail = current.value,
                    providers = providers,
                    onOpen = onOpen,
                    onWatch = { episode -> onWatch(current.value, episode) },
                    inLibrary = if (aniListUser != null) {
                        aniListEntries.any { it.anime.id == current.value.id }
                    } else {
                        library.any { it.id == current.value.id }
                    },
                    libraryUsesAniList = aniListUser != null,
                    libraryActionError = aniListActionError,
                    libraryActionPending = aniListActionPending,
                    onEditAniListEntry = if (remoteEntry != null && aniListUser != null) {
                        ({ showEntryEditor = true })
                    } else null,
                    queueManager = queueManager,
                    settings = settings,
                    localEpisodes = localEpisodes,
                    upscaled = localEpisodes.any { it.matchesAnime(current.value.id, current.value.title) && it.isUpscaled() },
                    onDownloadClick = { showDownloadDialog = true },
                    onToggleLibrary = {
                    val remoteEntry = aniListEntries.firstOrNull { it.anime.id == current.value.id }
                    if (aniListUser != null) {
                        scope.launch {
                            aniListActionPending = true
                            aniListActionError = null
                            try {
                                if (remoteEntry != null) AniListSync.deleteEntry(remoteEntry)
                                else AniListSync.addToList(current.value.toCard())
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Throwable) {
                                aniListActionError = failure.message ?: "Couldn't update your AniList list."
                            } finally {
                                aniListActionPending = false
                            }
                        }
                        null
                    } else {
                        try {
                            if (library.any { it.id == current.value.id }) {
                                repository.removeFromLibrary(current.value.id)
                            } else {
                                repository.addToLibrary(current.value.toCard())
                            }
                            null
                        } catch (error: IOException) {
                            error.message ?: "Check that AniLife can write to your app data folder."
                        }
                    }
                },
            )
                if (showEntryEditor) {
                    val account = aniListUser
                    if (remoteEntry != null && account != null) {
                    AniListEntryEditorDialog(
                        entry = remoteEntry,
                        scoreFormat = account.scoreFormat,
                        busy = aniListActionPending,
                        onClose = { showEntryEditor = false },
                        onUpdate = { status, progress, score ->
                            scope.launch {
                                aniListActionPending = true
                                aniListActionError = null
                                try {
                                    AniListSync.updateEntry(remoteEntry, status, progress, score)
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (failure: Throwable) {
                                    aniListActionError = failure.message ?: "Couldn't update your AniList list."
                                } finally {
                                    aniListActionPending = false
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                aniListActionPending = true
                                aniListActionError = null
                                try {
                                    AniListSync.deleteEntry(remoteEntry)
                                    showEntryEditor = false
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (failure: Throwable) {
                                    aniListActionError = failure.message ?: "Couldn't update your AniList list."
                                } finally {
                                    aniListActionPending = false
                                }
                            }
                        },
                    )
                    }
                }
            }
        }
        }

        CompositionLocalProvider(LocalGlassBackdrop provides glassSource) {
            BackButton(onBack, Modifier.align(Alignment.TopStart).padding(16.dp))
        }
    }
    if (showDownloadDialog && queueManager != null && loadedDetail != null) {
        val d = loadedDetail!!
        val downloaded = localEpisodes
            .filter { it.anime.id == d.id || MediaAutoDetector.matchScore(d.title, it.anime) >= 0.55 }
            .map { it.episode.toInt() }
            .toSet()
        DownloadConfigDialog(
            detail = d,
            downloadedEpisodeNumbers = downloaded,
            defaultStartEpisode = 1,
            defaultSaveDirectory = settings?.downloadDirectory ?: DirectMediaDownloader.defaultDownloadDirectory().toString(),
            onDismiss = { showDownloadDialog = false },
            onConfirm = { config ->
                showDownloadDialog = false
                scope.launch {
                    val titles = listOfNotNull(d.title, d.titleRomaji, d.titleNative) + d.synonyms
                    val dub = config.audio == AudioType.Dub
                    val selectedEpisodes = config.episodeNumbers
                    var queued = 0
                    try {
                        val downloadRoot = DirectMediaDownloader.ensureDownloadDirectory(
                            java.nio.file.Path.of(config.saveDirectory),
                        )
                        val target = me.enivzoy.anilife.app.data.DownloadTarget(
                            resolution = when (config.resolution) {
                                Resolution.Auto, Resolution.P1080 -> 1080
                                Resolution.P720 -> 720
                                Resolution.P480 -> 480
                                Resolution.P360 -> 360
                            },
                            server = config.firstChoiceServer,
                        )
                        selectedEpisodes.forEach { episode ->
                            val resolve: suspend (me.enivzoy.anilife.app.data.DownloadSourceExclusions) -> AnimeVideoSource? = { exclusions ->
                                val fresh = providers.sources(
                                    d.id,
                                    d.malId,
                                    titles,
                                    episode.toDouble(),
                                    dub,
                                    refresh = exclusions.urls.isNotEmpty(),
                                )
                                me.enivzoy.anilife.app.data.pickDownloadSource(fresh, target, exclusions)
                            }
                            queueManager.enqueue(
                                title = d.title,
                                episode = episode.toDouble(),
                                url = "",
                                audio = config.audio.name.lowercase(),
                                downloadRoot = downloadRoot,
                                anime = d.toCard(),
                                episodeTitle = null,
                                maxHeight = target.resolution,
                                resolve = resolve,
                            )
                            queued++
                        }
                        downloadNotice = "Downloads started" to
                            "$queued episode${if (queued == 1) " was" else "s were"} added to your download queue. Track progress in Downloads."
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        downloadNotice = "Couldn't start downloads" to
                            (failure.message?.takeIf(String::isNotBlank) ?: "AniLife couldn't prepare the download folder.")
                    }
                }
            },
        )
    }
    downloadNotice?.let { message ->
        AlertDialog(
            onDismissRequest = { downloadNotice = null },
            modifier = Modifier.appEnter(),
            containerColor = AniLifeColors.Card,
            titleContentColor = AniLifeColors.Foreground,
            textContentColor = AniLifeColors.MutedForeground,
            title = { Text(message.first, fontWeight = FontWeight.SemiBold) },
            text = { Text(message.second, fontSize = 14.sp, lineHeight = 21.sp) },
            confirmButton = {
                TextButton(onClick = { downloadNotice = null }) {
                    Text("Close", color = AniLifeColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    GlassIconButton(
        onClick = onBack,
        icon = Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = "Back",
        modifier = modifier,
        size = 40.dp,
        iconSize = 20.dp,
        shape = CircleShape,
        accent = AniLifeColors.Primary,
        alwaysGlass = true,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    detail: AnimeDetail,
    providers: AnimeVideoProviders,
    onOpen: (Long) -> Unit,
    onWatch: (Double?) -> Unit,
    inLibrary: Boolean,
    libraryUsesAniList: Boolean,
    libraryActionError: String?,
    libraryActionPending: Boolean,
    onToggleLibrary: () -> String?,
    onEditAniListEntry: (() -> Unit)? = null,
    queueManager: me.enivzoy.anilife.app.data.DownloadQueueManager? = null,
    settings: AppSettings? = null,
    localEpisodes: List<LocalEpisode> = emptyList(),
    upscaled: Boolean = false,
    onDownloadClick: () -> Unit = {},
) {
    var expanded by remember(detail.id) { mutableStateOf(false) }
    var notice by remember(detail.id) { mutableStateOf<Pair<String, String>?>(null) }
    val tint = remember(detail.color) { parseAnimeColor(detail.color) }

    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            Box(Modifier.fillMaxWidth()) {
             
             
            Box(Modifier.fillMaxWidth().height(288.dp).background(tint)) {
                RemoteImage(
                    url = detail.banner,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    placeholderColor = tint,
                    tint = AniLifeColors.Shadow,
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.45f to AniLifeColors.Background.copy(alpha = 0.35f),
                                1f to AniLifeColors.Background,
                            ),
                        ),
                )
            }

            Column(Modifier.padding(top = 128.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
                    Box(
                        modifier = Modifier
                            .width(208.dp)
                            .aspectRatio(2f / 3f)
                            .shadow(24.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(tint),
                    ) {
                        RemoteImage(
                            url = detail.cover,
                            contentDescription = detail.title,
                            modifier = Modifier.fillMaxSize(),
                            placeholderColor = tint,
                        )
                    }

                    Spacer(Modifier.width(32.dp))

                    Column(
                        modifier = Modifier.weight(1f).padding(top = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = detail.title,
                            color = AniLifeColors.Foreground,
                            fontSize = 30.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        val romaji = detail.titleRomaji
                        if (!romaji.isNullOrBlank() && romaji != detail.title) {
                            Text(
                                text = romaji,
                                color = AniLifeColors.MutedForeground,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            detail.score?.let {
                                Chip(
                                    text = "${it.toInt()}%",
                                    container = AniLifeColors.Primary,
                                    content = AniLifeColors.PrimaryForeground,
                                )
                            }
                            listOfNotNull(
                                formatLabel(detail.format),
                                statusLabel(detail.status),
                                detail.episodes?.let { "$it eps" },
                                seasonLabel(detail.season, detail.year),
                                detail.duration?.takeIf { it > 0 }?.let { "$it min" },
                            ).forEach { text ->
                                Chip(text = text, container = AniLifeColors.Secondary, content = AniLifeColors.Foreground)
                            }
                            detail.nextAiring?.let { next ->
                                Chip(
                                    text = (if (detail.format == "MOVIE") "Out" else "Episode ${next.episode}") + " " + countdown(next.airingAt),
                                    container = Color.Transparent,
                                    content = AniLifeColors.MutedForeground,
                                )
                            }
                            if (upscaled) {
                                Chip(
                                    text = "4K UPSCALED",
                                    container = AniLifeColors.Gold,
                                    content = AniLifeColors.GoldForeground,
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = { onWatch(null) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(44.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AniLifeColors.Primary,
                                    contentColor = AniLifeColors.PrimaryForeground,
                                ),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Watch now", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    onToggleLibrary()?.let { message ->
                                        notice = "Couldn't update library" to message
                                    }
                                },
                                enabled = !libraryActionPending,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(44.dp),
                                border = BorderStroke(1.dp, AniLifeColors.Border),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AniLifeColors.Foreground),
                            ) {
                                Icon(
                                    Icons.Rounded.BookmarkAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    when {
                                        libraryUsesAniList && inLibrary -> "Remove from AniList"
                                        libraryUsesAniList -> "Add to AniList"
                                        inLibrary -> "Remove from library"
                                        else -> "Add to library"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (queueManager != null) {
                                Button(
                                    onClick = { onDownloadClick() },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AniLifeColors.Secondary,
                                        contentColor = AniLifeColors.Foreground,
                                    ),
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Download", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        if (libraryActionPending) {
                            Text("Updating your list…", color = AniLifeColors.MutedForeground, fontSize = 12.sp)
                        }
                        onEditAniListEntry?.let { onEdit ->
                            TextButton(onClick = onEdit, enabled = !libraryActionPending) {
                                Text("Edit AniList entry", fontSize = 12.sp, color = AniLifeColors.MutedForeground)
                            }
                        }

                        if (detail.genres.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                detail.genres.forEach { genre ->
                                    Chip(text = genre, container = Color.Transparent, content = AniLifeColors.MutedForeground)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Synopsis", color = AniLifeColors.Foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        val synopsis = detail.description
                        if (synopsis.isNullOrBlank()) {
                            Text("AniList has no synopsis for this title.", color = AniLifeColors.MutedForeground, fontSize = 14.sp)
                        } else {
                            Text(
                                text = synopsis,
                                color = AniLifeColors.MutedForeground,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                maxLines = if (expanded) Int.MAX_VALUE else 9,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (synopsis.length > 460) {
                                TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                                    Text(if (expanded) "Show less" else "Read more", color = AniLifeColors.Primary, fontSize = 13.sp)
                                }
                            }
                        }

                        if (detail.tags.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                detail.tags.take(14).forEach { tag ->
                                    Chip(text = tag, container = AniLifeColors.Card, content = AniLifeColors.MutedForeground)
                                }
                            }
                        }
                    }

                    DetailsCard(detail)
                }

                val relationCards = detail.relations.map { it.anime }.distinctBy { it.id }
                Spacer(Modifier.height(34.dp))
                DetailSections(
                    detail = detail,
                    providers = providers,
                    relationCards = relationCards,
                    localEpisodes = localEpisodes,
                    onOpen = onOpen,
                    onWatch = onWatch,
                )

                val relatedIds = relationCards.map { it.id }.toSet()
                val suggestions = detail.recommendations.filter { it.id !in relatedIds && it.id != detail.id }
                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(38.dp))
                    AnimeRow(title = "More like this", items = suggestions, onOpen = onOpen)
                }

                Spacer(Modifier.height(56.dp))
            }
        }
        }
        PageScrollbar(rememberScrollbarAdapter(scrollState), Modifier.align(Alignment.CenterEnd))
    }
    LaunchedEffect(libraryActionError) {
        if (libraryActionError != null) notice = "Couldn't update AniList" to libraryActionError
    }

    if (notice != null) {
        AlertDialog(
            onDismissRequest = { notice = null },
            modifier = Modifier.appEnter(),
            containerColor = AniLifeColors.Card,
            titleContentColor = AniLifeColors.Foreground,
            textContentColor = AniLifeColors.MutedForeground,
            title = { Text(notice!!.first, fontWeight = FontWeight.SemiBold) },
            text = { Text(notice!!.second, fontSize = 14.sp, lineHeight = 21.sp) },
            confirmButton = {
                TextButton(onClick = { notice = null }) {
                    Text("Close", color = AniLifeColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

@Composable
private fun DetailSections(
    detail: AnimeDetail,
    providers: AnimeVideoProviders,
    relationCards: List<me.enivzoy.anilife.core.model.AnimeCard>,
    localEpisodes: List<LocalEpisode>,
    onOpen: (Long) -> Unit,
    onWatch: (Double?) -> Unit,
) {
    val aniListEntries by AniListSync.entries.collectAsState()
    val continueEntries by ContinueStoreInstance.entries.collectAsState()
    val watchedEpisodes = aniListEntries.firstOrNull { it.anime.id == detail.id }?.progress ?: 0
    val continueProgress = continueEntries.firstOrNull { it.anime.id == detail.id }?.progress
    val tabs = buildList {
        add("Episodes")
        if (detail.characters.isNotEmpty()) add("Characters")
        if (relationCards.isNotEmpty()) add("Watch order")
    }
    var selectedTab by remember(detail.id) { mutableIntStateOf(0) }
    val activeTab = selectedTab.coerceIn(tabs.indices)

    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEachIndexed { index, label ->
                val shape = RoundedCornerShape(8.dp)
                Box(
                    Modifier.clip(shape)
                        .background(if (activeTab == index) AniLifeColors.CardHover else Color.Transparent)
                        .border(
                            1.dp,
                            if (activeTab == index) AniLifeColors.Border else Color.Transparent,
                            shape,
                        )
                        .clickable { selectedTab = index }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        label,
                        color = if (activeTab == index) AniLifeColors.Foreground else AniLifeColors.MutedForeground,
                        fontSize = 13.sp,
                        fontWeight = if (activeTab == index) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        when (tabs[activeTab]) {
            "Episodes" -> EpisodeSection(
                detail = detail,
                providers = providers,
                aniListWatchedEpisodes = watchedEpisodes,
                continueProgress = continueProgress,
                localEpisodes = localEpisodes,
            ) { episode -> onWatch(episode) }
            "Characters" -> LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                itemsIndexed(
                    items = detail.characters,
                    key = { _, character -> "${character.name}|${character.role}|${character.voiceActor}" },
                ) { _, character ->
                    CharacterCard(character)
                }
            }
            "Watch order" -> {
                Text(
                    "Direct AniList relations, ordered by release year. This is not a complete franchise guide.",
                    color = AniLifeColors.MutedForeground,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                AnimeRow(
                    title = "Related titles",
                    items = (relationCards + detail.toCard()).distinctBy { it.id }.sortedWith(
                        compareBy({ it.year ?: Int.MAX_VALUE }, { it.id }),
                    ),
                    onOpen = onOpen,
                )
            }
        }
    }
}

private fun AnimeDetail.toCard() = me.enivzoy.anilife.core.model.AnimeCard(
    id = id,
    title = title,
    titleRomaji = titleRomaji,
    titleEnglish = titleEnglish,
    titleNative = titleNative,
    malId = malId,
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
)

@Composable
private fun DetailsCard(detail: AnimeDetail) {
    Column(
        modifier = Modifier
            .widthIn(min = 250.dp, max = 300.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AniLifeColors.Card)
            .border(1.dp, AniLifeColors.Border, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Details", color = AniLifeColors.Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        InfoLine("Format", formatLabel(detail.format))
        InfoLine("Episodes", detail.episodes?.toString())
        InfoLine("Status", statusLabel(detail.status))
        InfoLine("Aired", detail.startDate ?: detail.year?.toString())
        InfoLine("Studios", detail.studios.joinToString(", ").takeIf { it.isNotBlank() })
        InfoLine("Source", detail.source?.lowercase()?.replaceFirstChar { it.uppercase() })
        InfoLine("Duration", detail.duration?.let { "$it min" })
    }
}

@Composable
private fun InfoLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = label,
            color = AniLifeColors.MutedForeground,
            fontSize = 12.sp,
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = value,
            color = AniLifeColors.Foreground,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CharacterCard(character: AnimeDetail.Character) {
    Column(
        modifier = Modifier.width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RemoteImage(
            url = character.image,
            contentDescription = character.name,
            modifier = Modifier.size(78.dp).clip(CircleShape),
            placeholderColor = AniLifeColors.CardHover,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = character.name,
            color = AniLifeColors.Foreground,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val voiceActor = character.voiceActor
        if (!voiceActor.isNullOrBlank()) {
            Text(
                text = voiceActor,
                color = AniLifeColors.MutedForeground,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DetailSkeleton() {
    Column(Modifier.fillMaxSize()) {
        Shimmer(
            modifier = Modifier.fillMaxWidth().height(288.dp),
            shape = RoundedCornerShape(0.dp),
        )
        Row(Modifier.padding(horizontal = 32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            PosterSkeleton(width = 208.dp)
            Column(
                modifier = Modifier.weight(1f).padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Shimmer(Modifier.fillMaxWidth(0.7f).height(30.dp), RoundedCornerShape(6.dp))
                Shimmer(Modifier.fillMaxWidth(0.4f).height(16.dp), RoundedCornerShape(4.dp))
                Shimmer(Modifier.fillMaxWidth(0.55f).height(24.dp), CircleShape)
                Shimmer(Modifier.width(220.dp).height(44.dp), RoundedCornerShape(10.dp))
            }
        }
    }
}

