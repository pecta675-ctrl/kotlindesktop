package me.enivzoy.anilife.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import me.enivzoy.anilife.app.data.AniListConnection
import me.enivzoy.anilife.app.data.AniListListEntry
import me.enivzoy.anilife.app.data.AniListListStatus
import me.enivzoy.anilife.app.data.AniListRepository
import me.enivzoy.anilife.app.data.AniListScoreFormat
import me.enivzoy.anilife.app.data.AniListSync
import me.enivzoy.anilife.app.ui.AniLifeColors
import me.enivzoy.anilife.app.ui.AnimePosterCard
import me.enivzoy.anilife.app.ui.appEnter
import me.enivzoy.anilife.app.ui.EmptyPanel
import me.enivzoy.anilife.app.data.isUpscaled
import me.enivzoy.anilife.app.ui.PageScrollbar

@Composable
fun LibraryScreen(
    repository: AniListRepository,
    onOpen: (Long) -> Unit,
    onBrowse: () -> Unit,
) {
    val localEntries by repository.libraryEntries.collectAsState()
    val localEpisodes by repository.localEpisodes.collectAsState()
    val upscaledIds = remember(localEpisodes) {
        localEpisodes.filter { it.isUpscaled() }.mapTo(mutableSetOf()) { it.anime.id }
    }
    val user by AniListSync.user.collectAsState()
    val remoteEntries by AniListSync.entries.collectAsState()
    val syncStatus by AniListSync.status.collectAsState()

    when {
        syncStatus.connection == AniListConnection.RESTORING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AniLifeColors.Primary)
        }
        user != null -> AniListLibrary(
            entries = remoteEntries,
            userName = user!!.name,
            scoreFormat = user!!.scoreFormat,
            syncMessage = syncStatus.message,
            syncing = syncStatus.syncing,
            upscaledIds = upscaledIds,
            onOpen = onOpen,
            onBrowse = onBrowse,
        )
        else -> LocalLibrary(
            entries = localEntries,
            upscaledIds = upscaledIds,
            onOpen = onOpen,
            onBrowse = onBrowse,
        )
    }
}

@Composable
private fun LocalLibrary(
    entries: List<me.enivzoy.anilife.core.model.AnimeCard>,
    upscaledIds: Set<Long>,
    onOpen: (Long) -> Unit,
    onBrowse: () -> Unit,
) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyPanel(
                icon = Icons.Rounded.VideoLibrary,
                title = "Your AniList library is ready",
                message = "Connect AniList to see your account list here, or save anime locally from its detail page.",
                action = {
                    Button(
                        onClick = onBrowse,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AniLifeColors.Primary,
                            contentColor = AniLifeColors.PrimaryForeground,
                        ),
                    ) {
                        Text("Explore anime", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        }
    } else {
        val gridState = rememberLazyGridState()
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 30.dp, bottom = 16.dp),
            ) {
                Text("Library", color = AniLifeColors.Foreground, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${entries.size} local save${if (entries.size == 1) "" else "s"} · connect AniList to use your account list",
                    color = AniLifeColors.MutedForeground,
                    fontSize = 14.sp,
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(26.dp),
                ) {
                    items(items = entries, key = { it.id }) { anime ->
                        AnimePosterCard(
                            anime = anime,
                            onClick = { onOpen(anime.id) },
                            width = 160.dp,
                            upscaled = anime.id in upscaledIds,
                        )
                    }
                }
                PageScrollbar(rememberScrollbarAdapter(gridState), Modifier.align(Alignment.CenterEnd))
            }
        }
    }
}

