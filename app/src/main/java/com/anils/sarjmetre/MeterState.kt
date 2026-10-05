package com.anils.sarjmetre

import com.anils.sarjmetre.battery.CapacitySource

/** Everything the notification and the screen show, already normalized. Current is + into the battery. */
data class MeterState(
    val currentMa: Int?,
    val level: Int,
    val status: Int,
    val plugged: Int,
    val voltageMv: Int?,
    val temperatureC: Float?,
    val capacityMah: Double,
    val capacitySource: CapacitySource,
    val targetLevel: Int,
    val minutesToFull: Int?,
    val minutesToEmpty: Int?,
    val systemMinutesToFull: Int?,
    val todayChargedMah: Double,
    val todayDischargedMah: Double,
    val todayChargedPct: Int,
    val todayDischargedPct: Int,
    val currentRaw: Int?,
    val counterRaw: Int?,
    val unitIsMicro: Boolean,
    /** null while the automatic sign detection is still undecided. */
    val signInverted: Boolean?,
) {
    val isPlugged: Boolean get() = plugged != 0

    /** Battery-side power in watts, signed like [currentMa]. */
    val powerW: Double?
        get() = if (currentMa != null && voltageMv != null) currentMa.toDouble() * voltageMv / 1_000_000 else null
}
