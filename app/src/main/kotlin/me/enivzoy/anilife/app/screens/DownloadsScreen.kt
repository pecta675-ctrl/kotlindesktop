package me.enivzoy.anilife.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import me.enivzoy.anilife.app.data.AniListRepository
import me.enivzoy.anilife.app.data.AppSettings
import me.enivzoy.anilife.app.data.AutoDetectResult
import me.enivzoy.anilife.app.data.DetectedFolder
import me.enivzoy.anilife.app.data.LocalEpisode
import me.enivzoy.anilife.app.data.PortableImportResult
import me.enivzoy.anilife.app.data.ContinueStoreInstance
import me.enivzoy.anilife.app.data.DownloadJob
import me.enivzoy.anilife.app.data.DownloadQueueManager
import me.enivzoy.anilife.app.data.DownloadStatus
import me.enivzoy.anilife.app.data.DirectMediaDownloader
import me.enivzoy.anilife.app.data.NoticeStoreInstance
import me.enivzoy.anilife.app.data.UpscaleQueueManager
import me.enivzoy.anilife.app.ui.AniLifeColors
import me.enivzoy.anilife.app.ui.EmptyPanel
import me.enivzoy.anilife.app.ui.GlassButton
import me.enivzoy.anilife.app.ui.GlassSegmentedControl
import me.enivzoy.anilife.app.ui.GlassTabBar
import me.enivzoy.anilife.app.ui.appEnter
import me.enivzoy.anilife.app.ui.appTween
import me.enivzoy.anilife.app.ui.rememberAppFloat
import me.enivzoy.anilife.app.ui.ErrorPanel
import me.enivzoy.anilife.app.ui.hoverScale
import me.enivzoy.anilife.app.ui.liquidGlass
import me.enivzoy.anilife.app.ui.LoadState
import me.enivzoy.anilife.app.ui.PageScrollbar
import me.enivzoy.anilife.app.ui.RemoteImage
import me.enivzoy.anilife.app.ui.cardMeta
import me.enivzoy.anilife.app.ui.loadState
import me.enivzoy.anilife.app.ui.parseAnimeColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.io.File
import java.awt.Desktop
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val DownloadSurface get() = AniLifeColors.DownloadSurface
private val DownloadSurfaceRaised get() = AniLifeColors.DownloadSurfaceRaised
private val DownloadOutline get() = AniLifeColors.DownloadOutline
private val DownloadText get() = AniLifeColors.DownloadText
private val DownloadMuted get() = AniLifeColors.DownloadMuted
private val DownloadAccent get() = AniLifeColors.DownloadAccent
private val DownloadAccentForeground get() = AniLifeColors.DownloadAccentForeground

