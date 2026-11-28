package me.enivzoy.anilife.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.enivzoy.anilife.app.data.AniListRepository
import me.enivzoy.anilife.app.data.AniListSync
import me.enivzoy.anilife.app.data.AniListConnection
import me.enivzoy.anilife.app.data.MyAnimeListSync
import me.enivzoy.anilife.app.ui.AniLifeColors
import me.enivzoy.anilife.app.ui.ErrorPanel
import me.enivzoy.anilife.app.ui.LoadState
import me.enivzoy.anilife.app.ui.PageScrollbar
import me.enivzoy.anilife.app.ui.RemoteImage
import me.enivzoy.anilife.app.ui.loadState
import me.enivzoy.anilife.app.ui.parseAnimeColor
import me.enivzoy.anilife.core.model.AiringEntry
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ScheduleScreen(
    repository: AniListRepository,
    onOpen: (Long) -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<LoadState<List<AiringEntry>>>(LoadState.Loading) }
    val zone = remember { ZoneId.systemDefault() }
    var now by remember { mutableStateOf(Instant.now().epochSecond) }
    val today = Instant.ofEpochSecond(now).atZone(zone).toLocalDate()
    var selectedDate by remember(today) { mutableStateOf(today) }
    var windowAnchor by remember(today) { mutableStateOf(today) }
    val aniListUser by AniListSync.user.collectAsState()
    val aniListEntries by AniListSync.entries.collectAsState()
    val aniListStatus by AniListSync.status.collectAsState()
    val malUser by MyAnimeListSync.user.collectAsState()
    val malEntries by MyAnimeListSync.entries.collectAsState()
    var malLoading by remember { mutableStateOf(false) }
    var malError by remember { mutableStateOf<String?>(null) }
    var aniListError by remember { mutableStateOf<String?>(null) }
    var listAttempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(30_000)
            now = Instant.now().epochSecond
        }
    }
    LaunchedEffect(repository, windowAnchor, zone, attempt) {
        state = LoadState.Loading
        val (from, until) = scheduleRangeBounds(windowAnchor, zone)
        state = loadState { repository.schedule(from, until) }
    }
    LaunchedEffect(malUser?.id, listAttempt) {
        malError = null
        if (malUser == null) return@LaunchedEffect
        malLoading = true
        try {
            MyAnimeListSync.list()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            malError = "Couldn't refresh MyAnimeList. Results may be incomplete. ${failure.message.orEmpty()}"
        } finally {
            malLoading = false
        }
    }
    LaunchedEffect(aniListUser?.id, listAttempt) {
        aniListError = null
        if (aniListUser == null || listAttempt == 0) return@LaunchedEffect
        try {
            AniListSync.refreshList()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            aniListError = "Couldn't refresh AniList. ${failure.message.orEmpty()}"
        }
    }
    val libraryIds = remember(aniListUser, aniListEntries) {
        if (aniListUser == null) emptySet() else aniListEntries.mapTo(mutableSetOf()) { it.anime.id }
    }
    val malIds = remember(malUser, malEntries) {
        if (malUser == null) emptySet() else malEntries.mapTo(mutableSetOf()) { it.id }
    }
    ScheduleContent(
        entries = (state as? LoadState.Ready)?.value.orEmpty(),
        libraryIds = libraryIds,
        malIds = malIds,
        today = today,
        selectedDate = selectedDate,
        windowAnchor = windowAnchor,
        zone = zone,
        now = now,
        loading = state == LoadState.Loading,
        error = (state as? LoadState.Failed)?.message,
        onRetry = { attempt++ },
        listLoading = malLoading || aniListStatus.syncing || aniListStatus.connection == AniListConnection.RESTORING,
        listMessage = listOfNotNull(malError, aniListError ?: aniListStatus.message).joinToString(" · ").takeIf(String::isNotBlank),
        accountsConnected = aniListUser != null || malUser != null,
        onRetryLists = { listAttempt++ },
        onSelectDate = { date ->
            selectedDate = date
            if (date !in scheduleDateWindow(windowAnchor)) windowAnchor = date
        },
        onOpen = onOpen,
    )
}

