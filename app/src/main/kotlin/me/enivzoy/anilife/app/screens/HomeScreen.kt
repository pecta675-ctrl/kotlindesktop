package me.enivzoy.anilife.app.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.distinctUntilChanged
import me.enivzoy.anilife.app.data.AniListRepository
import me.enivzoy.anilife.app.data.AppSettings
import me.enivzoy.anilife.app.data.AniListListStatus
import me.enivzoy.anilife.app.data.AniListSync
import me.enivzoy.anilife.app.data.ContinueStoreInstance
import me.enivzoy.anilife.app.data.HomeSection
import me.enivzoy.anilife.app.data.isUpscaled
import me.enivzoy.anilife.app.ui.AniLifeColors
import me.enivzoy.anilife.app.ui.AnimePosterCard
import me.enivzoy.anilife.app.ui.adaptiveGlass
import me.enivzoy.anilife.app.ui.appEnter
import me.enivzoy.anilife.app.ui.appTween
import me.enivzoy.anilife.app.ui.AnimeRow
import me.enivzoy.anilife.app.ui.ContinueCard
import me.enivzoy.anilife.app.ui.ErrorPanel
import me.enivzoy.anilife.app.ui.Hero
import me.enivzoy.anilife.app.ui.LoadState
import me.enivzoy.anilife.app.ui.PageScrollbar
import me.enivzoy.anilife.app.ui.PosterSkeleton
import me.enivzoy.anilife.app.ui.Shimmer
import me.enivzoy.anilife.app.ui.loadState
import me.enivzoy.anilife.core.model.AnimeCard
import me.enivzoy.anilife.core.model.AnimeDetail
import me.enivzoy.anilife.core.model.Home

@Composable
fun HomeScreen(
    repository: AniListRepository,
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    onOpen: (Long) -> Unit,
    onContinue: (me.enivzoy.anilife.app.data.ContinueEntry) -> Unit = { entry -> onOpen(entry.anime.id) },
    onContentReady: () -> Unit = {},
) {
    var attempt by remember { mutableStateOf(0) }
    var state by remember(repository) {
        mutableStateOf<LoadState<Home>>(repository.cachedHome()?.let { LoadState.Ready(it) } ?: LoadState.Loading)
    }

    LaunchedEffect(attempt) {
        val cached = repository.cachedHome()
        if (cached == null || attempt > 0) state = LoadState.Loading
        when (val result = loadState { repository.home(force = cached != null || attempt > 0) }) {
            is LoadState.Ready -> state = result
            is LoadState.Failed -> if (cached == null || attempt > 0) state = result
            LoadState.Loading -> Unit
        }
    }

    LaunchedEffect(state) {
        if (state is LoadState.Ready || state is LoadState.Failed) onContentReady()
    }

    when (val current = state) {
        LoadState.Loading -> HomeSkeleton()
        is LoadState.Failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ErrorPanel(message = current.message, onRetry = { attempt++ })
        }
        is LoadState.Ready -> HomeContent(current.value, repository, settings, onSettingsChange, onOpen, onContinue)
    }
}

