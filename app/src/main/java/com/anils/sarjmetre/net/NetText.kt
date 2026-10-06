package com.anils.sarjmetre.net

import com.anils.sarjmetre.notification.MeterContent
import java.util.Locale
import kotlin.math.roundToInt

/** What the network meter's icon and notification say. */
object NetText {
    private val turkish: Locale = Locale.forLanguageTag("tr-TR")
    private const val KB = 1024.0
    private const val MB = KB * 1024
    private const val GB = MB * 1024

    fun content(state: NetState): MeterContent {
        val (value, unit) = iconLines(state.speed)
        return MeterContent(value, unit, status = "", title = title(state.speed), today = today(state), details = "")
    }

    /** While the screen is off nothing is measured; the notification keeps only today's use. */
    fun pausedContent(state: NetState): MeterContent =
        MeterContent("", "", status = "", title = "Ağ hızı", today = today(state), details = "")

    /** Before the first sample, e.g. right after the charger came out. */
    val measuring = iconLines(null).let { (value, unit) -> MeterContent(value, unit, status = "", title = title(null), today = "", details = "") }

    /** Two short lines for the status bar icon, e.g. "1,2" over "MB". Without "/s" the unit stays legible at that size. */
    fun iconLines(speed: Speed?): Pair<String, String> = speed?.totalBps?.let(::scaled) ?: ("--" to "KB")

    /** A speed as number and unit for text, e.g. "1,2" and "MB/s". */
    fun speedParts(bps: Double): Pair<String, String> = scaled(bps).let { (value, unit) -> value to "$unit/s" }

    fun speed(bps: Double): String = speedParts(bps).let { (value, unit) -> "$value $unit" }

    fun title(speed: Speed?): String =
        if (speed == null) "Ağ hızı ölçülüyor" else "↓ ${speed(speed.downBps)} · ↑ ${speed(speed.upBps)}"

    fun today(state: NetState): String = state.today
        ?.let { "Bugün mobil ${bytes(it.mobileBytes)} · Wi-Fi ${bytes(it.wifiBytes)}" }
        ?: "Günlük kullanım için uygulamada izin ver"

    fun bytes(count: Long): String = when {
        count < MB -> "${(count / KB).roundToInt()} KB"
        count < GB -> "${(count / MB).roundToInt()} MB"
        else -> decimal(count / GB) + " GB"
    }

    /** The number and the byte unit it counts in, per second. */
    private fun scaled(bps: Double): Pair<String, String> {
        val kb = bps / KB
        if (kb < 999.5) return kb.roundToInt().toString() to "KB"
        val mb = bps / MB
        return (if (mb < 9.95) decimal(mb) else mb.roundToInt().toString()) to "MB"
    }

    private fun decimal(value: Double): String = String.format(turkish, "%.1f", value)
}
