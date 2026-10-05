package com.anils.sarjmetre.ui

import android.os.BatteryManager
import com.anils.sarjmetre.MeterState
import com.anils.sarjmetre.notification.MeterText
import java.util.Locale

/** In-app wording; the notification keeps its own compact phrasing in [MeterText]. */
internal object UiText {
    private val turkish: Locale = Locale.forLanguageTag("tr-TR")

    fun signedMa(ma: Int): String = when {
        ma > 0 -> "+$ma"
        ma < 0 -> "−${-ma}"
        else -> "0"
    }

    fun decimal(value: Double, digits: Int): String = String.format(turkish, "%.${digits}f", value)

    fun stateLine(state: MeterState): String = when {
        state.status == BatteryManager.BATTERY_STATUS_FULL -> "Pil dolu"
        state.isPlugged && state.status == BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Şarj duraklatıldı"
        state.isPlugged && (state.currentMa ?: 0) < 0 -> "Takılı, ama telefon gelenden fazlasını harcıyor"
        state.isPlugged -> when (state.plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Adaptörle şarj oluyor"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB ile şarj oluyor"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Kablosuz şarj oluyor"
            else -> "Şarj oluyor"
        }
        else -> "Pilden çalışıyor"
    }

    fun timeLine(state: MeterState): String? {
        state.minutesToFull?.let { minutes ->
            val toFull = state.targetLevel >= 100
            return when {
                minutes <= 0 -> if (toFull) "Pil dolu" else "%${state.targetLevel} sınırına ulaştı"
                toFull -> "Tam dolmasına yaklaşık ${MeterText.duration(minutes)}"
                else -> "%${state.targetLevel} sınırına yaklaşık ${MeterText.duration(minutes)}"
            }
        }
        state.minutesToEmpty?.let { return "Bu kullanımla yaklaşık ${MeterText.duration(it)} dayanır" }
        // While the phone uses more than the charger gives, the state line already says so; no time to show.
        val charging = state.isPlugged && state.status == BatteryManager.BATTERY_STATUS_CHARGING
        return if (charging && (state.currentMa ?: 0) > 0) "Kalan süre hesaplanıyor" else null
    }

    /** Dial scale numbers in amperes: 500 → "0,5", 2000 → "2". */
    fun amps(ma: Int): String = if (ma % 1000 == 0) (ma / 1000).toString() else decimal(ma / 1000.0, 1)
}
