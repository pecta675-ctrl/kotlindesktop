package me.enivzoy.anilife.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.enivzoy.anilife.app.data.UpscaleJob
import me.enivzoy.anilife.app.data.UpscaleStatus
import me.enivzoy.anilife.app.data.formatUpscalePercent
import me.enivzoy.anilife.app.ui.AniLifeColors
import me.enivzoy.anilife.app.ui.PageScrollbar

@Composable
fun UpscaleProgressTab(
    jobs: List<UpscaleJob>,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onClearFinished: () -> Unit,
) {
    if (jobs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No upscale jobs yet. Use the Upscale button on the Downloads tab.",
                color = AniLifeColors.MutedForeground,
                fontSize = 13.sp,
            )
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        val listState = rememberLazyListState()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(jobs, key = { it.id }) { job ->
                    UpscaleJobCard(job, onCancel, onRetry)
                }
            }
            PageScrollbar(listState, Modifier.align(Alignment.CenterEnd))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClearFinished) {
                Text("Clear finished", color = AniLifeColors.MutedForeground, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun UpscaleJobCard(
    job: UpscaleJob,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AniLifeColors.Card),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        job.title,
                        color = AniLifeColors.Foreground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Episode ${formatUpscaleEpisode(job.episode)} · ${job.mode.label}",
                        color = AniLifeColors.MutedForeground,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    job.status.name,
                    color = when (job.status) {
                        UpscaleStatus.Failed -> AniLifeColors.Destructive
                        UpscaleStatus.Completed -> AniLifeColors.Success
                        else -> AniLifeColors.MutedForeground
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (job.status == UpscaleStatus.Upscaling) {
                LinearProgressIndicator(
                    progress = { job.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = AniLifeColors.Primary,
                    trackColor = AniLifeColors.Border,
                )
                Text(
                    "${formatUpscalePercent(job.progress)} · ${job.activity.stage.label}",
                    color = AniLifeColors.MutedForeground,
                    fontSize = 11.sp,
                )
                Text(
                    job.activity.detail,
                    color = AniLifeColors.MutedForeground,
                    fontSize = 11.sp,
                )
                if (job.activity.totalFrames > 0) {
                    val speed = job.activity.framesPerSecond?.let { " · %.3g frames/s".format(java.util.Locale.ROOT, it) }.orEmpty()
                    Text(
                        "${job.activity.completedFrames} / ${job.activity.totalFrames} frames$speed",
                        color = AniLifeColors.MutedForeground,
                        fontSize = 11.sp,
                    )
                }
                if (job.activity.secondsSinceOutput >= 120) {
                    Text(
                        "No process output for ${job.activity.secondsSinceOutput}s. The current step may be slow or stalled; check the AniLife log or cancel and retry.",
                        color = AniLifeColors.Warning,
                        fontSize = 11.sp,
                    )
                }
            }
            job.error?.let { message ->
                Text(message, color = AniLifeColors.Destructive, fontSize = 12.sp)
            }
            job.outputPath?.let { path ->
                Text(path.toString(), color = AniLifeColors.MutedForeground, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (job.status == UpscaleStatus.Queued || job.status == UpscaleStatus.Upscaling) {
                Spacer(Modifier.height(2.dp))
                Button(
                    onClick = { onCancel(job.id) },
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AniLifeColors.Secondary,
                        contentColor = AniLifeColors.Foreground,
                    ),
                ) { Text("Cancel", fontSize = 12.sp) }
            } else if (job.status == UpscaleStatus.Failed || job.status == UpscaleStatus.Cancelled) {
                TextButton(onClick = { onRetry(job.id) }) {
                    Text("Retry", color = AniLifeColors.Primary, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun formatUpscaleEpisode(episode: Double): String =
    if (episode % 1.0 == 0.0) episode.toInt().toString() else episode.toString()