@Composable
private fun AniListLibrary(
    entries: List<AniListListEntry>,
    userName: String,
    scoreFormat: AniListScoreFormat,
    syncMessage: String?,
    syncing: Boolean,
    upscaledIds: Set<Long>,
    onOpen: (Long) -> Unit,
    onBrowse: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var busyEntry by remember { mutableStateOf<Long?>(null) }
    var statusFilter by remember { mutableStateOf<AniListListStatus?>(null) }
    val filteredEntries = remember(entries, statusFilter) {
        if (statusFilter == null) entries else entries.filter { it.status == statusFilter }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 26.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("AniList Library", color = AniLifeColors.Foreground, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "$userName · ${filteredEntries.size} of ${entries.size} titles · synced with AniList",
                        color = AniLifeColors.MutedForeground,
                        fontSize = 13.sp,
                    )
                }
                if (syncing) CircularProgressIndicator(Modifier.width(20.dp), strokeWidth = 2.dp)
            }
            syncMessage?.let { Text(it, color = AniLifeColors.Destructive, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
            error?.let { Text(it, color = AniLifeColors.Destructive, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    label = "All",
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                )
                AniListListStatus.entries.forEach { status ->
                    val count = entries.count { it.status == status }
                    if (count > 0) {
                        FilterChip(
                            label = "${status.label} ($count)",
                            selected = statusFilter == status,
                            onClick = { statusFilter = status },
                        )
                    }
                }
            }
        }

        if (filteredEntries.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyPanel(
                    icon = Icons.Rounded.VideoLibrary,
                    title = "Your AniList list is empty",
                    message = "Add anime from its detail page or review local saves in Accounts.",
                    action = {
                        Button(
                            onClick = onBrowse,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AniLifeColors.Primary,
                                contentColor = AniLifeColors.PrimaryForeground,
                            ),
                        ) { Text("Explore anime") }
                    },
                )
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    items(filteredEntries, key = { it.entryId }) { entry ->
                        var showEditor by remember(entry.entryId) { mutableStateOf(false) }
                        Box(
                            Modifier.pointerInput(entry.entryId) {
                                // Right-click opens the entry editor; primary clicks stay with the card.
                                awaitEachGesture {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.buttons.isSecondaryPressed) {
                                        showEditor = true
                                        do {
                                            val follow = awaitPointerEvent(PointerEventPass.Initial)
                                        } while (follow.changes.any { it.pressed })
                                    }
                                }
                            },
                        ) {
                            AnimePosterCard(
                                anime = entry.anime,
                                onClick = { onOpen(entry.anime.id) },
                                width = 160.dp,
                                upscaled = entry.anime.id in upscaledIds,
                            )
                        }
                        if (showEditor) {
                            AniListEntryEditorDialog(
                                entry = entry,
                                scoreFormat = scoreFormat,
                                busy = busyEntry == entry.entryId,
                                onClose = { showEditor = false },
                                onUpdate = { status, progress, score ->
                                    scope.launch {
                                        busyEntry = entry.entryId
                                        error = null
                                        try {
                                            AniListSync.updateEntry(entry, status, progress, score)
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (failure: Throwable) {
                                            error = failure.message ?: "Couldn't update AniList."
                                        } finally {
                                            busyEntry = null
                                        }
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        busyEntry = entry.entryId
                                        error = null
                                        try {
                                            AniListSync.deleteEntry(entry)
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (failure: Throwable) {
                                            error = failure.message ?: "Couldn't remove this AniList entry."
                                        } finally {
                                            busyEntry = null
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
                PageScrollbar(rememberScrollbarAdapter(gridState), Modifier.align(Alignment.CenterEnd))
            }
        }
    }
}

/**
 * The AniList entry editor, presented as a flat rectangular dialog in the Anime Info card color.
 */
@Composable
internal fun AniListEntryEditorDialog(
    entry: AniListListEntry,
    scoreFormat: AniListScoreFormat,
    busy: Boolean,
    onClose: () -> Unit,
    onUpdate: (AniListListStatus, Int, Double) -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.appEnter(),
        title = { Text(entry.anime.title, color = AniLifeColors.Foreground, fontWeight = FontWeight.SemiBold) },
        text = {
            AniListEntryControls(
                entry = entry,
                scoreFormat = scoreFormat,
                busy = busy,
                onUpdate = onUpdate,
                onDelete = onDelete,
            )
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } },
        shape = RectangleShape,
        containerColor = AniLifeColors.Card,
        titleContentColor = AniLifeColors.Foreground,
        textContentColor = AniLifeColors.Foreground,
    )
}

@Composable
internal fun AniListEntryControls(
    entry: AniListListEntry,
    scoreFormat: AniListScoreFormat,
    busy: Boolean,
    onUpdate: (AniListListStatus, Int, Double) -> Unit,
    onDelete: () -> Unit,
) {
    var statusExpanded by remember(entry.entryId) { mutableStateOf(false) }
    var showScoreDialog by remember(entry.entryId) { mutableStateOf(false) }
    var showDeleteDialog by remember(entry.entryId) { mutableStateOf(false) }
    val total = entry.anime.episodes?.takeIf { it > 0 }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                OutlinedButton(onClick = { statusExpanded = true }, enabled = !busy) {
                    Text(entry.status.label, fontSize = 11.sp)
                }
                DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }, modifier = Modifier.appEnter()) {
                    AniListListStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.label) },
                            onClick = {
                                statusExpanded = false
                                onUpdate(status, entry.progress, entry.score)
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showDeleteDialog = true }, enabled = !busy) {
                Text("Remove", fontSize = 11.sp, color = AniLifeColors.MutedForeground)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onUpdate(entry.status, (entry.progress - 1).coerceAtLeast(0), entry.score) },
                enabled = !busy && entry.progress > 0,
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Decrease episode progress", tint = AniLifeColors.Foreground)
            }
            Text(
                if (total == null) "${entry.progress} watched" else "${entry.progress} / $total episodes",
                Modifier.weight(1f),
                color = AniLifeColors.Foreground,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(
                onClick = { onUpdate(entry.status, (entry.progress + 1).coerceAtMost(total ?: Int.MAX_VALUE), entry.score) },
                enabled = !busy && (total == null || entry.progress < total),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase episode progress", tint = AniLifeColors.Foreground)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Score ${formatScore(entry.score, scoreFormat)} / ${formatScore(scoreFormat.maximum, scoreFormat)}",
                Modifier.weight(1f),
                color = AniLifeColors.MutedForeground,
                fontSize = 11.sp,
            )
            TextButton(onClick = { showScoreDialog = true }, enabled = !busy) { Text("Edit score", fontSize = 11.sp) }
        }
        if (busy) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).width(18.dp), strokeWidth = 2.dp)
        }
    }

    if (showScoreDialog) {
        var value by remember(entry.entryId) { mutableStateOf(formatScore(entry.score, scoreFormat)) }
        AlertDialog(
            onDismissRequest = { showScoreDialog = false },
            modifier = Modifier.appEnter(),
            title = { Text("Edit AniList score") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Your AniList scale is ${scoreFormat.name.replace("POINT_", "").replace('_', ' ')}.",
                        color = AniLifeColors.MutedForeground,
                        fontSize = 12.sp,
                    )
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Score (0-${formatScore(scoreFormat.maximum, scoreFormat)})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val score = value.toDoubleOrNull()
                    if (score != null && score.isFinite() && score in 0.0..scoreFormat.maximum) {
                        onUpdate(entry.status, entry.progress, score)
                        showScoreDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showScoreDialog = false }) { Text("Cancel") } },
            containerColor = AniLifeColors.Card,
            titleContentColor = AniLifeColors.Foreground,
            textContentColor = AniLifeColors.Foreground,
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            modifier = Modifier.appEnter(),
            title = { Text("Remove from AniList?") },
            text = { Text("This removes ${entry.anime.title} from your AniList account list.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Remove", color = AniLifeColors.Destructive) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
            containerColor = AniLifeColors.Card,
            titleContentColor = AniLifeColors.Foreground,
            textContentColor = AniLifeColors.Foreground,
        )
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) AniLifeColors.Primary else AniLifeColors.Muted
    val fg = if (selected) AniLifeColors.PrimaryForeground else AniLifeColors.MutedForeground
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatScore(score: Double, format: AniListScoreFormat): String =
    if (format.step < 1.0) String.format(java.util.Locale.ROOT, "%.1f", score)
    else score.toInt().toString()
