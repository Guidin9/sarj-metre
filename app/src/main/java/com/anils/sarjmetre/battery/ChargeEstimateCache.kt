package com.anils.sarjmetre.battery

/**
 * The phone's own time-to-full is a call into BatteryStats, so it is asked at most every [everyMs]
 * while charging and counted down in between. Not charging, it is never asked.
 */
class ChargeEstimateCache(private val everyMs: Long, private val query: () -> Long?) {
    private var valueMs: Long? = null
    private var readAtMs = 0L
    private var fresh = false

    fun get(nowMs: Long, charging: Boolean): Long? {
        if (!charging) {
            fresh = false
            valueMs = null
            return null
        }
        if (!fresh || nowMs - readAtMs >= everyMs) {
            valueMs = query()
            readAtMs = nowMs
            fresh = true
        }
        return valueMs?.let { it - (nowMs - readAtMs) }?.takeIf { it > 0 }
    }
}
