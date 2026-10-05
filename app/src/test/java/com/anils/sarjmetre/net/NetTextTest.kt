package com.anils.sarjmetre.net

import org.junit.Assert.assertEquals
import org.junit.Test

class NetTextTest {
    @Test
    fun iconSwitchesFromKilobytesToMegabytes() {
        assertEquals("--" to "KB/s", NetText.iconLines(null))
        assertEquals("0" to "KB/s", NetText.iconLines(Speed(0.0, 0.0)))
        assertEquals("120" to "KB/s", NetText.iconLines(Speed(100 * 1024.0, 20 * 1024.0)))
        assertEquals("999" to "KB/s", NetText.iconLines(Speed(999 * 1024.0, 0.0)))
        assertEquals("1,0" to "MB/s", NetText.iconLines(Speed(999.6 * 1024, 0.0)))
        assertEquals("1,2" to "MB/s", NetText.iconLines(Speed(1.2 * 1024 * 1024, 0.0)))
        assertEquals("25" to "MB/s", NetText.iconLines(Speed(25.4 * 1024 * 1024, 0.0)))
    }

    @Test
    fun titleAndToday() {
        val state = NetState(
            Speed(1.5 * 1024 * 1024, 80 * 1024.0),
            DayUsage(mobileBytes = 120L * 1024 * 1024, wifiBytes = (1.4 * 1024 * 1024 * 1024).toLong()),
        )
        assertEquals("↓ 1,5 MB/s · ↑ 80 KB/s", NetText.title(state.speed))
        assertEquals("Bugün mobil 120 MB · Wi-Fi 1,4 GB", NetText.today(state))
        assertEquals("512 KB", NetText.bytes(512 * 1024))
    }

    @Test
    fun withoutUsageAccessAsksForIt() {
        assertEquals("Günlük kullanım için uygulamada izin ver", NetText.today(NetState(null, null)))
        assertEquals("Ağ hızı ölçülüyor", NetText.title(null))
    }
}
