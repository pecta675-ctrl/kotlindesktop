package me.enivzoy.anilife.app.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.renderComposeScene
import me.enivzoy.anilife.app.data.AppSettings
import me.enivzoy.anilife.app.data.LocalEpisode
import me.enivzoy.anilife.app.data.UpscaleMode
import me.enivzoy.anilife.app.ui.AniLifeTheme
import me.enivzoy.anilife.core.model.AnimeCard
import me.enivzoy.anilife.core.net.AniListClient
import me.enivzoy.anilife.app.ui.RemoteImages
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class UpscalePickerRenderTest {
    @TempDir
    lateinit var root: Path

    @Test
    fun libraryEpisodesAndEmptyStatesRenderAtDesktopAndCompactSizes() {
        val artwork = if (System.getenv("ANILIFE_PICKER_REAL_COVERS") == "true") runBlocking {
            AniListClient().cardsByIds(listOf(124080, 113415, 154587, 140960, 145064, 21827)).also { cards ->
                cards.forEach { it.cover?.let { url -> RemoteImages.load(url) } }
            }.associateBy { it.id }
        } else emptyMap()
        val ids = listOf(124080L, 113415L, 154587L, 140960L, 145064L, 21827L)
        val groups = groupUpscaleEpisodes(
            listOf("Horimiya", "JUJUTSU KAISEN", "Frieren: Beyond Journey's End", "SPY × FAMILY", "The Apothecary Diaries", "Violet Evergarden")
                .flatMapIndexed { index, title ->
                    (1..14).map { number ->
                        val card = artwork[ids[index]] ?: AnimeCard(ids[index], title, color = "#4A6758")
                        LocalEpisode(card, number.toDouble(), "sub", root.resolve("$index-$number.mkv"), 100, "Episode title $number")
                    }
                },
        )
        val selectedKeys = groups.first().episodes.take(3).mapTo(mutableSetOf(), ::upscaleKey)
        for ((width, height) in listOf(940 to 760, 580 to 680, 400 to 600)) {
            for (view in listOf("covers", "episodes", "empty")) {
                renderComposeScene(width, height) {
                    AniLifeTheme(AppSettings()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            UpscalePickerContent(
                                groups = if (view == "empty") emptyList() else groups,
                                active = if (view == "episodes") groups.first() else null,
                                selectedKeys = selectedKeys,
                                selectedCount = if (view == "empty") 0 else selectedKeys.size,
                                mode = UpscaleMode.Fast4K,
                                onOpenAnime = {}, onBack = {}, onToggle = {}, onSelectShow = {}, onClearShow = {},
                                onClearAll = {}, onModeChange = {}, onDismiss = {}, onConfirm = {},
                            )
                        }
                    }
                }.use { image ->
                    val bytes = image.encodeToData()!!.use { it.bytes }
                    assertTrue(bytes.size > 1000)
                    val output = System.getenv("ANILIFE_PICKER_PREVIEW_DIR")?.let(Path::of) ?: root
                    Files.createDirectories(output)
                    Files.write(output.resolve("upscale-$view-${width}x$height.png"), bytes)
                }
            }
        }
    }
}
