package com.anils.sarjmetre.net

/** Bytes per second in each direction. */
data class Speed(val downBps: Double, val upBps: Double) {
    val totalBps: Double get() = downBps + upBps
}

/** Speed from the change in the since-boot byte counters between two timer ticks. */
class SpeedMeter {
    private var lastMs = 0L
    private var lastRx = -1L
    private var lastTx = -1L

    /** Null on the first sample after a [reset] and whenever the counters went backwards. */
    fun update(nowMs: Long, rxBytes: Long, txBytes: Long): Speed? {
        val dtMs = nowMs - lastMs
        val speed = if (lastRx < 0 || dtMs <= 0 || rxBytes < lastRx || txBytes < lastTx) {
            null
        } else {
            Speed((rxBytes - lastRx) * 1000.0 / dtMs, (txBytes - lastTx) * 1000.0 / dtMs)
        }
        lastMs = nowMs
        lastRx = rxBytes
        lastTx = txBytes
        return speed
    }

    /** After the screen was off, the next difference would average over the whole gap. */
    fun reset() {
        lastRx = -1L
        lastTx = -1L
    }
}
