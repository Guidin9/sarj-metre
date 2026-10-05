package com.anils.sarjmetre.battery

import org.junit.Assert.assertEquals
import org.junit.Test

class ChargeEstimateCacheTest {
    private var queries = 0
    private var answer: Long? = 600_000
    private val cache = ChargeEstimateCache(everyMs = 30_000) { queries++; answer }

    @Test
    fun asksAtMostEveryThirtySecondsAndCountsDown() {
        assertEquals(600_000L, cache.get(0, charging = true))
        assertEquals(590_000L, cache.get(10_000, charging = true))
        assertEquals(1, queries)
        answer = 500_000
        assertEquals(500_000L, cache.get(30_000, charging = true))
        assertEquals(2, queries)
    }

    @Test
    fun neverAsksWhenNotCharging() {
        assertEquals(null, cache.get(0, charging = false))
        assertEquals(0, queries)
    }

    @Test
    fun asksAgainRightAfterReplugging() {
        cache.get(0, charging = true)
        cache.get(5_000, charging = false)
        cache.get(6_000, charging = true)
        assertEquals(2, queries)
    }

    @Test
    fun missingOrExpiredEstimateIsNull() {
        answer = null
        assertEquals(null, cache.get(0, charging = true))
        answer = 5_000
        cache.get(30_000, charging = true)
        assertEquals(null, cache.get(40_000, charging = true))
    }
}
