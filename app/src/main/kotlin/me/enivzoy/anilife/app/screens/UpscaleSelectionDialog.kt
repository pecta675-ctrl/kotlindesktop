package me.enivzoy.anilife.app.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.enivzoy.anilife.app.data.LocalEpisode
import me.enivzoy.anilife.app.data.UpscaleMode
import me.enivzoy.anilife.core.model.AnimeCard
import me.enivzoy.anilife.app.ui.AniLifeColors
import me.enivzoy.anilife.app.ui.PageScrollbar
import me.enivzoy.anilife.app.ui.RemoteImage
import me.enivzoy.anilife.app.ui.parseAnimeColor

@Composable
fun UpscaleSelectionDialog(
    episodes: List<LocalEpisode>,
    onDismiss: () -> Unit,
    onConfirm: (List<LocalEpisode>, UpscaleMode) -> Unit,
) {
    val groups = remember(episodes) { groupUpscaleEpisodes(episodes) }
    var activeKey by remember { mutableStateOf<String?>(null) }
    var selectedKeys by remember { mutableStateOf(emptySet<String>()) }
    var mode by remember { mutableStateOf(UpscaleMode.Fast4K) }
    val active = groups.firstOrNull { it.key == activeKey }
    val selected = remember(episodes, selectedKeys) { selectedUpscaleEpisodes(episodes, selectedKeys) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            UpscalePickerContent(
                groups = groups,
                active = active,
                selectedKeys = selectedKeys,
                selectedCount = selected.size,
                mode = mode,
                onOpenAnime = { activeKey = it.key },
                onBack = { activeKey = null },
                onToggle = { entry ->
                    val key = upscaleKey(entry)
                    selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                },
                onSelectShow = { active?.let { group -> selectedKeys = selectedKeys + group.episodes.map(::upscaleKey) } },
                onClearShow = { active?.let { group -> selectedKeys = selectedKeys - group.episodes.map(::upscaleKey).toSet() } },
                onClearAll = { selectedKeys = emptySet() },
                onModeChange = { mode = it },
                onDismiss = onDismiss,
                onConfirm = { onConfirm(selected, mode) },
            )
        }
    }
}

