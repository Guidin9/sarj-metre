package com.anils.sarjmetre.stats

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DailyTotalsTrackerTest {
    private class MemoryStore : DailyTotalsStore {
        var saved: DailyTotals? = null
        var saves = 0
        override fun load() = saved
        override fun save(totals: DailyTotals) {
            saved = totals
            saves++
        }
    }

    private val day1 = LocalDate.of(2026, 10, 5)
    private val day2 = day1.plusDays(1)
    private val store = MemoryStore()
    private val tracker = DailyTotalsTracker(store)

    @Test
    fun countsLevelStepsBothWays() {
        tracker.onSample(day1, 0, 50, null)
        tracker.onSample(day1, 1_000, 48, null)
        tracker.onSample(day1, 2_000, 60, null)
        val t = tracker.onSample(day1, 3_000, 57, null)
        assertEquals(12, t.chargedPct)
        assertEquals(5, t.dischargedPct)
    }

    @Test
    fun counterJitterBelowThresholdIsIgnored() {
        tracker.onSample(day1, 0, 50, 2500.0)
        tracker.onSample(day1, 1_000, 50, 2503.0)
        tracker.onSample(day1, 2_000, 50, 2498.0)
        val t = tracker.onSample(day1, 3_000, 50, 2501.0)
        assertEquals(0.0, t.chargedMah, 1e-9)
        assertEquals(0.0, t.dischargedMah, 1e-9)
    }

    @Test
    fun counterMovesCommitAgainstTheAnchor() {
        tracker.onSample(day1, 0, 50, 2500.0)
        tracker.onSample(day1, 1_000, 50, 2496.0) // within noise, anchor stays 2500
        tracker.onSample(day1, 2_000, 50, 2490.0) // −10 mAh
        val t = tracker.onSample(day1, 3_000, 51, 2550.0) // +60 mAh
        assertEquals(60.0, t.chargedMah, 1e-9)
        assertEquals(10.0, t.dischargedMah, 1e-9)
    }

    @Test
    fun newDayStartsFromZeroButKeepsAnchors() {
        tracker.onSample(day1, 0, 50, 2500.0)
        tracker.onSample(day1, 1_000, 40, 2000.0)
        // The drop across midnight belongs to the new day.
        val t = tracker.onSample(day2, 2_000, 35, 1750.0)
        assertEquals(day2.toString(), t.day)
        assertEquals(5, t.dischargedPct)
        assertEquals(250.0, t.dischargedMah, 1e-9)
        assertEquals(0, t.chargedPct)
    }

    @Test
    fun restartContinuesFromSavedState() {
        tracker.onSample(day1, 0, 50, 2500.0)
        tracker.onSample(day1, 1_000, 45, 2250.0)
        val restarted = DailyTotalsTracker(store)
        val t = restarted.onSample(day1, 0, 40, 2000.0)
        assertEquals(10, t.dischargedPct)
        assertEquals(500.0, t.dischargedMah, 1e-9)
    }

    @Test
    fun counterOnlyChangesAreThrottled() {
        tracker.onSample(day1, 0, 50, 2500.0)
        val before = store.saves
        tracker.onSample(day1, 1_000, 50, 2510.0)
        tracker.onSample(day1, 2_000, 50, 2520.0)
        assertEquals(before, store.saves)
        tracker.onSample(day1, 40_000, 50, 2530.0)
        assertEquals(before + 1, store.saves)
        assertEquals(30.0, store.saved!!.chargedMah, 1e-9)
    }

    @Test
    fun resetTodayKeepsAnchors() {
        tracker.onSample(day1, 0, 50, 2500.0)
        tracker.onSample(day1, 1_000, 60, 3000.0)
        tracker.resetToday(day1)
        val t = tracker.onSample(day1, 2_000, 61, 3050.0)
        assertEquals(1, t.chargedPct)
        assertEquals(50.0, t.chargedMah, 1e-9)
    }
}
