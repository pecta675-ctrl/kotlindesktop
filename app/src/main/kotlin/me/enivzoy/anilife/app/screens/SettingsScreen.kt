package me.enivzoy.anilife.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.enivzoy.anilife.app.data.AppSettings
import me.enivzoy.anilife.app.data.AniListRepository
import me.enivzoy.anilife.app.data.AppBackup
import me.enivzoy.anilife.app.data.AppSettingsStore
import me.enivzoy.anilife.app.data.UpdateChannel
import me.enivzoy.anilife.app.data.AutoDetectCandidate
import me.enivzoy.anilife.app.data.AutoDetectScan
import me.enivzoy.anilife.app.data.AutoDetectService
import me.enivzoy.anilife.app.data.BackupDocument
import me.enivzoy.anilife.app.data.DataTransfer
import me.enivzoy.anilife.app.data.DirectMediaDownloader
import me.enivzoy.anilife.app.data.DiscordOAuth
import me.enivzoy.anilife.app.data.DiscordOAuthState
import me.enivzoy.anilife.app.data.DiscordOAuthStatus
import me.enivzoy.anilife.app.data.DiscordPresenceState
import me.enivzoy.anilife.app.data.DiscordPresenceStatus
import me.enivzoy.anilife.app.data.DefaultPlayerKeyBindings
import me.enivzoy.anilife.app.data.PlayerAction
import me.enivzoy.anilife.app.data.playerKeyLabel
import me.enivzoy.anilife.app.data.rebindPlayerKey
import me.enivzoy.anilife.app.data.ANILIFE_ICON_URL
import me.enivzoy.anilife.app.ui.AniLifeColors
import me.enivzoy.anilife.app.ui.DiscordProfilePreview
import me.enivzoy.anilife.app.ui.appEnter
import me.enivzoy.anilife.app.ui.adaptiveGlass
import me.enivzoy.anilife.app.ui.PageScrollbar
import me.enivzoy.anilife.app.ui.ThemeEditor
import me.enivzoy.anilife.app.ui.glassSurface
import me.enivzoy.anilife.app.ui.theme.LocalAppearance
import me.enivzoy.anilife.app.ui.theme.ScrollAnimationDuration
import me.enivzoy.anilife.app.ui.theme.ScrollbarVisibility
import me.enivzoy.anilife.app.ui.theme.ThemeMode
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Settings, laid out as a sidebar of pages and a scrolling panel of rows. Every control maps to a
 * real [AppSettings] field; rows for engines that are not ported yet stay out of the list.
 */
