package me.enivzoy.anilife.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Desktop
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import me.enivzoy.anilife.app.data.AniListRepository
import me.enivzoy.anilife.app.data.AnimeVideoProviders
import me.enivzoy.anilife.app.data.AutoDetectService
import me.enivzoy.anilife.app.data.asAutoDetectLibrary
import me.enivzoy.anilife.app.data.mergeVideoSources
import me.enivzoy.anilife.app.data.DiscordOAuth
import me.enivzoy.anilife.app.data.AppSettingsStore
import me.enivzoy.anilife.app.data.SillyMode
import me.enivzoy.anilife.app.data.DEFAULT_DISCORD_APPLICATION_ID
import me.enivzoy.anilife.app.data.DiscordPresenceManager
import me.enivzoy.anilife.app.data.DownloadQueueManager
import me.enivzoy.anilife.app.data.DirectMediaDownloader
import me.enivzoy.anilife.app.data.DownloadStatus
import me.enivzoy.anilife.app.data.LocalPlayback
import me.enivzoy.anilife.app.data.isUpscaled
import me.enivzoy.anilife.app.data.toDetail
import me.enivzoy.anilife.app.data.ProviderCatalog
import me.enivzoy.anilife.app.screens.BrowseScreen
import me.enivzoy.anilife.app.screens.AnimeWatchScreen
import me.enivzoy.anilife.app.screens.CommandPalette
import me.enivzoy.anilife.app.screens.DetailScreen
import me.enivzoy.anilife.app.screens.DownloadsScreen
import me.enivzoy.anilife.app.screens.HomeScreen
import me.enivzoy.anilife.app.screens.LibraryScreen
import me.enivzoy.anilife.app.screens.PlayerScreen
import me.enivzoy.anilife.app.screens.ProvidersScreen
import me.enivzoy.anilife.app.screens.ScheduleScreen
import me.enivzoy.anilife.app.screens.SettingsScreen
import me.enivzoy.anilife.app.screens.UpdateDialog
import me.enivzoy.anilife.app.screens.ProfileScreen
import me.enivzoy.anilife.app.screens.NotificationsScreen
import me.enivzoy.anilife.app.data.MyAnimeListSync
import me.enivzoy.anilife.app.data.AniListSync
import me.enivzoy.anilife.app.data.Updater
import me.enivzoy.anilife.app.data.UpdateState
import me.enivzoy.anilife.app.data.UpdateException
import me.enivzoy.anilife.app.data.UpdateHttpException
import me.enivzoy.anilife.app.data.SizeMismatchException
import me.enivzoy.anilife.app.data.ChecksumException
import me.enivzoy.anilife.app.data.redactForLog
import me.enivzoy.anilife.app.data.MpvBootstrap
import me.enivzoy.anilife.app.data.NoticeStoreInstance
import me.enivzoy.anilife.app.data.ContinueStoreInstance
import me.enivzoy.anilife.app.data.ReleaseNotifier
import me.enivzoy.anilife.core.model.Update
import me.enivzoy.anilife.core.model.AnimeCard
import me.enivzoy.anilife.core.model.AnimeDetail
import me.enivzoy.anilife.core.diagnostics.ActionBreadcrumbs
import me.enivzoy.anilife.core.diagnostics.DiagnosticsPaths
import me.enivzoy.anilife.core.diagnostics.MemoryProfiler
import io.github.oshai.kotlinlogging.KotlinLogging
import me.enivzoy.anilife.app.ui.AniLifeColors
import me.enivzoy.anilife.app.ui.RemoteImages
import me.enivzoy.anilife.app.ui.AppSidebar
import me.enivzoy.anilife.app.ui.appEnter
import me.enivzoy.anilife.app.ui.discoMode
import me.enivzoy.anilife.app.ui.liquidGlass
import me.enivzoy.anilife.app.ui.rememberAppScreenTransition
import me.enivzoy.anilife.mpv.Mpv
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}
private val updaterLogger = KotlinLogging.logger("AniLife.updater")

internal fun downloadQueueProgress(jobs: List<me.enivzoy.anilife.app.data.DownloadJob>): Float? {
    val active = jobs.filter {
        it.status == me.enivzoy.anilife.app.data.DownloadStatus.Queued ||
            it.status == me.enivzoy.anilife.app.data.DownloadStatus.Running
    }
    if (active.isEmpty()) return null
    val fractions = active.map { job ->
        when {
            job.status == me.enivzoy.anilife.app.data.DownloadStatus.Queued -> 0f
            job.progressFraction != null -> job.progressFraction
            job.total != null && job.total > 0L ->
                (job.downloaded.toDouble() / job.total.toDouble()).toFloat().coerceIn(0f, 1f)
            else -> null
        }
    }
    if (fractions.any { it == null }) return Float.NaN
    return fractions.filterNotNull().average().toFloat().coerceIn(0f, 1f)
}
private const val UPDATE_LOG_TAIL_BYTES = 200 * 1024