@Composable
private fun ScheduleContent(
    entries: List<AiringEntry>,
    libraryIds: Set<Long>,
    malIds: Set<Long>,
    today: LocalDate,
    selectedDate: LocalDate,
    windowAnchor: LocalDate,
    zone: ZoneId,
    now: Long,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    listLoading: Boolean,
    listMessage: String?,
    accountsConnected: Boolean,
    onRetryLists: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onOpen: (Long) -> Unit,
) {
    var onlyMyList by remember { mutableStateOf(true) }
    var formatFilters by remember { mutableStateOf(emptySet<String>()) }
    val visibleEntries = remember(entries, formatFilters, onlyMyList, libraryIds, malIds) {
        filterScheduleEntries(entries, formatFilters, libraryIds, onlyMyList, malIds)
    }
    val selectedEntries = remember(visibleEntries, selectedDate, zone) {
        visibleEntries.filter { airingDate(it, zone) == selectedDate }.sortedBy { it.airingAt }
    }
    val dayCounts = remember(visibleEntries, zone) {
        visibleEntries.groupingBy { airingDate(it, zone) }.eachCount()
    }
    val matchedLibraryIds = remember(entries, libraryIds, malIds) {
        filterScheduleEntries(entries, emptySet(), libraryIds, true, malIds).mapTo(mutableSetOf()) { it.anime.id }
    }

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(AniLifeColors.Background)
            .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 16.dp),
    ) {
        val sidebarWidth = (maxWidth * 0.42f).coerceIn(180.dp, 300.dp)
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Schedule",
                    color = AniLifeColors.Foreground,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                MyListButton(selected = onlyMyList, onClick = { onlyMyList = !onlyMyList })
            }
            Text(
                when {
                    onlyMyList && listLoading -> "Loading your AniList / MyAnimeList lists…"
                    onlyMyList && !accountsConnected -> "Connect AniList or MyAnimeList in Profile to see your list, or turn off My AniList / MAL to browse all."
                    onlyMyList -> "Only anime in your AniList or MyAnimeList lists · confirmed AniList times"
                    else -> "Confirmed AniList airings · local time · select a future date to load its schedule"
                },
                color = AniLifeColors.MutedForeground,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (listMessage != null && onlyMyList) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(listMessage, color = AniLifeColors.Warning, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = onRetryLists) { Text("Retry lists", fontSize = 11.sp) }
                }
            }

            ScheduleDayStrip(
                today = today,
                anchor = windowAnchor,
                selected = selectedDate,
                counts = dayCounts,
                onSelect = onSelectDate,
                modifier = Modifier.padding(top = 14.dp),
            )

            Row(
                Modifier.weight(1f).fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ScheduleTimeline(
                    date = selectedDate,
                    today = today,
                    entries = selectedEntries,
                    now = now,
                    loading = loading || (onlyMyList && listLoading),
                    error = error,
                    onRetry = onRetry,
                    onlyMyList = onlyMyList,
                    onOpen = onOpen,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                ScheduleSidebar(
                    entries = visibleEntries,
                    libraryIds = matchedLibraryIds,
                    counts = dayCounts,
                    selectedDate = selectedDate,
                    month = YearMonth.from(selectedDate),
                    now = now,
                    formatFilters = formatFilters,
                    sidebarWidth = sidebarWidth,
                    onSelectDate = onSelectDate,
                    onToggleFormat = { format ->
                        formatFilters = if (format in formatFilters) formatFilters - format else formatFilters + format
                    },
                    onOpen = onOpen,
                )
            }
        }
    }
}

