package me.enivzoy.anilife.app.data

import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.nio.file.Path
import java.util.Properties

const val DEFAULT_DISCORD_APPLICATION_ID = "1567301811604553728"
val gpuAccelerationModes = setOf("auto", "off")

enum class HomeSection(val label: String) {
    CONTINUE_WATCHING("Continue watching"),
    BECAUSE_YOU_WATCHED("Because you watched..."),
    NEW_EPISODES("New episodes"),
    TRENDING_NOW("Trending now"),
    THIS_SEASON("This season"),
    ALL_TIME_POPULAR("All-time popular"),
    TOP_RATED("Top rated"),
    MOVIES("Movies"),
    COMING_SOON("Coming soon"),
    MY_LIBRARY("My library"),
    ;

    companion object {
        val defaultOrder: List<HomeSection> = entries.toList()

        fun parseOrder(value: String?): List<HomeSection> {
            val stored = value.orEmpty().split(',')
                .mapNotNull { name -> entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) } }
                .distinct()
            return stored + entries.filterNot(stored::contains)
        }

        fun parseHidden(value: String?): Set<HomeSection> =
            value.orEmpty().split(',')
                .mapNotNull { name -> entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) } }
                .toSet()

        fun normalizeOrder(order: List<HomeSection>): List<HomeSection> {
            val unique = order.distinct()
            return unique + entries.filterNot(unique::contains)
        }
    }
}

data class SubtitleAppearance(
    val fontFamily: String = "sans-serif",
    val fontFile: String = "",
    val scale: Float = 1f,
    val textColor: String = "#FFFFFF",
    val textOpacity: Float = 1f,
    val backgroundColor: String = "#000000",
    val backgroundOpacity: Float = 0f,
    val edgeStyle: String = "outline",
    val verticalPosition: String = "bottom",
    val verticalOffset: Float = 3f,
    val alignment: String = "center",
)

data class AppSettings(
    val audio: String = "sub",
    val subtitleLanguage: String = "en",
    val subtitlesByDefault: Boolean = true,
    val autoplayNextEpisode: Boolean = true,
    val resumePlayback: Boolean = true,
    val playbackBufferSize: String = "auto",
    val gpuAcceleration: String = "auto",
    val contentScale: String = "fit",
    val screenshotResolution: String = "native",
    val showSkipButton: Boolean = true,
    val skipButtonSeconds: Int = 85,
    val autoSkipIntro: Boolean = false,
    val autoSkipOutro: Boolean = false,
    val pauseDetails: Boolean = false,
    val volume: Int = 100,
    val subtitleScale: Float = 1f,
    val aniListSubmitPercent: Int = 80,
    /** Add titles saved by a restore or an import to the signed-in account's Plan to Watch. */
    val syncSavedToAccount: Boolean = true,
    val checkUpdatesOnLaunch: Boolean = true,
    val updateChannel: UpdateChannel = UpdateChannel.RELEASE,
    val captionFont: String = "sans-serif",
    val captionFontFile: String = "",
    val buttonColor: String = "#87D66A",
    val backgroundColor: String = "#0A0F0A",
    val animeCardColor: String = "#111A11",
    val downloadProgressColor: String = "#87D66A",
    val checkboxColor: String = "#87D66A",
    val checkboxTickColor: String = "#0A0F0A",
    val sidebarIconBackgroundColor: String = "#1A261A",
    val themeCss: String = "",
    val sidebarIconBackgroundEnabled: Boolean = false,
    val sidebarIconShape: String = "rounded",
    val sidebarIconsJoined: Boolean = false,
    val appFont: String = "SansSerif",
    val appFontFile: String = "",
    val downloadDirectory: String? = null,
    val playIntro: Boolean = true,
    val titleLanguage: String = "english",
    val discordPresenceEnabled: Boolean = true,
    val highRefreshRateUi: Boolean = false,
    val themeMode: String = "DEFAULT",
    val smoothScrolling: Boolean = true,
    val scrollbarVisibility: String = "AUTOMATIC",
    val scrollAnimationDuration: String = "NORMAL",
    val reducedMotion: Boolean = false,
    val glassBlurIntensity: Float = 1f,
    val glassTransparency: Float = 0.5f,
    val glassHighlightIntensity: Float = 1f,
    val discordApplicationId: String = DEFAULT_DISCORD_APPLICATION_ID,
    val homeSectionOrder: List<HomeSection> = HomeSection.defaultOrder,
    val hiddenHomeSections: Set<HomeSection> = emptySet(),
    val captionTextColor: String = "#FFFFFF",
    val captionTextOpacity: Float = 1f,
    val captionBackgroundColor: String = "#000000",
    val captionBackgroundOpacity: Float = 0f,
    val captionEdgeStyle: String = "outline",
    val captionVerticalPosition: String = "bottom",
    val captionVerticalOffset: Float = 3f,
    val captionAlignment: String = "center",
    val captionPreset: String = "classic",
    val keyBindings: Map<PlayerAction, Key> = DefaultPlayerKeyBindings,
    val expDebouncedSearch: Boolean = true,
    val expImageCaching: Boolean = true,
    val expAsyncFetching: Boolean = false,
    val expPagination: Boolean = false,
    val expFadeTransitions: Boolean = true,
    val expHardwareScrolling: Boolean = true,
    val expMpvBuffering: Boolean = true,
    val expSmartDownloads: Boolean = false,
    val expSmartDownloadsStartAfter: Int = 2,
    val autoDetectEnabled: Boolean = false,
    val autoDetectDirectories: List<String> = emptyList(),
    val autoDetectSettleSeconds: Int = 45,
) {
    val subtitleAppearance: SubtitleAppearance
        get() = SubtitleAppearance(
            fontFamily = captionFont,
            fontFile = captionFontFile,
            scale = subtitleScale,
            textColor = captionTextColor,
            textOpacity = captionTextOpacity,
            backgroundColor = captionBackgroundColor,
            backgroundOpacity = captionBackgroundOpacity,
            edgeStyle = captionEdgeStyle,
            verticalPosition = captionVerticalPosition,
            verticalOffset = captionVerticalOffset,
            alignment = captionAlignment,
        )
}

