package com.anils.sarjmetre.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anils.sarjmetre.settings.AppSettings

/** Brings the meter back after a reboot or an app update; both are allowed to start foreground services. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val relevant = intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (relevant && AppSettings(context).serviceEnabled) MeterService.start(context)
    }
}
