package com.anils.sarjmetre.battery

import com.anils.sarjmetre.settings.SignMode
import com.anils.sarjmetre.settings.UnitMode
import kotlin.math.abs

/** Where the results of the automatic unit and sign detection are remembered. */
interface DetectionStore {
    var detectedMicroamps: Boolean
    var detectedInverted: Boolean?
}

/**
 * Turns BATTERY_PROPERTY_CURRENT_NOW into signed mA, + flowing into the battery.
 * The API promises µA with + while charging, but vendors differ: Samsung usually
 * reports mA and some phones flip the sign. Both are detected once and remembered.
 */
class CurrentNormalizer(private val store: DetectionStore) {
    private var lastVote = 0
    private var streak = 0

    fun toMilliamps(raw: Int, plugged: Boolean, unitMode: UnitMode, signMode: SignMode): Int {
        val micro = when (unitMode) {
            UnitMode.MICROAMPS -> true
            UnitMode.MILLIAMPS -> false
            UnitMode.AUTO -> {
                // No phone battery carries 20 A, so a reading this large can only be µA.
                if (!store.detectedMicroamps && abs(raw) >= MICROAMP_THRESHOLD) store.detectedMicroamps = true
                store.detectedMicroamps
            }
        }
        val ma = if (micro) raw / 1000 else raw
        val inverted = when (signMode) {
            SignMode.NORMAL -> false
            SignMode.INVERTED -> true
            SignMode.AUTO -> store.detectedInverted ?: detectSign(ma, plugged)
        }
        return if (inverted) -ma else ma
    }

    fun reset() {
        lastVote = 0
        streak = 0
    }

    /** Unplugged, current can only leave the battery, so a steady positive reading means the sign is flipped. */
    private fun detectSign(ma: Int, plugged: Boolean): Boolean {
        if (plugged || abs(ma) < SIGN_MIN_MA) return false
        val vote = if (ma > 0) 1 else -1
        streak = if (vote == lastVote) streak + 1 else 1
        lastVote = vote
        if (streak < SIGN_STREAK) return false
        store.detectedInverted = vote > 0
        return vote > 0
    }

    companion object {
        const val MICROAMP_THRESHOLD = 20_000
        const val SIGN_MIN_MA = 30
        const val SIGN_STREAK = 5
    }
}