@Composable
fun DownloadsScreen(
    repository: AniListRepository,
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    onPlay: (String, String) -> Unit,
    onOpen: (Long) -> Unit = {},
    onBrowse: () -> Unit = {},
    queueManager: DownloadQueueManager,
    upscaleQueue: UpscaleQueueManager,
) {
    val episodes by repository.localEpisodes.collectAsState()
    val continueEntries by ContinueStoreInstance.entries.collectAsState()
    val scope = rememberCoroutineScope()
    var mediaRoot by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var scan by remember { mutableStateOf<AutoDetectResult?>(null) }
    var scanAnime by remember { mutableStateOf("") }
    var scanEpisode by remember { mutableStateOf("") }
    var selectedDownloadAnime by remember { mutableStateOf<me.enivzoy.anilife.core.model.AnimeCard?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    val candidateIndex = remember { mutableStateMapOf<String, Int>() }
    val downloadJobs by queueManager.jobs.collectAsState()
    var topTab by remember { mutableIntStateOf(0) }
    var showUpscaleDialog by remember { mutableStateOf(false) }
    val upscaleJobs by upscaleQueue.jobs.collectAsState()

    fun selectMediaFolder(): Path? {
        val parent = java.awt.Frame()
        parent.isVisible = false
        val dialog = java.awt.FileDialog(parent, "Choose the folder containing your anime folders", java.awt.FileDialog.LOAD)
        dialog.isVisible = true
        val selected = dialog.file?.let { java.io.File(dialog.directory, it) } ?: dialog.directory?.let { java.io.File(it) }
        parent.dispose()
        return selected?.toPath()
    }

    fun beginScan(root: Path) {
        mediaRoot = root.toAbsolutePath().normalize().toString()
        scan = null
        error = null
        scanAnime = ""
        scanEpisode = ""
        scanning = true
        scope.launch {
            try {
                scan = loadState {
                    repository.detectLocalMedia(root, settings.audio) { name, episode ->
                        scanAnime = name
                        scanEpisode = episode
                    }
                }.let { result ->
                    when (result) {
                        LoadState.Loading -> null
                        is LoadState.Failed -> throw IllegalStateException(result.message)
                        is LoadState.Ready -> result.value
                    }
                }
                candidateIndex.clear()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                error = t.message?.takeIf(String::isNotBlank) ?: "Couldn't scan this folder."
            } finally {
                scanning = false
                scanAnime = ""
                scanEpisode = ""
            }
        }
    }

    fun importPortable(json: Path, root: Path) {
        importing = true
        error = null
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { repository.importPortableLibrary(json, root) }
                message = buildImportMessage(result)
                mediaRoot = root.toAbsolutePath().normalize().toString()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                error = t.message?.takeIf(String::isNotBlank) ?: "Couldn't import the portable library."
            } finally {
                importing = false
            }
        }
    }

    fun selectPortableLibrary() {
        val parent = java.awt.Frame()
        parent.isVisible = false
        val chooser = java.awt.FileDialog(parent, "Import an AniLife portable library", java.awt.FileDialog.LOAD)
        chooser.isVisible = true
        val selectedFile = chooser.file?.let { java.io.File(chooser.directory, it) }
        parent.dispose()
        if (selectedFile == null) return
        val json = selectedFile.toPath()
        val defaultRoot = mediaRoot?.let(Path::of) ?: json.parent
        val folderParent = java.awt.Frame()
        folderParent.isVisible = false
        val folderChooser = java.awt.FileDialog(folderParent, "Choose the folder containing the anime folders", java.awt.FileDialog.LOAD)
        folderChooser.isVisible = true
        val folderSelected = folderChooser.file?.let { java.io.File(folderChooser.directory, it) } ?: folderChooser.directory?.let { java.io.File(it) }
        folderParent.dispose()
        if (folderSelected != null) {
            importPortable(json, folderSelected.toPath())
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
        GlassTabBar(
            labels = listOf("Downloads", "Upscale"),
            selectedIndex = topTab,
            onSelect = { topTab = it },
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        )
        if (topTab == 0) {
        error?.let { problem ->
            Box(Modifier.fillMaxWidth().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                ErrorPanel(problem, onRetry = { error = null })
            }
        }

        val detected = scan
        if (selectedDownloadAnime == null && detected != null) {
            ScanReview(
                result = detected,
                candidateIndex = candidateIndex,
                onCandidate = { folderName, index ->
                    candidateIndex[folderName] = index
                    val selectedFolder = detected.folders.firstOrNull { it.name == folderName }
                    val selectedAnime = selectedFolder?.matches?.getOrNull(index)
                    if (selectedFolder != null && selectedAnime != null) {
                        scope.launch {
                            val titles = repository.episodeTitles(selectedAnime, selectedFolder.episodes.map(LocalEpisode::episode))
                            if (candidateIndex[folderName] != index) return@launch
                            val currentScan = scan ?: return@launch
                            scan = currentScan.copy(
                                folders = currentScan.folders.map { current ->
                                    if (current.name != folderName) current
                                    else current.copy(episodes = current.episodes.map { episode ->
                                        episode.copy(episodeTitle = titles[episode.episode])
                                    })
                                },
                            )
                        }
                    }
                },
                onCancel = { scan = null },
                onImport = {
                    importing = true
                    scope.launch {
                        try {
                            val chosen = detected.folders.flatMap { folder ->
                                val anime = folder.matches[candidateIndex[folder.name] ?: 0]
                                val titles = repository.episodeTitles(anime, folder.episodes.map(LocalEpisode::episode))
                                folder.episodes.map { entry ->
                                    entry.copy(anime = anime, episodeTitle = titles[entry.episode] ?: entry.episodeTitle)
                                }
                            }
                            repository.addLocalEpisodes(chosen)
                            repository.addToLibrary(chosen.map { it.anime }.distinctBy { it.id })
                            message = "Added ${chosen.size} local episode${if (chosen.size == 1) "" else "s"} across ${detected.folders.size} matched anime."
                            scan = null
                        } catch (e: CancellationException) {
                            throw e
                        } catch (t: Throwable) {
                            error = t.message?.takeIf(String::isNotBlank) ?: "Couldn't save the detected episodes."
                        } finally {
                            importing = false
                        }
                    }
                },
            )
        } else if (selectedDownloadAnime == null) {
            val watchedKeys = remember(continueEntries) {
                buildSet<String> {
                    continueEntries.forEach { entry ->
                        val progress = entry.progress
                        val lastWatched = if (progress.position >= progress.duration * 0.9) progress.episode else progress.episode - 1.0
                        var episode = 1.0
                        // continue rows point at the next unwatched episode, so earlier ones are watched.
                        while (episode <= lastWatched) {
                            add(downloadWatchedKey(entry.anime.title, entry.anime.id, episode))
                            episode += 1.0
                        }
                    }
                }
            }
            DownloadsHome(
                episodes = episodes,
                downloadJobs = downloadJobs,
                watchedKeys = watchedKeys,
                downloadDirectory = settings.downloadDirectory,
                onSettingsChange = onSettingsChange,
                onPlay = onPlay,
                onSelectAnime = { selectedDownloadAnime = it },
                onAutoDetect = { selectMediaFolder()?.let(::beginScan) },
                onBrowse = onBrowse,
                browseEnabled = !importing,
                onImportPortable = ::selectPortableLibrary,
                onDirectDownload = { downloadError = null; showDownloadDialog = true },
                onUpscale = { showUpscaleDialog = true },
                upscaleEnabled = episodes.isNotEmpty(),
                actionsEnabled = !scanning && !importing,
                audioTrack = settings.audio,
                onAudioChange = { audio -> onSettingsChange { it.copy(audio = audio) } },
                onCancelDownload = queueManager::cancel,
                onRetryDownload = queueManager::retry,
                onRemoveDownload = queueManager::remove,
                onClearFinishedDownloads = queueManager::clearFinished,
                onRemoveEpisode = repository::removeLocalEpisode,
            )
        } else {
            EpisodeList(
                episodes = episodes,
                initialTab = if (episodes.isEmpty()) "queue" else "library",
                onPlay = onPlay,
                onRemove = repository::removeLocalEpisode,
                onOpen = onOpen,
                selectedAnime = selectedDownloadAnime,
                onSelectAnime = { selectedDownloadAnime = it },
                continueEpisode = continueEntries.firstOrNull { it.anime.id == selectedDownloadAnime?.id }?.progress?.episode,
                onAutoDetect = { selectMediaFolder()?.let(::beginScan) },
                onImportPortable = ::selectPortableLibrary,
                onDirectDownload = { downloadError = null; showDownloadDialog = true },
                actionsEnabled = !scanning && !importing,
                audioTrack = settings.audio,
                onAudioChange = { audio -> onSettingsChange { it.copy(audio = audio) } },
                downloadJobs = downloadJobs,
                onCancelDownload = queueManager::cancel,
                onRetryDownload = queueManager::retry,
                onRemoveDownload = queueManager::remove,
                onClearFinishedDownloads = queueManager::clearFinished,
                onUpscale = { showUpscaleDialog = true },
                upscaleEnabled = episodes.isNotEmpty(),
            )
        }
        } else {
            UpscaleProgressTab(
                jobs = upscaleJobs,
                onCancel = upscaleQueue::cancel,
                onRetry = upscaleQueue::retry,
                onClearFinished = upscaleQueue::clearFinished,
            )
        }
    }

    if (scanning) {
        Box(
            Modifier.fillMaxSize().padding(20.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AniLifeColors.Card)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Checking | ${scanAnime.ifBlank { "..." }}",
                    color = AniLifeColors.Foreground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (scanEpisode.isNotBlank()) {
                    Text(
                        "Episode $scanEpisode",
                        color = AniLifeColors.MutedForeground,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp).padding(top = 2.dp),
                    strokeWidth = 2.dp,
                    color = AniLifeColors.Primary,
                )
            }
        }
    }
    }

    }
    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            modifier = Modifier.appEnter(),
            containerColor = AniLifeColors.Card,
            titleContentColor = AniLifeColors.Foreground,
            textContentColor = AniLifeColors.MutedForeground,
            title = { Text("Downloads updated", fontWeight = FontWeight.SemiBold) },
            text = { Text(text, fontSize = 13.sp, lineHeight = 20.sp) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("Done", color = AniLifeColors.Primary) } },
        )
    }

    if (showDownloadDialog) {
        DirectDownloadDialog(
            audio = settings.audio,
            error = downloadError,
            onDismiss = { if (!importing) showDownloadDialog = false },
            onDownload = { title, episode, url ->
                downloadError = null
                error = null
                queueManager.enqueue(
                    title = title,
                    episode = episode,
                    url = url,
                    audio = settings.audio,
                    downloadRoot = settings.downloadDirectory?.let(Path::of),
                )
                showDownloadDialog = false
            },
        )
    }

    if (showUpscaleDialog) {
        UpscaleSelectionDialog(
            episodes = episodes,
            onDismiss = { showUpscaleDialog = false },
            onConfirm = { selected ->
                showUpscaleDialog = false
                upscaleQueue.enqueue(selected)
                topTab = 1
            },
        )
    }
}

