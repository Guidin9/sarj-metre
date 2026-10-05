package com.anils.sarjmetre.notification

import android.os.BatteryManager
import com.anils.sarjmetre.MeterState
import com.anils.sarjmetre.battery.CapacitySource
import com.anils.sarjmetre.settings.IconMode
import org.junit.Assert.assertEquals
import org.junit.Test

class MeterTextTest {
    private fun state(
        currentMa: Int? = 1850,
        level: Int = 67,
        status: Int = BatteryManager.BATTERY_STATUS_CHARGING,
        plugged: Int = BatteryManager.BATTERY_PLUGGED_AC,
        minutesToFull: Int? = 42,
        minutesToEmpty: Int? = null,
        targetLevel: Int = 100,
    ) = MeterState(
        currentMa = currentMa,
        level = level,
        status = status,
        plugged = plugged,
        voltageMv = 4100,
        temperatureC = 36.8f,
        capacityMah = 5000.0,
        capacitySource = CapacitySource.COUNTER,
        targetLevel = targetLevel,
        minutesToFull = minutesToFull,
        minutesToEmpty = minutesToEmpty,
        systemMinutesToFull = null,
        todayChargedMah = 2450.4,
        todayDischargedMah = 1800.0,
        todayChargedPct = 52,
        todayDischargedPct = 38,
        currentRaw = currentMa,
        counterRaw = null,
        unitIsMicro = false,
        signInverted = false,
    )

    @Test
    fun iconShowsMilliampsOrWatts() {
        assertEquals("1850" to "mA", MeterText.iconLines(state(), IconMode.MILLIAMPS))
        assertEquals("-420" to "mA", MeterText.iconLines(state(currentMa = -420), IconMode.MILLIAMPS))
        // 1850 mA × 4.1 V
        assertEquals("7,6" to "W", MeterText.iconLines(state(), IconMode.WATTS))
        assertEquals("--" to "mA", MeterText.iconLines(state(currentMa = null), IconMode.MILLIAMPS))
    }

    @Test
    fun chargingTitleAndDetails() {
        assertEquals("Şarj oluyor · 1850 mA · 7,6 W · ~42 dk", MeterText.title(state()))
        assertEquals("Bugün +2450 mAh (%52) · −1800 mAh (%38)", MeterText.today(state()))
        assertEquals("%67 · 4,10 V · 36,8 °C · Adaptör\nTam dolmasına ~42 dk", MeterText.details(state()))
    }

    @Test
    fun dischargingShowsTimeToEmpty() {
        val s = state(
            currentMa = -420,
            status = BatteryManager.BATTERY_STATUS_DISCHARGING,
            plugged = 0,
            minutesToFull = null,
            minutesToEmpty = 370,
        )
        assertEquals("Kullanım · −420 mA · 1,7 W · ~6 sa 10 dk", MeterText.title(s))
        assertEquals("%67 · 4,10 V · 36,8 °C\nBitmesine ~6 sa 10 dk", MeterText.details(s))
    }

    @Test
    fun chargeLimitWording() {
        assertEquals("Hedef %85: ~20 dk", MeterText.details(state(minutesToFull = 20, targetLevel = 85)).lines().last())
        assertEquals("Hedefe ulaştı (%85)", MeterText.details(state(minutesToFull = 0, targetLevel = 85)).lines().last())
        val paused = state(status = BatteryManager.BATTERY_STATUS_NOT_CHARGING, minutesToFull = null)
        assertEquals("Şarj duraklatıldı", MeterText.status(paused))
    }

    @Test
    fun durations() {
        assertEquals("42 dk", MeterText.duration(42))
        assertEquals("1 sa 0 dk", MeterText.duration(60))
        assertEquals("6 sa 10 dk", MeterText.duration(370))
        assertEquals("12 sa", MeterText.duration(725))
    }
}
