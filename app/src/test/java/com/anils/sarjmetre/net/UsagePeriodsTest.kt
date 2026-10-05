package com.anils.sarjmetre.net

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class UsagePeriodsTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    // Monday, 5 October 2026, 14:20
    private val now = ZonedDateTime.of(2026, 10, 5, 14, 20, 0, 0, zone)

    @Test
    fun todayHasOneBarPerHourSoFar() {
        val slots = UsagePeriods.slots(UsagePeriod.TODAY, now)
        assertEquals(15, slots.size)
        assertEquals("00", slots.first().label)
        assertEquals("14", slots.last().label)
        assertEquals(3_600_000L, slots.last().endMs - slots.last().startMs)
        assertEquals(now.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli(), slots.first().startMs)
    }

    @Test
    fun weekIsTheLastSevenDaysEndingToday() {
        val slots = UsagePeriods.slots(UsagePeriod.WEEK, now)
        assertEquals(listOf("Sal", "Çar", "Per", "Cum", "Cmt", "Paz", "Pzt"), slots.map { it.label })
        assertEquals(slots[0].endMs, slots[1].startMs)
    }

    @Test
    fun monthRunsFromTheFirst() {
        val slots = UsagePeriods.slots(UsagePeriod.MONTH, now)
        assertEquals((1..5).map { it.toString() }, slots.map { it.label })
    }
}