@Composable
private fun DownloadsHome(
    episodes: List<LocalEpisode>,
    downloadJobs: List<DownloadJob>,
    watchedKeys: Set<String>,
    downloadDirectory: String?,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    onPlay: (String, String) -> Unit,
    onSelectAnime: (me.enivzoy.anilife.core.model.AnimeCard) -> Unit,
    onAutoDetect: () -> Unit,
    onBrowse: () -> Unit,
    browseEnabled: Boolean,
    onImportPortable: () -> Unit,
    onDirectDownload: () -> Unit,
    onUpscale: () -> Unit,
    upscaleEnabled: Boolean,
    actionsEnabled: Boolean,
    audioTrack: String,
    onAudioChange: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (String) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onClearFinishedDownloads: () -> Unit,
    onRemoveEpisode: (LocalEpisode) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val isSearching = query.isNotBlank()
    val root = remember(downloadDirectory) {
        downloadDirectory?.takeIf(String::isNotBlank) ?: DirectMediaDownloader.defaultDownloadDirectory().toString()
    }
    val statsText = remember(episodes, root) {
        "${formatBytes(episodes.sumOf(LocalEpisode::bytes))} in downloads · ${formatBytes(downloadRootFreeSpace(root))} free · ${shortenDownloadPath(root)}"
    }
    val allItems = remember(episodes, downloadJobs, watchedKeys) {
        buildDownloadsHomeItems(episodes, downloadJobs, watchedKeys)
    }
    val visibleItems = remember(allItems, query, filter) {
        val q = query.trim().lowercase()
        allItems.filter { item ->
            (q.isBlank() || item.title.lowercase().contains(q)) &&
                when (filter) {
                    "Unwatched" -> !item.watched
                    "Watched" -> item.watched
                    else -> true
                }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                statsText,
                color = AniLifeColors.MutedForeground,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { chooseDownloadDirectory(onSettingsChange) },
                modifier = Modifier.hoverScale().pointerHoverIcon(PointerIcon.Hand),
            ) { Text("Change", color = AniLifeColors.Primary, fontSize = 12.sp) }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search downloads", color = AniLifeColors.MutedForeground, fontSize = 13.sp) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
            )
            listOf("All", "Unwatched", "Watched").let { filters ->
                GlassSegmentedControl(
                    options = filters,
                    selectedIndex = filters.indexOf(filter).coerceAtLeast(0),
                    onSelect = { filter = filters[it] },
                    modifier = Modifier.width(276.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("Auto detect" to onAutoDetect, "Import" to onImportPortable, "Direct URL" to onDirectDownload).forEach { (label, action) ->
                GlassButton(
                    onClick = action,
                    enabled = actionsEnabled,
                    compact = true,
                    shape = RoundedCornerShape(50),
                ) { Text(label, fontSize = 12.sp) }
            }
            GlassButton(
                onClick = onUpscale,
                enabled = upscaleEnabled && actionsEnabled,
                compact = true,
                accent = true,
                shape = RoundedCornerShape(50),
            ) { Text("Upscale", fontSize = 12.sp) }
            GlassButton(
                onClick = { onAudioChange(if (audioTrack == "sub") "dub" else "sub") },
                compact = true,
                shape = RoundedCornerShape(50),
            ) { Text("Audio: ${audioTrack.uppercase()}", fontSize = 12.sp) }
            TextButton(
                onClick = onClearFinishedDownloads,
                modifier = Modifier.hoverScale().pointerHoverIcon(PointerIcon.Hand),
            ) { Text("Clear finished", color = AniLifeColors.MutedForeground, fontSize = 12.sp) }
        }
        if (episodes.isEmpty() && downloadJobs.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyPanel(
                    icon = Icons.Rounded.Download,
                    title = "No local episodes yet",
                    message = "Auto detect scans folders and matches their anime titles. Import a portable library JSON to restore the files listed by your other AniLife install.",
                    action = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassButton(
                                onClick = onAutoDetect,
                                enabled = actionsEnabled,
                                leadingIcon = Icons.Rounded.AutoAwesome,
                                accent = true,
                            ) { Text("Auto detect") }
                            GlassButton(
                                onClick = onBrowse,
                                enabled = browseEnabled,
                                leadingIcon = Icons.Rounded.Explore,
                            ) { Text("Browse") }
                        }
                    },
                )
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth()) {
                key(isSearching) {
                    AnimatedVisibility(
                        visible = isSearching || visibleItems.isNotEmpty(),
                        enter = fadeIn(appTween(400)) + scaleIn(initialScale = 0.95f),
                        exit = fadeOut(appTween(200)) + scaleOut(targetScale = 0.95f),
                    ) {
                    if (visibleItems.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No downloads match \"$query\".", color = AniLifeColors.MutedForeground, fontSize = 13.sp)
                        }
                    } else {
                        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                        Box(Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 6.dp, bottom = 28.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(visibleItems, key = { it.key }) { item ->
                                    DownloadsHomeCard(
                                        item = item,
                                        onPlay = {
                                            item.localEpisode?.let { episode -> onPlay(episode.anime.title, episode.path.toString()) }
                                        },
                                        onCancel = { item.job?.id?.let(onCancelDownload) },
                                        onRetry = { item.job?.id?.let(onRetryDownload) },
                                        onDelete = {
                                            val job = item.job
                                            if (job != null) onRemoveDownload(job.id) else item.localEpisode?.let(onRemoveEpisode)
                                        },
                                        onOpenDetail = { item.anime?.let(onSelectAnime) },
                                    )
                                }
                            }
                            PageScrollbar(rememberScrollbarAdapter(listState), Modifier.align(Alignment.CenterEnd))
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsHomeCard(
    item: DownloadsListItem,
    onPlay: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val scale by rememberAppFloat(if (hovered) 1.015f else 1f, durationMillis = 180)
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(AniLifeColors.Card)
            .liquidGlass()
            .clickable(interactionSource = interaction, indication = null, onClick = onOpenDetail)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteImage(
            url = item.cover,
            contentDescription = item.title,
            modifier = Modifier.size(width = 60.dp, height = 90.dp).clip(RoundedCornerShape(8.dp)),
            placeholderColor = parseAnimeColor(item.color),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                item.title,
                color = AniLifeColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.statusText,
                color = if (item.error != null) AniLifeColors.Destructive else AniLifeColors.MutedForeground,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.progress?.let { fraction ->
                if (fraction.isNaN()) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = AniLifeColors.Primary,
                        trackColor = AniLifeColors.SurfaceVariant,
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = AniLifeColors.Primary,
                        trackColor = AniLifeColors.SurfaceVariant,
                    )
                }
            }
            item.error?.let { problem ->
                Text(problem, color = AniLifeColors.Destructive, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (item.job?.status == DownloadStatus.Queued || item.job?.status == DownloadStatus.Running) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.hoverScale().pointerHoverIcon(PointerIcon.Hand),
                ) { Text("Cancel", color = AniLifeColors.Destructive, fontSize = 12.sp) }
            }
            if (item.job?.status == DownloadStatus.Failed || item.job?.status == DownloadStatus.Cancelled) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.hoverScale().pointerHoverIcon(PointerIcon.Hand),
                ) { Text("Retry", color = AniLifeColors.Primary, fontSize = 12.sp) }
            }
            if (item.localEpisode != null && item.job?.status != DownloadStatus.Running && item.job?.status != DownloadStatus.Queued) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = AniLifeColors.Primary, contentColor = AniLifeColors.PrimaryForeground),
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.hoverScale().pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Play", fontSize = 12.sp)
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.hoverScale().pointerHoverIcon(PointerIcon.Hand),
            ) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove download", tint = AniLifeColors.MutedForeground) }
        }
    }
}

