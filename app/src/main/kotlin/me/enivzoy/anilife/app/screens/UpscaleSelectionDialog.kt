package me.enivzoy.anilife.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.enivzoy.anilife.app.data.LocalEpisode
import me.enivzoy.anilife.app.data.UpscaleMode
import me.enivzoy.anilife.app.ui.AniLifeColors
import me.enivzoy.anilife.app.ui.PageScrollbar
import me.enivzoy.anilife.app.ui.adaptiveGlass
import me.enivzoy.anilife.app.ui.appEnter

private val UpscaleDialogShape = RoundedCornerShape(16.dp)

@Composable
fun UpscaleSelectionDialog(
    episodes: List<LocalEpisode>,
    onDismiss: () -> Unit,
    onConfirm: (List<LocalEpisode>, UpscaleMode) -> Unit,
) {
    val selectedKeys = remember { mutableStateListOf<String>() }
    var mode by remember { mutableStateOf(UpscaleMode.Fast4K) }
    val selected by remember(episodes) {
        derivedStateOf { episodes.filter { upscaleKey(it) in selectedKeys } }
    }
    val estimatedGigabytes = "%.1f".format(selected.size * 1.5)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 850.dp)
                    .fillMaxWidth()
                    .heightIn(max = 900.dp)
                    .clip(UpscaleDialogShape)
                    .background(AniLifeColors.Card)
                    .adaptiveGlass(shape = UpscaleDialogShape)
                    .appEnter()
                    .border(1.dp, AniLifeColors.Border, UpscaleDialogShape),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Upscale downloads to 4K",
                        color = AniLifeColors.Foreground,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.height(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AniLifeColors.MutedForeground,
                            modifier = Modifier.height(18.dp),
                        )
                    }
                }
                HorizontalDivider(color = AniLifeColors.Border)

                val scrollState = rememberScrollState()
                Box(Modifier.weight(1f)) {
                Column(
                    Modifier.fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        UpscaleMode.entries.forEach { option ->
                            val active = mode == option
                            Row(
                                Modifier.weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) AniLifeColors.Secondary else AniLifeColors.Card)
                                    .border(1.dp, if (active) AniLifeColors.Primary else AniLifeColors.Border, RoundedCornerShape(12.dp))
                                    .clickable { mode = option }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                RadioButton(selected = active, onClick = { mode = option })
                                Column(Modifier.weight(1f).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(option.label, color = AniLifeColors.Foreground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(option.description, color = AniLifeColors.MutedForeground, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    Text("Creates a separate 2160p copy. Your original file is kept.", color = AniLifeColors.MutedForeground, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UpscaleChip("Select All") { selectedKeys.addAll(episodes.map(::upscaleKey)) }
                        UpscaleChip("Clear All") { selectedKeys.clear() }
                    }

                    if (episodes.isEmpty()) {
                        Text("No downloaded episodes found.", color = AniLifeColors.MutedForeground, fontSize = 13.sp)
                    } else {
                        val episodeGridState = rememberLazyGridState()
                        Box(Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 56.dp),
                                modifier = Modifier.fillMaxSize(),
                                state = episodeGridState,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(episodes, key = ::upscaleKey) { entry ->
                                    val isSelected = upscaleKey(entry) in selectedKeys
                                    Box(
                                        Modifier.aspectRatio(1.6f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) AniLifeColors.Primary else AniLifeColors.Secondary)
                                            .clickable {
                                                val key = upscaleKey(entry)
                                                if (key in selectedKeys) selectedKeys.remove(key) else selectedKeys.add(key)
                                            }
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(1.dp),
                                        ) {
                                            Text(
                                                formatEpisode(entry.episode),
                                                color = if (isSelected) AniLifeColors.PrimaryForeground else AniLifeColors.Foreground,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                            )
                                            Text(
                                                entry.anime.title,
                                                color = if (isSelected) AniLifeColors.PrimaryForeground else AniLifeColors.MutedForeground,
                                                fontSize = 8.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                            PageScrollbar(episodeGridState, Modifier.align(Alignment.CenterEnd))
                        }
                    }

                    Box(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AniLifeColors.Secondary)
                            .border(1.dp, AniLifeColors.Border, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                    ) {
                        Text(
                            "Estimated output: ~$estimatedGigabytes GB",
                            color = AniLifeColors.MutedForeground,
                            fontSize = 13.sp,
                        )
                    }
                }
                PageScrollbar(scrollState, Modifier.align(Alignment.CenterEnd))
                }

                HorizontalDivider(color = AniLifeColors.Border)
                Row(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AniLifeColors.Secondary,
                            contentColor = AniLifeColors.Foreground,
                        ),
                    ) { Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                    Button(
                        onClick = { onConfirm(selected, mode) },
                        enabled = selected.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AniLifeColors.Primary,
                            contentColor = AniLifeColors.PrimaryForeground,
                            disabledContainerColor = AniLifeColors.Secondary,
                            disabledContentColor = AniLifeColors.MutedForeground,
                        ),
                    ) {
                        Text(
                            "${mode.label} · ${selected.size} ${if (selected.size == 1) "Episode" else "Episodes"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpscaleChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(18.dp))
            .background(AniLifeColors.Secondary)
            .border(1.dp, AniLifeColors.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        Text(label, color = AniLifeColors.Foreground, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun upscaleKey(entry: LocalEpisode): String = entry.path.toAbsolutePath().normalize().toString()

private fun formatEpisode(episode: Double): String =
    if (episode % 1.0 == 0.0) episode.toInt().toString() else episode.toString()