class AppSettingsStore(private val file: File = settingsFile()) {
    private val lock = Any()
    private val mutableSettings = MutableStateFlow(readSettings())
    val settings = mutableSettings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) = synchronized(lock) {
        val updated = transform(mutableSettings.value).validated()
        writeSettings(updated)
        mutableSettings.value = updated
    }

    private fun readSettings(): AppSettings {
        if (!file.isFile) return AppSettings()
        val properties = Properties()
        file.inputStream().use(properties::load)
        return AppSettings(
            audio = properties.getProperty("audio", "sub"),
            subtitleLanguage = properties.getProperty("subtitleLanguage", "en"),
            subtitlesByDefault = properties.getProperty("subtitlesByDefault", "true").toBooleanStrictOrNull() ?: true,
            autoplayNextEpisode = properties.getProperty("autoplayNextEpisode", "true").toBooleanStrictOrNull() ?: true,
            aniListSubmitPercent = properties.getProperty("aniListSubmitPercent", "80")
                .toIntOrNull()?.coerceIn(1, 100) ?: 80,
            syncSavedToAccount = properties.getProperty("syncSavedToAccount", "true").toBooleanStrictOrNull() ?: true,
            resumePlayback = properties.getProperty("resumePlayback", "true").toBooleanStrictOrNull() ?: true,
            playbackBufferSize = properties.getProperty("playbackBufferSize", "auto"),
            gpuAcceleration = properties.getProperty("gpuAcceleration")
                ?: if (properties.getProperty("hardwareDecoding") == "false") "off" else "auto",
            contentScale = properties.getProperty("contentScale", "fit"),
            screenshotResolution = properties.getProperty("screenshotResolution", "native"),
            showSkipButton = properties.getProperty("showSkipButton", "true").toBooleanStrictOrNull() ?: true,
            skipButtonSeconds = properties.getProperty("skipButtonSeconds", "85").toIntOrNull() ?: 85,
            autoSkipIntro = properties.getProperty("autoSkipIntro", "false").toBooleanStrictOrNull() ?: false,
            autoSkipOutro = properties.getProperty("autoSkipOutro", "false").toBooleanStrictOrNull() ?: false,
            pauseDetails = properties.getProperty("pauseDetails", "false").toBooleanStrictOrNull() ?: false,
            volume = properties.getProperty("volume", "100").toIntOrNull() ?: 100,
            subtitleScale = properties.getProperty("subtitleScale", "1.0").toFloatOrNull() ?: 1f,
            checkUpdatesOnLaunch = properties.getProperty("checkUpdatesOnLaunch", "true").toBooleanStrictOrNull() ?: true,
            updateChannel = UpdateChannel.parse(properties.getProperty("updateChannel")),
            captionFont = properties.getProperty("captionFont", "sans-serif"),
            captionFontFile = properties.getProperty("captionFontFile", ""),
            buttonColor = properties.getProperty("buttonColor", "#87D66A"),
            backgroundColor = properties.getProperty("backgroundColor", "#0A0F0A"),
            animeCardColor = properties.getProperty("animeCardColor", "#111A11"),
            downloadProgressColor = properties.getProperty("downloadProgressColor", "#87D66A"),
            checkboxColor = properties.getProperty("checkboxColor", "#87D66A"),
            checkboxTickColor = properties.getProperty("checkboxTickColor", "#0A0F0A"),
            sidebarIconBackgroundColor = properties.getProperty("sidebarIconBackgroundColor", "#1A261A"),
            themeCss = properties.getProperty("themeCss", ""),
            sidebarIconBackgroundEnabled = properties.getProperty("sidebarIconBackgroundEnabled", "false").toBooleanStrictOrNull() ?: false,
            sidebarIconShape = properties.getProperty("sidebarIconShape", "rounded"),
            sidebarIconsJoined = properties.getProperty("sidebarIconsJoined", "false").toBooleanStrictOrNull() ?: false,
            appFont = properties.getProperty("appFont", "SansSerif"),
            appFontFile = properties.getProperty("appFontFile", ""),
            downloadDirectory = properties.getProperty("downloadDirectory")?.takeIf(String::isNotBlank),
            playIntro = properties.getProperty("playIntro", "true").toBooleanStrictOrNull() ?: true,
            titleLanguage = properties.getProperty("titleLanguage") ?: if (properties.getProperty("preferEnglishTitles", "true").toBooleanStrictOrNull() ?: true) "english" else "romaji",
            discordPresenceEnabled = properties.getProperty("discordPresenceEnabled", "true").toBooleanStrictOrNull() ?: true,
            highRefreshRateUi = properties.getProperty("highRefreshRateUi", "false").toBooleanStrictOrNull() ?: false,
            themeMode = properties.getProperty("themeMode", "DEFAULT"),
            smoothScrolling = properties.getProperty("smoothScrolling", "true").toBooleanStrictOrNull() ?: true,
            scrollbarVisibility = properties.getProperty("scrollbarVisibility", "AUTOMATIC"),
            scrollAnimationDuration = properties.getProperty("scrollAnimationDuration", "NORMAL"),
            reducedMotion = properties.getProperty("reducedMotion", "false").toBooleanStrictOrNull() ?: false,
            glassBlurIntensity = properties.getProperty("glassBlurIntensity", "1.0").toFloatOrNull() ?: 1f,
            glassTransparency = properties.getProperty("glassTransparency", "0.5").toFloatOrNull() ?: 0.5f,
            glassHighlightIntensity = properties.getProperty("glassHighlightIntensity", "1.0").toFloatOrNull() ?: 1f,
            discordApplicationId = properties.getProperty("discordApplicationId", DEFAULT_DISCORD_APPLICATION_ID),
            homeSectionOrder = HomeSection.parseOrder(properties.getProperty("homeSectionOrder")),
            hiddenHomeSections = HomeSection.parseHidden(properties.getProperty("hiddenHomeSections")),
            captionTextColor = properties.getProperty("captionTextColor", "#FFFFFF"),
            captionTextOpacity = properties.getProperty("captionTextOpacity", "1.0").toFloatOrNull() ?: 1f,
            captionBackgroundColor = properties.getProperty("captionBackgroundColor", "#000000"),
            captionBackgroundOpacity = properties.getProperty("captionBackgroundOpacity", "0.0").toFloatOrNull() ?: 0f,
            captionEdgeStyle = properties.getProperty("captionEdgeStyle", "outline"),
            captionVerticalPosition = properties.getProperty("captionVerticalPosition", "bottom"),
            captionVerticalOffset = properties.getProperty("captionVerticalOffset", "3.0").toFloatOrNull() ?: 3f,
            captionAlignment = properties.getProperty("captionAlignment", "center"),
            captionPreset = properties.getProperty("captionPreset", "classic"),
            keyBindings = parsePlayerKeyBindings(properties.getProperty("playerKeyBindings")),
            expDebouncedSearch = properties.getProperty("expDebouncedSearch", "true").toBooleanStrictOrNull() ?: true,
            expImageCaching = properties.getProperty("expImageCaching", "true").toBooleanStrictOrNull() ?: true,
            expAsyncFetching = properties.getProperty("expAsyncFetching", "false").toBooleanStrictOrNull() ?: false,
            expPagination = properties.getProperty("expPagination", "false").toBooleanStrictOrNull() ?: false,
            expFadeTransitions = properties.getProperty("expFadeTransitions", "true").toBooleanStrictOrNull() ?: true,
            expHardwareScrolling = properties.getProperty("expHardwareScrolling", "true").toBooleanStrictOrNull() ?: true,
            expMpvBuffering = properties.getProperty("expMpvBuffering", "true").toBooleanStrictOrNull() ?: true,
            expSmartDownloads = properties.getProperty("expSmartDownloads", "false").toBooleanStrictOrNull() ?: false,
            expSmartDownloadsStartAfter = properties.getProperty("expSmartDownloadsStartAfter", "2")
                .toIntOrNull()?.coerceIn(0, 3) ?: 2,
            autoDetectEnabled = properties.getProperty("autoDetectEnabled", "false").toBooleanStrictOrNull() ?: false,
            autoDetectDirectories = parseDirectories(properties.getProperty("autoDetectDirectories")),
            autoDetectSettleSeconds = properties.getProperty("autoDetectSettleSeconds", "45")
                .toIntOrNull()?.coerceIn(10, 600) ?: 45,
        ).validated()
    }

    private fun writeSettings(settings: AppSettings) {
        file.parentFile?.mkdirs()
        val properties = Properties().apply {
            setProperty("audio", settings.audio)
            setProperty("subtitleLanguage", settings.subtitleLanguage)
            setProperty("subtitlesByDefault", settings.subtitlesByDefault.toString())
            setProperty("autoplayNextEpisode", settings.autoplayNextEpisode.toString())
            setProperty("aniListSubmitPercent", settings.aniListSubmitPercent.toString())
            setProperty("syncSavedToAccount", settings.syncSavedToAccount.toString())
            setProperty("resumePlayback", settings.resumePlayback.toString())
            setProperty("playbackBufferSize", settings.playbackBufferSize)
            setProperty("gpuAcceleration", settings.gpuAcceleration)
            setProperty("contentScale", settings.contentScale)
            setProperty("screenshotResolution", settings.screenshotResolution)
            setProperty("showSkipButton", settings.showSkipButton.toString())
            setProperty("skipButtonSeconds", settings.skipButtonSeconds.toString())
            setProperty("autoSkipIntro", settings.autoSkipIntro.toString())
            setProperty("autoSkipOutro", settings.autoSkipOutro.toString())
            setProperty("pauseDetails", settings.pauseDetails.toString())
            setProperty("volume", settings.volume.toString())
            setProperty("subtitleScale", settings.subtitleScale.toString())
            setProperty("checkUpdatesOnLaunch", settings.checkUpdatesOnLaunch.toString())
            setProperty("updateChannel", settings.updateChannel.name)
            setProperty("captionFont", settings.captionFont)
            setProperty("captionFontFile", settings.captionFontFile)
            setProperty("buttonColor", settings.buttonColor)
            setProperty("backgroundColor", settings.backgroundColor)
            setProperty("animeCardColor", settings.animeCardColor)
            setProperty("downloadProgressColor", settings.downloadProgressColor)
            setProperty("checkboxColor", settings.checkboxColor)
            setProperty("checkboxTickColor", settings.checkboxTickColor)
            setProperty("sidebarIconBackgroundColor", settings.sidebarIconBackgroundColor)
            setProperty("themeCss", settings.themeCss)
            setProperty("sidebarIconBackgroundEnabled", settings.sidebarIconBackgroundEnabled.toString())
            setProperty("sidebarIconShape", settings.sidebarIconShape)
            setProperty("sidebarIconsJoined", settings.sidebarIconsJoined.toString())
            setProperty("appFont", settings.appFont)
            setProperty("appFontFile", settings.appFontFile)
            settings.downloadDirectory?.let { setProperty("downloadDirectory", it) }
            setProperty("playIntro", settings.playIntro.toString())
            setProperty("titleLanguage", settings.titleLanguage)
            setProperty("discordPresenceEnabled", settings.discordPresenceEnabled.toString())
            setProperty("highRefreshRateUi", settings.highRefreshRateUi.toString())
            setProperty("themeMode", settings.themeMode)
            setProperty("smoothScrolling", settings.smoothScrolling.toString())
            setProperty("scrollbarVisibility", settings.scrollbarVisibility)
            setProperty("scrollAnimationDuration", settings.scrollAnimationDuration)
            setProperty("reducedMotion", settings.reducedMotion.toString())
            setProperty("glassBlurIntensity", settings.glassBlurIntensity.toString())
            setProperty("glassTransparency", settings.glassTransparency.toString())
            setProperty("glassHighlightIntensity", settings.glassHighlightIntensity.toString())
            setProperty("discordApplicationId", settings.discordApplicationId)
            setProperty("homeSectionOrder", settings.homeSectionOrder.joinToString(",") { it.name })
            setProperty("hiddenHomeSections", settings.hiddenHomeSections.joinToString(",") { it.name })
            setProperty("captionTextColor", settings.captionTextColor)
            setProperty("captionTextOpacity", settings.captionTextOpacity.toString())
            setProperty("captionBackgroundColor", settings.captionBackgroundColor)
            setProperty("captionBackgroundOpacity", settings.captionBackgroundOpacity.toString())
            setProperty("captionEdgeStyle", settings.captionEdgeStyle)
            setProperty("captionVerticalPosition", settings.captionVerticalPosition)
            setProperty("captionVerticalOffset", settings.captionVerticalOffset.toString())
            setProperty("captionAlignment", settings.captionAlignment)
            setProperty("captionPreset", settings.captionPreset)
            setProperty("playerKeyBindings", serializePlayerKeyBindings(settings.keyBindings))
            setProperty("expDebouncedSearch", settings.expDebouncedSearch.toString())
            setProperty("expImageCaching", settings.expImageCaching.toString())
            setProperty("expAsyncFetching", settings.expAsyncFetching.toString())
            setProperty("expPagination", settings.expPagination.toString())
            setProperty("expFadeTransitions", settings.expFadeTransitions.toString())
            setProperty("expHardwareScrolling", settings.expHardwareScrolling.toString())
            setProperty("expMpvBuffering", settings.expMpvBuffering.toString())
            setProperty("expSmartDownloads", settings.expSmartDownloads.toString())
            setProperty("expSmartDownloadsStartAfter", settings.expSmartDownloadsStartAfter.toString())
            setProperty("autoDetectEnabled", settings.autoDetectEnabled.toString())
            setProperty("autoDetectDirectories", settings.autoDetectDirectories.joinToString("\n"))
            setProperty("autoDetectSettleSeconds", settings.autoDetectSettleSeconds.toString())
        }
        file.outputStream().use { properties.store(it, null) }
    }

    private fun AppSettings.validated(): AppSettings = copy(
        audio = if (audio == "dub") "dub" else "sub",
        subtitleLanguage = subtitleLanguage.takeIf { it in supportedSubtitleLanguages } ?: "en",
        playbackBufferSize = playbackBufferSize.takeIf { it in bufferSizes } ?: "auto",
        gpuAcceleration = gpuAcceleration.takeIf { it in gpuAccelerationModes } ?: "auto",
        contentScale = contentScale.takeIf { it in contentScales } ?: "fit",
        screenshotResolution = screenshotResolution.takeIf { it in screenshotResolutions } ?: "native",
        skipButtonSeconds = skipButtonSeconds.coerceIn(10, 300),
        volume = volume.coerceIn(0, 100),
        subtitleScale = subtitleScale.takeIf(Float::isFinite)?.coerceIn(0.5f, 2f) ?: 1f,
        captionFont = captionFont.trim().take(128).ifBlank { "sans-serif" }
            .let { if (it.equals("SansSerif", ignoreCase = true)) "sans-serif" else it },
        buttonColor = validColor(buttonColor, "#87D66A"),
        backgroundColor = validColor(backgroundColor, "#0A0F0A"),
        animeCardColor = validColor(animeCardColor, "#111A11"),
        downloadProgressColor = validColor(downloadProgressColor, "#87D66A"),
        checkboxColor = validColor(checkboxColor, "#87D66A"),
        checkboxTickColor = validColor(checkboxTickColor, "#0A0F0A"),
        sidebarIconBackgroundColor = validColor(sidebarIconBackgroundColor, "#1A261A"),
        themeCss = themeCss.take(20_000),
        sidebarIconShape = sidebarIconShape.takeIf { it in sidebarShapes } ?: "rounded",
        appFont = appFont.takeIf { it in fontFamilies } ?: "SansSerif",
        themeMode = themeMode.takeIf { it in themeModes } ?: "DEFAULT",
        scrollbarVisibility = scrollbarVisibility.takeIf { it in scrollbarVisibilities } ?: "AUTOMATIC",
        scrollAnimationDuration = scrollAnimationDuration.takeIf { it in scrollAnimationDurations } ?: "NORMAL",
        glassBlurIntensity = glassBlurIntensity.takeIf(Float::isFinite)?.coerceIn(0.5f, 2f) ?: 1f,
        glassTransparency = glassTransparency.takeIf(Float::isFinite)?.coerceIn(0.25f, 0.85f) ?: 0.5f,
        glassHighlightIntensity = glassHighlightIntensity.takeIf(Float::isFinite)?.coerceIn(0f, 1.5f) ?: 1f,
        captionFontFile = captionFontFile.take(2048),
        appFontFile = appFontFile.take(2048),
        downloadDirectory = downloadDirectory?.let { runCatching { Path.of(it).toAbsolutePath().normalize().toString() }.getOrNull() },
        discordApplicationId = discordApplicationId.filter(Char::isDigit).take(20),
        homeSectionOrder = HomeSection.normalizeOrder(homeSectionOrder),
        hiddenHomeSections = hiddenHomeSections.intersect(HomeSection.entries.toSet()),
        captionTextColor = validColor(captionTextColor, "#FFFFFF"),
        captionTextOpacity = captionTextOpacity.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 1f,
        captionBackgroundColor = validColor(captionBackgroundColor, "#000000"),
        captionBackgroundOpacity = captionBackgroundOpacity.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0f,
        captionEdgeStyle = captionEdgeStyle.takeIf { it in captionEdgeStyles } ?: "outline",
        captionVerticalPosition = captionVerticalPosition.takeIf { it in captionVerticalPositions } ?: "bottom",
        captionVerticalOffset = captionVerticalOffset.takeIf(Float::isFinite)?.coerceIn(0f, 30f) ?: 3f,
        captionAlignment = captionAlignment.takeIf { it in captionAlignments } ?: "center",
        captionPreset = captionPreset.takeIf { it in captionPresets } ?: "custom",
        keyBindings = keyBindings.withPlayerDefaults(),
        expSmartDownloadsStartAfter = expSmartDownloadsStartAfter.coerceIn(0, 3),
        autoDetectDirectories = autoDetectDirectories
            .map { it.trim() }
            .filter(String::isNotBlank)
            .mapNotNull { runCatching { Path.of(it).toAbsolutePath().normalize().toString() }.getOrNull() }
            .distinct(),
        autoDetectSettleSeconds = autoDetectSettleSeconds.coerceIn(10, 600),
    )

    companion object {
        val bufferSizes = listOf("auto", "small", "medium", "large")
        val contentScales = listOf("fit", "crop", "fill")
        val screenshotResolutions = listOf("native", "4k", "8k")
        val supportedSubtitleLanguages = listOf("en", "ja", "es", "pt", "fr", "de", "it", "ru", "ar", "zh", "none")
        val fontFamilies = listOf("SansSerif", "Serif", "Monospaced", "Cursive")
        val themeModes = setOf("DEFAULT", "FROSTED_GLASS", "LIQUID_GLASS")
        val scrollbarVisibilities = setOf("AUTOMATIC", "ALWAYS", "HIDDEN")
        val scrollAnimationDurations = setOf("SHORT", "NORMAL", "LONG")
        val captionEdgeStyles = setOf("none", "outline", "shadow")
        val captionVerticalPositions = setOf("top", "bottom")
        val captionAlignments = setOf("left", "center", "right")
        val captionPresets = setOf("classic", "boxed", "cinema", "high-contrast", "custom")
        val sidebarShapes = listOf("rounded", "circle", "rectangular")
        private fun validColor(value: String, fallback: String): String =
            value.trim().uppercase().takeIf { it.matches(Regex("#[0-9A-F]{6}([0-9A-F]{2})?")) } ?: fallback

        private fun parseDirectories(value: String?): List<String> =
            value.orEmpty().split('\n')
                .map(String::trim)
                .filter(String::isNotBlank)

        private fun settingsFile(): File {
            val base = System.getenv("LOCALAPPDATA")?.takeIf(String::isNotBlank)?.let(::File)
                ?: File(System.getProperty("user.home"), ".local/share")
            return File(base, "AniLife/settings.properties")
        }
    }
}