private data class DownloadsListItem(
    val key: String,
    val title: String,
    val cover: String?,
    val color: String?,
    val statusText: String,
    val progress: Float?,
    val error: String?,
    val localEpisode: LocalEpisode?,
    val job: DownloadJob?,
    val watched: Boolean,
    val anime: me.enivzoy.anilife.core.model.AnimeCard?,
)

private fun buildDownloadsHomeItems(
    episodes: List<LocalEpisode>,
    jobs: List<DownloadJob>,
    watchedKeys: Set<String>,
): List<DownloadsListItem> {
    val items = linkedMapOf<String, DownloadsListItem>()
    episodes.forEach { entry ->
        val anime = entry.anime
        val key = downloadEpisodeKey(entry)
        items[key] = DownloadsListItem(
            key = key,
            title = anime.title,
            cover = anime.cover,
            color = anime.color,
            statusText = "Episode ${entry.episode.episodeLabel()} · Downloaded",
            progress = null,
            error = null,
            localEpisode = entry,
            job = null,
            watched = downloadWatchedKey(anime.title, anime.id, entry.episode) in watchedKeys,
            anime = anime,
        )
    }
    jobs.forEach { job ->
        val local = job.localEpisode
        val key = local?.let(::downloadEpisodeKey) ?: "job:${job.id}"
        val existing = items[key]
        val anime = local?.anime ?: job.anime ?: existing?.anime
        val title = anime?.title ?: job.title
        val fraction = when {
            job.status == DownloadStatus.Queued -> 0f
            job.progressFraction != null -> job.progressFraction
            job.total != null && job.total > 0L -> me.enivzoy.anilife.app.data.downloadProgressFraction(job.downloaded, job.total)
            else -> null
        }
        val percent = fraction?.let { (it * 100).toInt().coerceIn(0, 100) }
        val statusText: String
        val progress: Float?
        val error: String?
        when (job.status) {
            DownloadStatus.Running -> {
                statusText = percent?.let { "Downloading Episode ${job.episode.episodeLabel()} · $it%" }
                    ?: "Downloading Episode ${job.episode.episodeLabel()} · ${formatBytes(job.downloaded)} downloaded"
                progress = fraction ?: Float.NaN
                error = null
            }
            DownloadStatus.Queued -> {
                statusText = "Episode ${job.episode.episodeLabel()} · Queued"
                progress = 0f
                error = null
            }
            DownloadStatus.Failed, DownloadStatus.Cancelled -> {
                statusText = "Episode ${job.episode.episodeLabel()} · ${job.status.name}"
                progress = null
                error = job.error
            }
            DownloadStatus.Done -> {
                statusText = "Episode ${job.episode.episodeLabel()} · Downloaded"
                progress = null
                error = null
            }
        }
        items[key] = DownloadsListItem(
            key = key,
            title = title,
            cover = anime?.cover,
            color = anime?.color,
            statusText = statusText,
            progress = progress,
            error = error,
            localEpisode = local ?: existing?.localEpisode,
            job = job,
            watched = downloadWatchedKey(title, anime?.id, job.episode) in watchedKeys,
            anime = anime,
        )
    }
    return items.values.sortedWith(
        compareByDescending<DownloadsListItem> { it.job?.status == DownloadStatus.Running || it.job?.status == DownloadStatus.Queued }
            .thenBy { it.title.lowercase() }
            .thenBy { it.statusText },
    )
}

private fun downloadWatchedKey(title: String, animeId: Long?, episode: Double): String = "${animeId ?: title}|${episode}"

private fun shortenDownloadPath(path: String): String {
    val normalized = path.replace('/', File.separatorChar).trimEnd(File.separatorChar)
    val parts = normalized.split(File.separatorChar).filter(String::isNotBlank)
    return if (parts.size <= 2) normalized else "...${File.separator}${parts.takeLast(2).joinToString(File.separator)}"
}

private fun downloadRootFreeSpace(path: String): Long {
    var root = File(path)
    while (!root.exists() && root.parentFile != null) root = root.parentFile!!
    return root.usableSpace
}

private fun chooseDownloadDirectory(onSettingsChange: ((AppSettings) -> AppSettings) -> Unit) {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Choose the download folder"
    }
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        val chosen = chooser.selectedFile ?: return
        onSettingsChange { it.copy(downloadDirectory = chosen.toPath().toAbsolutePath().normalize().toString()) }
    }
}

