package com.anils.sarjmetre.battery

import kotlin.math.roundToInt

/** Recent current samples, cleared on plug/unplug so charging and discharging are never averaged together. */
class CurrentHistory {
    private class Sample(val timeMs: Long, val ma: Int)

    private val samples = ArrayDeque<Sample>()
    private var plugged: Boolean? = null

    fun add(timeMs: Long, ma: Int, plugged: Boolean) {
        if (plugged != this.plugged) samples.clear()
        this.plugged = plugged
        samples.addLast(Sample(timeMs, ma))
        while (timeMs - samples.first().timeMs > KEEP_MS) samples.removeFirst()
    }

    /** Mean of the newest few samples: steady enough to read, still live. */
    fun recent(count: Int = 3): Int? =
        samples.takeLast(count).takeIf { it.isNotEmpty() }?.map { it.ma }?.average()?.roundToInt()

    /** Mean over the last [windowMs], used for the time estimates. */
    fun average(windowMs: Long): Double? {
        val newest = samples.lastOrNull() ?: return null
        return samples.filter { newest.timeMs - it.timeMs <= windowMs }.map { it.ma }.average()
    }

    fun clear() {
        samples.clear()
        plugged = null
    }

    private companion object {
        const val KEEP_MS = 5 * 60_000L
    }
}