@Composable
internal fun SettingsScreen(
    settings: AppSettings,
    repository: AniListRepository,
    discordState: DiscordPresenceState,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    onDiscordConnect: () -> Unit,
    currentVersion: String = "unknown",
    updateCheckStatus: String? = null,
    checkingForUpdates: Boolean = false,
    onCheckForUpdates: () -> Unit = {},
    autoDetect: AutoDetectService? = null,
    autoDetectScan: AutoDetectScan? = null,
    autoDetectRunning: Boolean = false,
    autoDetectStatus: String? = null,
    modifier: Modifier = Modifier,
) {
    var page by remember { mutableStateOf(SettingsPage.Player) }
    var query by remember { mutableStateOf("") }
    var listeningForAction by remember { mutableStateOf<PlayerAction?>(null) }
    var shortcutMessage by remember { mutableStateOf<String?>(null) }

    val results = remember(query) {
        val needle = query.trim()
        if (needle.isEmpty()) emptyList()
        else settingSearchEntries.filter { entry ->
            "${entry.label} ${entry.page.title} ${entry.keywords}".contains(needle, ignoreCase = true)
        }
    }

    Box(
        Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            val target = listeningForAction ?: return@onPreviewKeyEvent false
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent true
            if (event.key == Key.Escape) {
                listeningForAction = null
                shortcutMessage = null
                return@onPreviewKeyEvent true
            }
            val rebind = rebindPlayerKey(settings.keyBindings, target, event.key)
            onSettingsChange { current -> current.copy(keyBindings = rebind.bindings) }
            shortcutMessage = buildString {
                append("Saved ${playerKeyLabel(event.key)} for ${target.label}")
                rebind.conflictWith?.let { append("; swapped with ${it.label}") }
            }
            listeningForAction = null
            true
        }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val settingsSidebarWidth = (maxWidth * 0.32f).coerceIn(180.dp, 300.dp)
            Row(modifier.fillMaxSize().background(AniLifeColors.SettingsBackground)) {
                SettingsSidebar(
                    page = page,
                    query = query,
                    width = settingsSidebarWidth,
                    onQueryChange = { query = it },
                    onSelect = {
                        page = it
                        query = ""
                    },
                )
                Box(Modifier.width(1.dp).fillMaxHeight().background(AniLifeColors.SettingsDivider))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    SettingsHeader(
                        title = if (query.isBlank()) page.title else "Search results",
                        subtitle = if (query.isBlank()) {
                            page.subtitle
                        } else {
                            "${results.size} setting${if (results.size == 1) "" else "s"} matching “${query.trim()}”"
                        },
                    )
                    val settingsScrollState = rememberScrollState()
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxSize()
                                .padding(start = 4.dp, end = 8.dp)
                                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                                .background(AniLifeColors.SettingsPanel)
                                .adaptiveGlass(shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                                .border(1.dp, AniLifeColors.SettingsBorder, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                                .verticalScroll(settingsScrollState)
                                .padding(horizontal = 26.dp, vertical = 16.dp),
                        ) {
                            if (query.isNotBlank()) {
                                SearchResults(results) {
                                    page = it
                                    query = ""
                                }
                            } else {
                                when (page) {
                                    SettingsPage.Player -> PlayerSettings(
                                        settings = settings,
                                        onSettingsChange = onSettingsChange,
                                        listeningForAction = listeningForAction,
                                        onListen = { listeningForAction = it },
                                        shortcutMessage = shortcutMessage,
                                    )
                                    SettingsPage.Subtitles -> SubtitleSettings(settings, onSettingsChange)
                                    SettingsPage.Downloads -> DownloadSettings(
                                        settings = settings,
                                        onSettingsChange = onSettingsChange,
                                        autoDetect = autoDetect,
                                        autoDetectScan = autoDetectScan,
                                        autoDetectRunning = autoDetectRunning,
                                        autoDetectStatus = autoDetectStatus,
                                    )
                                    SettingsPage.Servers -> ServerSettings()
                                    SettingsPage.Accounts -> AccountSettings(
                                        settings = settings,
                                        discordState = discordState,
                                        onSettingsChange = onSettingsChange,
                                        onDiscordConnect = onDiscordConnect,
                                    )
                                    SettingsPage.Notifications -> NotificationSettings()
                                    SettingsPage.Content -> ContentSettings(settings, onSettingsChange)
                                    SettingsPage.Appearance -> AppearanceSettings(settings, onSettingsChange)
                                    SettingsPage.Data -> DataSettings(settings, onSettingsChange, repository)
                                    SettingsPage.App -> AppSettingsPage(
                                        settings = settings,
                                        onSettingsChange = onSettingsChange,
                                        currentVersion = currentVersion,
                                        updateCheckStatus = updateCheckStatus,
                                        checkingForUpdates = checkingForUpdates,
                                        onCheckForUpdates = onCheckForUpdates,
                                    )
                                    SettingsPage.About -> AboutSettings(currentVersion, settings)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        PageScrollbar(
                            rememberScrollbarAdapter(settingsScrollState),
                            Modifier.align(Alignment.CenterEnd),
                        )
                    }
                }
            }
        }
        listeningForAction?.let { target ->
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AniLifeColors.Card)
                    .border(1.dp, AniLifeColors.Border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    "Press any key to bind to '${target.label}'... (Press ESC to cancel)",
                    color = AniLifeColors.Foreground,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

private enum class SettingsPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val keywords: String,
) {
    Player("Player", "Playback, skipping, and how the player looks", Icons.Rounded.PlayCircle, "audio dubbed sub resume autoplay skip volume picture gpu hardware decoding performance"),
    Subtitles("Subtitles", "Captions, language, and text size", Icons.Rounded.Subtitles, "captions subtitles language text size"),
    Downloads("Downloads", "Where episodes are saved", Icons.Rounded.Download, "folder directory storage path"),
    Servers("Servers", "The video providers AniLife plays through", Icons.Rounded.Dns, "providers streaming wasm servers extensions"),
    Accounts("Accounts & sync", "Discord presence and list sync", Icons.Rounded.Sync, "discord rich presence myanimelist mal account"),
    Notifications("Notifications", "Release and episode alerts", Icons.Rounded.Notifications, "alerts reminders episodes releases"),
    Content("Content", "Titles and what the app opens with", Icons.Rounded.VideoLibrary, "title language romaji english native"),
    Appearance("Appearance", "Theme and type", Icons.Rounded.Palette, "css theme colors palette font typeface editor"),
    Data("Data", "Backups, MyAnimeList files, and storage", Icons.Rounded.Storage, "buffer memory cache storage backup restore export import xml json"),
    App("App", "Updates and startup", Icons.Rounded.Apps, "updates version release startup intro splash"),
    About("About", "Version, keyboard shortcuts, and the people behind it", Icons.Rounded.Info, "app version metadata player providers credits links website discord shortcuts repository"),
}

private data class SettingSearchEntry(
    val label: String,
    val page: SettingsPage,
    val keywords: String,
)

private val settingSearchEntries = listOf(
    SettingSearchEntry("Playback version", SettingsPage.Player, "audio sub dubbed language track"),
    SettingSearchEntry("Resume where you left off", SettingsPage.Player, "continue progress resume"),
    SettingSearchEntry("Autoplay next episode", SettingsPage.Player, "next episode countdown autoplay"),
    SettingSearchEntry("Skip button", SettingsPage.Player, "opening ending intro outro forward"),
    SettingSearchEntry("Skip button length", SettingsPage.Player, "seconds duration skip"),
    SettingSearchEntry("Skip intro and recap automatically", SettingsPage.Player, "intro outro recap opening skip auto"),
    SettingSearchEntry("Skip outro automatically", SettingsPage.Player, "intro outro recap ending skip auto"),
    SettingSearchEntry("Screen size", SettingsPage.Player, "fit crop fill scaling display"),
    SettingSearchEntry("Player volume", SettingsPage.Player, "audio sound volume"),
SettingSearchEntry("Keyboard shortcuts", SettingsPage.Player, "keys binding rebind shortcuts pause seek volume fullscreen"),
    SettingSearchEntry("GPU acceleration", SettingsPage.Player, "hardware decoding gpu graphics card cpu decoder performance"),
    SettingSearchEntry("Show captions by default", SettingsPage.Subtitles, "captions subtitles text default"),
    SettingSearchEntry("Preferred caption language", SettingsPage.Subtitles, "captions subtitles language"),
    SettingSearchEntry("Caption size", SettingsPage.Subtitles, "captions subtitles text scale size"),
    SettingSearchEntry("Download folder", SettingsPage.Downloads, "directory location path storage"),
    SettingSearchEntry("Auto-detect downloads", SettingsPage.Downloads, "watch folder monitor auto import scan detect library"),
    SettingSearchEntry("Watched folders", SettingsPage.Downloads, "auto detect watch directory downloads inbox"),
    SettingSearchEntry("Wait before importing", SettingsPage.Downloads, "settle delay finish complete quiet auto detect"),
    SettingSearchEntry("Video providers", SettingsPage.Servers, "providers servers streaming wasm extensions"),
    SettingSearchEntry("Discord Rich Presence", SettingsPage.Accounts, "discord activity status watching idle connect"),
    SettingSearchEntry("Discord Application ID", SettingsPage.Accounts, "discord developer application id"),
    SettingSearchEntry("Title display language", SettingsPage.Content, "anime name title romaji english native"),
    SettingSearchEntry("High Refresh Rate UI", SettingsPage.Appearance, "120fps animation frame rate smooth refresh"),
    SettingSearchEntry("Appearance theme", SettingsPage.Appearance, "default frosted glass liquid glass theme mode look preview"),
    SettingSearchEntry("Smooth scrolling", SettingsPage.Appearance, "scroll animation programmatic jump fling"),
    SettingSearchEntry("Scrollbar visibility", SettingsPage.Appearance, "scrollbar automatic always hidden rounded thumb"),
    SettingSearchEntry("Scroll animation duration", SettingsPage.Appearance, "scroll speed short normal long"),
    SettingSearchEntry("Reduced motion", SettingsPage.Appearance, "accessibility animation off no motion"),
    SettingSearchEntry("Glass customization", SettingsPage.Appearance, "blur transparency highlight edge reflections"),
    SettingSearchEntry("App font", SettingsPage.Appearance, "font typeface text sans serif mono"),
    SettingSearchEntry("Theme colors", SettingsPage.Appearance, "css theme colors palette editor custom look style"),
    SettingSearchEntry("Playback buffer", SettingsPage.Data, "stream buffering memory"),
    SettingSearchEntry("Back up to a file", SettingsPage.Data, "backup save export library settings json"),
    SettingSearchEntry("Restore from a backup", SettingsPage.Data, "restore import backup android json"),
    SettingSearchEntry("Export MyAnimeList XML", SettingsPage.Data, "mal export xml list"),
    SettingSearchEntry("Import MyAnimeList XML", SettingsPage.Data, "mal import xml list"),
    SettingSearchEntry("Play startup intro", SettingsPage.App, "animation splash opening startup"),
    SettingSearchEntry("Update channel", SettingsPage.App, "release testing pre-release hotfix updates"),
    SettingSearchEntry("Check for updates on launch", SettingsPage.App, "automatic updates release startup"),
    SettingSearchEntry("Check for updates now", SettingsPage.App, "update version release check"),
    SettingSearchEntry("About AniLife", SettingsPage.About, "app version metadata player credits links website discord shortcuts repository"),
)

@Composable
private fun SettingsSidebar(
    page: SettingsPage,
    query: String,
    width: Dp,
    onQueryChange: (String) -> Unit,
    onSelect: (SettingsPage) -> Unit,
) {
    val pageScrollState = rememberScrollState()
    Column(
        Modifier.width(width)
            .fillMaxHeight()
            .background(AniLifeColors.SettingsBackground)
            .padding(top = 18.dp),
    ) {
        Text(
            "Settings",
            color = AniLifeColors.Foreground,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(12.dp))
        SidebarSearch(query = query, onQueryChange = onQueryChange)
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.weight(1f)
                .fillMaxWidth()
                .verticalScroll(pageScrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            SettingsPage.entries.forEach { entry ->
                val selected = entry == page && query.isBlank()
                Row(
                    Modifier.fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) AniLifeColors.SettingsSelected else Color.Transparent)
                        .clickable { onSelect(entry) }
                        .padding(start = 17.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        entry.icon,
                        contentDescription = null,
                        tint = if (selected) AniLifeColors.SettingsSelectedForeground else AniLifeColors.MutedForeground,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(13.dp))
                    Text(
                        entry.title,
                        color = if (selected) AniLifeColors.SettingsSelectedForeground else AniLifeColors.MutedForeground,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarSearch(query: String, onQueryChange: (String) -> Unit) {
    Row(
        Modifier.padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AniLifeColors.SettingsControl)
            .border(1.dp, AniLifeColors.SettingsControlBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = "Search settings",
            tint = AniLifeColors.MutedForeground,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = AniLifeColors.Foreground, fontSize = 13.sp),
            cursorBrush = SolidColor(AniLifeColors.Foreground),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (query.isBlank()) {
                        Text("Search settings", color = AniLifeColors.MutedForeground, fontSize = 13.sp)
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun SettingsHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(start = 26.dp, top = 20.dp, bottom = 14.dp)) {
        Text(title, color = AniLifeColors.Foreground, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = AniLifeColors.MutedForeground, fontSize = 13.sp)
    }
}

@Composable
private fun SearchResults(results: List<SettingSearchEntry>, onOpen: (SettingsPage) -> Unit) {
    if (results.isEmpty()) {
        Text(
            "No matching settings. Try captions, skipping, volume, Discord, or theme.",
            color = AniLifeColors.MutedForeground,
            fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        return
    }
    results.forEach { result ->
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onOpen(result.page) }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(result.label, color = AniLifeColors.Foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(result.page.title, color = AniLifeColors.MutedForeground, fontSize = 12.sp)
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = AniLifeColors.MutedForeground,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PlayerSettings(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    listeningForAction: PlayerAction?,
    onListen: (PlayerAction?) -> Unit,
    shortcutMessage: String?,
) {
    SettingsRow(
        "Playback",
        "Which version to start with. You can switch per episode.",
    ) {
        SettingsSegmented(
            choices = listOf("sub" to "Subtitled", "dub" to "Dubbed"),
            selected = settings.audio,
            onSelect = { value -> onSettingsChange { it.copy(audio = value) } },
        )
    }
    SettingsRow(
        "Resume where you left off",
        "Start unfinished episodes from the saved position.",
    ) {
        SettingsSwitch(settings.resumePlayback) { enabled -> onSettingsChange { it.copy(resumePlayback = enabled) } }
    }
    SettingsRow(
        "Autoplay next episode",
        "Offer the next episode in the last half-minute, and play it after a short countdown.",
    ) {
        SettingsSwitch(settings.autoplayNextEpisode) { enabled -> onSettingsChange { it.copy(autoplayNextEpisode = enabled) } }
    }
    SettingsRow(
        "Submit AniList progress",
        "Send an episode to AniList as watched once this share of it has been watched.",
    ) {
        SettingsStepper(
            value = settings.aniListSubmitPercent,
            valueLabel = "${settings.aniListSubmitPercent}%",
            range = 10..100,
            step = 5,
            onChange = { value -> onSettingsChange { it.copy(aniListSubmitPercent = value) } },
        )
    }
    SettingsSection("Skipping") {
        SettingsRow(
            "Skip button",
            "A one-press jump in the player when a show has no skip timings.",
        ) {
            SettingsSwitch(settings.showSkipButton) { enabled -> onSettingsChange { it.copy(showSkipButton = enabled) } }
        }
        if (settings.showSkipButton) {
            SettingsRow(
                "Skip button length",
                "How far one press jumps. A TV anime opening is 85 seconds.",
            ) {
                SettingsStepper(
                    value = settings.skipButtonSeconds,
                    valueLabel = "${settings.skipButtonSeconds} seconds",
                    range = 10..180,
                    step = 5,
                    onChange = { value -> onSettingsChange { it.copy(skipButtonSeconds = value) } },
                )
            }
        }
        SettingsRow(
            "Skip intro and recap automatically",
            "Jump over the recap and opening once they start, after a short countdown.",
        ) {
            SettingsSwitch(settings.autoSkipIntro) { enabled -> onSettingsChange { it.copy(autoSkipIntro = enabled) } }
        }
        SettingsRow(
            "Skip outro automatically",
            "Jump to the end of the episode when the credits reach the last quarter.",
        ) {
            SettingsSwitch(settings.autoSkipOutro) { enabled -> onSettingsChange { it.copy(autoSkipOutro = enabled) } }
        }
        SettingsRow(
            "Anime details when paused",
            "Show the series, the episode and its synopsis over a paused picture.",
        ) {
            SettingsSwitch(settings.pauseDetails) { enabled -> onSettingsChange { it.copy(pauseDetails = enabled) } }
        }
    }
    SettingsSection("Picture") {
        SettingsRow(
            "Screen size",
            "Fit shows the whole picture; Crop fills the window and cuts the edges; Fill stretches it.",
        ) {
            SettingsSegmented(
                choices = listOf("fit" to "Fit", "crop" to "Crop", "fill" to "Fill"),
                selected = settings.contentScale,
                onSelect = { value -> onSettingsChange { it.copy(contentScale = value) } },
            )
        }
        SettingsRow(
            "Screenshot resolution",
            "Screenshots are saved to Pictures/AniLife. 4K and 8K upscale the frame with Real-ESRGAN and take a few seconds.",
        ) {
            SettingsSegmented(
                choices = listOf("native" to "Native", "4k" to "4K", "8k" to "8K"),
                selected = settings.screenshotResolution,
                onSelect = { value -> onSettingsChange { it.copy(screenshotResolution = value) } },
            )
        }
        SettingsRow(
            "Volume",
            "Default player volume.",
        ) {
            SettingsSliderRow(
                value = settings.volume.toFloat(),
                valueRange = 0f..100f,
                valueLabel = "${settings.volume}%",
                onChange = { value -> onSettingsChange { it.copy(volume = value.toInt()) } },
            )
        }
    }
KeyboardShortcutsSection(
        bindings = settings.keyBindings,
        listeningForAction = listeningForAction,
        onListen = onListen,
        message = shortcutMessage,
        onBindingsChange = { bindings -> onSettingsChange { it.copy(keyBindings = bindings) } },
    )
    SettingsSection("Performance") {
        SettingsRow(
            "GPU acceleration",
            "Decode video on your graphics card to cut CPU use. Takes effect the next time a video starts. Turn it off if you see glitches or green frames.",
        ) {
            SettingsSegmented(
                choices = listOf("auto" to "Auto", "off" to "Off"),
                selected = settings.gpuAcceleration,
                onSelect = { value -> onSettingsChange { it.copy(gpuAcceleration = value) } },
            )
        }
    }
}

@Composable
private fun KeyboardShortcutsSection(
    bindings: Map<PlayerAction, Key>,
    listeningForAction: PlayerAction?,
    onListen: (PlayerAction?) -> Unit,
    message: String?,
    onBindingsChange: (Map<PlayerAction, Key>) -> Unit,
) {
    SettingsSection("Keyboard Shortcuts") {
        SettingsNote("Pick an action and press a key. A key already in use swaps its binding.")
        val shape = RoundedCornerShape(12.dp)
        val shortcutListState = rememberLazyListState()
        Box(
            Modifier.fillMaxWidth()
                .height(324.dp)
                .clip(shape)
                .background(AniLifeColors.SettingsControl)
                .border(1.dp, AniLifeColors.SettingsControlBorder, shape)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            LazyColumn(Modifier.fillMaxSize(), state = shortcutListState) {
                PlayerAction.entries.forEach { action ->
                    item(key = action.name) {
                        ShortcutBindingRow(
                            action = action,
                            key = bindings[action] ?: DefaultPlayerKeyBindings.getValue(action),
                            listening = listeningForAction == action,
                            onListen = onListen,
                        )
                    }
                }
            }
            PageScrollbar(shortcutListState, Modifier.align(Alignment.CenterEnd))
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsButton("Restore Default Shortcuts") { onBindingsChange(DefaultPlayerKeyBindings) }
            if (!message.isNullOrBlank()) {
                Text(message, color = AniLifeColors.MutedForeground, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ShortcutBindingRow(
    action: PlayerAction,
    key: Key,
    listening: Boolean,
    onListen: (PlayerAction?) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            action.label,
            Modifier.weight(1f).padding(end = 12.dp),
            color = AniLifeColors.Foreground,
            fontSize = 13.sp,
        )
        ShortcutBadge(if (listening) "..." else playerKeyLabel(key))
        Spacer(Modifier.width(10.dp))
        SettingsButton(if (listening) "Listening..." else "Change") {
            onListen(if (listening) null else action)
        }
    }
}

@Composable
private fun ShortcutBadge(label: String) {
    val shape = RoundedCornerShape(7.dp)
    Text(
        label,
        Modifier.clip(shape)
            .background(AniLifeColors.Background)
            .border(1.dp, AniLifeColors.Border, shape)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        color = AniLifeColors.Foreground,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
private fun SubtitleSettings(settings: AppSettings, onSettingsChange: ((AppSettings) -> AppSettings) -> Unit) {
    var fontError by remember { mutableStateOf<String?>(null) }

    fun updateCaption(transform: (AppSettings) -> AppSettings) {
        onSettingsChange { transform(it).copy(captionPreset = "custom") }
    }

    fun importCustomFont() {
        val chooser = JFileChooser().apply {
            dialogTitle = "Import subtitle font"
            isAcceptAllFileFilterUsed = false
            fileFilter = FileNameExtensionFilter("Font files (*.ttf, *.otf)", "ttf", "otf")
        }
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return

        try {
            val source = chooser.selectedFile
            require(source.length() <= 20L * 1024L * 1024L) { "Font files must be 20 MB or smaller." }
            val family = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, source).family
            val directory = captionFontsDirectory()
            if (!directory.exists() && !directory.mkdirs()) {
                throw IllegalStateException("Couldn't create the custom fonts folder.")
            }
            val imported = File(directory, source.name)
            if (source.canonicalFile != imported.canonicalFile) {
                Files.copy(source.toPath(), imported.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            updateCaption { it.copy(captionFont = family, captionFontFile = imported.absolutePath) }
            fontError = null
        } catch (_: Exception) {
            fontError = "Couldn't import that font. Choose a valid TTF or OTF file."
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Caption appearance", color = AniLifeColors.Foreground, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "For plain subtitles. Styled ASS subtitles keep their own look.",
            color = AniLifeColors.MutedForeground,
            fontSize = 12.sp,
        )
        SubtitlePreview(settings)
        SettingsSegmented(
            choices = listOf(
                "classic" to "Classic",
                "boxed" to "Boxed",
                "cinema" to "Cinema",
                "high-contrast" to "High contrast",
            ),
            selected = settings.captionPreset.takeIf { it != "custom" },
            onSelect = { preset -> onSettingsChange { applyCaptionPreset(it, preset) } },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CaptionControlHeader("Font", settings.captionFont)
                captionFontOptions.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (label, family) ->
                            CaptionOptionButton(
                                modifier = Modifier.weight(1f),
                                label = label,
                                selected = settings.captionFont.equals(family, ignoreCase = true),
                                onClick = {
                                    updateCaption { it.copy(captionFont = family, captionFontFile = "") }
                                },
                            )
                        }
                    }
                }
                SettingsButton("Import custom font", onClick = ::importCustomFont)
                fontError?.let { Text(it, color = AniLifeColors.Destructive, fontSize = 11.sp) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CaptionControlHeader("Size", "${(settings.subtitleScale * 100).toInt()}%")
                CaptionSlider(
                    value = settings.subtitleScale,
                    range = 0.5f..2f,
                    valueLabel = "${(settings.subtitleScale * 100).toInt()}%",
                    onChange = { value -> updateCaption { it.copy(subtitleScale = value) } },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsButton("A−") {
                        updateCaption { it.copy(subtitleScale = (it.subtitleScale - 0.1f).coerceAtLeast(0.5f)) }
                    }
                    SettingsButton("A+") {
                        updateCaption { it.copy(subtitleScale = (it.subtitleScale + 0.1f).coerceAtMost(2f)) }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CaptionControlHeader("Text colour", "${(settings.captionTextOpacity * 100).toInt()}% opacity")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    captionTextColors.forEach { color ->
                        CaptionColorSwatch(
                            color = color,
                            selected = settings.captionTextColor.equals(color, ignoreCase = true),
                            description = "$color subtitle text",
                            onClick = { updateCaption { it.copy(captionTextColor = color) } },
                        )
                    }
                }
                CaptionSlider(
                    value = settings.captionTextOpacity,
                    range = 0f..1f,
                    valueLabel = "${(settings.captionTextOpacity * 100).toInt()}%",
                    onChange = { value -> updateCaption { it.copy(captionTextOpacity = value) } },
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CaptionControlHeader(
                    "Background",
                    if (settings.captionBackgroundOpacity <= 0f) "None"
                    else "${(settings.captionBackgroundOpacity * 100).toInt()}% opacity",
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsButton("None") {
                        updateCaption { it.copy(captionBackgroundOpacity = 0f) }
                    }
                    captionBackgroundColors.forEach { color ->
                        CaptionColorSwatch(
                            color = color,
                            selected = settings.captionBackgroundOpacity > 0f &&
                                settings.captionBackgroundColor.equals(color, ignoreCase = true),
                            description = "$color subtitle background",
                            onClick = {
                                updateCaption {
                                    it.copy(
                                        captionBackgroundColor = color,
                                        captionBackgroundOpacity = it.captionBackgroundOpacity.takeIf { alpha -> alpha > 0f } ?: 0.55f,
                                    )
                                }
                            },
                        )
                    }
                }
                CaptionSlider(
                    value = settings.captionBackgroundOpacity,
                    range = 0f..1f,
                    valueLabel = "${(settings.captionBackgroundOpacity * 100).toInt()}%",
                    onChange = { value -> updateCaption { it.copy(captionBackgroundOpacity = value) } },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CaptionControlHeader("Edge", settings.captionEdgeStyle.replaceFirstChar(Char::uppercase))
                SettingsSegmented(
                    choices = listOf("none" to "None", "outline" to "Outline", "shadow" to "Shadow"),
                    selected = settings.captionEdgeStyle,
                    onSelect = { value -> updateCaption { it.copy(captionEdgeStyle = value) } },
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CaptionControlHeader(
                    "Position",
                    "${settings.captionVerticalOffset.toInt()}% from the ${settings.captionVerticalPosition}",
                )
                SettingsSegmented(
                    choices = listOf("bottom" to "Bottom", "top" to "Top"),
                    selected = settings.captionVerticalPosition,
                    onSelect = { value -> updateCaption { it.copy(captionVerticalPosition = value) } },
                )
                CaptionSlider(
                    value = settings.captionVerticalOffset,
                    range = 0f..30f,
                    valueLabel = "${settings.captionVerticalOffset.toInt()}%",
                    onChange = { value -> updateCaption { it.copy(captionVerticalOffset = value) } },
                )
                SettingsSegmented(
                    choices = listOf("left" to "←", "center" to "Center", "right" to "→"),
                    selected = settings.captionAlignment,
                    onSelect = { value -> updateCaption { it.copy(captionAlignment = value) } },
                )
            }
        }

        SettingsSection("Playback") {
            SettingsRow(
                "Show captions by default",
                "Start playback with text captions enabled.",
            ) {
                SettingsSwitch(settings.subtitlesByDefault) {
                    enabled -> onSettingsChange { it.copy(subtitlesByDefault = enabled) }
                }
            }
            SettingsRow(
                "Preferred caption language",
                "Ask the player to choose this language when a matching track is available.",
            ) {
                SettingsDropdown(
                    choices = listOf(
                        "en" to "English",
                        "ja" to "Japanese",
                        "es" to "Spanish",
                        "pt" to "Portuguese",
                        "fr" to "French",
                        "de" to "German",
                        "it" to "Italian",
                        "ru" to "Russian",
                        "ar" to "Arabic",
                        "zh" to "Chinese",
                        "none" to "No preference",
                    ),
                    selected = settings.subtitleLanguage,
                    onSelect = { value -> onSettingsChange { it.copy(subtitleLanguage = value) } },
                )
            }
        }
    }
}

private val captionFontOptions = listOf(
    "Sans" to "sans-serif",
    "Serif" to "serif",
    "Mono" to "monospace",
    "Narrow" to "Arial Narrow",
    "Casual" to "Comic Sans MS",
    "Script" to "cursive",
)

private val captionTextColors = listOf("#FFFFFF", "#FFEB3B", "#4DD0E1", "#81C784", "#FF80AB")
private val captionBackgroundColors = listOf("#000000", "#424242", "#0B2848", "#FFFFFF")

private fun captionFontsDirectory(): File {
    val base = System.getenv("LOCALAPPDATA")?.takeIf(String::isNotBlank)?.let(::File)
        ?: File(System.getProperty("user.home"), ".local/share")
    return File(base, "AniLife/fonts")
}

private fun applyCaptionPreset(settings: AppSettings, preset: String): AppSettings = when (preset) {
    "classic" -> settings.copy(
        captionPreset = preset,
        captionFont = "sans-serif",
        captionFontFile = "",
        subtitleScale = 1f,
        captionTextColor = "#FFFFFF",
        captionTextOpacity = 1f,
        captionBackgroundColor = "#000000",
        captionBackgroundOpacity = 0f,
        captionEdgeStyle = "outline",
        captionVerticalPosition = "bottom",
        captionVerticalOffset = 3f,
        captionAlignment = "center",
    )
    "boxed" -> settings.copy(
        captionPreset = preset,
        captionFont = "sans-serif",
        captionFontFile = "",
        subtitleScale = 1f,
        captionTextColor = "#FFFFFF",
        captionTextOpacity = 1f,
        captionBackgroundColor = "#000000",
        captionBackgroundOpacity = 0.65f,
        captionEdgeStyle = "none",
        captionVerticalPosition = "bottom",
        captionVerticalOffset = 3f,
        captionAlignment = "center",
    )
    "cinema" -> settings.copy(
        captionPreset = preset,
        captionFont = "serif",
        captionFontFile = "",
        subtitleScale = 1f,
        captionTextColor = "#FFF4DC",
        captionTextOpacity = 1f,
        captionBackgroundColor = "#000000",
        captionBackgroundOpacity = 0f,
        captionEdgeStyle = "shadow",
        captionVerticalPosition = "bottom",
        captionVerticalOffset = 8f,
        captionAlignment = "center",
    )
    "high-contrast" -> settings.copy(
        captionPreset = preset,
        captionFont = "sans-serif",
        captionFontFile = "",
        subtitleScale = 1.1f,
        captionTextColor = "#FFFF00",
        captionTextOpacity = 1f,
        captionBackgroundColor = "#000000",
        captionBackgroundOpacity = 0.9f,
        captionEdgeStyle = "none",
        captionVerticalPosition = "bottom",
        captionVerticalOffset = 4f,
        captionAlignment = "center",
    )
    else -> settings.copy(captionPreset = "custom")
}

@Composable
private fun CaptionControlHeader(title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = AniLifeColors.Foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = AniLifeColors.MutedForeground, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun CaptionOptionButton(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier.clip(shape)
            .background(if (selected) AniLifeColors.Primary else Color.Transparent)
            .border(1.dp, if (selected) AniLifeColors.Primary else AniLifeColors.Border, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) AniLifeColors.PrimaryForeground else AniLifeColors.Foreground,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun CaptionColorSwatch(
    color: String,
    selected: Boolean,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(28.dp)
            .clip(CircleShape)
            .background(captionColor(color, 1f))
            .border(if (selected) 2.dp else 1.dp, if (selected) AniLifeColors.Primary else AniLifeColors.Border, CircleShape)
            .semantics { contentDescription = description }
            .selectable(selected, role = Role.RadioButton, onClick = onClick),
    )
}

@Composable
private fun CaptionSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = value,
            onValueChange = { onChange(it.coerceIn(range)) },
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = AniLifeColors.Primary,
                activeTrackColor = AniLifeColors.Primary,
                inactiveTrackColor = AniLifeColors.Muted,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Text(valueLabel, color = AniLifeColors.MutedForeground, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SubtitlePreview(settings: AppSettings) {
    val style = settings.subtitleAppearance
    val fontFamily = remember(style.fontFamily, style.fontFile) {
        captionPreviewFontFamily(style.fontFamily, style.fontFile)
    }
    val contentAlignment = when (style.verticalPosition) {
        "top" -> when (style.alignment) {
            "left" -> Alignment.TopStart
            "right" -> Alignment.TopEnd
            else -> Alignment.TopCenter
        }
        else -> when (style.alignment) {
            "left" -> Alignment.BottomStart
            "right" -> Alignment.BottomEnd
            else -> Alignment.BottomCenter
        }
    }
    val textAlignment = when (style.alignment) {
        "left" -> TextAlign.Start
        "right" -> TextAlign.End
        else -> TextAlign.Center
    }
    val sample = "This is how subtitles will look,\nover whatever is playing."
    val shadow = when (style.edgeStyle) {
        "none" -> null
        "shadow" -> Shadow(Color.Black.copy(alpha = 0.9f), Offset(2f, 2f), 4f)
        else -> Shadow(Color.Black.copy(alpha = 0.95f), Offset(0f, 1f), 2f)
    }
    val outerShape = RoundedCornerShape(14.dp)

    BoxWithConstraints(
        Modifier.fillMaxWidth()
            .aspectRatio(16f / 7f)
            .clip(outerShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF12203A), Color(0xFF60445F), Color(0xFF162B39)),
                ),
            ),
    ) {
        val topInset = if (style.verticalPosition == "top") maxHeight * style.verticalOffset / 100f else 0.dp
        val bottomInset = if (style.verticalPosition == "bottom") maxHeight * style.verticalOffset / 100f else 0.dp
        Box(
            Modifier.fillMaxSize().padding(
                start = 18.dp,
                end = 18.dp,
                top = topInset,
                bottom = bottomInset,
            ),
            contentAlignment = contentAlignment,
        ) {
            Text(
                sample,
                Modifier.widthIn(max = 480.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(captionColor(style.backgroundColor, style.backgroundOpacity))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                style = TextStyle(
                    color = captionColor(style.textColor, style.textOpacity),
                    fontFamily = fontFamily,
                    fontSize = (18f * style.scale).sp,
                    textAlign = textAlignment,
                    shadow = shadow,
                ),
                lineHeight = (22f * style.scale).sp,
            )
        }
        Text(
            "SUBTITLE PREVIEW",
            Modifier.align(Alignment.TopStart).padding(10.dp),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun captionPreviewFontFamily(fontFamily: String, fontFile: String): FontFamily {
    if (fontFile.isNotBlank()) {
        val file = File(fontFile)
        if (file.isFile) runCatching { FontFamily(Font(file)) }.getOrNull()?.let { return it }
    }
    return when (fontFamily.lowercase()) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }
}

private fun captionColor(hex: String, opacity: Float): Color {
    val rgb = hex.removePrefix("#").takeLast(6).toLongOrNull(16) ?: 0xFFFFFFL
    return Color(0xFF000000L or rgb).copy(alpha = opacity.coerceIn(0f, 1f))
}

@Composable
private fun DownloadSettings(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    autoDetect: AutoDetectService?,
    autoDetectScan: AutoDetectScan?,
    autoDetectRunning: Boolean,
    autoDetectStatus: String?,
) {
    val defaultDirectory = DirectMediaDownloader.defaultDownloadDirectory().toFile()
    val selectedDirectory = settings.downloadDirectory?.let(::File) ?: defaultDirectory
    SettingsRow(
        "Download folder",
        selectedDirectory.absolutePath,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsButton("Choose folder") {
                val chooser = javax.swing.JFileChooser().apply {
                    fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = "Choose the download folder"
                }
                if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                    val selected = chooser.selectedFile
                    if (selected != null) {
                        onSettingsChange {
                            it.copy(downloadDirectory = selected.toPath().toAbsolutePath().normalize().toString())
                        }
                    }
                }
            }
            if (settings.downloadDirectory != null) {
                SettingsButton("Use default") { onSettingsChange { it.copy(downloadDirectory = null) } }
            }
        }
    }
    SettingsNote("Direct video URLs can be downloaded to this folder.")
    Spacer(Modifier.height(6.dp))
    AutoDetectSettings(
        settings = settings,
        onSettingsChange = onSettingsChange,
        autoDetect = autoDetect,
        autoDetectScan = autoDetectScan,
        autoDetectRunning = autoDetectRunning,
        autoDetectStatus = autoDetectStatus,
    )
}

@Composable
private fun AutoDetectSettings(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    autoDetect: AutoDetectService?,
    autoDetectScan: AutoDetectScan?,
    autoDetectRunning: Boolean,
    autoDetectStatus: String?,
) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AniLifeColors.Card)
            .border(1.dp, AniLifeColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = AniLifeColors.Primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Auto-detect downloads",
                    color = AniLifeColors.Foreground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Watches your download folders and adds finished anime to your library. " +
                        "Separate from the download folder above.",
                    color = AniLifeColors.MutedForeground,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
            SettingsSwitch(settings.autoDetectEnabled) { enabled ->
                onSettingsChange { it.copy(autoDetectEnabled = enabled) }
            }
        }
        settings.autoDetectDirectories.forEach { directory ->
            SettingsRow("Watched folder", directory) {
                SettingsButton("Remove") {
                    onSettingsChange { current ->
                        current.copy(autoDetectDirectories = current.autoDetectDirectories - directory)
                    }
                }
            }
        }
        SettingsRow(
            if (settings.autoDetectDirectories.isEmpty()) "No folders yet" else "Add another folder",
            if (settings.autoDetectDirectories.isEmpty()) {
                "Pick the folder your downloads land in, or the parent of your anime folders."
            } else {
                null
            },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsButton("Choose folder") {
                    val chooser = javax.swing.JFileChooser().apply {
                        fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                        dialogTitle = "Choose a folder to watch for downloads"
                    }
                    if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                        val selected = chooser.selectedFile
                        if (selected != null) {
                            val path = selected.toPath().toAbsolutePath().normalize().toString()
                            onSettingsChange { current ->
                                current.copy(autoDetectDirectories = (current.autoDetectDirectories + path).distinct())
                            }
                        }
                    }
                }
                if (settings.autoDetectEnabled && autoDetect != null) {
                    SettingsButton("Scan now") {
                        scope.launch {
                            val result = runCatching { autoDetect.scanOnce() }
                                .getOrElse { failure ->
                                    message = failure.message?.takeIf(String::isNotBlank)
                                        ?: "Couldn't scan the watched folders."
                                    return@launch
                                }
                            message = if (result.detected.isEmpty()) {
                                "No finished downloads found yet."
                            } else {
                                "${result.ready.size} added, ${result.needsConfirmation.size} need a choice."
                            }
                        }
                    }
                }
            }
        }
        SettingsRow(
            "Wait before importing",
            "A folder is only imported once its files stop changing.",
        ) {
            SettingsStepper(
                value = settings.autoDetectSettleSeconds,
                valueLabel = "${settings.autoDetectSettleSeconds}s",
                range = 10..600,
                step = 15,
            ) { onSettingsChange { current -> current.copy(autoDetectSettleSeconds = it) } }
        }
        val status = when {
            autoDetectStatus != null -> autoDetectStatus
            autoDetectRunning -> "Watching for new downloads."
            settings.autoDetectEnabled -> "Add a folder to start watching."
            else -> "Off. Turn it on to watch folders in the background."
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(8.dp).clip(CircleShape).background(
                    when {
                        autoDetectRunning -> AniLifeColors.Success
                        settings.autoDetectEnabled -> AniLifeColors.Warning
                        else -> AniLifeColors.MutedForeground
                    },
                ),
            )
            Text(status, color = AniLifeColors.MutedForeground, fontSize = 12.sp, lineHeight = 17.sp)
        }
        message?.let { text ->
            Text(text, color = AniLifeColors.Primary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
        }
        val folders = autoDetectScan?.folders.orEmpty()
        if (folders.isNotEmpty() && autoDetect != null) {
            Text(
                "Detected",
                color = AniLifeColors.Foreground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
            )
            folders.forEach { folder ->
                AutoDetectRow(
                    folder = folder,
                    onImport = { index ->
                        scope.launch {
                            val outcome = runCatching { autoDetect.importChosen(folder, index) }.getOrNull()
                            message = if (outcome != null && outcome.importedEpisodes > 0) {
                                "Added ${outcome.importedEpisodes} episode(s) of ${folder.matches.getOrNull(index)?.anime?.title ?: folder.folderName}."
                            } else {
                                "Nothing to add for ${folder.folderName}."
                            }
                        }
                    },
                    onIgnore = { autoDetect.ignore(folder.path) },
                )
            }
        }
        val skipped = autoDetectScan?.skipped.orEmpty()
        if (skipped.isNotEmpty()) {
            Text(
                skipped.take(3).joinToString("\n"),
                color = AniLifeColors.MutedForeground,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        val ignored = autoDetectScan?.ignored.orEmpty()
        if (ignored.isNotEmpty() && autoDetect != null) {
            SettingsRow(
                "Ignored folders",
                "${ignored.size} folder${if (ignored.size == 1) "" else "s"} will never be imported.",
            ) {
                SettingsButton("Restore all") { ignored.forEach(autoDetect::unignore) }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun AutoDetectRow(
    folder: AutoDetectCandidate,
    onImport: (Int) -> Unit,
    onIgnore: () -> Unit,
) {
    var expanded by remember(folder.path) { mutableStateOf(false) }
    var choice by remember(folder.path) { mutableIntStateOf(0) }
    val selected = folder.matches.getOrNull(choice) ?: folder.bestMatch
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AniLifeColors.SurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                folder.folderName,
                color = AniLifeColors.MutedForeground,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                selected?.anime?.title ?: "No match yet",
                color = AniLifeColors.Foreground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = buildString {
                append("${folder.episodes.size} episode${if (folder.episodes.size == 1) "" else "s"}")
                if (selected != null) append(" · ${"%.0f".format(selected.score * 100)}% match")
                if (!folder.settled) append(" · still downloading")
                if (folder.needsConfirmation && folder.matches.size > 1) append(" · pick the right title")
            }
            Text(detail, color = AniLifeColors.MutedForeground, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (folder.matches.size > 1) {
            Box {
                SettingsButton("Choose") { expanded = true }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    folder.matches.forEachIndexed { index, match ->
                        DropdownMenuItem(
                            text = { Text("${match.anime.title} · ${"%.0f".format(match.score * 100)}%", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = {
                                choice = index
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
        SettingsButton("Add", enabled = folder.settled && selected != null) { onImport(choice) }
        SettingsButton("Ignore") { onIgnore() }
    }
}

@Composable
private fun ServerSettings() {
    SettingsNote(
        "The video providers AniLife plays through are listed on the Providers screen in the sidebar. " +
            "Extra provider modules can be added there.",
    )
}

@Composable
private fun DiscordOAuthSection() {
    val state by DiscordOAuth.state.collectAsState()
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    val connecting = state.status == DiscordOAuthStatus.CONNECTING

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(AniLifeColors.Card).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Link, contentDescription = null, tint = AniLifeColors.Primary)
            Column(Modifier.weight(1f)) {
                Text("Discord account", color = AniLifeColors.Foreground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    state.user?.displayName?.let { "Connected as $it" } ?: "Sign in with Discord (OAuth2) to link your account.",
                    color = AniLifeColors.MutedForeground,
                    fontSize = 12.sp,
                )
            }
            if (state.user != null) {
                TextButton(onClick = { DiscordOAuth.disconnect() }) { Text("Disconnect") }
            }
        }
        if (state.user != null) {
            state.user?.avatarUrl?.let { url ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Avatar:", color = AniLifeColors.MutedForeground, fontSize = 11.sp)
                    Text(url, color = AniLifeColors.MutedForeground, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
            Text("User ID: ${state.user?.id}", color = AniLifeColors.MutedForeground, fontSize = 11.sp)
        } else {
            Button(
                onClick = {
                    scope.launch {
                        error = null
                        try {
                            DiscordOAuth.signIn()
                        } catch (e: Throwable) {
                            error = e.message ?: "Discord sign-in failed."
                        }
                    }
                },
                enabled = !connecting,
            ) {
                if (connecting) CircularProgressIndicator(Modifier.width(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Rounded.Link, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Sign in with Discord")
            }
        }
        state.message?.let { Text(it, color = AniLifeColors.MutedForeground, fontSize = 12.sp) }
        error?.let { Text(it, color = AniLifeColors.Destructive, fontSize = 12.sp) }
    }
}

@Composable
private fun AccountSettings(
    settings: AppSettings,
    discordState: DiscordPresenceState,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    onDiscordConnect: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val oauthState by DiscordOAuth.state.collectAsState()
    SettingsRow(
        "Discord Rich Presence",
        "Show \"Watching AniLife\" with an elapsed timer, or the anime open in the player, on your Discord profile.",
    ) {
        SettingsSwitch(settings.discordPresenceEnabled) { enabled ->
            onSettingsChange { it.copy(discordPresenceEnabled = enabled) }
        }
    }
    Text(
        text = "Example of how it looks on your Discord profile",
        color = AniLifeColors.MutedForeground,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
    )
    DiscordProfilePreview(
        displayName = oauthState.user?.displayName ?: discordState.accountName ?: "FleurdelysLover",
        username = oauthState.user?.username ?: discordState.username ?: "witherbl_de",
        avatarUrl = oauthState.user?.avatarUrl ?: discordState.avatarUrl,
        activityDetails = "On S01E11",
        activityArtworkUrl = ANILIFE_ICON_URL,
    )
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        DiscordConnectionRow(
            state = discordState,
            enabled = settings.discordPresenceEnabled,
            onConnect = onDiscordConnect,
        )
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        SettingsButton("Star on GitHub") {
            try { uriHandler.openUri("https://github.com/anilifecommunity/kotlindesktop") } catch (_: Exception) {}
        }
    }
    DiscordOAuthSection()
    SettingsNote("MyAnimeList sync is available from Profile after signing in.")
}

@Composable
private fun NotificationSettings() {
    SettingsNote(
        "New episodes for titles you follow, release reminders and AniList activity will appear in " +
            "Notifications once accounts are connected.",
    )
}

@Composable
private fun ContentSettings(settings: AppSettings, onSettingsChange: ((AppSettings) -> AppSettings) -> Unit) {
    SettingsRow(
        "Title display language",
        "Choose which AniList title to show across browse, search, library and the player.",
    ) {
        SettingsSegmented(
            choices = listOf("english" to "English", "romaji" to "Romaji", "native" to "Native"),
            selected = settings.titleLanguage,
            onSelect = { value -> onSettingsChange { it.copy(titleLanguage = value) } },
        )
    }
}

@Composable
private fun AppearanceSettings(settings: AppSettings, onSettingsChange: ((AppSettings) -> AppSettings) -> Unit) {
    val themeMode = ThemeMode.fromStorage(settings.themeMode)

    SettingsSection("Theme") {
        SettingsNote("Default keeps the original AniLife look. Glass themes apply instantly and are remembered in your app settings.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemeMode.entries.forEach { mode ->
                ThemePreviewCard(
                    mode = mode,
                    selected = mode == themeMode,
                    modifier = Modifier.weight(1f),
                    onClick = { onSettingsChange { it.copy(themeMode = mode.storageKey) } },
                )
            }
        }
    }

    SettingsSection("Surface and motion") {
        SettingsRow(
            "High Refresh Rate UI",
            "Drives hover, reveal, scroll and loading motion from a 120 Hz frame clock. Only used on high-refresh displays.",
        ) {
            SettingsSwitch(settings.highRefreshRateUi) { enabled ->
                onSettingsChange { it.copy(highRefreshRateUi = enabled) }
            }
        }
        SettingsRow(
            "Reduced motion",
            "Skip decorative animation and jump straight to the final state.",
        ) {
            SettingsSwitch(settings.reducedMotion) { enabled ->
                onSettingsChange { it.copy(reducedMotion = enabled) }
            }
        }
        SettingsRow(
            "Smooth scrolling",
            "Animate programmatic jumps, row arrows and scroll-to-item requests.",
        ) {
            SettingsSwitch(settings.smoothScrolling) { enabled ->
                onSettingsChange { it.copy(smoothScrolling = enabled) }
            }
        }
        SettingsRow(
            "Scrollbar visibility",
            "When the rounded scrollbar appears on scrolling surfaces.",
        ) {
            SettingsSegmented(
                choices = ScrollbarVisibility.entries.map { it.storageKey to it.label },
                selected = settings.scrollbarVisibility,
                onSelect = { value -> onSettingsChange { it.copy(scrollbarVisibility = value) } },
            )
        }
        SettingsRow(
            "Scroll animation duration",
            "Length of animated scroll-to and scroll-to-item requests.",
        ) {
            SettingsSegmented(
                choices = ScrollAnimationDuration.entries.map { it.storageKey to it.label },
                selected = settings.scrollAnimationDuration,
                onSelect = { value -> onSettingsChange { it.copy(scrollAnimationDuration = value) } },
            )
        }
    }

    if (themeMode != ThemeMode.DEFAULT) {
        SettingsSection("Glass customization") {
            SettingsNote("Conservative adjustments keep legibility and performance intact. Themes with limited blur support fall back to tinted surfaces.")
            SettingsRow("Blur strength", "Strength of the backdrop blur on glass surfaces.") {
                SettingsSliderRow(
                    value = settings.glassBlurIntensity,
                    valueRange = 0.5f..2f,
                    valueLabel = "${(settings.glassBlurIntensity * 100).roundToInt()}%",
                    onChange = { value -> onSettingsChange { it.copy(glassBlurIntensity = value) } },
                )
            }
            SettingsRow("Transparency", "How much of the surface tint is blended in.") {
                SettingsSliderRow(
                    value = settings.glassTransparency,
                    valueRange = 0.25f..0.85f,
                    valueLabel = "${(settings.glassTransparency * 100).roundToInt()}%",
                    onChange = { value -> onSettingsChange { it.copy(glassTransparency = value) } },
                )
            }
            // Edge highlights drive the liquid refraction shader, so they only apply to Liquid Glass.
            if (themeMode == ThemeMode.LIQUID_GLASS) {
                SettingsRow("Edge highlights", "Brightness of borders and reflections on glass surfaces.") {
                    SettingsSliderRow(
                        value = settings.glassHighlightIntensity,
                        valueRange = 0f..1.5f,
                        valueLabel = "${(settings.glassHighlightIntensity * 100).roundToInt()}%",
                        onChange = { value -> onSettingsChange { it.copy(glassHighlightIntensity = value) } },
                    )
                }
            }
        }
    }

    SettingsSection("Typeface") {
        SettingsRow(
            "App font",
            "Typeface used across the interface.",
        ) {
            SettingsDropdown(
                choices = AppSettingsStore.fontFamilies.map { it to fontLabel(it) },
                selected = settings.appFont,
                onSelect = { value -> onSettingsChange { it.copy(appFont = value) } },
            )
        }
        SettingsRow(
            "Custom font file",
            "Point the app typeface at a .ttf or .otf file on this computer.",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val parent = java.awt.Frame()
                        parent.isVisible = false
                        val dialog = java.awt.FileDialog(parent, "Choose the app font", java.awt.FileDialog.LOAD)
                        dialog.isVisible = true
                        val selected = dialog.file?.let { java.io.File(dialog.directory, it) }
                        parent.dispose()
                        if (selected != null) {
                            onSettingsChange { it.copy(appFontFile = selected.absolutePath) }
                        }
                    },
                ) { Text("Choose font file") }
                if (settings.appFontFile.isNotBlank()) {
                    OutlinedButton(onClick = { onSettingsChange { it.copy(appFontFile = "") } }) { Text("Clear") }
                }
            }
        }
    }

    SettingsSection("Theme colors") {
        ThemeEditor(
            css = settings.themeCss,
            onChange = { css -> onSettingsChange { it.copy(themeCss = css) } },
        )
    }
}

@Composable
private fun ThemePreviewCard(
    mode: ThemeMode,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) AniLifeColors.Primary else AniLifeColors.Border
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(AniLifeColors.Card)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        val appearance = LocalAppearance.current
        CompositionLocalProvider(LocalAppearance provides appearance.copy(themeMode = mode)) {
            Box(
                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(AniLifeColors.Background),
            ) {
                Box(
                    Modifier.padding(5.dp).size(width = 11.dp, height = 38.dp)
                        .clip(RoundedCornerShape(4.dp)).background(AniLifeColors.Sidebar),
                )
                Box(
                    Modifier.align(Alignment.TopEnd).padding(7.dp).size(width = 34.dp, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp)).background(AniLifeColors.Muted),
                )
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(7.dp)
                        .size(width = 52.dp, height = 18.dp)
                        .glassSurface(shape = RoundedCornerShape(5.dp), alpha = 0.4f),
                )
            }
        }
        Text(
            mode.label,
            color = if (selected) AniLifeColors.Primary else AniLifeColors.Foreground,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/** What the last Data action did, for the line under the buttons. */
private data class DataResult(val message: String, val isError: Boolean = false)

/** Backups and MyAnimeList files: the same four actions, and the same files, as the Android app's Data screen. */
@Composable
private fun DataSettings(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    repository: AniListRepository,
) {
    val scope = rememberCoroutineScope()
    val latestSettings by rememberUpdatedState(settings)
    val transfer = remember(repository) { DataTransfer(repository, syncSaved = { latestSettings.syncSavedToAccount }) }
    var busy by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<DataResult?>(null) }
    var pendingRestore by remember { mutableStateOf<BackupDocument?>(null) }

    fun run(label: String, work: suspend () -> DataResult) {
        busy = label
        result = null
        scope.launch {
            result = try {
                work()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                DataResult(failure.message?.takeIf(String::isNotBlank) ?: "That didn't work.", isError = true)
            } finally {
                busy = null
            }
        }
    }

    fun progressLabel(prefix: String): (Int, Int) -> Unit = { done, total ->
        if (total > 0) busy = "$prefix ($done/$total)"
    }

    fun backUp() {
        val target = chooseDataFile("Save an Anilife backup", save = true, suggested = AppBackup.fileName(), extension = ".json") ?: return
        run("Backing up") {
            withContext(Dispatchers.IO) {
                val document = transfer.createBackup(settings)
                java.nio.file.Files.writeString(target, AppBackup.encode(document))
                DataResult("Saved ${document.describe()} to ${target.fileName}.")
            }
        }
    }

    fun chooseBackup() {
        val source = chooseDataFile("Choose an Anilife backup", save = false) ?: return
        run("Reading") {
            withContext(Dispatchers.IO) {
                if (java.nio.file.Files.size(source) > AppBackup.MAX_SOURCE_BYTES) {
                    throw IllegalArgumentException("That file is too large to be an Anilife backup.")
                }
                pendingRestore = AppBackup.decode(java.nio.file.Files.readString(source))
            }
            DataResult("")
        }
    }

    fun restore(document: BackupDocument) {
        run("Restoring") {
            val summary = transfer.restore(document, applySettings = onSettingsChange, onProgress = progressLabel("Restoring"))
            val extras = buildList {
                if (summary.notRestored.isNotEmpty()) add("Not carried over: ${summary.notRestored.joinToString(", ")}.")
                addAll(summary.warnings)
                summary.accountNote?.let(::add)
            }
            DataResult(("Restored ${summary.describe()}. " + extras.joinToString(" ")).trim())
        }
    }

    fun exportMal() {
        run("Exporting") {
            val export = transfer.buildMalExport(progressLabel("Exporting"))
            if (export.exportedCount == 0) {
                return@run DataResult(
                    if (export.skippedCount > 0) "None of your ${export.skippedCount} titles has a MyAnimeList id yet, so there is nothing to export."
                    else "There is nothing to export yet. Save some anime to your library first.",
                    isError = true,
                )
            }
            val target = chooseDataFile("Save a MyAnimeList export", save = true, suggested = export.fileName, extension = ".xml")
                ?: return@run DataResult("Export cancelled.")
            withContext(Dispatchers.IO) { java.nio.file.Files.writeString(target, export.xml) }
            val skipped = if (export.skippedCount > 0) " ${export.skippedCount} without a MyAnimeList id were left out." else ""
            val note = export.note?.let { " $it" }.orEmpty()
            DataResult("Exported ${export.exportedCount} anime to ${target.fileName}.$skipped$note")
        }
    }

    fun importMal() {
        val source = chooseDataFile("Choose a MyAnimeList export (.xml or .xml.gz)", save = false) ?: return
        run("Importing") {
            val bytes = withContext(Dispatchers.IO) {
                if (java.nio.file.Files.size(source) > me.enivzoy.anilife.app.data.MalImport.MAX_SOURCE_BYTES) {
                    throw IllegalArgumentException("The MAL export is larger than the 16 MB safety limit.")
                }
                java.nio.file.Files.readAllBytes(source)
            }
            val summary = transfer.importMalXml(bytes, progressLabel("Matching titles"))
            val parts = buildList {
                add("${summary.added} added")
                if (summary.alreadySaved > 0) add("${summary.alreadySaved} already saved")
                if (summary.unmatched > 0) add("${summary.unmatched} not found on AniList")
            }
            val account = summary.accountNote?.let { " $it" }.orEmpty()
            DataResult("Imported ${summary.totalEntries} entries: ${parts.joinToString(", ")}.$account")
        }
    }

    SettingsSection("Backup and restore") {
        SettingsRow(
            "Back up to a file",
            "Saved anime and Continue Watching, plus the settings both apps share. The file opens in the Android app too.",
        ) { SettingsButton(text = "Back up", enabled = busy == null, onClick = ::backUp) }
        SettingsRow(
            "Restore from a backup",
            "Replaces the shared settings; adds saved anime and history to this PC. Works with files from the Android app.",
        ) { SettingsButton(text = "Restore", enabled = busy == null, onClick = ::chooseBackup) }
    }
    SettingsSection("Saved titles and your account") {
        SettingsRow(
            "Add saved titles to my account",
            "After a restore or an import, titles saved here that your signed-in MyAnimeList or AniList " +
                "account does not have are added to its Plan to Watch. Titles already on your list are never changed.",
        ) {
            SettingsSwitch(settings.syncSavedToAccount) { enabled ->
                onSettingsChange { it.copy(syncSavedToAccount = enabled) }
            }
        }
    }
    SettingsSection("MyAnimeList files") {
        SettingsRow(
            "Export MyAnimeList XML",
            "Save your list as a MAL-compatible file you can import on myanimelist.net.",
        ) { SettingsButton(text = "Export", enabled = busy == null, onClick = ::exportMal) }
        SettingsRow(
            "Import MyAnimeList XML",
            "Add a MAL export to your library. Titles you already saved are left alone.",
        ) { SettingsButton(text = "Import", enabled = busy == null, onClick = ::importMal) }
    }
    busy?.let { SettingsNote("$it…") }
    result?.takeIf { it.message.isNotBlank() }?.let { outcome ->
        Text(
            outcome.message,
            color = if (outcome.isError) AniLifeColors.Destructive else AniLifeColors.Primary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(vertical = 10.dp),
        )
    }

    SettingsSection("Playback") {
        SettingsRow(
            "Playback buffer",
            "Larger buffers use more memory and can help unstable connections.",
        ) {
            SettingsSegmented(
                choices = listOf("auto" to "Auto", "small" to "Small", "medium" to "Medium", "large" to "Large"),
                selected = settings.playbackBufferSize,
                onSelect = { value -> onSettingsChange { it.copy(playbackBufferSize = value) } },
            )
        }
    }
    SettingsNote("Downloads are stored in the download folder chosen on the Downloads page.")

    pendingRestore?.let { document ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            modifier = Modifier.appEnter(),
            containerColor = AniLifeColors.Card,
            titleContentColor = AniLifeColors.Foreground,
            textContentColor = AniLifeColors.MutedForeground,
            title = { Text("Restore this backup?", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "The file holds ${document.describe()}. Settings both apps share are replaced; saved anime " +
                        "and Continue Watching are added to what is already here, never removed.",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { pendingRestore = null; restore(document) }) {
                    Text("Restore", color = AniLifeColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) {
                    Text("Cancel", color = AniLifeColors.MutedForeground)
                }
            },
        )
    }
}

/** A native open or save dialog; null when the viewer cancels. A save always ends in [extension]. */
private fun chooseDataFile(
    title: String,
    save: Boolean,
    suggested: String? = null,
    extension: String? = null,
): java.nio.file.Path? {
    val parent = java.awt.Frame()
    parent.isVisible = false
    val dialog = java.awt.FileDialog(parent, title, if (save) java.awt.FileDialog.SAVE else java.awt.FileDialog.LOAD)
    suggested?.let { dialog.file = it }
    dialog.isVisible = true
    val chosen = dialog.file?.let { name ->
        val named = if (save && extension != null && !name.endsWith(extension, ignoreCase = true)) name + extension else name
        File(dialog.directory, named)
    }
    parent.dispose()
    return chosen?.toPath()
}

@Composable
private fun AppSettingsPage(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    currentVersion: String,
    updateCheckStatus: String?,
    checkingForUpdates: Boolean,
    onCheckForUpdates: () -> Unit,
) {
    SettingsSection("Updates") {
        SettingsRow(
            "Update channel",
            "Choose which release tags to check. Release is the stable default; other channels may contain less-tested builds.",
        ) {
            SettingsDropdown(
                choices = UpdateChannel.entries.map { it.name to it.label },
                selected = settings.updateChannel.name,
                onSelect = { value ->
                    val channel = UpdateChannel.parse(value)
                    onSettingsChange { it.copy(updateChannel = channel) }
                },
            )
        }
        SettingsRow(
            "Check for updates on launch",
            "Look for a newer AniLife release when the app starts. You choose whether to install it.",
        ) {
            SettingsSwitch(settings.checkUpdatesOnLaunch) { enabled ->
                onSettingsChange { it.copy(checkUpdatesOnLaunch = enabled) }
            }
        }
        SettingsRow(
            "Check now",
            updateCheckStatus ?: "Installed version $currentVersion",
        ) {
            SettingsButton(
                text = if (checkingForUpdates) "Checking…" else "Check for updates",
                enabled = !checkingForUpdates,
                onClick = onCheckForUpdates,
            )
        }
    }
    SettingsSection("Startup") {
        SettingsRow(
            "Play startup intro",
            "Show the AniLife animation when the app opens.",
        ) {
            SettingsSwitch(settings.playIntro) { enabled -> onSettingsChange { it.copy(playIntro = enabled) } }
        }
    }

    Spacer(Modifier.height(26.dp))
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(7.dp)
                    .clip(CircleShape)
                    .background(AniLifeColors.Destructive),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Experimental",
                color = AniLifeColors.Destructive,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(
            Modifier.fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AniLifeColors.Destructive.copy(alpha = 0.10f))
                .border(1.dp, AniLifeColors.Destructive.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(14.dp),
        ) {
            Text(
                "These features are unfinished and may change or break between releases. " +
                    "Turn them off if the app misbehaves.",
                color = AniLifeColors.Destructive,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
        ExperimentalRow(
            title = "Debounced search",
            description = "Wait for a pause in typing before asking AniList, instead of searching on every keystroke.",
            checked = settings.expDebouncedSearch,
        ) { enabled -> onSettingsChange { it.copy(expDebouncedSearch = enabled) } }
        ExperimentalRow(
            title = "Disk image cache",
            description = "Keep downloaded posters on disk so they reappear instantly on the next launch.",
            checked = settings.expImageCaching,
        ) { enabled -> onSettingsChange { it.copy(expImageCaching = enabled) } }
        ExperimentalRow(
            title = "Async fetching",
            description = "Keep the previous results on screen while the next request is loading.",
            checked = settings.expAsyncFetching,
        ) { enabled -> onSettingsChange { it.copy(expAsyncFetching = enabled) } }
        ExperimentalRow(
            title = "Infinite pagination",
            description = "Load additional search pages as you scroll toward the bottom.",
            checked = settings.expPagination,
        ) { enabled -> onSettingsChange { it.copy(expPagination = enabled) } }
        ExperimentalRow(
            title = "Screen fade transitions",
            description = "Animate screen changes. Turn off for instant cuts and less GPU work.",
            checked = settings.expFadeTransitions,
        ) { enabled -> onSettingsChange { it.copy(expFadeTransitions = enabled) } }
        ExperimentalRow(
            title = "Stable scroll keys",
            description = "Reuse list items by anime id while scrolling for smoother poster grids.",
            checked = settings.expHardwareScrolling,
        ) { enabled -> onSettingsChange { it.copy(expHardwareScrolling = enabled) } }
        ExperimentalRow(
            title = "Extended mpv buffering",
            description = "Use a larger stream cache and automatic reconnect. Turn off on low-memory machines.",
            checked = settings.expMpvBuffering,
        ) { enabled -> onSettingsChange { it.copy(expMpvBuffering = enabled) } }
        ExperimentalRow(
            title = "Smart downloads",
            description = "Queue the next episode at the halfway point and remove the episodes you have moved past.",
            checked = settings.expSmartDownloads,
        ) { enabled -> onSettingsChange { it.copy(expSmartDownloads = enabled) } }
        if (settings.expSmartDownloads) {
            SettingsRow(
                "Delete old downloads after",
                "How many episodes you can move past before the oldest download is removed. Zero never deletes.",
            ) {
                SettingsStepper(
                    value = settings.expSmartDownloadsStartAfter,
                    valueLabel = if (settings.expSmartDownloadsStartAfter == 0) {
                        "Never"
                    } else {
                        "${settings.expSmartDownloadsStartAfter} episodes"
                    },
                    range = 0..3,
                    step = 1,
                ) { value -> onSettingsChange { it.copy(expSmartDownloadsStartAfter = value) } }
            }
        }
    }
}

@Composable
private fun ExperimentalRow(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    SettingsRow(title, description) {
        SettingsSwitch(checked, onChange)
    }
}

@Composable
private fun AboutSettings(currentVersion: String, settings: AppSettings) {
    val uriHandler = LocalUriHandler.current
    var linkError by remember { mutableStateOf(false) }

    fun openLink(uri: String) {
        try {
            uriHandler.openUri(uri)
            linkError = false
        } catch (_: Exception) {
            linkError = true
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AboutCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(54.dp).clip(CircleShape).background(AniLifeColors.Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = AniLifeColors.PrimaryForeground,
                        modifier = Modifier.size(38.dp),
                    )
                }
                Column(
                    Modifier.padding(start = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text("AniLife", color = AniLifeColors.Foreground, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Version $currentVersion", color = AniLifeColors.MutedForeground, fontSize = 13.sp)
                }
            }

            AboutLinkRow("W", "Website", "anilife.cc") {
                openLink("https://anilife.cc")
            }
            AboutLinkRow("D", "Discord server", "Community, support, and release news") {
                openLink("https://discord.gg/C3EUYzdfZZ")
            }

            Text(
                "An anime app for finding episodes, keeping track of what you watch, and picking up where you left off.",
                color = AniLifeColors.MutedForeground,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = AniLifeColors.Primary,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    "  Made with love",
                    color = AniLifeColors.Foreground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                Modifier.clip(RoundedCornerShape(8.dp))
                    .clickable { openLink("https://github.com/anilifecommunity/kotlindesktop") }
                    .padding(vertical = 3.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = null,
                    tint = AniLifeColors.MutedForeground,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    "  Star the repo",
                    color = AniLifeColors.MutedForeground,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (linkError) {
                Text(
                    "Couldn't open that link. Please try again or open it in your browser.",
                    color = AniLifeColors.Destructive,
                    fontSize = 12.sp,
                )
            }
        }

        AboutCard {
            Text("Keyboard shortcuts", color = AniLifeColors.Foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Player shortcuts work while a video is open. Change them under Player settings.",
                color = AniLifeColors.MutedForeground,
                fontSize = 12.sp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayerAction.entries.chunked(2).forEach { actions ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        actions.forEach { action ->
                            AboutShortcut(
                                Modifier.weight(1f),
                                playerKeyLabel(settings.keyBindings[action] ?: DefaultPlayerKeyBindings.getValue(action)),
                                action.label,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(AniLifeColors.Card)
            .border(1.dp, AniLifeColors.Border, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun AboutLinkRow(
    badge: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(AniLifeColors.Background)
            .border(1.dp, AniLifeColors.Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(AniLifeColors.Primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(badge, color = AniLifeColors.PrimaryForeground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            Modifier.weight(1f).padding(start = 10.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, color = AniLifeColors.Foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = AniLifeColors.MutedForeground, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Icon(
            Icons.Rounded.Link,
            contentDescription = "Open $title",
            tint = AniLifeColors.MutedForeground,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun AboutShortcut(modifier: Modifier, keyLabel: String, description: String) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val shape = RoundedCornerShape(7.dp)
        Text(
            keyLabel,
            Modifier.clip(shape)
                .background(AniLifeColors.Background)
                .border(1.dp, AniLifeColors.Border, shape)
                .padding(horizontal = 7.dp, vertical = 5.dp),
            color = AniLifeColors.Foreground,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            description,
            Modifier.weight(1f).padding(start = 8.dp),
            color = AniLifeColors.MutedForeground,
            fontSize = 11.sp,
            maxLines = 2,
            lineHeight = 15.sp,
        )
    }
}

private fun fontLabel(value: String): String = when (value) {
    "Serif" -> "Serif"
    "Monospaced" -> "Mono"
    "Cursive" -> "Cursive"
    else -> "Sans"
}

@Composable
private fun SettingsRow(
    title: String,
    description: String? = null,
    control: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f).padding(end = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, color = AniLifeColors.Foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (description != null) {
                Text(description, color = AniLifeColors.MutedForeground, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        control()
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            color = AniLifeColors.Foreground,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp, bottom = 2.dp),
        )
        content()
    }
}

@Composable
private fun SettingsNote(text: String) {
    Text(
        text,
        color = AniLifeColors.MutedForeground,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier.padding(vertical = 10.dp),
    )
}

@Composable
private fun SettingsSegmented(
    choices: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier.clip(RoundedCornerShape(9.dp))
            .background(AniLifeColors.SettingsControl)
            .border(1.dp, AniLifeColors.SettingsControlBorder, RoundedCornerShape(9.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        choices.forEach { (value, label) ->
            val active = value == selected
            Box(
                Modifier.clip(RoundedCornerShape(7.dp))
                    .background(if (active) AniLifeColors.SettingsControlActive else Color.Transparent)
                    .then(if (active) Modifier else Modifier.clickable { onSelect(value) })
                    .padding(horizontal = 13.dp, vertical = 6.dp),
            ) {
                Text(
                    label,
                    color = if (active) AniLifeColors.SettingsControlActiveForeground else AniLifeColors.MutedForeground,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier.width(34.dp)
            .height(20.dp)
            .clip(CircleShape)
            .background(if (checked) AniLifeColors.SettingsControlActive else AniLifeColors.SettingsSwitchTrack)
            .clickable { onChange(!checked) },
    ) {
        Box(
            Modifier.offset(x = if (checked) 16.dp else 2.dp, y = 2.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(if (checked) AniLifeColors.SettingsControlActiveForeground else AniLifeColors.SettingsSwitchKnob),
        )
    }
}

@Composable
private fun SettingsStepper(
    value: Int,
    valueLabel: String,
    range: IntRange,
    step: Int,
    onChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(valueLabel, color = AniLifeColors.Foreground, fontSize = 13.sp)
        StepperButton("−") { onChange((value - step).coerceIn(range.first, range.last)) }
        StepperButton("+") { onChange((value + step).coerceIn(range.first, range.last)) }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AniLifeColors.SettingsControl)
            .border(1.dp, AniLifeColors.SettingsControlBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = AniLifeColors.Foreground, fontSize = 15.sp)
    }
}

@Composable
private fun SettingsSliderRow(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = valueRange,
            modifier = Modifier.width(180.dp),
            colors = SliderDefaults.colors(
                thumbColor = AniLifeColors.SettingsControlActive,
                activeTrackColor = AniLifeColors.SettingsControlActive,
                inactiveTrackColor = AniLifeColors.SettingsSwitchTrack,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Text(valueLabel, color = AniLifeColors.MutedForeground, fontSize = 12.sp, modifier = Modifier.width(46.dp))
    }
}

@Composable
private fun SettingsDropdown(
    choices: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val selectedLabel = choices.firstOrNull { it.first == selected }?.second ?: selected
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(8.dp))
                .background(AniLifeColors.SettingsControl)
                .border(1.dp, AniLifeColors.SettingsControlBorder, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(start = 12.dp, end = 9.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(selectedLabel, color = AniLifeColors.Foreground, fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = AniLifeColors.MutedForeground,
                modifier = Modifier.size(16.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.appEnter()) {
            choices.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label, fontSize = 13.sp) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(AniLifeColors.SettingsControl)
            .border(1.dp, AniLifeColors.SettingsControlBorder, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            color = if (enabled) AniLifeColors.Foreground else AniLifeColors.MutedForeground,
            fontSize = 12.sp,
        )
    }
}



@Composable
private fun DiscordConnectionRow(
    state: DiscordPresenceState,
    enabled: Boolean,
    onConnect: () -> Unit,
) {
    val dot: Color
    val label: String
    when {
        !enabled -> {
            dot = AniLifeColors.MutedForeground
            label = "Presence is off"
        }
        state.status == DiscordPresenceStatus.Connected -> {
            dot = AniLifeColors.Success
            label = state.accountName?.let { "Connected as $it" } ?: "Connected to Discord"
        }
        state.status == DiscordPresenceStatus.Connecting -> {
            dot = AniLifeColors.Warning
            label = "Connecting to Discord…"
        }
        else -> {
            dot = AniLifeColors.Destructive
            label = "Discord isn't running"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(AniLifeColors.SettingsControl)
            .border(1.dp, AniLifeColors.SettingsControlBorder, RoundedCornerShape(9.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(8.dp))
        Text(label, color = AniLifeColors.MutedForeground, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        SettingsButton(if (state.status == DiscordPresenceStatus.Connected) "Reconnect" else "Connect") {
            onConnect()
        }
    }
}
