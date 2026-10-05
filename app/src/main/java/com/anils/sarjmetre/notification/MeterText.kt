package com.anils.sarjmetre.notification

import android.os.BatteryManager
import com.anils.sarjmetre.MeterState
import com.anils.sarjmetre.settings.IconMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** What the status bar icon and the notification say; equal contents mean no repost is needed. */
data class MeterContent(
    val iconValue: String,
    val iconUnit: String,
    val title: String,
    val today: String,
    val details: String,
)

object MeterText {
    private val turkish: Locale = Locale.forLanguageTag("tr-TR")
    private const val MINUS = "−"

    fun content(state: MeterState, iconMode: IconMode): MeterContent {
        val (value, unit) = iconLines(state, iconMode)
        return MeterContent(value, unit, title(state), today(state), details(state))
    }

    /** Two short lines for the status bar icon, e.g. "1850" over "mA". The icon keeps a plain "-" to save width. */
    fun iconLines(state: MeterState, mode: IconMode): Pair<String, String> {
        val ma = state.currentMa ?: return "--" to "mA"
        return when (mode) {
            IconMode.MILLIAMPS -> ma.toString() to "mA"
            IconMode.WATTS -> (state.powerW?.let { decimal(it, 1) } ?: "--") to "W"
        }
    }

    fun status(state: MeterState): String = when {
        state.status == BatteryManager.BATTERY_STATUS_FULL -> "Doldu"
        state.isPlugged && state.status == BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Şarj duraklatıldı"
        state.isPlugged && state.status == BatteryManager.BATTERY_STATUS_CHARGING -> "Şarj oluyor"
        state.isPlugged -> "Şarjda"
        else -> "Kullanım"
    }

    fun current(state: MeterState): String = state.currentMa?.let { signed(it) + " mA" } ?: "-- mA"

    fun title(state: MeterState): String = listOfNotNull(
        status(state),
        current(state),
        state.powerW?.let { decimal(abs(it), 1) + " W" },
        shortTime(state),
    ).joinToString(" · ")

    fun today(state: MeterState): String =
        "Bugün +${state.todayChargedMah.roundToInt()} mAh (%${state.todayChargedPct}) · " +
            "$MINUS${state.todayDischargedMah.roundToInt()} mAh (%${state.todayDischargedPct})"

    fun details(state: MeterState): String {
        val facts = listOfNotNull(
            "%${state.level}",
            state.voltageMv?.let { decimal(it / 1000.0, 2) + " V" },
            state.temperatureC?.let { decimal(it.toDouble(), 1) + " °C" },
            plugLabel(state.plugged),
        ).joinToString(" · ")
        return listOfNotNull(facts, longTime(state)).joinToString("\n")
    }

    fun duration(minutes: Int): String = when {
        minutes < 60 -> "$minutes dk"
        minutes < 600 -> "${minutes / 60} sa ${minutes % 60} dk"
        else -> "${minutes / 60} sa"
    }

    private fun shortTime(state: MeterState): String? {
        val minutes = state.minutesToFull?.takeIf { it > 0 } ?: state.minutesToEmpty ?: return null
        return "~" + duration(minutes)
    }

    private fun longTime(state: MeterState): String? {
        state.minutesToFull?.let { minutes ->
            val toFull = state.targetLevel >= 100
            return when {
                minutes <= 0 -> if (toFull) "Doldu" else "Hedefe ulaştı (%${state.targetLevel})"
                toFull -> "Tam dolmasına ~${duration(minutes)}"
                else -> "Hedef %${state.targetLevel}: ~${duration(minutes)}"
            }
        }
        return state.minutesToEmpty?.let { "Bitmesine ~${duration(it)}" }
    }

    private fun plugLabel(plugged: Int): String? = when (plugged) {
        0 -> null
        BatteryManager.BATTERY_PLUGGED_AC -> "Adaptör"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Kablosuz"
        else -> "Dock"
    }

    private fun signed(value: Int): String = if (value < 0) "$MINUS${-value}" else value.toString()

    private fun decimal(value: Double, digits: Int): String = String.format(turkish, "%.${digits}f", value)
}