@Composable
private fun DirectDownloadDialog(
    audio: String,
    error: String?,
    onDismiss: () -> Unit,
    onDownload: (String, Double, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var episode by remember { mutableStateOf("1") }
    var url by remember { mutableStateOf("") }
    val number = episode.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.appEnter(),
        containerColor = AniLifeColors.Card,
        titleContentColor = AniLifeColors.Foreground,
        textContentColor = AniLifeColors.MutedForeground,
        title = { Text("Download a video URL", fontWeight = FontWeight.SemiBold) },
        text = {
            val dialogScrollState = rememberScrollState()
            Box(Modifier.widthIn(max = 520.dp)) {
                Column(Modifier.fillMaxWidth().verticalScroll(dialogScrollState), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Downloads direct video files and HLS/DASH playlists. Playlists are saved with FFmpeg.", fontSize = 12.sp, lineHeight = 18.sp)
                    if (error != null) Text(error, color = AniLifeColors.Destructive, fontSize = 12.sp, lineHeight = 18.sp)
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Anime title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = episode, onValueChange = { episode = it }, label = { Text("Episode number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("HTTP(S) video or HLS/DASH URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Audio: ${audio.uppercase()} · saved in your download folder", fontSize = 11.sp)
                }
                PageScrollbar(dialogScrollState, Modifier.align(Alignment.CenterEnd))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AniLifeColors.MutedForeground) } },
        confirmButton = {
            Button(
                onClick = { if (number != null) onDownload(title.trim(), number, url.trim()) },
                enabled = title.isNotBlank() && number != null && number > 0.0 && url.startsWith("http", ignoreCase = true),
                colors = ButtonDefaults.buttonColors(containerColor = AniLifeColors.Primary, contentColor = AniLifeColors.PrimaryForeground),
            ) { Text("Download", fontWeight = FontWeight.SemiBold) }
        },
    )
}

@Composable
private fun ScanReview(
    result: AutoDetectResult,
    candidateIndex: Map<String, Int>,
    onCandidate: (String, Int) -> Unit,
    onCancel: () -> Unit,
    onImport: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${result.folders.size} folders matched · ${result.folders.sumOf { it.episodes.size }} episodes",
                color = AniLifeColors.Foreground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCancel) { Text("Cancel", color = AniLifeColors.MutedForeground) }
            Button(
                onClick = onImport,
                enabled = result.folders.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AniLifeColors.Primary,
                    contentColor = AniLifeColors.PrimaryForeground,
                ),
            ) { Text("Add episodes", fontWeight = FontWeight.SemiBold) }
        }
        if (result.skipped.isNotEmpty()) {
            Text(
                "Skipped ${result.skipped.size} item(s) · ${result.skipped.take(3).joinToString(" · ")}",
                color = AniLifeColors.MutedForeground,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp),
            )
        }
        val state = androidx.compose.foundation.lazy.rememberLazyListState()
        Box(Modifier.weight(1f)) {
            LazyColumn(
                state = state,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(result.folders, key = { "${it.name}-${it.episodes.firstOrNull()?.path}" }) { folder ->
                    MatchRow(folder, candidateIndex[folder.name] ?: 0) { onCandidate(folder.name, it) }
                }
            }
            PageScrollbar(rememberScrollbarAdapter(state), Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun MatchRow(folder: DetectedFolder, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val selected = folder.matches.getOrNull(selectedIndex) ?: folder.matches.first()
    var expanded by remember(folder.name) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AniLifeColors.Card)
            .border(1.dp, AniLifeColors.Border, RoundedCornerShape(10.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteImage(
            url = selected.cover,
            contentDescription = selected.title,
            modifier = Modifier.size(width = 48.dp, height = 68.dp).clip(RoundedCornerShape(6.dp)),
            placeholderColor = parseAnimeColor(selected.color),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(folder.name, color = AniLifeColors.MutedForeground, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(selected.title, color = AniLifeColors.Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${folder.episodes.size} episodes · ${cardMeta(selected)} · ${"%.0f".format(folder.confidence * 100)}% match",
                color = AniLifeColors.MutedForeground,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val titles = folder.episodes.mapNotNull { entry ->
                entry.episodeTitle?.let { "E${entry.episode.episodeLabel()}: $it" }
            }
            if (titles.isNotEmpty()) {
                Text(
                    titles.take(3).joinToString(" · "),
                    color = AniLifeColors.MutedForeground,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box {
            TextButton(onClick = { expanded = true }) { Text("Change", color = AniLifeColors.Primary, fontSize = 12.sp) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.appEnter()) {
                folder.matches.forEachIndexed { index, anime ->
                    DropdownMenuItem(
                        text = { Text(anime.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = { onSelect(index); expanded = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeList(
    episodes: List<LocalEpisode>,
    initialTab: String = "library",
    onPlay: (String, String) -> Unit,
    onRemove: (LocalEpisode) -> Unit,
    onOpen: (Long) -> Unit,
    selectedAnime: me.enivzoy.anilife.core.model.AnimeCard?,
    onSelectAnime: (me.enivzoy.anilife.core.model.AnimeCard?) -> Unit,
    continueEpisode: Double?,
    onAutoDetect: () -> Unit,
    onImportPortable: () -> Unit,
    onDirectDownload: () -> Unit,
    onUpscale: () -> Unit,
    upscaleEnabled: Boolean,
    actionsEnabled: Boolean,
    audioTrack: String,
    onAudioChange: (String) -> Unit,
    downloadJobs: List<DownloadJob>,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (String) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onClearFinishedDownloads: () -> Unit,
) {
    val groups = remember(episodes) {
        episodes.groupBy { it.anime.id }
            .values
            .map { entries ->
                entries.first().anime to entries.sortedWith(compareBy({ it.episode }, { it.audio }))
            }
            .sortedBy { it.first.title.lowercase() }
    }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("title") }
    var tab by remember { mutableStateOf(initialTab) }
    var actionsExpanded by remember { mutableStateOf(false) }
    val activeJobIds = remember(downloadJobs) {
        downloadJobs.filter { it.status == DownloadStatus.Queued || it.status == DownloadStatus.Running }
            .map { it.id }.toSet()
    }
    LaunchedEffect(activeJobIds) { if (activeJobIds.isNotEmpty()) tab = "queue" }

    val filteredGroups = remember(groups, searchQuery, sortMode) {
        val q = searchQuery.trim().lowercase()
        val base = if (q.isBlank()) groups else groups.filter { it.first.title.lowercase().contains(q) }
        when (sortMode) {
            "recent" -> base.sortedByDescending { it.second.maxOfOrNull { e -> e.path.toString() } ?: "" }
            "count" -> base.sortedByDescending { it.second.size }
            else -> base.sortedBy { it.first.title.lowercase() }
        }
    }

    val selectedEpisodes = remember(episodes, selectedAnime) {
        selectedAnime?.let { anime ->
            episodes.filter { it.anime.id == anime.id }
                .distinctBy { "${it.anime.id}|${it.episode}|${it.audio}" }
                .sortedWith(compareBy({ it.episode }, { it.audio }))
        } ?: emptyList()
    }

    if (selectedAnime != null) {
        val anime = selectedAnime
        var gridLayout by remember(anime.id) { mutableStateOf(false) }
        var selectedKeys by remember(anime.id) { mutableStateOf(emptySet<String>()) }
        val episodeKeys = selectedEpisodes.map(::downloadEpisodeKey).toSet()
        val allSelected = episodeKeys.isNotEmpty() && selectedKeys.containsAll(episodeKeys)
        val resumeEntry = selectedEpisodes.firstOrNull { it.episode == continueEpisode } ?: selectedEpisodes.firstOrNull()
        Column(Modifier.fillMaxSize()) {
            TextButton(
                onClick = { onSelectAnime(null) },
                colors = ButtonDefaults.textButtonColors(contentColor = DownloadMuted),
                modifier = Modifier.padding(start = 18.dp, top = 8.dp, bottom = 2.dp),
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text("Downloads", fontSize = 13.sp)
            }
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp).height(190.dp)
                    .clip(RoundedCornerShape(18.dp)).background(DownloadSurface),
            ) {
                RemoteImage(
                    url = anime.cover,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.2f },
                    placeholderColor = DownloadSurface,
                )
                Box(Modifier.fillMaxSize().background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        0f to DownloadSurface.copy(alpha = 0.96f),
                        0.62f to DownloadSurface.copy(alpha = 0.84f),
                        1f to DownloadSurface.copy(alpha = 0.55f),
                    ),
                ))
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    RemoteImage(
                        url = anime.cover,
                        contentDescription = anime.title,
                        modifier = Modifier.size(width = 96.dp, height = 144.dp).clip(RoundedCornerShape(10.dp)),
                        placeholderColor = parseAnimeColor(anime.color),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(anime.title, color = DownloadText, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            buildString {
                                append("${selectedEpisodes.size} episodes saved · ${formatBytes(selectedEpisodes.sumOf(LocalEpisode::bytes))}")
                                continueEpisode?.let { append(" · Continue at Episode ${it.episodeLabel()}") }
                            },
                            color = DownloadMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { resumeEntry?.let { onPlay(anime.title, it.path.toUri().toString()) } },
                                enabled = resumeEntry != null,
                                colors = ButtonDefaults.buttonColors(containerColor = DownloadAccent, contentColor = DownloadAccentForeground),
                                shape = RoundedCornerShape(9.dp),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(if (continueEpisode != null) "Continue Episode ${continueEpisode.episodeLabel()}" else "Play Episode ${resumeEntry?.episode?.episodeLabel() ?: ""}", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = { onOpen(anime.id) },
                                shape = RoundedCornerShape(9.dp),
                                border = BorderStroke(1.dp, DownloadOutline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DownloadText),
                            ) {
                                Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Title page")
                            }
                            OutlinedButton(
                                onClick = { resumeEntry?.let { showEpisodeFolder(it) } },
                                enabled = resumeEntry != null,
                                shape = RoundedCornerShape(9.dp),
                                border = BorderStroke(1.dp, DownloadOutline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DownloadText),
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Show in folder")
                            }
                            TextButton(
                                onClick = { selectedEpisodes.forEach(onRemove); selectedKeys = emptySet() },
                                enabled = selectedEpisodes.isNotEmpty(),
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = AniLifeColors.Destructive, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Remove all", color = AniLifeColors.Destructive)
                            }
                        }
                    }
                }
            }
            val episodeGridState = rememberLazyGridState()
            val episodeListState = androidx.compose.foundation.lazy.rememberLazyListState()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = allSelected, onCheckedChange = { checked -> selectedKeys = if (checked) episodeKeys else emptySet() }, colors = CheckboxDefaults.colors(checkedColor = DownloadAccent, uncheckedColor = DownloadOutline, checkmarkColor = DownloadAccentForeground))
                Text(if (selectedKeys.isEmpty()) "Episodes" else "${selectedKeys.size} selected", color = DownloadText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (selectedKeys.isNotEmpty()) {
                    TextButton(onClick = {
                        selectedEpisodes.filter { downloadEpisodeKey(it) in selectedKeys }.forEach(onRemove)
                        selectedKeys = emptySet()
                    }, colors = ButtonDefaults.textButtonColors(contentColor = DownloadMuted)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = DownloadMuted, modifier = Modifier.size(16.dp))
                        Text("Remove selected", color = DownloadMuted)
                    }
                }
                Row(Modifier.clip(RoundedCornerShape(9.dp)).background(DownloadSurfaceRaised).padding(3.dp)) {
                    IconButton(
                        onClick = { gridLayout = false },
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(7.dp)).background(if (!gridLayout) DownloadOutline else Color.Transparent),
                    ) {
                        Icon(Icons.Rounded.ViewList, contentDescription = "List view", tint = if (!gridLayout) DownloadText else DownloadMuted, modifier = Modifier.size(17.dp))
                    }
                    IconButton(
                        onClick = { gridLayout = true },
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(7.dp)).background(if (gridLayout) DownloadOutline else Color.Transparent),
                    ) {
                        Icon(Icons.Rounded.GridView, contentDescription = "Grid view", tint = if (gridLayout) DownloadText else DownloadMuted, modifier = Modifier.size(17.dp))
                    }
                }
            }
            if (gridLayout) {
                Box(Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    state = episodeGridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(selectedEpisodes, key = { it.path.toString() }) { entry ->
                        DownloadEpisodeGridCard(
                            entry = entry,
                            selected = downloadEpisodeKey(entry) in selectedKeys,
                            onToggle = { checked -> selectedKeys = selectedKeys.toMutableSet().apply { if (checked) add(downloadEpisodeKey(entry)) else remove(downloadEpisodeKey(entry)) } },
                            onPlay = onPlay,
                            onShowInFolder = ::showEpisodeFolder,
                            onRemove = onRemove,
                        )
                    }
                }
                PageScrollbar(episodeGridState, Modifier.align(Alignment.CenterEnd))
                }
            } else {
                Box(Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = episodeListState,
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(selectedEpisodes, key = { it.path.toString() }) { entry ->
                        DownloadEpisodeRow(
                            entry = entry,
                            selected = downloadEpisodeKey(entry) in selectedKeys,
                            onToggle = { checked -> selectedKeys = selectedKeys.toMutableSet().apply { if (checked) add(downloadEpisodeKey(entry)) else remove(downloadEpisodeKey(entry)) } },
                            onPlay = onPlay,
                            onShowInFolder = ::showEpisodeFolder,
                            onRemove = onRemove,
                        )
                    }
                }
                PageScrollbar(episodeListState, Modifier.align(Alignment.CenterEnd))
                }
            }
        }
    } else {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${groups.size} anime · ${episodes.size} episode${if (episodes.size == 1) "" else "s"} on this device",
                color = AniLifeColors.Foreground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("library" to "Library", "queue" to "Queue").forEach { (value, label) ->
                    if (tab == value) {
                        Button(
                            onClick = { tab = value },
                            colors = ButtonDefaults.buttonColors(containerColor = AniLifeColors.Primary, contentColor = AniLifeColors.PrimaryForeground),
                            shape = RoundedCornerShape(8.dp),
                        ) { Text(label, fontSize = 12.sp) }
                    } else {
                        OutlinedButton(onClick = { tab = value }, shape = RoundedCornerShape(8.dp)) { Text(label, fontSize = 12.sp) }
                    }
                }
            }
            OutlinedButton(
                onClick = onUpscale,
                enabled = upscaleEnabled && actionsEnabled,
                shape = RoundedCornerShape(8.dp),
            ) { Text("Upscale", fontSize = 12.sp) }
            Box {
                TextButton(onClick = { actionsExpanded = true }, enabled = actionsEnabled) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Download actions", tint = AniLifeColors.MutedForeground)
                }
                DropdownMenu(expanded = actionsExpanded, onDismissRequest = { actionsExpanded = false }, modifier = Modifier.appEnter()) {
                    DropdownMenuItem(
                        text = { Text("Auto detect local episodes") },
                        onClick = { actionsExpanded = false; onAutoDetect() },
                        enabled = actionsEnabled,
                    )
                    DropdownMenuItem(
                        text = { Text("Import portable library") },
                        onClick = { actionsExpanded = false; onImportPortable() },
                        enabled = actionsEnabled,
                    )
                    DropdownMenuItem(
                        text = { Text("Download direct URL") },
                        onClick = { actionsExpanded = false; onDirectDownload() },
                        enabled = actionsEnabled,
                    )
                    DropdownMenuItem(
                        text = { Text("Upscale downloaded episodes") },
                        onClick = { actionsExpanded = false; onUpscale() },
                        enabled = upscaleEnabled && actionsEnabled,
                    )
                    DropdownMenuItem(
                        text = { Text(if (audioTrack == "sub") "Audio: Sub ✓" else "Audio: Sub") },
                        onClick = { actionsExpanded = false; onAudioChange("sub") },
                    )
                    DropdownMenuItem(
                        text = { Text(if (audioTrack == "dub") "Audio: Dub ✓" else "Audio: Dub") },
                        onClick = { actionsExpanded = false; onAudioChange("dub") },
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                placeholder = { Text("Search downloads", color = AniLifeColors.MutedForeground, fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
            )
            OutlinedButton(onClick = { sortMode = if (sortMode == "title") "count" else if (sortMode == "count") "recent" else "title" }, shape = RoundedCornerShape(8.dp)) {
                Text("Sort: ${sortMode.replaceFirstChar { it.uppercase() }}", fontSize = 12.sp)
            }
        }

        if (tab == "queue") {
            val queueListState = androidx.compose.foundation.lazy.rememberLazyListState()
            if (downloadJobs.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No downloads queued.", color = AniLifeColors.MutedForeground, fontSize = 13.sp, lineHeight = 18.sp)
                }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(Modifier.fillMaxSize(), state = queueListState, contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(downloadJobs, key = { it.id }) { job ->
                        DownloadJobRow(
                            job = job,
                            onCancel = { onCancelDownload(job.id) },
                            onRetry = { onRetryDownload(job.id) },
                            onRemove = { onRemoveDownload(job.id) },
                        )
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onClearFinishedDownloads) { Text("Clear finished", color = AniLifeColors.MutedForeground, fontSize = 12.sp) }
                        }
                    }
                }
                PageScrollbar(queueListState, Modifier.align(Alignment.CenterEnd))
                }
            }
        } else if (filteredGroups.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyPanel(icon = Icons.Rounded.Download, title = "No downloads yet", message = "Use Auto detect to register your local anime folders.")
            }
        } else {
            val gridState = rememberLazyGridState()
            Box(Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    state = gridState,
                    contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    filteredGroups.forEach { (anime, animeEpisodes) ->
                        item(key = "anime-${anime.id}") {
                            DownloadAnimeCard(
                                anime = anime,
                                downloadedCount = animeEpisodes.size,
                                downloadedBytes = animeEpisodes.sumOf(LocalEpisode::bytes),
                                onClick = { onSelectAnime(anime) },
                            )
                        }
                    }
                }
                PageScrollbar(rememberScrollbarAdapter(gridState), Modifier.align(Alignment.CenterEnd))
            }
        }
    }
}

}

