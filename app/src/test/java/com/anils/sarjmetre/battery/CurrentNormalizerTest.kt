package com.anils.sarjmetre.battery

import com.anils.sarjmetre.settings.SignMode
import com.anils.sarjmetre.settings.UnitMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentNormalizerTest {
    private class MemoryStore : DetectionStore {
        override var detectedMicroamps = false
        override var detectedInverted: Boolean? = null
    }

    private val store = MemoryStore()
    private val normalizer = CurrentNormalizer(store)

    private fun auto(raw: Int, plugged: Boolean) = normalizer.toMilliamps(raw, plugged, UnitMode.AUTO, SignMode.AUTO)

    @Test
    fun milliampDeviceStaysInMilliamps() {
        assertEquals(1850, auto(1850, plugged = true))
        assertEquals(-420, auto(-420, plugged = false))
        assertFalse(store.detectedMicroamps)
    }

    @Test
    fun largeReadingSwitchesToMicroampsForGood() {
        assertEquals(-350, auto(-350_000, plugged = false))
        assertTrue(store.detectedMicroamps)
        // A small µA value after detection is no longer mistaken for mA.
        assertEquals(15, auto(15_000, plugged = true))
    }

    @Test
    fun steadyPositiveWhileUnpluggedFlipsSign() {
        repeat(CurrentNormalizer.SIGN_STREAK - 1) { assertEquals(400, auto(400, plugged = false)) }
        assertEquals(-400, auto(400, plugged = false))
        assertEquals(true, store.detectedInverted)
        assertEquals(1500, auto(-1500, plugged = true))
    }

    @Test
    fun negativeWhileUnpluggedConfirmsNormalSign() {
        repeat(CurrentNormalizer.SIGN_STREAK) { auto(-400, plugged = false) }
        assertEquals(false, store.detectedInverted)
    }

    @Test
    fun pluggedAndTinyReadingsDoNotVote() {
        repeat(20) { auto(2000, plugged = true) }
        repeat(20) { auto(10, plugged = false) }
        assertNull(store.detectedInverted)
    }

    @Test
    fun brokenStreakStartsOver() {
        repeat(CurrentNormalizer.SIGN_STREAK - 1) { auto(400, plugged = false) }
        auto(-400, plugged = false)
        auto(400, plugged = false)
        assertNull(store.detectedInverted)
    }

    @Test
    fun manualModesOverrideDetection() {
        store.detectedMicroamps = true
        store.detectedInverted = true
        assertEquals(1200, normalizer.toMilliamps(1200, true, UnitMode.MILLIAMPS, SignMode.NORMAL))
        assertEquals(-1200, normalizer.toMilliamps(1_200_000, true, UnitMode.MICROAMPS, SignMode.INVERTED))
    }
}