@Composable
internal fun UpscalePickerContent(
    groups: List<UpscaleAnimeGroup>,
    active: UpscaleAnimeGroup?,
    selectedKeys: Set<String>,
    selectedCount: Int,
    mode: UpscaleMode,
    onOpenAnime: (UpscaleAnimeGroup) -> Unit,
    onBack: () -> Unit,
    onToggle: (LocalEpisode) -> Unit,
    onSelectShow: () -> Unit,
    onClearShow: () -> Unit,
    onClearAll: () -> Unit,
    onModeChange: (UpscaleMode) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    BoxWithConstraints(
        Modifier.widthIn(max = 940.dp).fillMaxWidth().heightIn(max = 800.dp).fillMaxHeight()
            .clip(shape).background(AniLifeColors.Card).border(1.dp, AniLifeColors.Border, shape),
    ) {
        val compact = maxWidth < 620.dp
        val inset = if (compact) 16.dp else 24.dp
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = inset, end = 12.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Upscale to 4K", color = AniLifeColors.Foreground, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                    Text("Choose an anime, then its downloaded episodes.", color = AniLifeColors.MutedForeground, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close upscale picker", tint = AniLifeColors.MutedForeground) }
            }
            HorizontalDivider(color = AniLifeColors.Border)
            if (active == null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = inset, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Your downloads", color = AniLifeColors.Foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text("${groups.size} anime", color = AniLifeColors.MutedForeground, fontSize = 12.sp)
                }
                val gridState = rememberLazyGridState()
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (groups.isEmpty()) {
                        Text("Download an episode first, then choose it here.", color = AniLifeColors.MutedForeground, modifier = Modifier.align(Alignment.Center).padding(inset))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(if (compact) 112.dp else 132.dp),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = inset, end = inset, bottom = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(groups, key = { it.key }) { group ->
                                UpscaleCoverButton(group, group.episodes.count { upscaleKey(it) in selectedKeys }) { onOpenAnime(group) }
                            }
                        }
                        PageScrollbar(gridState, Modifier.align(Alignment.CenterEnd))
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = inset, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to anime covers", tint = AniLifeColors.Foreground)
                    }
                    UpscaleArtwork(active.anime, Modifier.width(38.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(6.dp)), small = true)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(active.anime.title, color = AniLifeColors.Foreground, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${active.episodes.size} downloaded files", color = AniLifeColors.MutedForeground, fontSize = 11.sp)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(start = inset, end = inset, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Select episodes", color = AniLifeColors.Foreground, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = onSelectShow) { Text("Select all", fontSize = 12.sp) }
                    TextButton(onClick = onClearShow) { Text("Clear", fontSize = 12.sp) }
                }
                val gridState = key(active.key) { rememberLazyGridState() }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(if (compact) 145.dp else 190.dp),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = inset, end = inset, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(active.episodes, key = ::upscaleKey) { entry ->
                            UpscaleEpisodeButton(entry, upscaleKey(entry) in selectedKeys) { onToggle(entry) }
                        }
                    }
                    PageScrollbar(gridState, Modifier.align(Alignment.CenterEnd))
                }
            }
            HorizontalDivider(color = AniLifeColors.Border)
            Column(Modifier.fillMaxWidth().padding(horizontal = inset, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UpscaleMode.entries.forEach { option ->
                        val chosen = mode == option
                        Row(
                            Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (chosen) AniLifeColors.Primary.copy(alpha = 0.10f) else AniLifeColors.Background)
                                .border(1.dp, if (chosen) AniLifeColors.Primary else AniLifeColors.Border, RoundedCornerShape(10.dp))
                                .selectable(chosen, role = Role.RadioButton, onClick = { onModeChange(option) })
                                .pointerHoverIcon(PointerIcon.Hand).padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SelectionMark(chosen)
                            Text(option.label, color = AniLifeColors.Foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text(mode.description, color = AniLifeColors.MutedForeground, fontSize = 11.sp, lineHeight = 15.sp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("$selectedCount ${if (selectedCount == 1) "file" else "files"} selected", color = AniLifeColors.Foreground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        if (!compact) Text("Originals are kept. Selections stay when switching anime.", color = AniLifeColors.MutedForeground, fontSize = 11.sp)
                    }
                    if (selectedCount > 0) TextButton(onClick = onClearAll) { Text("Clear all", fontSize = 12.sp) }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = onConfirm, enabled = selectedCount > 0, shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AniLifeColors.Primary, contentColor = AniLifeColors.PrimaryForeground),
                    ) { Text("Start ${mode.label}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun UpscaleCoverButton(group: UpscaleAnimeGroup, selectedCount: Int, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onClick).pointerHoverIcon(PointerIcon.Hand),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(9.dp))) {
            UpscaleArtwork(group.anime, Modifier.fillMaxSize())
            if (selectedCount > 0) {
                Text(
                    "$selectedCount selected",
                    Modifier.align(Alignment.BottomStart).padding(7.dp).clip(RoundedCornerShape(6.dp))
                        .background(AniLifeColors.Primary).padding(horizontal = 7.dp, vertical = 4.dp),
                    color = AniLifeColors.PrimaryForeground, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(group.anime.title, color = AniLifeColors.Foreground, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("${group.episodes.map { it.episode }.distinct().size} episodes", color = AniLifeColors.MutedForeground, fontSize = 11.sp)
    }
}

@Composable
private fun UpscaleArtwork(anime: AnimeCard, modifier: Modifier, small: Boolean = false) {
    val tint = parseAnimeColor(anime.color)
    if (!anime.cover.isNullOrBlank()) {
        RemoteImage(anime.cover, "Downloaded episodes for ${anime.title}", modifier, placeholderColor = tint)
    } else {
        Box(modifier.background(tint.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
            val initials = anime.title.split(Regex("\\s+")).filter { it.firstOrNull()?.isLetterOrDigit() == true }
                .take(2).joinToString("") { it.first().uppercaseChar().toString() }
            Text(initials, color = AniLifeColors.Foreground.copy(alpha = 0.65f), fontSize = if (small) 12.sp else 30.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun UpscaleEpisodeButton(entry: LocalEpisode, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (selected) AniLifeColors.Primary.copy(alpha = 0.10f) else AniLifeColors.Background)
            .border(1.dp, if (selected) AniLifeColors.Primary else AniLifeColors.Border, RoundedCornerShape(10.dp))
            .toggleable(selected, role = Role.Checkbox, onValueChange = { onClick() }).pointerHoverIcon(PointerIcon.Hand).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Episode ${formatEpisode(entry.episode)}", color = AniLifeColors.Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            SelectionMark(selected)
        }
        Text(entry.episodeTitle?.takeIf(String::isNotBlank) ?: entry.path.fileName.toString(), color = AniLifeColors.MutedForeground, fontSize = 11.sp, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(upscaleVariant(entry), color = if (selected) AniLifeColors.Primary else AniLifeColors.MutedForeground, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SelectionMark(selected: Boolean) {
    Box(
        Modifier.size(18.dp).clip(RoundedCornerShape(5.dp))
            .background(if (selected) AniLifeColors.Primary else AniLifeColors.Input),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Rounded.Check, null, tint = AniLifeColors.PrimaryForeground, modifier = Modifier.size(13.dp))
    }
}

private fun formatEpisode(episode: Double): String =
    if (episode % 1.0 == 0.0) episode.toInt().toString() else episode.toString()
