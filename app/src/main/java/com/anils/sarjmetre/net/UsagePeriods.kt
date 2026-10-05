package com.anils.sarjmetre.net

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class UsagePeriod { TODAY, WEEK, MONTH }

/** One bar of the usage chart. */
data class UsageSlot(val startMs: Long, val endMs: Long, val label: String)

/** The chart's bars for a period: hours of today, the last seven days, or the days of this month. Plain time math, testable. */
object UsagePeriods {
    private val weekdays = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

    fun slots(period: UsagePeriod, now: ZonedDateTime): List<UsageSlot> {
        val zone = now.zone
        val today = now.toLocalDate()
        return when (period) {
            UsagePeriod.TODAY -> (0..now.hour).map { hour ->
                val start = today.atTime(hour, 0)
                UsageSlot(ms(start, zone), ms(start.plusHours(1), zone), hour.toString().padStart(2, '0'))
            }
            UsagePeriod.WEEK -> (6 downTo 0).map { back -> day(today.minusDays(back.toLong()), zone, weekdays[today.minusDays(back.toLong()).dayOfWeek.ordinal]) }
            UsagePeriod.MONTH -> (1..today.dayOfMonth).map { d -> day(today.withDayOfMonth(d), zone, d.toString()) }
        }
    }

    private fun day(date: LocalDate, zone: ZoneId, label: String) =
        UsageSlot(ms(date.atStartOfDay(), zone), ms(date.plusDays(1).atStartOfDay(), zone), label)

    private fun ms(time: LocalDateTime, zone: ZoneId) = time.atZone(zone).toInstant().toEpochMilli()
}
