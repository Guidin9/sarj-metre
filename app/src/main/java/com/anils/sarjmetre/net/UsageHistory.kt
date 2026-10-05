@file:Suppress("DEPRECATION") // ConnectivityManager.TYPE_* is still what NetworkStatsManager takes.

package com.anils.sarjmetre.net

import android.app.AppOpsManager
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import java.time.ZonedDateTime

/** Bytes used in one chart slot. */
data class UsageBar(val slot: UsageSlot, val mobileBytes: Long, val wifiBytes: Long) {
    val totalBytes: Long get() = mobileBytes + wifiBytes
}

data class DayUsage(val mobileBytes: Long, val wifiBytes: Long)

/**
 * Mobile and Wi-Fi use from the system's own network statistics, the record Samsung's data usage
 * screen reads. It needs the usage access permission, keeps hourly detail for weeks, and counts
 * VPN traffic once, on the network that carried it.
 */
class UsageHistory(context: Context) {
    private val appContext = context.applicationContext
    private val stats = appContext.getSystemService(NetworkStatsManager::class.java)

    fun hasAccess(): Boolean {
        val ops = appContext.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Blocking binder calls into the system: call off the main thread. Null without access. */
    fun bars(period: UsagePeriod, now: ZonedDateTime = ZonedDateTime.now()): List<UsageBar>? {
        if (!hasAccess()) return null
        // Each query makes the system fold in the newest traffic first; Android rate-limits that for apps.
        return UsagePeriods.slots(period, now).map { slot ->
            UsageBar(slot, bytes(ConnectivityManager.TYPE_MOBILE, slot), bytes(ConnectivityManager.TYPE_WIFI, slot))
        }
    }

    /** Today's totals for the notification. Null without access. */
    fun today(now: ZonedDateTime = ZonedDateTime.now()): DayUsage? {
        if (!hasAccess()) return null
        val start = now.toLocalDate().atStartOfDay(now.zone).toInstant().toEpochMilli()
        val slot = UsageSlot(start, now.toInstant().toEpochMilli() + 1, "")
        return DayUsage(bytes(ConnectivityManager.TYPE_MOBILE, slot), bytes(ConnectivityManager.TYPE_WIFI, slot))
    }

    // A null subscriber id asks for every SIM, which is what apps may do since Android 10.
    private fun bytes(type: Int, slot: UsageSlot): Long = runCatching {
        stats.querySummaryForDevice(type, null, slot.startMs, slot.endMs).let { it.rxBytes + it.txBytes }
    }.getOrDefault(0L)
}
