package com.anils.sarjmetre.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeedMeterTest {
    private val meter = SpeedMeter()

    @Test
    fun speedFromCounterChange() {
        assertNull(meter.update(0, 1_000, 500))
        val s = meter.update(2_000, 1_000 + 2_048_000, 500 + 20_480)!!
        assertEquals(1_024_000.0, s.downBps, 1e-6)
        assertEquals(10_240.0, s.upBps, 1e-6)
        assertEquals(1_034_240.0, s.totalBps, 1e-6)
    }

    @Test
    fun resetAndRebootGiveNoSpeed() {
        meter.update(0, 10_000, 10_000)
        meter.reset()
        assertNull(meter.update(60_000, 900_000, 900_000))
        assertNull(meter.update(62_000, 100, 100))
        assertEquals(50.0, meter.update(64_000, 200, 100)!!.downBps, 1e-6)
    }
}
