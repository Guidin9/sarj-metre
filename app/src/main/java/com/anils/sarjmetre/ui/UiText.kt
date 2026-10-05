package com.anils.sarjmetre.ui

import android.os.BatteryManager
import com.anils.sarjmetre.MeterState
import com.anils.sarjmetre.notification.MeterText
import java.util.Locale

/** In-app wording, kept short the way Apple's system apps phrase status; the notification has its own in [MeterText]. */
internal object UiText {
    private val turkish: Locale = Locale.forLanguageTag("tr-TR")

    fun signedMa(ma: Int): String = when {
        ma > 0 -> "+$ma"
        ma < 0 -> "−${-ma}"
        else -> "0"
    }

    fun decimal(value: Double, digits: Int): String = String.format(turkish, "%.${digits}f", value)

    fun stateLine(state: MeterState): String = when {
        state.status == BatteryManager.BATTERY_STATUS_FULL -> "Tam şarj oldu"
        state.isPlugged && state.status == BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Şarj duraklatıldı"
        state.isPlugged && (state.currentMa ?: 0) < 0 -> "Takılı, ama harcama şarjdan fazla"
        state.isPlugged -> when (state.plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Adaptörle şarj oluyor"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB ile şarj oluyor"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Kablosuz şarj oluyor"
            else -> "Şarj oluyor"
        }
        else -> "Pil kullanılıyor"
    }

    /** Null when there is no estimate worth showing. */
    fun timeLine(state: MeterState): String? {
        state.minutesToFull?.let { minutes ->
            val toFull = state.targetLevel >= 100
            return when {
                minutes <= 0 -> if (toFull) "Tam şarj oldu" else "%${state.targetLevel} sınırına ulaştı"
                toFull -> "Tam şarja ${MeterText.duration(minutes)}"
                else -> "%${state.targetLevel} sınırına ${MeterText.duration(minutes)}"
            }
        }
        return state.minutesToEmpty?.let { "Bu kullanımla ${MeterText.duration(it)} gider" }
    }

    fun source(plugged: Int): String = when (plugged) {
        0 -> "Pil"
        BatteryManager.BATTERY_PLUGGED_AC -> "Adaptör"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Kablosuz"
        else -> "Dock"
    }
}