@Composable
fun AniLifeApp(
    settingsStore: AppSettingsStore,
    onPlayerFullscreen: (Boolean) -> Unit = {},
    onPlayerPip: (Boolean) -> Unit = {},
    onExitApplication: () -> Unit = {},
) {
    val animeVideoProviders = remember { AnimeVideoProviders() }
    val repository = remember {
        AniListRepository(
            titleLanguage = settingsStore.settings.value.titleLanguage,
            videoProviders = animeVideoProviders,
        )
    }
    val mediaProviders = remember { ProviderCatalog() }
    val discordPresence = remember { DiscordPresenceManager() }
    val releaseNotifier = remember(repository) { ReleaseNotifier(repository) }
    val updater = remember { Updater() }
    val availableUpdate by updater.update.collectAsState()
    val downloadScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    val downloadQueue = remember(repository) { DownloadQueueManager(repository, downloadScope, 2) }
    val downloadJobs by downloadQueue.jobs.collectAsState()
    val autoDetect = remember(repository) {
        AutoDetectService(library = repository.asAutoDetectLibrary(), scope = downloadScope)
    }
    val autoDetectScan by autoDetect.scan.collectAsState()
    val autoDetectRunning by autoDetect.running.collectAsState()
    val autoDetectStatus by autoDetect.status.collectAsState()
    val settings by settingsStore.settings.collectAsState()
    val discordState by discordPresence.state.collectAsState()
    val smartDownloaded = remember(repository) { mutableSetOf<String>() }
    LaunchedEffect(settings.expImageCaching) {
        RemoteImages.diskCacheEnabled = settings.expImageCaching
    }
    val backStack = remember { mutableStateListOf<Screen>() }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val mergedAudioKeys = remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(screen is Screen.Player) {
        if (screen !is Screen.Player) mergedAudioKeys.value = emptySet()
    }
    var homeLoaded by remember { mutableStateOf(false) }
    var playerFullscreen by remember { mutableStateOf(false) }
    var isPipMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var updateCheckStatus by remember { mutableStateOf<String?>(null) }
    var checkingForUpdates by remember { mutableStateOf(false) }
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var shareNotice by remember { mutableStateOf(false) }

    suspend fun checkForUpdates(showUpToDate: Boolean) {
        if (checkingForUpdates) return
        checkingForUpdates = true
        updateCheckStatus = if (showUpToDate) "Checking for updates…" else null
        shareNotice = false
        updater.clear()
        try {
            val currentVersion = Updater.currentVersion()
            val update = updater.check(currentVersion, settings.updateChannel)
            updateCheckStatus = if (update == null && showUpToDate) {
                "AniLife $currentVersion is up to date."
            } else {
                null
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            val detail = failure.message?.takeIf(String::isNotBlank) ?: "Unknown error"
            updateCheckStatus = "Update check failed: $detail"
            updaterLogger.error {
                "update check failed ${failure::class.java.simpleName}: ${redactForLog(detail)}"
            }
        } finally {
            checkingForUpdates = false
        }
    }

    fun shareUpdateLogs() {
        val text = runCatching {
            val bytes = Files.readAllBytes(Updater.logFile())
            val tail = if (bytes.size > UPDATE_LOG_TAIL_BYTES) {
                bytes.copyOfRange(bytes.size - UPDATE_LOG_TAIL_BYTES, bytes.size)
            } else {
                bytes
            }
            String(tail, StandardCharsets.UTF_8)
        }.getOrElse { failure ->
            updaterLogger.warn {
                "update logs not read ${failure::class.java.simpleName}: " +
                    redactForLog(failure.message.orEmpty())
            }
            "updater.log is missing."
        }
        clipboard.setText(AnnotatedString(text))
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            runCatching { Desktop.getDesktop().open(DiagnosticsPaths.logs.toFile()) }
                .onFailure { failure ->
                    updaterLogger.warn {
                        "update logs folder not opened ${failure::class.java.simpleName}: " +
                            redactForLog(failure.message.orEmpty())
                    }
                }
        }
        shareNotice = true
    }

    fun installUpdate(update: Update) {
        if (updateState.busy) return
        scope.launch {
            shareNotice = false
            updateState = UpdateState.Downloading(null)
            var lastPercent = -1
            try {
                val extension = if (update.url.contains(".msi", ignoreCase = true)) "msi" else "exe"
                val destination = Updater.updatesDirectory().resolve("anilife-${update.version}.$extension")
                updater.download(update, destination) { bytes, total ->
                    val percent = if (total != null && total > 0L) {
                        (bytes * 100 / total).toInt()
                    } else {
                        -1
                    }
                    if (percent != lastPercent) {
                        lastPercent = percent
                        updateState = UpdateState.Downloading(if (percent >= 0) percent / 100f else null)
                    }
                }
                updateState = UpdateState.Installing
                updater.installWindows(destination)
                delay(1500L)
                updater.clear()
                onExitApplication()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                updaterLogger.error {
                    "install failed ${failure::class.java.simpleName}: " +
                        redactForLog(failure.message.orEmpty())
                }
                updateState = UpdateState.Failed(shortUpdateFailureMessage(failure))
            }
        }
    }

    LaunchedEffect(Unit) {
        MemoryProfiler.start(this)
        Updater.reportPreviousInstall()
        AniListSync.restore()
        MyAnimeListSync.configure()
        MyAnimeListSync.restore()
        DiscordOAuth.restore()
    }
    DisposableEffect(Unit) {
        onDispose { MemoryProfiler.stop() }
    }
    LaunchedEffect(settings.checkUpdatesOnLaunch, settings.updateChannel) {
        if (settings.checkUpdatesOnLaunch) {
            checkForUpdates(showUpToDate = false)
        } else {
            updater.clear()
            updateCheckStatus = null
        }
    }
    LaunchedEffect(Unit) {
        val path = MpvBootstrap.ensure { status -> NoticeStoreInstance.add(kind = "mpv", title = "mpv", body = status) }
        if (path == null) {
            NoticeStoreInstance.add(kind = "mpv", title = "mpv", body = "libmpv not found. Install mpv manually.")
        }
    }

    // Configuration only: the player reports position/pause/episode changes directly to the worker.
    LaunchedEffect(settings.discordPresenceEnabled, settings.discordApplicationId) {
        discordPresence.configure(
            enabled = settings.discordPresenceEnabled,
            configuredApplicationId = settings.discordApplicationId,
        )
    }
    DisposableEffect(discordPresence) {
        onDispose { discordPresence.close() }
    }

    LaunchedEffect(
        settings.autoDetectEnabled,
        settings.autoDetectDirectories,
        settings.autoDetectSettleSeconds,
        settings.audio,
    ) {
        autoDetect.configure(
            directories = settings.autoDetectDirectories,
            settleSeconds = settings.autoDetectSettleSeconds,
            audio = settings.audio,
            autoImport = true,
        )
        if (settings.autoDetectEnabled) autoDetect.start() else autoDetect.stop()
    }
    DisposableEffect(autoDetect) {
        onDispose { autoDetect.stop() }
    }

    fun updateSettings(transform: (me.enivzoy.anilife.app.data.AppSettings) -> me.enivzoy.anilife.app.data.AppSettings) {
        settingsStore.update(transform)
        repository.setTitleLanguage(settingsStore.settings.value.titleLanguage)
    }

    fun connectDiscord() {
        val id = settings.discordApplicationId.ifBlank { DEFAULT_DISCORD_APPLICATION_ID }
        if (!settings.discordPresenceEnabled || settings.discordApplicationId != id) {
            updateSettings { it.copy(discordPresenceEnabled = true, discordApplicationId = id) }
        }
        discordPresence.configure(
            enabled = true,
            configuredApplicationId = id,
            reconnect = true,
        )
    }

    var picking by remember { mutableStateOf(false) }
    var pickError by remember { mutableStateOf<String?>(null) }
    val mpvLibrary = remember { runCatching { Mpv.defaultLibraryPath() }.getOrNull() }
    var showIntro by remember { mutableStateOf(settings.playIntro) }

    LaunchedEffect(screen) {
        playerFullscreen = screen is Screen.Player
        if (screen !is Screen.Player) isPipMode = false
    }
    LaunchedEffect(playerFullscreen) { onPlayerFullscreen(playerFullscreen) }
    LaunchedEffect(isPipMode) { onPlayerPip(isPipMode) }

    fun navigateTo(next: Screen) {
        if (next == screen) return
        backStack.add(screen)
        screen = next
    }

    fun goBack() {
        screen = backStack.removeLastOrNull() ?: Screen.Home
    }

    fun pickRandom() {
        if (picking) return
        picking = true
        scope.launch {
            try {
                val pick = repository.random()
                if (pick != null) {
                    navigateTo(Screen.Detail(pick.id))
                } else {
                    pickError = "AniList returned no title. Try the dice again."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                pickError = t.message?.takeIf { it.isNotBlank() } ?: "Couldn't reach AniList."
            } finally {
                picking = false
            }
        }
    }

    fun localPlayerScreen(
        detail: AnimeDetail,
        entry: me.enivzoy.anilife.app.data.LocalEpisode,
    ): Screen.Player {
        val all = repository.localEpisodes.value
        val quality = if (entry.isUpscaled()) "4K" else "Downloaded"
        return Screen.Player(
            title = "${detail.title} · Episode ${formatPlayerEpisode(entry.episode)} · $quality",
            url = entry.path.toUri().toString(),
            detail = detail,
            episode = entry.episode,
            episodeTitle = entry.episodeTitle,
            episodes = LocalPlayback.seriesEpisodes(all, detail.id, detail.title, detail.episodes),
            audioTrack = entry.audio,
        )
    }

    suspend fun resolvePlayerEpisode(target: Screen.Player, episode: Double, audio: String) {
        val anime = target.detail ?: return
        val current = screen as? Screen.Player ?: return
        if (current.detail?.id != anime.id) return
        val label = formatPlayerEpisode(episode)
        val local = me.enivzoy.anilife.app.data.LocalPlayback.find(
            entries = repository.localEpisodes.value,
            animeId = anime.id,
            title = anime.title,
            episode = episode,
            audio = audio,
        )
        if (local != null) {
            val quality = if (local.isUpscaled()) "4K" else "Downloaded"
            logger.info { "local playback used episode=$episode audio=${local.audio}" }
            screen = current.copy(
                title = "${anime.title} · Episode $label · $quality",
                url = local.path.toUri().toString(),
                headers = emptyMap(),
                alternatives = emptyList(),
                audioTrack = local.audio,
                audioResolving = false,
                audioError = null,
                episode = episode,
                episodeTitle = local.episodeTitle ?: target.episodeTitles[episode],
                episodes = me.enivzoy.anilife.app.data.LocalPlayback.seriesEpisodes(
                    repository.localEpisodes.value,
                    anime.id,
                    anime.title,
                    anime.episodes,
                ).let { localNumbers ->
                    (target.episodes + localNumbers).distinct().sorted()
                },
            )
            return
        }
        val startedAt = System.nanoTime()
        val onlineAudio = if (audio.equals(me.enivzoy.anilife.app.data.UPSCALED_AUDIO, ignoreCase = true)) settings.audio else audio
        val rows = animeVideoProviders.episodes(anime.id, anime.episodes, anime.malId)
        val resolved = animeVideoProviders.sources(
            anilistId = anime.id,
            malId = anime.malId,
            titles = listOfNotNull(anime.title, anime.titleRomaji, anime.titleNative) + anime.synonyms,
            episode = episode,
            dub = onlineAudio.equals("Dub", ignoreCase = true),
        )
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L
        val selected = resolved.firstOrNull()
        if (selected == null) {
            logger.warn { "stream resolution found no servers; episode=$episode audio=$audio elapsedMs=$elapsedMs" }
            return
        }
        val fallback = !target.url.isNullOrBlank() && resolved.none { it.url == target.url }
        logger.info {
            "stream resolved episode=$episode audio=$audio servers=${resolved.size} elapsedMs=$elapsedMs " +
                "fallback=$fallback selected=${selected.provider}/${selected.server}"
        }
        if (fallback) logger.warn { "fallback server used for episode=$episode selected=${selected.provider}/${selected.server}" }
        val now = screen as? Screen.Player ?: return
        if (now.detail?.id != anime.id) return
        val numbers = (rows.map { it.number } + me.enivzoy.anilife.app.data.LocalPlayback.availableEpisodes(
            repository.localEpisodes.value,
            anime.id,
            anime.title,
        )).distinct().sorted()
        screen = now.copy(
            title = "${anime.title} · Episode $label · ${selected.provider} ${selected.quality ?: selected.format.uppercase()}",
            url = selected.url,
            headers = selected.headers,
            alternatives = resolved,
            audioTrack = audio,
            audioResolving = false,
            audioError = null,
            episode = episode,
            episodeTitle = rows.firstOrNull { it.number == episode }?.title,
            episodes = numbers,
            episodeTitles = rows.mapNotNull { row -> row.title?.takeIf(String::isNotBlank)?.let { row.number to it } }.toMap(),
            episodeThumbnails = rows.mapNotNull { row -> row.thumbnail?.takeIf(String::isNotBlank)?.let { row.number to it } }.toMap(),
        )
    }

    fun smartDownloadNext(anime: AnimeDetail?, episode: Double, audio: String) {
        if (!settings.expSmartDownloads || anime == null) return
        val key = "${anime.id}:$episode"
        if (key in smartDownloaded) return
        val alreadyDownloaded = repository.localEpisodes.value.any { it.anime.id == anime.id && it.episode == episode }
        val alreadyQueued = downloadQueue.jobs.value.any { job ->
            job.anime?.id == anime.id && job.episode == episode &&
                job.status != DownloadStatus.Failed && job.status != DownloadStatus.Cancelled
        }
        if (alreadyDownloaded || alreadyQueued) return
        smartDownloaded += key
        scope.launch {
            try {
                val titles = listOfNotNull(anime.title, anime.titleRomaji, anime.titleNative) + anime.synonyms
                val sources = animeVideoProviders.sources(
                    anilistId = anime.id,
                    malId = anime.malId,
                    titles = titles,
                    episode = episode,
                    dub = audio.equals("Dub", ignoreCase = true),
                )
                val source = me.enivzoy.anilife.app.data.pickDownloadSource(
                    sources,
                    me.enivzoy.anilife.app.data.DownloadTarget(resolution = 1080, server = "Auto"),
                )
                if (source == null) {
                    smartDownloaded -= key
                    return@launch
                }
                val root = settings.downloadDirectory?.takeIf(String::isNotBlank)?.let(java.nio.file.Path::of)
                    ?: DirectMediaDownloader.defaultDownloadDirectory()
                downloadQueue.enqueue(
                    title = anime.title,
                    episode = episode,
                    url = source.url,
                    audio = source.audio.lowercase(),
                    downloadRoot = root,
                    anime = anime.toCard(),
                    headers = source.headers,
                    maxHeight = 1080,
                    resolve = { exclusions ->
                        val fresh = animeVideoProviders.sources(
                            anilistId = anime.id,
                            malId = anime.malId,
                            titles = titles,
                            episode = episode,
                            dub = audio.equals("Dub", ignoreCase = true),
                            refresh = exclusions.urls.isNotEmpty(),
                        )
                        me.enivzoy.anilife.app.data.pickDownloadSource(
                            fresh,
                            me.enivzoy.anilife.app.data.DownloadTarget(resolution = 1080, server = "Auto"),
                            exclusions,
                        )
                    },
                )
            } catch (cancelled: CancellationException) {
                smartDownloaded -= key
                throw cancelled
            } catch (_: Throwable) {
                smartDownloaded -= key
            }
        }
    }

    fun deleteSmartDownload(anime: AnimeDetail?, episode: Double) {
        if (!settings.expSmartDownloads || anime == null) return
        if ("${anime.id}:$episode" !in smartDownloaded) return
        val queued = downloadQueue.jobs.value.filter { job ->
            job.anime?.id == anime.id && job.episode == episode
        }
        queued.forEach { downloadQueue.remove(it.id) }
        val entry = repository.localEpisodes.value
            .firstOrNull { it.anime.id == anime.id && it.episode == episode }
        if (entry != null) {
            runCatching { Files.deleteIfExists(entry.path) }
            repository.removeLocalEpisode(entry)
        }
        smartDownloaded -= "${anime.id}:$episode"
    }

    var paletteOpen by remember { mutableStateOf(false) }
    var isSillyMode by remember { mutableStateOf(false) }

    LaunchedEffect(isSillyMode) {
        if (isSillyMode) SillyMode.start() else SillyMode.stop()
    }
    LaunchedEffect(screen is Screen.Player, isSillyMode) {
        if (screen is Screen.Player) SillyMode.pause() else if (isSillyMode) SillyMode.resume()
    }
    DisposableEffect(Unit) {
        onDispose { SillyMode.stop() }
    }

    Box(
        Modifier.fillMaxSize()
            .discoMode(isSillyMode && screen !is Screen.Player)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.isCtrlPressed || event.isMetaPressed) &&
                    event.key == Key.K
                ) {
                    paletteOpen = !paletteOpen
                    true
                } else {
                    false
                }
            },
    ) {
    Row(Modifier.fillMaxSize()) {
        if (screen !is Screen.Player) {
            AppSidebar(
                current = screen,
                onNavigate = ::navigateTo,
                onRandom = ::pickRandom,
                onSearch = { paletteOpen = true },
                downloadProgress = downloadQueueProgress(downloadJobs),
                sillyMode = isSillyMode,
                onSillyMode = {
                    isSillyMode = !isSillyMode
                    repository.setOnlyAdult(isSillyMode)
                    if (isSillyMode) updateSettings { it.copy(volume = 100) }
                },
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape &&
                        (screen is Screen.Detail || screen is Screen.Watch || screen is Screen.Player)
                    ) {
                        goBack()
                        true
                    } else {
                        false
                    }
                },
        ) {
            key(settings.titleLanguage, isSillyMode) {
            val screenTransition = rememberAppScreenTransition()
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    if (settings.expFadeTransitions) screenTransition
                    else EnterTransition.None togetherWith ExitTransition.None
                },
                label = "screen",
            ) { target ->
                Box(Modifier.fillMaxSize().appEnter()) {
                when (target) {
                    Screen.Home -> HomeScreen(
                        repository = repository,
                        settings = settings,
                        onSettingsChange = ::updateSettings,
                        onOpen = { navigateTo(Screen.Detail(it)) },
                        onContinue = { entry ->
                            val local = LocalPlayback.pick(
                                repository.localEpisodes.value,
                                entry.anime.id,
                                entry.anime.title,
                                entry.progress.episode.toDouble(),
                                settings.audio.lowercase(),
                            )
                            if (local != null) {
                                navigateTo(localPlayerScreen(entry.anime.toDetail(), local))
                            } else {
                                scope.launch {
                                    val detail = runCatching { repository.detail(entry.anime.id) }.getOrNull()
                                    if (detail != null) {
                                        val nextEp = (entry.progress.episode).takeIf { it > 0.0 }
                                        navigateTo(Screen.Watch(detail, nextEp))
                                    } else {
                                        navigateTo(Screen.Detail(entry.anime.id))
                                    }
                                }
                            }
                        },
                        onContentReady = { homeLoaded = true },
                    )
                    Screen.Browse -> BrowseScreen(
                        repository = repository,
                        settings = settings,
                        onOpen = { navigateTo(Screen.Detail(it)) },
                    )
                    Screen.Schedule -> ScheduleScreen(repository = repository, onOpen = { navigateTo(Screen.Detail(it)) })
                    Screen.Library -> LibraryScreen(
                        repository = repository,
                        onOpen = { navigateTo(Screen.Detail(it)) },
                        onBrowse = { navigateTo(Screen.Browse) },
                    )
                    Screen.Settings -> SettingsScreen(
                        settings = settings,
                        repository = repository,
                        discordState = discordState,
                        onSettingsChange = ::updateSettings,
                        onDiscordConnect = ::connectDiscord,
                        autoDetect = autoDetect,
                        autoDetectScan = autoDetectScan,
                        autoDetectRunning = autoDetectRunning,
                        autoDetectStatus = autoDetectStatus,
                        currentVersion = Updater.currentVersion(),
                        updateCheckStatus = updateCheckStatus,
                        checkingForUpdates = checkingForUpdates,
                        onCheckForUpdates = { scope.launch { checkForUpdates(showUpToDate = true) } },
                    )
                    Screen.Providers -> ProvidersScreen(catalog = mediaProviders)
                    Screen.Notifications -> NotificationsScreen(
                        store = NoticeStoreInstance,
                        notifier = releaseNotifier,
                        onOpenAnime = { navigateTo(Screen.Detail(it)) },
                    )
                    Screen.Profile -> ProfileScreen(onOpenAnime = { navigateTo(Screen.Detail(it)) })
                    Screen.Downloads -> DownloadsScreen(
                        repository = repository,
                        settings = settings,
                        onSettingsChange = ::updateSettings,
                        onPlay = { title, url ->
                            val entry = repository.localEpisodes.value
                                .firstOrNull { LocalPlayback.matchesLocation(it, url) }
                            if (entry == null) {
                                navigateTo(Screen.Player(title, url))
                            } else {
                                navigateTo(localPlayerScreen(entry.anime.toDetail(), entry))
                            }
                        },
                        onOpen = { id -> navigateTo(Screen.Detail(id)) },
                        onBrowse = { navigateTo(Screen.Browse) },
                        queueManager = downloadQueue,
                    )
                    is Screen.Detail -> DetailScreen(
                        repository = repository,
                        providers = animeVideoProviders,
                        id = target.id,
                        onBack = ::goBack,
                        onOpen = { navigateTo(Screen.Detail(it)) },
                        onWatch = { detail, episode ->
                            val continueEpisode = ContinueStoreInstance.entries.value
                                .firstOrNull { it.anime.id == detail.id }
                                ?.progress?.episode?.toDouble()
                            val audio = settings.audio.lowercase()
                            val wanted = episode ?: continueEpisode
                            val local = when {
                                wanted != null -> LocalPlayback.find(
                                    repository.localEpisodes.value,
                                    detail.id,
                                    detail.title,
                                    wanted,
                                    audio,
                                )
                                else -> LocalPlayback.find(
                                    repository.localEpisodes.value,
                                    detail.id,
                                    detail.title,
                                    1.0,
                                    audio,
                                )
                            }
                            if (local != null) {
                                navigateTo(localPlayerScreen(detail, local))
                            } else {
                                navigateTo(Screen.Watch(detail, wanted))
                            }
                        },
                        queueManager = downloadQueue,
                        settings = settings,
                    )
                    is Screen.Watch -> AnimeWatchScreen(
                        detail = target.detail,
                        repository = repository,
                        providers = animeVideoProviders,
                        initialEpisode = target.initialEpisode,
                        onBack = ::goBack,
                        onPlay = { title, url, headers, alternatives, episode, episodeTitle, episodes, episodeTitles, episodeThumbnails ->
                            navigateTo(
                                Screen.Player(
                                    title = title,
                                    url = url,
                                    headers = headers,
                                    alternatives = alternatives,
                                    audioTrack = alternatives.firstOrNull { it.url == url }?.audio ?: "Sub",
                                    detail = target.detail,
                                    episode = episode,
                                    episodeTitle = episodeTitle,
                                    episodes = episodes,
                                    episodeTitles = episodeTitles,
                                    episodeThumbnails = episodeThumbnails,
                                ),
                            )
                        },
                    )
                    is Screen.Player -> {
                        LaunchedEffect(target.detail?.id, target.episode) {
                            val anime = target.detail ?: return@LaunchedEffect
                            val episodeNumber = target.episode ?: return@LaunchedEffect
                            // downloaded/local playback uses a filesystem path, so there is nothing to resolve
                            if (target.url?.startsWith("http", ignoreCase = true) != true) return@LaunchedEffect
                            val key = "${anime.id}:$episodeNumber"
                            if (key in mergedAudioKeys.value) return@LaunchedEffect
                            val current = screen as? Screen.Player ?: return@LaunchedEffect
                            val currentAudio = current.alternatives.firstOrNull { it.url == current.url }?.audio ?: current.audioTrack
                            if (current.alternatives.any { it.audio.equals("Dub", true) } &&
                                current.alternatives.any { it.audio.equals("Sub", true) }
                            ) {
                                mergedAudioKeys.value = mergedAudioKeys.value + key
                                return@LaunchedEffect
                            }
                            val fetchDub = !currentAudio.equals("Dub", true)
                            mergedAudioKeys.value = mergedAudioKeys.value + key
                            screen = current.copy(audioResolving = true, audioError = null)
                            var failure = false
                            val others = try {
                                animeVideoProviders.sources(
                                    anilistId = anime.id,
                                    malId = anime.malId,
                                    titles = listOfNotNull(anime.title, anime.titleRomaji, anime.titleNative) + anime.synonyms,
                                    episode = episodeNumber,
                                    dub = fetchDub,
                                )
                            } catch (cancelled: CancellationException) {
                                mergedAudioKeys.value = mergedAudioKeys.value - key
                                throw cancelled
                            } catch (error: Throwable) {
                                failure = true
                                logger.warn { "other-audio lookup failed episode=$episodeNumber ${error.message}" }
                                emptyList()
                            }
                            val now = screen as? Screen.Player ?: return@LaunchedEffect
                            if (now.detail?.id != anime.id || now.episode != episodeNumber) return@LaunchedEffect
                            screen = now.copy(
                                alternatives = mergeVideoSources(now.alternatives + others),
                                audioResolving = false,
                                audioError = if (failure) "Couldn't check the other audio tracks." else null,
                            )
                        }
                        PlayerScreen(
                            title = target.title,
                            showTitle = target.detail?.title?.takeIf(String::isNotBlank) ?: target.title.substringBefore(" · Episode "),
                            url = target.url,
                            headers = target.headers,
                            alternatives = target.alternatives,
                            audioResolving = target.audioResolving,
                            audioError = target.audioError,
                            episode = target.episode,
                            episodeTitle = target.episodeTitle,
                            durationMinutes = target.detail?.duration,
                            episodes = target.episodes,
                            episodeTitles = target.episodeTitles,
                            episodeThumbnails = target.episodeThumbnails,
                            detail = target.detail,
                            settings = settings,
                            onSettingsChange = { updated -> updateSettings { updated } },
                            isFullscreen = playerFullscreen,
                            isPipMode = isPipMode,
                            onTogglePip = { isPipMode = !isPipMode },
                            onClosePip = { isPipMode = false },
                            onToggleFullscreen = { playerFullscreen = !playerFullscreen },
                            onSourceSelect = { source ->
                                val serverName = source.server.takeIf(String::isNotBlank) ?: source.provider
                                ActionBreadcrumbs.record("Clicked server: $serverName")
                                logger.info { "server selected provider=${source.provider} server=${source.server} quality=${source.quality} url=${source.url}" }
                                screen = target.copy(
                                    title = "${target.detail?.title ?: target.title.substringBefore(" · Episode ")} · Episode ${formatPlayerEpisode(target.episode ?: 1.0)} · ${source.provider} ${source.quality ?: source.format.uppercase()}",
                                    url = source.url,
                                    headers = source.headers,
                                    audioTrack = source.audio,
                                    audioResolving = false,
                                    audioError = null,
                                )
                            },
                            onSmartDownload = { nextEpisode ->
                                smartDownloadNext(target.detail, nextEpisode, target.audioTrack)
                            },
                            onSmartDelete = { oldEpisode -> deleteSmartDownload(target.detail, oldEpisode) },
                            onEpisodeChange = { episode ->
                                if (target.detail != null) scope.launch { resolvePlayerEpisode(target, episode, target.audioTrack) }
                                else screen = target.copy(episode = episode, episodeTitle = target.episodeTitles[episode])
                            },
                            onPresence = { presence ->
                                if (presence != null) {
                                    discordPresence.show(presence)
                                } else {
                                    // mpv reports an end-of-file and Compose disposes the old engine when
                                    // switching episodes. Don't flash idle unless we really left the player.
                                    if (screen !is Screen.Player) {
                                        discordPresence.clearPlayer()
                                    } else {
                                        scope.launch {
                                            delay(700)
                                            if (screen !is Screen.Player) discordPresence.clearPlayer()
                                        }
                                    }
                                }
                            },
                            onBack = ::goBack,
                        )
                    }
                }
                }
                }
            }
        }
    }
        if (showIntro) IntroOverlay(homeReady = homeLoaded, onFinished = { showIntro = false })
    }

    if (picking) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AniLifeColors.Scrim.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(AniLifeColors.Card)
                    .padding(horizontal = 22.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = AniLifeColors.Foreground,
                    strokeWidth = 2.dp,
                )
                Text("Rolling the dice…", color = AniLifeColors.Foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    if (paletteOpen) {
        CommandPalette(
            repository = repository,
            onOpenAnime = { navigateTo(Screen.Detail(it)) },
            onNavigate = ::navigateTo,
            onDismiss = { paletteOpen = false },
        )
    }

    pickError?.let { message ->
        AlertDialog(
            onDismissRequest = { pickError = null },
            modifier = Modifier.liquidGlass(shape = RoundedCornerShape(28.dp)).appEnter(),
            containerColor = AniLifeColors.Card,
            titleContentColor = AniLifeColors.Foreground,
            textContentColor = AniLifeColors.MutedForeground,
            title = { Text("Random pick failed", fontWeight = FontWeight.SemiBold) },
            text = { Text(message, fontSize = 14.sp, lineHeight = 21.sp) },
            confirmButton = {
                TextButton(onClick = { pickError = null }) {
                    Text("Close", color = AniLifeColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }

    availableUpdate?.let { update ->
        UpdateDialog(
            update = update,
            state = updateState,
            onUpdate = {
                shareNotice = false
                installUpdate(update)
            },
            onLater = {
                updater.clear()
                updateState = UpdateState.Idle
                shareNotice = false
            },
            onShareLogs = { shareUpdateLogs() },
            shareNotice = shareNotice,
        )
    }
}

private fun shortUpdateFailureMessage(failure: Exception): String = when (failure) {
    is UpdateHttpException -> "Update request failed (HTTP ${failure.status})."
    is SizeMismatchException -> "Downloaded file size didn't match. Retry or share logs."
    is ChecksumException -> "Checksum didn't match. Retry or share logs."
    is UpdateException -> failure.message?.takeIf(String::isNotBlank) ?: "Update failed."
    else -> "Update failed. Check your connection and retry."
}

private fun formatPlayerEpisode(number: Double): String =
    if (number % 1.0 == 0.0) number.toLong().toString() else number.toString()

private fun AnimeDetail.toCard() = AnimeCard(
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

    
