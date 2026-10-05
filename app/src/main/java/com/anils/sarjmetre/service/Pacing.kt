package com.anils.sarjmetre.service

import android.os.PowerManager
import kotlin.math.max

/** When the meter service samples and reposts; plain functions so they can be tested without a device. */
object Pacing {
    /** Slowest interval used while the phone is warm. */
    const val HOT_INTERVAL_MS = 5_000L

    /** Icon changes smaller than this wait (the user picked ±10 mA). */
    const val ICON_THRESHOLD_MA = 10

    /** How long text-only notification changes (voltage, temperature, times) may wait. */
    const val TEXT_EVERY_MS = 30_000L

    /** From thermal status MODERATE up the timer slows to [HOT_INTERVAL_MS], never faster than the user's choice. */
    fun intervalMs(userMs: Long, thermalStatus: Int): Long =
        if (thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE) max(userMs, HOT_INTERVAL_MS) else userMs

    /**
     * Samsung sends ACTION_BATTERY_CHANGED often. While the timer runs, only plugging, unplugging
     * or a charging status change are worth an update right away.
     */
    fun isUrgent(oldPlugged: Int?, oldStatus: Int?, plugged: Int, status: Int): Boolean =
        oldPlugged != plugged || oldStatus != status
}