@Composable
private fun DownloadAnimeCard(
    anime: me.enivzoy.anilife.core.model.AnimeCard,
    downloadedCount: Int,
    downloadedBytes: Long,
    onClick: () -> Unit,
) {
    val totalEpisodes = anime.episodes?.takeIf { it > 0 }
    val downloadFraction = totalEpisodes?.let { (downloadedCount.toFloat() / it).coerceIn(0f, 1f) } ?: 0f
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val scale by rememberAppFloat(if (hovered) 1.06f else 1f, durationMillis = 200)
    Column(
        Modifier.fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(10.dp))
            .background(DownloadSurface)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(parseAnimeColor(anime.color)),
        ) {
            RemoteImage(
                url = anime.cover,
                contentDescription = anime.title,
                modifier = Modifier.fillMaxSize(),
                placeholderColor = parseAnimeColor(anime.color),
            )
            Text(
                text = totalEpisodes?.let { "$downloadedCount / $it" } ?: "$downloadedCount",
                color = AniLifeColors.Foreground,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart)
                    .padding(7.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(AniLifeColors.Background.copy(alpha = 0.82f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 2.dp).height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AniLifeColors.Border),
        ) {
            Box(
                Modifier.fillMaxWidth(downloadFraction).height(3.dp)
                    .background(AniLifeColors.Primary),
            )
        }
        Text(
            text = anime.title,
            color = AniLifeColors.Foreground,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp, start = 8.dp, end = 8.dp),
        )
        Text(
            text = buildString {
                append(formatBytes(downloadedBytes))
                append(" · ")
                if (totalEpisodes != null) append("$downloadedCount of $totalEpisodes downloaded")
                else append("$downloadedCount downloaded")
            },
            color = AniLifeColors.MutedForeground,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun DownloadEpisodeRow(
    entry: LocalEpisode,
    selected: Boolean,
    onToggle: (Boolean) -> Unit,
    onPlay: (String, String) -> Unit,
    onShowInFolder: (LocalEpisode) -> Unit,
    onRemove: (LocalEpisode) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(DownloadSurfaceRaised)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = onToggle, colors = CheckboxDefaults.colors(checkedColor = DownloadAccent, uncheckedColor = DownloadOutline, checkmarkColor = DownloadAccentForeground))
        RemoteImage(
            url = entry.anime.cover,
            contentDescription = "Episode ${entry.episode.episodeLabel()} thumbnail",
            modifier = Modifier.size(width = 140.dp, height = 78.dp).clip(RoundedCornerShape(8.dp)),
            placeholderColor = parseAnimeColor(entry.anime.color),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Episode ${entry.episode.episodeLabel()}", color = DownloadText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.episodeTitle?.takeIf(String::isNotBlank) ?: "${entry.audio.uppercase()} audio", color = DownloadMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatBytes(entry.bytes), color = DownloadMuted.copy(alpha = 0.75f), fontSize = 11.sp)
        }
        IconButton(onClick = { onPlay(entry.anime.title, entry.path.toUri().toString()) }) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play episode", tint = DownloadText)
        }
        IconButton(onClick = { onShowInFolder(entry) }) {
            Icon(Icons.Rounded.FolderOpen, contentDescription = "Show episode in folder", tint = DownloadMuted)
        }
        IconButton(onClick = { onRemove(entry) }) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove episode", tint = DownloadMuted)
        }
    }
}

