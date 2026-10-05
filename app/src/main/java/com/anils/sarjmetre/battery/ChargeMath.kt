package com.anils.sarjmetre.battery

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/** Charge and capacity arithmetic, free of Android types so it can be unit-tested. */
object ChargeMath {
    /** Level where phones leave constant-current charging and the current starts to taper. */
    const val CV_START_LEVEL = 80

    /** Assumed current at 100 % as a share of the constant-current value. Tuned against the phone's own estimate. */
    const val END_CURRENT_RATIO = 0.2

    /**
     * Hours from [level] to [target] % at net charging current [currentMa].
     * Constant current up to 80 %, then the current falls linearly to its end value at 100 %.
     */
    fun hoursToTarget(level: Int, target: Int, currentMa: Double, capacityMah: Double): Double? {
        if (currentMa <= 0.0 || capacityMah <= 0.0) return null
        if (level >= target) return 0.0
        val taperSpan = 100 - CV_START_LEVEL
        // Past 80 % the measured current is already on the slope; work back to the constant-current value.
        val constant = if (level <= CV_START_LEVEL) currentMa
        else currentMa / (1 - (1 - END_CURRENT_RATIO) * (level - CV_START_LEVEL) / taperSpan)
        val end = min(max(constant * END_CURRENT_RATIO, capacityMah / 20), constant)

        fun currentAt(at: Int): Double =
            if (at <= CV_START_LEVEL) constant else constant + (end - constant) * (at - CV_START_LEVEL) / taperSpan

        var hours = 0.0
        if (level < CV_START_LEVEL) {
            hours += capacityMah * (min(target, CV_START_LEVEL) - level) / 100 / constant
        }
        if (target > CV_START_LEVEL) {
            val from = max(level, CV_START_LEVEL)
            val startCurrent = if (level > CV_START_LEVEL) currentMa else constant
            hours += taperHours(capacityMah * (target - from) / 100, startCurrent, currentAt(target))
        }
        return hours
    }

    fun hoursToEmpty(level: Int, dischargeMa: Double, capacityMah: Double): Double? =
        if (dischargeMa <= 0.0 || capacityMah <= 0.0) null else capacityMah * level / 100 / dischargeMa

    /** Full capacity in mAh from CHARGE_COUNTER, or null when the reading is too coarse or implausible. */
    fun capacityFromCounter(counterRaw: Int, level: Int): Double? {
        if (level < 30 || counterRaw <= 0) return null
        return (counterToMah(counterRaw) * 100 / level).takeIf { it in 1000.0..30_000.0 }
    }

    /** CHARGE_COUNTER should be µAh but some devices report mAh; no phone holds 100 Ah. */
    fun counterToMah(counterRaw: Int): Double =
        if (counterRaw >= 100_000) counterRaw / 1000.0 else counterRaw.toDouble()

    /** Time for [deltaMah] while the current falls linearly from [from] to [to] mA (the integral of dQ/I). */
    private fun taperHours(deltaMah: Double, from: Double, to: Double): Double =
        if (abs(from - to) < 1e-6) deltaMah / from else deltaMah * ln(from / to) / (from - to)
}
