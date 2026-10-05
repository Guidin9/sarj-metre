package com.anils.sarjmetre.settings

import android.content.Context
import androidx.core.content.edit
import com.anils.sarjmetre.battery.DetectionStore

enum class IconMode { MILLIAMPS, WATTS }

enum class UnitMode { AUTO, MICROAMPS, MILLIAMPS }

enum class SignMode { AUTO, NORMAL, INVERTED }

class AppSettings(context: Context) : DetectionStore {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Also decides whether the meter comes back after a reboot. */
    var serviceEnabled: Boolean
        get() = prefs.getBoolean("service_enabled", true)
        set(value) = prefs.edit { putBoolean("service_enabled", value) }

    /** Second status bar icon with the network speed. */
    var netMeterEnabled: Boolean
        get() = prefs.getBoolean("net_meter_enabled", true)
        set(value) = prefs.edit { putBoolean("net_meter_enabled", value) }

    var iconMode: IconMode
        get() = enumOr(prefs.getString("icon_mode", null), IconMode.MILLIAMPS)
        set(value) = prefs.edit { putString("icon_mode", value.name) }

    var intervalMs: Long
        get() = prefs.getLong("interval_ms", 1000L)
        set(value) = prefs.edit { putLong("interval_ms", value) }

    var unitMode: UnitMode
        get() = enumOr(prefs.getString("unit_mode", null), UnitMode.AUTO)
        set(value) = prefs.edit { putString("unit_mode", value.name) }

    var signMode: SignMode
        get() = enumOr(prefs.getString("sign_mode", null), SignMode.AUTO)
        set(value) = prefs.edit { putString("sign_mode", value.name) }

    /** 0 means automatic. */
    var capacityOverrideMah: Int
        get() = prefs.getInt("capacity_override_mah", 0)
        set(value) = prefs.edit { putInt("capacity_override_mah", value) }

    /** Charge limit the time-to-full aims at, e.g. 85 with Samsung's battery protection. */
    var targetLevel: Int
        get() = prefs.getInt("target_level", 100)
        set(value) = prefs.edit { putInt("target_level", value) }

    var learnedCapacityMah: Float
        get() = prefs.getFloat("learned_capacity_mah", 0f)
        set(value) = prefs.edit { putFloat("learned_capacity_mah", value) }

    override var detectedMicroamps: Boolean
        get() = prefs.getBoolean("detected_microamps", false)
        set(value) = prefs.edit { putBoolean("detected_microamps", value) }

    override var detectedInverted: Boolean?
        get() = if (prefs.contains("detected_inverted")) prefs.getBoolean("detected_inverted", false) else null
        set(value) = prefs.edit {
            if (value == null) remove("detected_inverted") else putBoolean("detected_inverted", value)
        }

    fun resetDetection() {
        detectedMicroamps = false
        detectedInverted = null
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
