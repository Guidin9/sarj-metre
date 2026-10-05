package com.anils.sarjmetre

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.anils.sarjmetre.battery.BatteryReader
import com.anils.sarjmetre.battery.CapacityResolver
import com.anils.sarjmetre.battery.ChargeMath
import com.anils.sarjmetre.battery.CurrentHistory
import com.anils.sarjmetre.battery.CurrentNormalizer
import com.anils.sarjmetre.net.NetMeter
import com.anils.sarjmetre.settings.AppSettings
import com.anils.sarjmetre.settings.SignMode
import com.anils.sarjmetre.settings.UnitMode
import com.anils.sarjmetre.stats.DailyTotalsTracker
import com.anils.sarjmetre.stats.PrefsDailyTotalsStore
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Turns raw battery readings into [MeterState] and holds the network meter. One per process,
 * used from the main thread only.
 */
class MeterEngine private constructor(context: Context) {
    val settings = AppSettings(context)
    val net = NetMeter(context)
    private val reader = BatteryReader(context)
    private val normalizer = CurrentNormalizer(settings)
    private val history = CurrentHistory()
    private val capacity = CapacityResolver(context, settings)
    private val totals = DailyTotalsTracker(PrefsDailyTotalsStore(context))

    fun sample(batteryIntent: Intent? = null): MeterState {
        val raw = reader.read(batteryIntent)
        val plugged = raw.plugged != 0
        val currentMa = raw.currentRaw?.let { normalizer.toMilliamps(it, plugged, settings.unitMode, settings.signMode) }
        if (currentMa != null) history.add(raw.elapsedMs, currentMa, plugged)

        capacity.update(raw.counterRaw, raw.level)
        val (capacityMah, capacitySource) = capacity.current()
        val counterMah = raw.counterRaw?.let(ChargeMath::counterToMah)
        val today = totals.onSample(LocalDate.now(), raw.elapsedMs, raw.level, counterMah)

        val target = settings.targetLevel
        val charging = plugged && raw.status == BatteryManager.BATTERY_STATUS_CHARGING
        val systemMinutes = raw.systemFullMs?.let { (it / 60_000.0).roundToInt() }
        val minutesToFull = when {
            !charging -> null
            // The phone's own estimate (on Samsung the one the lock screen shows) knows its charging
            // curve best, but it always aims at 100 %.
            target >= 100 && systemMinutes != null -> systemMinutes
            else -> history.average(CHARGE_WINDOW_MS)
                ?.let { ChargeMath.hoursToTarget(raw.level, target, it, capacityMah) }
                ?.let { (it * 60).roundToInt() }
        }
        val minutesToEmpty = if (plugged) null else history.average(DISCHARGE_WINDOW_MS)
            ?.let { ChargeMath.hoursToEmpty(raw.level, -it, capacityMah) }
            ?.let { (it * 60).roundToInt() }

        // Without a charge counter, today's mAh come from the percentage steps and the capacity.
        val hasCounter = counterMah != null
        return MeterState(
            currentMa = if (currentMa == null) null else history.recent(),
            level = raw.level,
            status = raw.status,
            plugged = raw.plugged,
            voltageMv = raw.voltageMv,
            temperatureC = raw.temperatureC,
            capacityMah = capacityMah,
            capacitySource = capacitySource,
            targetLevel = target,
            minutesToFull = minutesToFull,
            minutesToEmpty = minutesToEmpty,
            systemMinutesToFull = systemMinutes,
            todayChargedMah = if (hasCounter) today.chargedMah else today.chargedPct * capacityMah / 100,
            todayDischargedMah = if (hasCounter) today.dischargedMah else today.dischargedPct * capacityMah / 100,
            todayChargedPct = today.chargedPct,
            todayDischargedPct = today.dischargedPct,
            currentRaw = raw.currentRaw,
            counterRaw = raw.counterRaw,
            unitIsMicro = when (settings.unitMode) {
                UnitMode.AUTO -> settings.detectedMicroamps
                UnitMode.MICROAMPS -> true
                UnitMode.MILLIAMPS -> false
            },
            signInverted = when (settings.signMode) {
                SignMode.AUTO -> settings.detectedInverted
                SignMode.NORMAL -> false
                SignMode.INVERTED -> true
            },
        )
    }

    fun resetToday() = totals.resetToday(LocalDate.now())

    fun resetDetection() {
        settings.resetDetection()
        normalizer.reset()
        history.clear()
    }

    companion object {
        private const val CHARGE_WINDOW_MS = 60_000L
        private const val DISCHARGE_WINDOW_MS = 5 * 60_000L

        @Volatile
        private var instance: MeterEngine? = null

        fun get(context: Context): MeterEngine =
            instance ?: synchronized(this) {
                instance ?: MeterEngine(context.applicationContext).also { instance = it }
            }
    }
}