@Composable
private fun MyListButton(selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.clip(shape)
            .background(if (selected) AniLifeColors.Primary else AniLifeColors.Card)
            .border(1.dp, if (selected) AniLifeColors.Primary else AniLifeColors.Border, shape)
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            Icons.Rounded.BookmarkBorder,
            contentDescription = null,
            tint = if (selected) AniLifeColors.PrimaryForeground else AniLifeColors.Foreground,
            modifier = Modifier.size(16.dp),
        )
        Text(
            "My AniList / MAL",
            color = if (selected) AniLifeColors.PrimaryForeground else AniLifeColors.Foreground,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ScheduleDayStrip(
    today: LocalDate,
    anchor: LocalDate,
    selected: LocalDate,
    counts: Map<LocalDate, Int>,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        scheduleDateWindow(anchor).forEach { date ->
            val isSelected = date == selected
            val shape = RoundedCornerShape(13.dp)
            Column(
                Modifier.weight(1f)
                    .height(68.dp)
                    .clip(shape)
                    .background(if (isSelected) AniLifeColors.Primary else AniLifeColors.Card.copy(alpha = 0.45f))
                    .border(1.dp, if (isSelected) AniLifeColors.Primary else AniLifeColors.Border, shape)
                    .clickable { onSelect(date) }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    dateLabel(date, today),
                    color = if (isSelected) AniLifeColors.PrimaryForeground else AniLifeColors.Foreground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())),
                    color = if (isSelected) AniLifeColors.PrimaryForeground.copy(alpha = 0.78f) else AniLifeColors.MutedForeground,
                    fontSize = 10.sp,
                )
                Text(
                    "${counts[date] ?: 0} eps",
                    color = if (isSelected) AniLifeColors.PrimaryForeground else AniLifeColors.MutedForeground,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun ScheduleTimeline(
    date: LocalDate,
    today: LocalDate,
    entries: List<AiringEntry>,
    now: Long,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onlyMyList: Boolean,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val groups = remember(entries) {
        entries.sortedBy { it.airingAt }
            .groupBy { Instant.ofEpochSecond(it.airingAt).atZone(ZoneId.systemDefault()).toLocalDateTime().withSecond(0).withNano(0) }
            .toList()
    }
    Box(modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item(key = "schedule-date-heading") {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())),
                        color = AniLifeColors.Foreground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "· ${entries.size} episodes · ${entries.count { it.airingAt < now }} aired",
                        color = AniLifeColors.MutedForeground,
                        fontSize = 12.sp,
                    )
                }
            }
            if (loading) {
                item(key = "schedule-loading") {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AniLifeColors.Primary, modifier = Modifier.size(28.dp))
                    }
                }
            } else if (error != null) {
                item(key = "schedule-error") { ErrorPanel(error, onRetry = onRetry) }
            } else if (groups.isEmpty()) {
                item(key = "schedule-empty") {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 70.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (onlyMyList) "No confirmed airings from your lists on this date."
                            else "AniList has no confirmed airings for this date yet.",
                            color = AniLifeColors.MutedForeground,
                            fontSize = 14.sp,
                        )
                    }
                }
            } else {
                items(groups, key = { (time, _) -> time.toString() }) { (time, groupEntries) ->
                    ScheduleTimeGroup(
                        time = time,
                        entries = groupEntries,
                        now = now,
                        onOpen = onOpen,
                    )
                }
            }
        }
        PageScrollbar(rememberScrollbarAdapter(listState), Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun ScheduleTimeGroup(
    time: LocalDateTime,
    entries: List<AiringEntry>,
    now: Long,
    onOpen: (Long) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.width(75.dp).padding(top = 2.dp)) {
            Text(
                time.format(DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())),
                color = AniLifeColors.Foreground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                relativeAiring(entries.first().airingAt, now),
                color = AniLifeColors.MutedForeground,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            entries.chunked(3).forEach { rowEntries ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowEntries.forEach { entry ->
                        ScheduleEpisodeCard(
                            entry = entry,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpen(entry.anime.id) },
                        )
                    }
                    repeat(3 - rowEntries.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ScheduleEpisodeCard(
    entry: AiringEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val anime = entry.anime
    val shape = RoundedCornerShape(13.dp)
    val tint = remember(anime.color) { parseAnimeColor(anime.color) }
    Row(
        modifier.clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(tint.copy(alpha = 0.25f), AniLifeColors.Card, AniLifeColors.Card),
                ),
            )
            .border(BorderStroke(1.dp, AniLifeColors.Border), shape)
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(10.dp)
            .height(88.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RemoteImage(
            url = anime.cover,
            contentDescription = anime.title,
            modifier = Modifier.width(58.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp)),
            placeholderColor = tint,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                anime.title,
                color = AniLifeColors.Foreground,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Episode ${entry.episode}${anime.episodes?.let { " / $it" }.orEmpty()}",
                color = AniLifeColors.MutedForeground,
                fontSize = 11.sp,
                maxLines = 1,
            )
            Text(
                listOfNotNull(anime.format?.let(::formatLabel), anime.genres.firstOrNull())
                    .joinToString(" · "),
                color = AniLifeColors.MutedForeground,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
            Box(
                Modifier.fillMaxSize()
                    .clip(CircleShape)
                    .background(AniLifeColors.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Open ${anime.title}",
                    tint = AniLifeColors.PrimaryForeground,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

@Composable
private fun ScheduleSidebar(
    entries: List<AiringEntry>,
    libraryIds: Set<Long>,
    counts: Map<LocalDate, Int>,
    selectedDate: LocalDate,
    month: YearMonth,
    now: Long,
    formatFilters: Set<String>,
    sidebarWidth: Dp,
    onSelectDate: (LocalDate) -> Unit,
    onToggleFormat: (String) -> Unit,
    onOpen: (Long) -> Unit,
) {
    var calendarMonth by remember(selectedDate) { mutableStateOf(month) }
    val upcoming = entries.filter { it.airingAt >= now }.minByOrNull { it.airingAt }
    val fromLibrary = entries
        .filter { it.airingAt >= now && it.anime.id in libraryIds }
        .sortedBy { it.airingAt }
        .take(4)

    val scrollState = rememberScrollState()
    Box(Modifier.width(sidebarWidth).fillMaxHeight()) {
    Column(
        Modifier.fillMaxSize().verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        upcoming?.let { UpcomingCard(it, now, onOpen) }
        FromLibraryCard(fromLibrary, onOpen)
        CalendarCard(
            month = calendarMonth,
            counts = counts,
            selectedDate = selectedDate,
            onPrevious = { calendarMonth = calendarMonth.minusMonths(1) },
            onNext = { calendarMonth = calendarMonth.plusMonths(1) },
            onSelectDate = { date ->
                calendarMonth = YearMonth.from(date)
                onSelectDate(date)
            },
        )
        FormatFilters(formatFilters, onToggleFormat)
    }
    PageScrollbar(scrollState, Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun UpcomingCard(entry: AiringEntry, now: Long, onOpen: (Long) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val tint = remember(entry.anime.color) { parseAnimeColor(entry.anime.color) }
    Box(
        Modifier.fillMaxWidth().height(154.dp)
            .clip(shape)
            .background(AniLifeColors.Card)
            .border(1.dp, AniLifeColors.Border, shape)
            .clickable { onOpen(entry.anime.id) }
            .pointerHoverIcon(PointerIcon.Hand),
    ) {
        RemoteImage(
            url = entry.anime.cover,
            contentDescription = null,
            modifier = Modifier.matchParentSize().alpha(0.18f),
            placeholderColor = tint,
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    listOf(AniLifeColors.Card.copy(alpha = 0.18f), AniLifeColors.Background.copy(alpha = 0.96f)),
                ),
            ),
        )
        Column(
            Modifier.fillMaxSize().padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "UP NEXT",
                Modifier.clip(RoundedCornerShape(5.dp))
                    .background(AniLifeColors.Primary)
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                color = AniLifeColors.PrimaryForeground,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            Column {
                Text(
                    entry.anime.title,
                    color = AniLifeColors.Foreground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Episode ${entry.episode}", color = AniLifeColors.MutedForeground, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    relativeAiring(entry.airingAt, now),
                    color = AniLifeColors.Primary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun FromLibraryCard(entries: List<AiringEntry>, onOpen: (Long) -> Unit) {
    SchedulePanel {
        Text("From your lists in this window", color = AniLifeColors.Foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (entries.isEmpty()) {
            Text(
                "No upcoming episodes from your list.",
                color = AniLifeColors.MutedForeground,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                entries.forEach { entry ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(7.dp))
                            .clickable { onOpen(entry.anime.id) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RemoteImage(
                            url = entry.anime.cover,
                            contentDescription = entry.anime.title,
                            modifier = Modifier.width(28.dp).height(38.dp).clip(RoundedCornerShape(5.dp)),
                            placeholderColor = AniLifeColors.CardHover,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.anime.title,
                                color = AniLifeColors.Foreground,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text("Episode ${entry.episode}", color = AniLifeColors.MutedForeground, fontSize = 10.sp)
                        }
                        Text(
                            Instant.ofEpochSecond(entry.airingAt).atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("EEE HH:mm", Locale.getDefault())),
                            color = AniLifeColors.MutedForeground,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarCard(
    month: YearMonth,
    counts: Map<LocalDate, Int>,
    selectedDate: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
) {
    val locale = Locale.getDefault()
    val first = month.atDay(1)
    val daysBeforeMonth = first.dayOfWeek.value % 7
    val dates = (0 until 42).map { first.minusDays(daysBeforeMonth.toLong()).plusDays(it.toLong()) }

    SchedulePanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Rounded.ChevronLeft, "Previous month", tint = AniLifeColors.MutedForeground, modifier = Modifier.size(17.dp))
            }
            Text(
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                Modifier.weight(1f),
                color = AniLifeColors.Foreground,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onNext, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Rounded.ChevronRight, "Next month", tint = AniLifeColors.MutedForeground, modifier = Modifier.size(17.dp))
            }
        }
        Text(
            "Select any date to load its confirmed airings.",
            color = AniLifeColors.MutedForeground,
            fontSize = 10.sp,
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    day,
                    Modifier.weight(1f),
                    color = AniLifeColors.MutedForeground,
                    fontSize = 10.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        dates.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val selected = date == selectedDate
                    val inMonth = YearMonth.from(date) == month
                    Column(
                        Modifier.weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (selected) AniLifeColors.Primary else AniLifeColors.Background.copy(alpha = 0f))
                            .clickable { onSelectDate(date) }
                            .pointerHoverIcon(PointerIcon.Hand),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            color = when {
                                selected -> AniLifeColors.PrimaryForeground
                                inMonth -> AniLifeColors.Foreground
                                else -> AniLifeColors.MutedForeground.copy(alpha = 0.45f)
                            },
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                        val count = counts[date] ?: 0
                        if (count > 0) {
                            Text(
                                count.toString(),
                                color = if (selected) AniLifeColors.PrimaryForeground else AniLifeColors.MutedForeground,
                                fontSize = 8.sp,
                                lineHeight = 8.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatFilters(selected: Set<String>, onToggle: (String) -> Unit) {
    val formats = listOf("TV", "TV Short", "ONA", "Special", "Movie")
    SchedulePanel {
        Text("Filters", color = AniLifeColors.Foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            formats.take(3).forEach { format ->
                FormatChip(format, format in selected, onToggle)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            formats.drop(3).forEach { format ->
                FormatChip(format, format in selected, onToggle)
            }
        }
    }
}

@Composable
private fun FormatChip(text: String, selected: Boolean, onToggle: (String) -> Unit) {
    val shape = RoundedCornerShape(100.dp)
    Text(
        text,
        Modifier.clip(shape)
            .background(if (selected) AniLifeColors.Primary else AniLifeColors.Background.copy(alpha = 0f))
            .border(1.dp, if (selected) AniLifeColors.Primary else AniLifeColors.Border, shape)
            .clickable { onToggle(text) }
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        color = if (selected) AniLifeColors.PrimaryForeground else AniLifeColors.Foreground,
        fontSize = 10.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
    )
}

@Composable
private fun SchedulePanel(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AniLifeColors.Card.copy(alpha = 0.55f))
            .border(1.dp, AniLifeColors.Border, RoundedCornerShape(14.dp))
            .padding(12.dp),
        content = { content() },
    )
}

internal fun scheduleDateWindow(today: LocalDate): List<LocalDate> =
    (-2L..6L).map(today::plusDays)

internal fun scheduleRangeBounds(anchor: LocalDate, zone: ZoneId): Pair<Long, Long> {
    val dates = scheduleDateWindow(anchor)
    return dates.first().atStartOfDay(zone).toEpochSecond() to
        dates.last().plusDays(1).atStartOfDay(zone).toEpochSecond()
}

internal fun filterScheduleEntries(
    entries: List<AiringEntry>,
    formatFilters: Set<String>,
    libraryIds: Set<Long>,
    onlyMyList: Boolean,
    malIds: Set<Long> = emptySet(),
): List<AiringEntry> = entries.filter { entry ->
    (!onlyMyList || entry.anime.id in libraryIds || entry.anime.malId in malIds) &&
        (formatFilters.isEmpty() || formatLabel(entry.anime.format) in formatFilters)
}

private fun airingDate(entry: AiringEntry, zone: ZoneId): LocalDate =
    Instant.ofEpochSecond(entry.airingAt).atZone(zone).toLocalDate()

private fun dateLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    today.plusDays(1) -> "Tomorrow"
    else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
}

private fun formatLabel(format: String?): String? = when (format?.uppercase()) {
    "TV" -> "TV"
    "TV_SHORT" -> "TV Short"
    "ONA" -> "ONA"
    "SPECIAL" -> "Special"
    "MOVIE" -> "Movie"
    else -> format?.replace('_', ' ')?.lowercase()?.replaceFirstChar(Char::uppercase)
}

private fun relativeAiring(airingAt: Long, now: Long): String {
    val delta = airingAt - now
    val minutes = kotlin.math.abs(delta) / 60
    val value = when {
        minutes >= 1_440 -> "${minutes / 1_440}d ${minutes % 1_440 / 60}h"
        minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes.coerceAtLeast(1)}m"
    }
    return if (delta >= 0) "in $value" else "$value ago"
}
