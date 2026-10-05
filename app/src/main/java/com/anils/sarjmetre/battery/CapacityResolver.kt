package com.anils.sarjmetre.battery

import android.annotation.SuppressLint
import android.content.Context
import com.anils.sarjmetre.settings.AppSettings
import kotlin.math.abs

enum class CapacitySource { MANUAL, COUNTER, DESIGN, DEFAULT }

/** Capacity for the time estimates: manual setting, learned from CHARGE_COUNTER, design value, or a guess. */
class CapacityResolver(private val context: Context, private val settings: AppSettings) {
    private var learned = settings.learnedCapacityMah.toDouble()
    private val design: Double? by lazy { readDesignCapacity() }

    fun update(counterRaw: Int?, level: Int) {
        val estimate = counterRaw?.let { ChargeMath.capacityFromCounter(it, level) } ?: return
        // Level comes in whole percents, so single estimates wobble by about a percent; average them out.
        learned = if (learned <= 0) estimate else learned + (estimate - learned) * 0.02
        if (abs(learned - settings.learnedCapacityMah) >= 10) settings.learnedCapacityMah = learned.toFloat()
    }

    fun current(): Pair<Double, CapacitySource> {
        val manual = settings.capacityOverrideMah
        return when {
            manual in 500..30_000 -> manual.toDouble() to CapacitySource.MANUAL
            learned > 0 -> learned to CapacitySource.COUNTER
            else -> design?.let { it to CapacitySource.DESIGN } ?: (DEFAULT_MAH to CapacitySource.DEFAULT)
        }
    }

    /** Design capacity from the hidden PowerProfile class, which many battery apps rely on; may be missing. */
    @SuppressLint("PrivateApi")
    private fun readDesignCapacity(): Double? = runCatching {
        val profileClass = Class.forName("com.android.internal.os.PowerProfile")
        val profile = profileClass.getConstructor(Context::class.java).newInstance(context)
        profileClass.getMethod("getBatteryCapacity").invoke(profile) as Double
    }.getOrNull()?.takeIf { it in 1000.0..30_000.0 }

    private companion object {
        const val DEFAULT_MAH = 4000.0
    }
}
