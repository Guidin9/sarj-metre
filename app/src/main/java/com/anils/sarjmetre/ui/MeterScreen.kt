package com.anils.sarjmetre.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anils.sarjmetre.LiveState
import com.anils.sarjmetre.MeterState
import com.anils.sarjmetre.R
import com.anils.sarjmetre.ui.components.BatteryHero
import com.anils.sarjmetre.ui.components.CurrentChart
import com.anils.sarjmetre.ui.components.InsetGroup
import com.anils.sarjmetre.ui.components.LargeTitlePage
import com.anils.sarjmetre.ui.components.NoticeRow
import com.anils.sarjmetre.ui.components.RowSeparator
import com.anils.sarjmetre.ui.components.ValueRow
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MeterScreen(scrollState: ScrollState, intervalMs: Long, onStart: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalAppleColors.current
    // The live values are read by the sections below, so a tick only recomposes those, not the page.
    val running by LiveState.running.collectAsStateWithLifecycle()
    var notificationsOn by remember { mutableStateOf(true) }
    var batteryExempt by remember { mutableStateOf(true) }
    // Both can change in system settings while the app is in the background.
    LifecycleResumeEffect(Unit) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        batteryExempt = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
        onPauseOrDispose {}
    }

    LargeTitlePage(title = "Batarya", scrollState = scrollState) {
        if (!running || !notificationsOn || !batteryExempt) {
            InsetGroup(modifier = Modifier.padding(bottom = 4.dp)) {
                var first = true
                if (!running) {
                    NoticeRow(R.drawable.ic_bolt_fill, colors.green, "Gösterge kapalı", "Status bar'da akım görünmüyor.", "Başlat", onStart)
                    first = false
                }
                if (!notificationsOn) {
                    if (!first) RowSeparator(inset = 58)
                    NoticeRow(R.drawable.ic_bell_slash_fill, colors.red, "Bildirimler kapalı", "Status bar göstergesi gizli kalıyor.", "Aç") {
                        openNotificationSettings(context)
                    }
                    first = false
                }
                if (!batteryExempt) {
                    if (!first) RowSeparator(inset = 58)
                    NoticeRow(R.drawable.ic_battery_alert_fill, colors.orange, "Pil kısıtlaması açık", "Samsung göstergeyi kapatabilir.", "Kaldır") {
                        requestBatteryExemption(context)
                    }
                }
            }
        }

        LiveHero(running, Modifier.padding(top = 16.dp))

        InsetGroup(header = "Akım") {
            LiveChart(intervalMs)
        }

        LiveReadings()
    }
}

@Composable
private fun LiveHero(running: Boolean, modifier: Modifier = Modifier) {
    val state by LiveState.state.collectAsStateWithLifecycle()
    BatteryHero(state, running, modifier)
}

@Composable
private fun LiveChart(intervalMs: Long) {
    val history by LiveState.history.collectAsStateWithLifecycle()
    CurrentChart(history, intervalMs)
}

@Composable
private fun LiveReadings() {
    val colors = LocalAppleColors.current
    val reading = LiveState.state.collectAsStateWithLifecycle().value ?: return
    InsetGroup(header = "Ayrıntılar") { Details(reading) }
    InsetGroup(header = "Bugün", footer = "Sayaçlar her gece yarısı sıfırlanır.") {
        ValueRow("Şarj edilen", "+${reading.todayChargedMah.roundToInt()} mAh", detail = "%${reading.todayChargedPct}", valueColor = colors.green)
        RowSeparator()
        ValueRow("Harcanan", "−${reading.todayDischargedMah.roundToInt()} mAh", detail = "%${reading.todayDischargedPct}", valueColor = colors.orange)
    }
}

@Composable
private fun Details(state: MeterState) {
    val colors = LocalAppleColors.current
    // A battery this warm is worth noticing.
    val hot = (state.temperatureC ?: 0f) >= 42f
    ValueRow("Güç", state.powerW?.let { UiText.decimal(abs(it), 1) + " W" } ?: "–")
    RowSeparator()
    ValueRow("Voltaj", state.voltageMv?.let { UiText.decimal(it / 1000.0, 2) + " V" } ?: "–")
    RowSeparator()
    ValueRow(
        "Sıcaklık",
        state.temperatureC?.let { UiText.decimal(it.toDouble(), 1) + " °C" } ?: "–",
        valueColor = if (hot) colors.orange else null,
    )
    RowSeparator()
    ValueRow("Kaynak", UiText.source(state.plugged))
    RowSeparator()
    ValueRow("Kapasite", "${state.capacityMah.roundToInt()} mAh")
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )
}

// Play restricts this intent, but this app is sideloaded for personal use.
@SuppressLint("BatteryLife")
private fun requestBatteryExemption(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri()),
    )
}
