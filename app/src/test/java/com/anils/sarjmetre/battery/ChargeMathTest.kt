package com.anils.sarjmetre.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.ln

class ChargeMathTest {
    private fun minutes(level: Int, target: Int, ma: Double, capacity: Double = 5000.0) =
        ChargeMath.hoursToTarget(level, target, ma, capacity)!! * 60

    @Test
    fun constantCurrentOnlyBelowEighty() {
        // 2500 mAh at 3000 mA
        assertEquals(50.0, minutes(30, 80, 3000.0), 0.01)
    }

    @Test
    fun fullChargeAddsTaperAboveEighty() {
        // 1000 mAh while the current falls from 3000 to 600 mA
        val taper = 1000 * ln(3000.0 / 600) / 2400 * 60
        assertEquals(50.0 + taper, minutes(30, 100, 3000.0), 0.01)
    }

    @Test
    fun chargeLimitStopsPartWayDownTheSlope() {
        // 80→85 %: current falls from 3000 to 2400 mA over 250 mAh
        val taper = 250 * ln(3000.0 / 2400) / 600 * 60
        assertEquals(50.0 + taper, minutes(30, 85, 3000.0), 0.01)
    }

    @Test
    fun alreadyTaperingInfersTheConstantCurrent() {
        // At 90 % with 1200 mA the slope started at 2000 mA, so it ends at 400 mA.
        val expected = 500 * ln(1200.0 / 400) / 800 * 60
        assertEquals(expected, minutes(90, 100, 1200.0), 0.01)
    }

    @Test
    fun atOrAboveTargetIsZero() {
        assertEquals(0.0, minutes(85, 85, 1000.0), 0.0)
        assertEquals(0.0, minutes(100, 100, 50.0), 0.0)
    }

    @Test
    fun noChargingCurrentGivesNoEstimate() {
        assertNull(ChargeMath.hoursToTarget(50, 100, 0.0, 5000.0))
        assertNull(ChargeMath.hoursToTarget(50, 100, -300.0, 5000.0))
    }

    @Test
    fun timeToEmpty() {
        assertEquals(5.0, ChargeMath.hoursToEmpty(50, 500.0, 5000.0)!!, 1e-9)
        assertNull(ChargeMath.hoursToEmpty(50, 0.0, 5000.0))
    }

    @Test
    fun counterUnitsAndCapacity() {
        assertEquals(3000.0, ChargeMath.counterToMah(3_000_000), 1e-9)
        assertEquals(3000.0, ChargeMath.counterToMah(3000), 1e-9)
        assertEquals(5000.0, ChargeMath.capacityFromCounter(2_500_000, 50)!!, 1e-9)
        assertEquals(5000.0, ChargeMath.capacityFromCounter(2500, 50)!!, 1e-9)
        assertNull(ChargeMath.capacityFromCounter(1_000_000, 20))
        assertNull(ChargeMath.capacityFromCounter(50, 50))
    }
}
