package com.anils.sarjmetre.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat

/** One reading in whatever units the device reports; [CurrentNormalizer] fixes the current later. */
data class RawBattery(
    val elapsedMs: Long,
    val currentRaw: Int?,
    val counterRaw: Int?,
    val level: Int,
    val status: Int,
    val plugged: Int,
    val voltageMv: Int?,
    val temperatureC: Float?,
    /** The phone's own time-to-full estimate, when it offers one. */
    val systemFullMs: Long?,
)

class BatteryReader(context: Context) {
    private val appContext = context.applicationContext
    private val batteryManager = appContext.getSystemService(BatteryManager::class.java)
    private val systemFull = ChargeEstimateCache(SYSTEM_FULL_EVERY_MS) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) batteryManager.computeChargeTimeRemaining().takeIf { it > 0 } else null
    }

    /** [batteryIntent] is the latest ACTION_BATTERY_CHANGED if the caller already holds it. */
    fun read(batteryIntent: Intent? = null): RawBattery {
        val intent = batteryIntent ?: ContextCompat.registerReceiver(
            appContext,
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val level = intent?.let {
            val steps = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (steps >= 0 && scale > 0) steps * 100 / scale else null
        } ?: batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val now = SystemClock.elapsedRealtime()

        return RawBattery(
            elapsedMs = now,
            currentRaw = property(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
            counterRaw = property(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.takeIf { it > 0 },
            level = level.coerceIn(0, 100),
            status = status,
            plugged = plugged,
            // A few devices report volts instead of millivolts.
            voltageMv = when {
                voltage <= 0 -> null
                voltage < 100 -> voltage * 1000
                else -> voltage
            },
            temperatureC = if (temperature == Int.MIN_VALUE) null else temperature / 10f,
            systemFullMs = systemFull.get(now, charging = plugged != 0 && status == BatteryManager.BATTERY_STATUS_CHARGING),
        )
    }

    /** Unsupported properties come back as Int.MIN_VALUE. */
    private fun property(id: Int): Int? =
        batteryManager.getIntProperty(id).takeIf { it != Int.MIN_VALUE }

    private companion object {
        const val SYSTEM_FULL_EVERY_MS = 30_000L
    }
}
