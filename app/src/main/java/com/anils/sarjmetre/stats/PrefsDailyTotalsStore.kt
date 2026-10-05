package com.anils.sarjmetre.stats

import android.content.Context
import androidx.core.content.edit

class PrefsDailyTotalsStore(context: Context) : DailyTotalsStore {
    private val prefs = context.applicationContext.getSharedPreferences("daily_totals", Context.MODE_PRIVATE)

    override fun load(): DailyTotals? {
        val day = prefs.getString("day", null) ?: return null
        return DailyTotals(
            day = day,
            chargedPct = prefs.getInt("charged_pct", 0),
            dischargedPct = prefs.getInt("discharged_pct", 0),
            chargedMah = prefs.getFloat("charged_mah", 0f).toDouble(),
            dischargedMah = prefs.getFloat("discharged_mah", 0f).toDouble(),
            lastLevel = prefs.getInt("last_level", -1).takeIf { it >= 0 },
            anchorMah = prefs.getFloat("anchor_mah", -1f).takeIf { it >= 0 }?.toDouble(),
        )
    }

    override fun save(totals: DailyTotals) = prefs.edit {
        putString("day", totals.day)
        putInt("charged_pct", totals.chargedPct)
        putInt("discharged_pct", totals.dischargedPct)
        putFloat("charged_mah", totals.chargedMah.toFloat())
        putFloat("discharged_mah", totals.dischargedMah.toFloat())
        putInt("last_level", totals.lastLevel ?: -1)
        putFloat("anchor_mah", totals.anchorMah?.toFloat() ?: -1f)
    }
}
