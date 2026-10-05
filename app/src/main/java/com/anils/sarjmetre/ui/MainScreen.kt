package com.anils.sarjmetre.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anils.sarjmetre.LiveState
import com.anils.sarjmetre.MeterState
import com.anils.sarjmetre.R
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    dialSwept: Boolean,
    onDialSwept: () -> Unit,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalMeterColors.current
    val state by LiveState.state.collectAsStateWithLifecycle()
    val running by LiveState.running.collectAsStateWithLifecycle()
    var notificationsOn by remember { mutableStateOf(true) }
    var batteryExempt by remember { mutableStateOf(true) }
    // Both can change in system settings while the app is in the background.
    LifecycleResumeEffect(Unit) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        batteryExempt = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)
        onPauseOrDispose {}
    }

    Scaffold(containerColor = colors.background, topBar = { TitleBar(onOpenSettings) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            if (!running) {
                Notice(R.drawable.ic_battery_alert, "Gösterge kapalı, status bar'da akım görünmüyor.", "Göstergeyi başlat", onStart)
            }
            if (!notificationsOn) {
                Notice(
                    R.drawable.ic_notifications_off,
                    "Bildirimler kapalı olduğu için status bar'daki gösterge görünmüyor.",
                    "Bildirimleri aç",
                ) { openNotificationSettings(context) }
            }
            if (!batteryExempt) {
                Notice(
                    R.drawable.ic_battery_alert,
                    "Pil kısıtlaması açık. Samsung göstergeyi arka planda kapatabilir.",
                    "Kısıtlamayı kaldır",
                ) { requestBatteryExemption(context) }
            }

            AmmeterDial(
                currentMa = state?.currentMa,
                sweep = !dialSwept,
                onSwept = onDialSwept,
                modifier = Modifier.padding(top = 12.dp),
            )
            Readout(state, running)

            val reading = state ?: return@Column
            Spacer(Modifier.height(32.dp))
            LevelSection(reading)
            HorizontalDivider(Modifier.padding(vertical = 24.dp), color = colors.hairline)
            ReadingsRow(reading)
            HorizontalDivider(Modifier.padding(vertical = 24.dp), color = colors.hairline)
            TodaySection(reading)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TitleBar(onOpenSettings: () -> Unit) {
    val colors = LocalMeterColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Şarj Metre", style = MaterialTheme.typography.titleLarge, color = colors.ink, modifier = Modifier.weight(1f))
        IconButton(onClick = onOpenSettings) {
            Icon(painterResource(R.drawable.ic_settings), contentDescription = "Ayarlar", tint = colors.ink)
        }
    }
}

@Composable
private fun Readout(state: MeterState?, running: Boolean) {
    val colors = LocalMeterColors.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            Text(
                text = state?.currentMa?.let(UiText::signedMa) ?: "–",
                style = MeterType.hero,
                color = colors.ink,
                modifier = Modifier.alignByBaseline(),
            )
            Text(" mA", style = MeterType.heroUnit, color = colors.inkMuted, modifier = Modifier.alignByBaseline())
        }
        Text(
            text = state?.let(UiText::stateLine) ?: if (running) "Ölçülüyor" else "Gösterge kapalı",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.inkMuted,
        )
    }
}

@Composable
private fun LevelSection(state: MeterState) {
    val colors = LocalMeterColors.current
    val charging = state.isPlugged && (state.currentMa ?: 0) > 0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("%${state.level}", style = MeterType.figureLarge, color = colors.ink, modifier = Modifier.alignByBaseline())
            Spacer(Modifier.width(16.dp))
            Text(
                text = UiText.timeLine(state).orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(1f)
                    .alignByBaseline(),
            )
        }
        LevelBar(state.level, state.targetLevel, if (charging) colors.charge else colors.ink)
        if (state.targetLevel < 100) {
            Text("Şarj sınırı %${state.targetLevel}", style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
    }
}

@Composable
private fun LevelBar(level: Int, target: Int, fill: Color) {
    val colors = LocalMeterColors.current
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(10.dp),
    ) {
        val corner = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(colors.hairline, cornerRadius = corner)
        drawRoundRect(fill, size = Size(size.width * level / 100f, size.height), cornerRadius = corner)
        if (target < 100) {
            val x = size.width * target / 100f
            drawLine(colors.ink, Offset(x, -4.dp.toPx()), Offset(x, size.height + 4.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun ReadingsRow(state: MeterState) {
    val colors = LocalMeterColors.current
    // A battery this warm is worth noticing, so the temperature borrows the amber.
    val hot = (state.temperatureC ?: 0f) >= 42f
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Reading("Güç", state.powerW?.let { UiText.decimal(abs(it), 1) }, "W", Modifier.weight(1f))
        Rule()
        Reading(
            "Voltaj",
            state.voltageMv?.let { UiText.decimal(it / 1000.0, 2) },
            "V",
            Modifier
                .weight(1f)
                .padding(start = 16.dp),
        )
        Rule()
        Reading(
            "Sıcaklık",
            state.temperatureC?.let { UiText.decimal(it.toDouble(), 1) },
            "°C",
            Modifier
                .weight(1f)
                .padding(start = 16.dp),
            valueColor = if (hot) colors.drainText else colors.ink,
        )
    }
}

@Composable
private fun Reading(label: String, value: String?, unit: String, modifier: Modifier, valueColor: Color = LocalMeterColors.current.ink) {
    val colors = LocalMeterColors.current
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        Row {
            Text(value ?: "–", style = MeterType.figure, color = valueColor, modifier = Modifier.alignByBaseline())
            Text(" $unit", style = MeterType.unit, color = colors.inkMuted, modifier = Modifier.alignByBaseline())
        }
    }
}

@Composable
private fun Rule() {
    Box(
        Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(LocalMeterColors.current.hairline),
    )
}

@Composable
private fun TodaySection(state: MeterState) {
    val colors = LocalMeterColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Bugün", style = MaterialTheme.typography.titleMedium, color = colors.ink)
        Row(Modifier.fillMaxWidth()) {
            TodayFigure(
                value = "+${state.todayChargedMah.roundToInt()}",
                caption = "şarj edildi (%${state.todayChargedPct})",
                color = colors.charge,
                modifier = Modifier.weight(1f),
            )
            TodayFigure(
                value = "−${state.todayDischargedMah.roundToInt()}",
                caption = "harcandı (%${state.todayDischargedPct})",
                color = colors.drainText,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TodayFigure(value: String, caption: String, color: Color, modifier: Modifier) {
    val colors = LocalMeterColors.current
    Column(modifier) {
        Row {
            Text(value, style = MeterType.figure, color = color, modifier = Modifier.alignByBaseline())
            Text(" mAh", style = MeterType.unit, color = colors.inkMuted, modifier = Modifier.alignByBaseline())
        }
        Text(caption, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
    }
}

/** The only tinted container on the screen, so a setup problem can't be missed. */
@Composable
private fun Notice(@DrawableRes icon: Int, text: String, action: String, onAction: () -> Unit) {
    val colors = LocalMeterColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.drainSoft)
            .padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 2.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = colors.drainText, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
            TextButton(onClick = onAction, contentPadding = PaddingValues(vertical = 8.dp)) { Text(action) }
        }
    }
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
