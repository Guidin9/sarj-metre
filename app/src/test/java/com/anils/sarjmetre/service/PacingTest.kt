package com.anils.sarjmetre.service

import android.os.BatteryManager
import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PacingTest {
    @Test
    fun warmPhoneSlowsToFiveSeconds() {
        assertEquals(2_000L, Pacing.intervalMs(2_000, PowerManager.THERMAL_STATUS_LIGHT))
        assertEquals(5_000L, Pacing.intervalMs(2_000, PowerManager.THERMAL_STATUS_MODERATE))
        assertEquals(5_000L, Pacing.intervalMs(2_000, PowerManager.THERMAL_STATUS_SEVERE))
    }

    @Test
    fun warmPhoneNeverSpeedsUpASlowerChoice() {
        assertEquals(10_000L, Pacing.intervalMs(10_000, PowerManager.THERMAL_STATUS_MODERATE))
    }

    @Test
    fun onlyPlugAndStatusChangesAreUrgent() {
        val charging = BatteryManager.BATTERY_STATUS_CHARGING
        val full = BatteryManager.BATTERY_STATUS_FULL
        val usb = BatteryManager.BATTERY_PLUGGED_USB
        assertFalse(Pacing.isUrgent(usb, charging, usb, charging))
        assertTrue(Pacing.isUrgent(0, BatteryManager.BATTERY_STATUS_DISCHARGING, usb, charging))
        assertTrue(Pacing.isUrgent(usb, charging, usb, full))
        assertTrue(Pacing.isUrgent(null, null, usb, charging))
    }
}
