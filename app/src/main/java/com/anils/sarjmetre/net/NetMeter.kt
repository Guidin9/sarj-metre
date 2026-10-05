package com.anils.sarjmetre.net

import android.content.Context
import android.net.TrafficStats
import android.os.SystemClock

/** Latest network reading for the icon, the notification and the screen. [today] is null without usage access. */
data class NetState(val speed: Speed?, val today: DayUsage?)

/**
 * Live speed from the since-boot byte counters on every tick, plus today's use from the system
 * statistics, refreshed every few minutes. Used from the main thread only.
 */
class NetMeter(context: Context) {
    val history = UsageHistory(context)
    private val speedMeter = SpeedMeter()
    private var today: DayUsage? = null
    private var todayAtMs = 0L
    private var todayFresh = false

    /** Null when the phone does not report traffic counters. */
    fun sample(): NetState? {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) return null
        return NetState(speedMeter.update(SystemClock.elapsedRealtime(), rx, tx), today)
    }

    /** True when today's use should be read again; the caller does that off the main thread. */
    fun todayDue(): Boolean = !todayFresh || SystemClock.elapsedRealtime() - todayAtMs >= TODAY_EVERY_MS

    fun setToday(usage: DayUsage?) {
        today = usage
        todayAtMs = SystemClock.elapsedRealtime()
        todayFresh = true
    }

    /** After the screen was off, the next difference would average over the whole gap. */
    fun pause() {
        speedMeter.reset()
        todayFresh = false
    }

    private companion object {
        const val TODAY_EVERY_MS = 5 * 60_000L
    }
}
