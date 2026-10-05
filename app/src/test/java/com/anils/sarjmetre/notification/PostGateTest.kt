package com.anils.sarjmetre.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostGateTest {
    private val gate = PostGate(thresholdMa = 10, textEveryMs = 30_000)

    private fun content(icon: String, status: String = "Kullanım", details: String = "3,85 V") =
        MeterContent(icon, "mA", status, "title", "today", details)

    @Test
    fun iconHoldsSmallChanges() {
        assertEquals(-400, gate.iconMa(-400, plugged = false))
        assertEquals(-400, gate.iconMa(-409, plugged = false))
        assertEquals(-400, gate.iconMa(-391, plugged = false))
        assertEquals(-410, gate.iconMa(-410, plugged = false))
    }

    @Test
    fun iconFollowsSignFlipAndPlugChange() {
        gate.iconMa(3, plugged = true)
        assertEquals(-2, gate.iconMa(-2, plugged = true))
        assertEquals(-5, gate.iconMa(-5, plugged = false))
    }

    @Test
    fun iconFollowsMissingAndReturningValues() {
        gate.iconMa(-400, plugged = false)
        assertEquals(null, gate.iconMa(null, plugged = false))
        assertEquals(-401, gate.iconMa(-401, plugged = false))
    }

    @Test
    fun iconOrStatusChangePostsAtOnce() {
        assertTrue(gate.shouldPost(content("-400"), 0))
        assertFalse(gate.shouldPost(content("-400"), 1_000))
        assertTrue(gate.shouldPost(content("-410"), 2_000))
        assertTrue(gate.shouldPost(content("-410", status = "Şarj oluyor"), 3_000))
    }

    @Test
    fun textOnlyChangeWaits() {
        assertTrue(gate.shouldPost(content("-400"), 0))
        assertFalse(gate.shouldPost(content("-400", details = "3,84 V"), 10_000))
        assertTrue(gate.shouldPost(content("-400", details = "3,84 V"), 30_000))
        assertFalse(gate.shouldPost(content("-400", details = "3,84 V"), 90_000))
    }

    @Test
    fun resetPostsAgain() {
        gate.iconMa(-400, plugged = false)
        assertTrue(gate.shouldPost(content("-400"), 0))
        gate.reset()
        assertEquals(-405, gate.iconMa(-405, plugged = false))
        assertTrue(gate.shouldPost(content("-405"), 1_000))
    }
}
