package me.enivzoy.anilife.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

enum class UpscaleStatus { Queued, Upscaling, Completed, Failed, Cancelled }

data class UpscaleJob(
    val id: String = UUID.randomUUID().toString(),
    val localEpisode: LocalEpisode,
    val title: String,
    val episode: Double,
    val status: UpscaleStatus = UpscaleStatus.Queued,
    val progress: Float = 0f,
    val error: String? = null,
    val outputPath: Path? = null,
    val activity: UpscaleProgress = UpscaleProgress(),
    val mode: UpscaleMode = UpscaleMode.AIQuality,
)

class UpscaleQueueManager(
    private val repository: AniListRepository,
    private val scope: CoroutineScope,
    private val concurrency: Int = 1,
) {
    private val _jobs = MutableStateFlow<List<UpscaleJob>>(emptyList())
    val jobs: StateFlow<List<UpscaleJob>> = _jobs.asStateFlow()

    // ncnn-vulkan saturates the gpu even with a single job running
    private val semaphore = Semaphore(concurrency.coerceAtLeast(1))
    private val handles = mutableMapOf<String, Job>()
    private val upscaler = AnimeUpscaler()

    fun enqueue(episodes: List<LocalEpisode>, mode: UpscaleMode = UpscaleMode.AIQuality) {
        episodes.forEach { entry ->
            val job = UpscaleJob(
                localEpisode = entry,
                title = entry.anime.title,
                episode = entry.episode,
                mode = mode,
            )
            _jobs.update { it + job }
            launchJob(job)
        }
    }

    private fun launchJob(job: UpscaleJob) {
        val handle = scope.launch {
            semaphore.withPermit {
                if (!isActive) return@withPermit
                update(job.id) { copy(status = UpscaleStatus.Upscaling, progress = 0f, error = null) }
                try {
                    val output = upscaler.upscale(job.localEpisode, job.mode) { activity ->
                        update(job.id) { copy(progress = activity.fraction.coerceIn(0f, 1f), activity = activity) }
                    }
                    update(job.id) { copy(status = UpscaleStatus.Completed, progress = 1f, outputPath = output) }
                    // a different audio label keeps the 4k file beside the original in the local store
                    repository.addLocalEpisodes(
                        listOf(
                            job.localEpisode.copy(
                                path = output,
                                audio = job.mode.audioTag,
                                bytes = runCatching { Files.size(output) }.getOrDefault(0L),
                            ),
                        ),
                    )
                } catch (e: CancellationException) {
                    update(job.id) { copy(status = UpscaleStatus.Cancelled) }
                    throw e
                } catch (t: Throwable) {
                    update(job.id) {
                        copy(
                            status = UpscaleStatus.Failed,
                            error = t.message?.takeIf(String::isNotBlank) ?: "Upscaling failed.",
                        )
                    }
                }
            }
        }
        handles[job.id] = handle
    }

    private fun update(id: String, change: UpscaleJob.() -> UpscaleJob) {
        _jobs.update { list -> list.map { if (it.id == id) it.change() else it } }
    }

    fun cancel(id: String) {
        handles[id]?.cancel()
        _jobs.update { list ->
            list.map {
                if (it.id == id && (it.status == UpscaleStatus.Queued || it.status == UpscaleStatus.Upscaling)) {
                    it.copy(status = UpscaleStatus.Cancelled)
                } else it
            }
        }
    }

    fun retry(id: String) {
        val job = _jobs.value.firstOrNull { it.id == id } ?: return
        if (job.status != UpscaleStatus.Failed && job.status != UpscaleStatus.Cancelled) return
        update(id) { copy(status = UpscaleStatus.Queued, progress = 0f, error = null, outputPath = null, activity = UpscaleProgress()) }
        launchJob(_jobs.value.first { it.id == id })
    }

    fun clearFinished() {
        _jobs.update { list ->
            list.filter {
                it.status != UpscaleStatus.Completed && it.status != UpscaleStatus.Failed && it.status != UpscaleStatus.Cancelled
            }
        }
    }
}
