package com.anils.sarjmetre.stats

import java.time.LocalDate

data class DailyTotals(
    val day: String,
    val chargedPct: Int = 0,
    val dischargedPct: Int = 0,
    val chargedMah: Double = 0.0,
    val dischargedMah: Double = 0.0,
    // Anchors carry over across days and restarts, so nothing between two samples is lost.
    val lastLevel: Int? = null,
    val anchorMah: Double? = null,
)

interface DailyTotalsStore {
    fun load(): DailyTotals?
    fun save(totals: DailyTotals)
}

/** Today's charged and used amounts: the battery version of a data meter's daily WiFi/mobile totals. */
class DailyTotalsTracker(private val store: DailyTotalsStore) {
    private var totals: DailyTotals? = null
    private var lastSaveMs: Long? = null

    fun onSample(today: LocalDate, nowMs: Long, level: Int, counterMah: Double?): DailyTotals {
        val day = today.toString()
        val before = totals ?: store.load() ?: DailyTotals(day)
        var t = if (before.day == day) before else DailyTotals(day, lastLevel = before.lastLevel, anchorMah = before.anchorMah)

        t.lastLevel?.let { last ->
            if (level > last) t = t.copy(chargedPct = t.chargedPct + level - last)
            if (level < last) t = t.copy(dischargedPct = t.dischargedPct + last - level)
        }
        t = t.copy(lastLevel = level)

        if (counterMah != null) {
            val anchor = t.anchorMah
            // The counter jitters by a few mAh; only commit once it has really moved.
            t = when {
                anchor == null -> t.copy(anchorMah = counterMah)
                counterMah - anchor >= NOISE_MAH ->
                    t.copy(chargedMah = t.chargedMah + counterMah - anchor, anchorMah = counterMah)
                anchor - counterMah >= NOISE_MAH ->
                    t.copy(dischargedMah = t.dischargedMah + anchor - counterMah, anchorMah = counterMah)
                else -> t
            }
        }

        totals = t
        // Fast charging moves the counter every few seconds, so throttle those writes; level and day changes go out at once.
        // Totals and anchors are saved together, so anything not yet saved is simply counted again after a restart.
        val urgent = t.day != before.day || t.lastLevel != before.lastLevel
        val due = lastSaveMs?.let { nowMs - it >= SAVE_INTERVAL_MS } ?: true
        if (t != before && (urgent || due)) {
            store.save(t)
            lastSaveMs = nowMs
        }
        return t
    }

    fun resetToday(today: LocalDate) {
        val current = totals ?: store.load()
        val reset = DailyTotals(today.toString(), lastLevel = current?.lastLevel, anchorMah = current?.anchorMah)
        totals = reset
        store.save(reset)
    }

    companion object {
        const val NOISE_MAH = 5.0
        const val SAVE_INTERVAL_MS = 30_000L
    }
}