@Composable
private fun DownloadEpisodeGridCard(
    entry: LocalEpisode,
    selected: Boolean,
    onToggle: (Boolean) -> Unit,
    onPlay: (String, String) -> Unit,
    onShowInFolder: (LocalEpisode) -> Unit,
    onRemove: (LocalEpisode) -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(DownloadSurfaceRaised)) {
        Box {
            RemoteImage(
                url = entry.anime.cover,
                contentDescription = "Episode ${entry.episode.episodeLabel()} thumbnail",
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                placeholderColor = parseAnimeColor(entry.anime.color),
            )
            Checkbox(checked = selected, onCheckedChange = onToggle, modifier = Modifier.align(Alignment.TopStart), colors = CheckboxDefaults.colors(checkedColor = DownloadAccent, uncheckedColor = Color(0xFFE0E1E4).copy(alpha = 0.8f), checkmarkColor = DownloadAccentForeground))
        }
        Text("Episode ${entry.episode.episodeLabel()}", color = DownloadText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(entry.episodeTitle?.takeIf(String::isNotBlank) ?: formatBytes(entry.bytes), color = DownloadMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { onPlay(entry.anime.title, entry.path.toUri().toString()) }) { Icon(Icons.Rounded.PlayArrow, contentDescription = "Play episode", tint = DownloadText) }
            IconButton(onClick = { onShowInFolder(entry) }) { Icon(Icons.Rounded.FolderOpen, contentDescription = "Show episode in folder", tint = DownloadMuted) }
            IconButton(onClick = { onRemove(entry) }) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove episode", tint = DownloadMuted) }
        }
    }
}

