package me.enivzoy.anilife.app.screens

import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import me.enivzoy.anilife.core.model.AiringEntry
import me.enivzoy.anilife.core.model.AnimeCard

class ScheduleScreenTest {
    @Test
    fun dateWindowShowsTwoPastDaysAndTheNextSixDays() {
        val today = LocalDate.of(2026, 11, 15)

        val dates = scheduleDateWindow(today)

        assertEquals(9, dates.size)
        assertEquals(today.minusDays(2), dates.first())
        assertEquals(today, dates[2])
        assertEquals(today.plusDays(6), dates.last())
    }

    @Test
    fun scheduleFiltersMatchFormatAndLocalLibrary() {
        val tv = AiringEntry(1_800_000_000, 3, AnimeCard(id = 1, title = "Series", format = "TV"))
        val movie = AiringEntry(1_800_000_100, 1, AnimeCard(id = 2, title = "Film", format = "MOVIE"))

        val filtered = filterScheduleEntries(
            entries = listOf(tv, movie),
            formatFilters = setOf("TV"),
            libraryIds = setOf(1),
            onlyMyList = true,
        )

        assertEquals(listOf(tv), filtered)
        assertEquals(
            listOf(tv, movie),
            filterScheduleEntries(listOf(tv, movie), emptySet(), emptySet(), onlyMyList = false),
        )
    }

    @Test
    fun futureDatesLoadTheirOwnWindowInsteadOfTheCurrentWeek() {
        val future = LocalDate.of(2027, 6, 15)
        val zone = ZoneId.of("Asia/Karachi")
        val (from, until) = scheduleRangeBounds(future, zone)
        assertEquals(future.minusDays(2).atStartOfDay(zone).toEpochSecond(), from)
        assertEquals(future.plusDays(7).atStartOfDay(zone).toEpochSecond(), until)
    }

    @Test
    fun dateBoundsRespectDaylightSavingRatherThanAssuming24HourDays() {
        val (from, until) = scheduleRangeBounds(LocalDate.of(2027, 3, 26), ZoneId.of("Europe/Berlin"))
        assertEquals((9 * 24 - 1) * 3600L, until - from)
    }

    @Test
    fun remoteListFilteringUsesEachServicesOwnIdAndUnionsBothLists() {
        val malOnly = AiringEntry(1_800_000_000, 1, AnimeCard(5, "MAL title", malId = 99, format = "TV"))
        val aniListOnly = AiringEntry(1_800_000_100, 2, AnimeCard(99, "AniList title", malId = 5, format = "MOVIE"))
        val other = AiringEntry(1_800_000_200, 3, AnimeCard(6, "Not on either list", malId = 6, format = "TV"))
        val entries = listOf(malOnly, aniListOnly, other)
        assertEquals(listOf(malOnly), filterScheduleEntries(entries, emptySet(), emptySet(), true, setOf(99)))
        assertEquals(listOf(aniListOnly), filterScheduleEntries(entries, emptySet(), setOf(99), true))
        assertEquals(listOf(malOnly, aniListOnly), filterScheduleEntries(entries, emptySet(), setOf(99), true, setOf(99)))
        assertEquals(listOf(malOnly), filterScheduleEntries(entries, setOf("TV"), setOf(99), true, setOf(99)))
        assertEquals(emptyList(), filterScheduleEntries(entries, emptySet(), emptySet(), true))
        assertEquals(entries, filterScheduleEntries(entries, emptySet(), emptySet(), false))
    }
}