@Composable
private fun HomeContent(
    home: Home,
    repository: AniListRepository,
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    onOpen: (Long) -> Unit,
    onContinue: (me.enivzoy.anilife.app.data.ContinueEntry) -> Unit,
) {
    val continueEntries by ContinueStoreInstance.entries.collectAsState()
    val libraryEntries by repository.library.entries.collectAsState()
    val localEpisodes by repository.localEpisodes.collectAsState()
    val upscaledIds = remember(localEpisodes) {
        localEpisodes.filter { it.isUpscaled() }.mapTo(mutableSetOf()) { it.anime.id }
    }
    val aniListEntries by AniListSync.entries.collectAsState()
    LaunchedEffect(aniListEntries) {
        aniListEntries.filter { it.status == AniListListStatus.CURRENT }.forEach { entry ->
            ContinueStoreInstance.upsertFromAniList(entry.anime, entry.progress)
        }
    }
    val featured = remember(home) {
        home.trending.filter { !it.banner.isNullOrBlank() }
            .ifEmpty { home.trending }
            .take(6)
    }
    val spotlightItems = remember(home) { home.trending.take(12).ifEmpty { home.newest.take(12) } }
    var heroAnime by remember { mutableStateOf<AnimeCard?>(spotlightItems.firstOrNull() ?: featured.firstOrNull()) }
    val spotlightState = rememberLazyListState()
    LaunchedEffect(spotlightState, spotlightItems) {
        snapshotFlow { spotlightState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val item = spotlightItems.getOrNull(index)
                if (item != null) heroAnime = item
            }
    }

    var heroDetail by remember { mutableStateOf<AnimeDetail?>(null) }
    LaunchedEffect(heroAnime) {
        val anime = heroAnime ?: return@LaunchedEffect
        heroDetail = runCatching { repository.detail(anime.id) }.getOrNull()
    }

    var becauseSeed by remember { mutableStateOf<AnimeCard?>(null) }
    var becauseItems by remember { mutableStateOf<List<AnimeCard>>(emptyList()) }
    LaunchedEffect(libraryEntries) {
        val seed = libraryEntries.firstOrNull()
        becauseSeed = seed
        becauseItems = if (seed != null) {
            runCatching { repository.detail(seed.id).recommendations.take(12) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    }

    val listState = rememberLazyListState()
    var showArrangementDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            val heroItems = heroAnime?.let { listOf(it) } ?: featured.take(1)
            if (heroItems.isNotEmpty()) {
                item(key = "featured") {
                    Hero(
                        items = heroItems,
                        loading = false,
                        onOpen = onOpen,
                        synopsis = heroDetail?.description?.replace(Regex("<[^>]*>"), "")?.trim(),
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            settings.homeSectionOrder.forEach { section ->
                if (section in settings.hiddenHomeSections) return@forEach
                when (section) {
                    HomeSection.CONTINUE_WATCHING -> if (continueEntries.isNotEmpty()) {
                        item(key = "continue") {
                            Text(
                                text = section.label,
                                color = AniLifeColors.Foreground,
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 32.dp),
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(top = 12.dp),
                            ) {
                                items(continueEntries, key = { "${it.anime.id}-${it.updatedAt}" }) { entry ->
                                    ContinueCard(
                                        entry = entry,
                                        onResume = { e -> onContinue(e) },
                                        onDetails = onOpen,
                                        onRemove = { id -> ContinueStoreInstance.remove(id) },
                                        upscaled = entry.anime.id in upscaledIds,
                                    )
                                }
                            }
                            Spacer(Modifier.height(38.dp))
                        }
                    }

                    HomeSection.BECAUSE_YOU_WATCHED -> if (becauseSeed != null && becauseItems.isNotEmpty()) {
                        item(key = "because") {
                            AnimeRow(
                                title = "Because you watched ${becauseSeed!!.title}",
                                items = becauseItems,
                                onOpen = onOpen,
                                upscaledIds = upscaledIds,
                            )
                            Spacer(Modifier.height(38.dp))
                        }
                    }

                    HomeSection.NEW_EPISODES -> addHomeRow(section, home.newest, onOpen, upscaledIds)
                    HomeSection.TRENDING_NOW -> if (spotlightItems.isNotEmpty()) {
                        item(key = "spotlight") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Rounded.DragHandle,
                                        contentDescription = null,
                                        tint = AniLifeColors.MutedForeground.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        section.label,
                                        color = AniLifeColors.Foreground,
                                        fontSize = 18.sp,
                                        lineHeight = 24.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(Modifier.weight(1f))
                                }
                                LazyRow(
                                    state = spotlightState,
                                    contentPadding = PaddingValues(horizontal = 32.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    items(spotlightItems, key = { it.id }) { anime ->
                                        AnimePosterCard(
                                            anime = anime,
                                            onClick = { onOpen(anime.id); heroAnime = anime },
                                            upscaled = anime.id in upscaledIds,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(38.dp))
                        }
                    }

                    HomeSection.THIS_SEASON -> addHomeRow(section, home.seasonal, onOpen, upscaledIds)
                    HomeSection.ALL_TIME_POPULAR -> addHomeRow(section, home.popular, onOpen, upscaledIds)
                    HomeSection.TOP_RATED -> addHomeRow(section, home.topRated, onOpen, upscaledIds)
                    HomeSection.MOVIES -> addHomeRow(section, home.movies, onOpen, upscaledIds)
                    HomeSection.COMING_SOON -> addHomeRow(section, home.upcoming, onOpen, upscaledIds)
                    HomeSection.MY_LIBRARY -> if (libraryEntries.isNotEmpty()) {
                        item(key = "library") {
                            AnimeRow(title = section.label, items = libraryEntries, onOpen = onOpen, upscaledIds = upscaledIds)
                            Spacer(Modifier.height(38.dp))
                        }
                    }
                }
            }

            item(key = "footer") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    OutlinedButton(onClick = { showArrangementDialog = true }) {
                        Icon(
                            Icons.Rounded.DragHandle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Arrange Home", fontSize = 13.sp)
                    }
                }
            }
        }
        PageScrollbar(rememberScrollbarAdapter(listState), Modifier.align(Alignment.CenterEnd))
    }

    if (showArrangementDialog) {
        HomeArrangementDialog(
            order = settings.homeSectionOrder,
            hiddenSections = settings.hiddenHomeSections,
            onDismiss = { showArrangementDialog = false },
            onApply = { order, hidden ->
                onSettingsChange {
                    it.copy(homeSectionOrder = order, hiddenHomeSections = hidden)
                }
                showArrangementDialog = false
            },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.addHomeRow(
    section: HomeSection,
    items: List<AnimeCard>,
    onOpen: (Long) -> Unit,
    upscaledIds: Set<Long>,
) {
    if (items.isEmpty()) return
    item(key = section.name) {
        AnimeRow(title = section.label, items = items, onOpen = onOpen, upscaledIds = upscaledIds)
        Spacer(Modifier.height(38.dp))
    }
}

@Composable
private fun HomeArrangementDialog(
    order: List<HomeSection>,
    hiddenSections: Set<HomeSection>,
    onDismiss: () -> Unit,
    onApply: (List<HomeSection>, Set<HomeSection>) -> Unit,
) {
    var draftOrder by remember(order) { mutableStateOf(HomeSection.normalizeOrder(order)) }
    var hidden by remember(hiddenSections) { mutableStateOf(hiddenSections) }
    val defaultOrder = HomeSection.defaultOrder
    val shape = RoundedCornerShape(18.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(AniLifeColors.Card)
                    .adaptiveGlass(shape = shape)
                    .appEnter()
                    .border(1.dp, AniLifeColors.Border, shape)
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Arrange Home",
                        Modifier.weight(1f),
                        color = AniLifeColors.Foreground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = AniLifeColors.MutedForeground,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    "Rows with nothing in them are skipped either way.",
                    color = AniLifeColors.MutedForeground,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )

                val sectionScrollState = rememberScrollState()
                Box(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                Column(
                    Modifier.fillMaxSize()
                        .verticalScroll(sectionScrollState),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    draftOrder.forEachIndexed { index, section ->
                        Row(
                            Modifier.fillMaxWidth().height(36.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                section.label,
                                Modifier.weight(1f),
                                color = AniLifeColors.Foreground,
                                fontSize = 13.sp,
                                maxLines = 1,
                            )
                            IconButton(
                                enabled = index > 0,
                                onClick = {
                                    draftOrder = draftOrder.toMutableList().apply {
                                        add(index - 1, removeAt(index))
                                    }
                                },
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowUpward,
                                    contentDescription = "Move ${section.label} up",
                                    tint = if (index > 0) AniLifeColors.Foreground else AniLifeColors.MutedForeground.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            IconButton(
                                enabled = index < draftOrder.lastIndex,
                                onClick = {
                                    draftOrder = draftOrder.toMutableList().apply {
                                        add(index + 1, removeAt(index))
                                    }
                                },
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowDownward,
                                    contentDescription = "Move ${section.label} down",
                                    tint = if (index < draftOrder.lastIndex) AniLifeColors.Foreground else AniLifeColors.MutedForeground.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            HomeSectionSwitch(section !in hidden) {
                                hidden = if (section in hidden) hidden - section else hidden + section
                            }
                        }
                    }
                }
                PageScrollbar(sectionScrollState, Modifier.align(Alignment.CenterEnd))
                }

                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(AniLifeColors.Border))
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            draftOrder = defaultOrder
                            hidden = emptySet()
                        },
                    ) {
                        Text("Reset", color = AniLifeColors.Foreground)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onApply(draftOrder, hidden) }) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionSwitch(checked: Boolean, onCheckedChange: () -> Unit) {
    val knobOffset by animateDpAsState(if (checked) 16.dp else 2.dp, appTween(220), label = "home-section-switch")
    Box(
        Modifier.size(width = 34.dp, height = 20.dp)
            .clip(CircleShape)
            .background(if (checked) AniLifeColors.Primary else AniLifeColors.Muted)
            .toggleable(checked, role = Role.Switch) { onCheckedChange() },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.padding(start = knobOffset)
                .size(16.dp)
                .clip(CircleShape)
                .background(if (checked) AniLifeColors.PrimaryForeground else AniLifeColors.Foreground),
        )
    }
}

@Composable
private fun HomeSkeleton() {
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            item {
                Shimmer(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    shape = RoundedCornerShape(0.dp),
                )
            }
            item { Spacer(Modifier.height(36.dp)) }
            items(3, key = { it }) { SkeletonShelf() }
        }
        PageScrollbar(rememberScrollbarAdapter(listState), Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun SkeletonShelf() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Shimmer(
                modifier = Modifier.width(150.dp).height(18.dp),
                shape = RoundedCornerShape(4.dp),
            )
            Spacer(Modifier.weight(1f))
            Shimmer(Modifier.width(60.dp).height(22.dp), CircleShape)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(8, key = { it }) { PosterSkeleton() }
        }
        Spacer(Modifier.height(26.dp))
    }
}