private fun downloadEpisodeKey(entry: LocalEpisode): String = "${entry.anime.id}|${entry.episode}|${entry.audio}"

private fun showEpisodeFolder(entry: LocalEpisode) {
    val parent = entry.path.parent?.toFile() ?: return
    runCatching {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(parent)
    }
}

private fun buildImportMessage(result: PortableImportResult): String = buildString {
    append("Imported ${result.anime.size} anime and ${result.episodes.size} local episodes.")
    if (result.missingFiles.isNotEmpty()) {
        append("\n\n${result.missingFiles.size} listed episode file(s) were not found and were skipped:")
        result.missingFiles.take(8).forEach { append("\n• ").append(it) }
        if (result.missingFiles.size > 8) append("\n… and ${result.missingFiles.size - 8} more")
    }
}

private fun Double.episodeLabel(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

@Composable
private fun DownloadJobRow(
    job: DownloadJob,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    val fraction = job.progressFraction ?: job.total?.takeIf { it > 0L }
        ?.let { me.enivzoy.anilife.app.data.downloadProgressFraction(job.downloaded, it) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DownloadSurface)
            .border(1.dp, DownloadOutline, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(job.title, color = DownloadText, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Episode ${job.episode.episodeLabel()} · ${when {
                        job.status == DownloadStatus.Running && job.downloaded == 0L -> "Finding video source…"
                        job.status == DownloadStatus.Running && fraction != null -> "Downloading · ${(fraction * 100).toInt().coerceIn(0, 100)}%"
                        job.status == DownloadStatus.Running -> "Downloading"
                        else -> job.status.name.lowercase().replaceFirstChar { it.uppercase() }
                    }}",
                    color = DownloadMuted,
                    fontSize = 12.sp,
                )
            }
            when (job.status) {
                DownloadStatus.Running, DownloadStatus.Queued -> {
                    OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(8.dp)) { Text("Cancel", fontSize = 12.sp) }
                }
                DownloadStatus.Failed, DownloadStatus.Cancelled -> {
                    OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(8.dp)) { Text("Retry", fontSize = 12.sp) }
                }
                DownloadStatus.Done -> {}
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = DownloadMuted)
            }
        }
        if (job.status == DownloadStatus.Running || job.status == DownloadStatus.Queued) {
            Text(
                when {
                    job.status == DownloadStatus.Queued -> "Waiting to start…"
                    job.downloaded == 0L -> "Finding a video source…"
                    job.total != null && job.total > 0L -> "${formatBytes(job.downloaded)} of ${formatBytes(job.total)}"
                    fraction != null -> "${formatBytes(job.downloaded)} downloaded · ${(fraction * 100).toInt().coerceIn(0, 100)}% of segments"
                    else -> "${formatBytes(job.downloaded)} downloaded · total size unknown"
                },
                color = DownloadMuted,
                fontSize = 11.sp,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DownloadSurfaceRaised),
            ) {
                if (fraction != null) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .height(6.dp)
                            .background(DownloadAccent),
                    )
                } else {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = DownloadAccent,
                        trackColor = DownloadSurfaceRaised,
                    )
                }
            }
        }
        if (job.status == DownloadStatus.Failed && job.error != null) {
            Text(job.error, color = AniLifeColors.Destructive, fontSize = 11.sp, lineHeight = 16.sp)
        }
        if (job.status == DownloadStatus.Done && job.localEpisode != null) {
            Text("Saved · ${job.localEpisode.path.fileName}", color = DownloadMuted, fontSize = 11.sp)
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024L * 1024 -> "%.0f MB".format(bytes / (1024.0 * 1024))
    else -> "%.0f KB".format(bytes / 1024.0)
}

