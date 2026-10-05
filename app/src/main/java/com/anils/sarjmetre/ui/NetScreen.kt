package com.anils.sarjmetre.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anils.sarjmetre.LiveState
import com.anils.sarjmetre.R
import com.anils.sarjmetre.net.NetText
import com.anils.sarjmetre.net.Speed
import com.anils.sarjmetre.net.UsageBar
import com.anils.sarjmetre.net.UsageHistory
import com.anils.sarjmetre.net.UsagePeriod
import com.anils.sarjmetre.ui.components.GroupShape
import com.anils.sarjmetre.ui.components.InsetGroup
import com.anils.sarjmetre.ui.components.LargeTitlePage
import com.anils.sarjmetre.ui.components.ListRow
import com.anils.sarjmetre.ui.components.NoticeRow
import com.anils.sarjmetre.ui.components.SegmentedControl
import com.anils.sarjmetre.ui.components.UsageChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun NetScreen(scrollState: ScrollState, history: UsageHistory, netEnabled: Boolean) {
    val context = LocalContext.current
    val colors = LocalAppleColors.current
    var hasAccess by remember { mutableStateOf(history.hasAccess()) }
    var refresh by remember { mutableIntStateOf(0) }
    // Access is granted in system settings, and the numbers move while the app is away.
    LifecycleResumeEffect(Unit) {
        hasAccess = history.hasAccess()
        refresh++
        onPauseOrDispose {}
    }

    LargeTitlePage(title = "Ağ", scrollState = scrollState) {
        if (!hasAccess) {
            InsetGroup(modifier = Modifier.padding(bottom = 4.dp)) {
                NoticeRow(R.drawable.ic_stat_net, colors.blue, "Kullanım erişimi kapalı", "Günlük, haftalık ve aylık kullanım için gerekli.", "Aç") {
                    openUsageAccessSettings(context)
                }
            }
        }

        SpeedHero(netEnabled, Modifier.padding(top = 16.dp))

        if (hasAccess) UsageSection(history, refresh)
    }
}

@Composable
private fun SpeedHero(netEnabled: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalAppleColors.current
    val net by LiveState.net.collectAsStateWithLifecycle()
    val speed = net?.speed
    val connection = rememberConnection()

    Column(
        modifier
            .fillMaxWidth()
            .clip(GroupShape)
            .background(colors.cell)
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) {
            SpeedColumn("İndirme", "↓", speed?.downBps, Modifier.weight(1f))
            SpeedColumn("Yükleme", "↑", speed?.upBps, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        val line = when {
            !netEnabled -> "Ağ hızı göstergesi Ayarlar'da kapalı"
            net == null -> "Ölçülüyor"
            else -> connection
        }
        Text(line, style = AppleType.subheadline, color = colors.secondaryLabel)
    }
}

@Composable
private fun SpeedColumn(title: String, arrow: String, bps: Double?, modifier: Modifier = Modifier) {
    val colors = LocalAppleColors.current
    val (value, unit) = bps?.let { NetText.iconLines(Speed(it, 0.0)) } ?: ("–" to "")
    Column(
        modifier.clearAndSetSemantics { contentDescription = "$title ${if (bps == null) "yok" else "$value $unit"}" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$arrow $title", style = AppleType.subheadline.copy(fontWeight = FontWeight.SemiBold), color = colors.secondaryLabel)
        Row {
            Text(value, style = AppleType.heroNumber.copy(fontSize = AppleType.heroNumber.fontSize * 0.8f), color = colors.label, modifier = Modifier.alignByBaseline())
            if (unit.isNotEmpty()) {
                Text(" $unit", style = AppleType.headline, color = colors.secondaryLabel, modifier = Modifier.alignByBaseline())
            }
        }
    }
}

@Composable
private fun UsageSection(history: UsageHistory, refresh: Int) {
    val colors = LocalAppleColors.current
    var period by rememberSaveable { mutableStateOf(UsagePeriod.TODAY) }
    var selected by remember(period) { mutableStateOf<Int?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    // Today's bars grow while the screen is open.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            tick++
        }
    }
    val bars by produceState<List<UsageBar>?>(null, period, refresh, tick) {
        value = withContext(Dispatchers.IO) { history.bars(period) }
    }

    InsetGroup(header = "Kullanım", footer = peakLine(bars, period)) {
        ListRow {
            SegmentedControl(
                options = listOf(UsagePeriod.TODAY to "Bugün", UsagePeriod.WEEK to "7 gün", UsagePeriod.MONTH to "Bu ay"),
                selected = period,
                onSelect = { period = it },
            )
        }
        val data = bars
        val pick = selected?.let { data?.getOrNull(it) }
        val mobile = pick?.mobileBytes ?: data?.sumOf { it.mobileBytes } ?: 0L
        val wifi = pick?.wifiBytes ?: data?.sumOf { it.wifiBytes } ?: 0L
        Column(Modifier.padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 14.dp)) {
            Text(
                pick?.let { selectionLabel(it, period) } ?: periodLabel(period),
                style = AppleType.footnote,
                color = colors.secondaryLabel,
            )
            Text(NetText.bytes(mobile + wifi), style = AppleType.title3.merge(AppleType.figures), color = colors.label)
            Spacer(Modifier.height(8.dp))
            // The legend doubles as the direct labels: each series with its own total.
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                LegendValue(colors.blue, "Wi-Fi", NetText.bytes(wifi))
                LegendValue(colors.mobileData, "Mobil", NetText.bytes(mobile))
            }
            Spacer(Modifier.height(14.dp))
            if (data == null) {
                Box(Modifier.fillMaxWidth().height(170.dp))
            } else {
                val slots = when (period) {
                    UsagePeriod.TODAY -> 24
                    UsagePeriod.WEEK -> 7
                    UsagePeriod.MONTH -> YearMonth.now().lengthOfMonth()
                }
                UsageChart(
                    bars = data,
                    slots = slots,
                    labelEvery = when (period) {
                        UsagePeriod.TODAY -> 6
                        UsagePeriod.WEEK -> 1
                        UsagePeriod.MONTH -> 5
                    },
                    label = { i ->
                        when (period) {
                            UsagePeriod.TODAY -> i.toString().padStart(2, '0')
                            UsagePeriod.MONTH -> (i + 1).toString()
                            UsagePeriod.WEEK -> data.getOrNull(i)?.slot?.label
                        }
                    },
                    selected = selected,
                    onSelect = { selected = it },
                )
            }
        }
    }
}

@Composable
private fun LegendValue(color: Color, name: String, value: String) {
    val colors = LocalAppleColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(name, style = AppleType.subheadline, color = colors.secondaryLabel)
        Spacer(Modifier.width(6.dp))
        Text(value, style = AppleType.subheadline.merge(AppleType.figures).copy(fontWeight = FontWeight.SemiBold), color = colors.label)
    }
}

private fun periodLabel(period: UsagePeriod): String = when (period) {
    UsagePeriod.TODAY -> "Bugün toplam"
    UsagePeriod.WEEK -> "Son 7 gün toplam"
    UsagePeriod.MONTH -> "Bu ay toplam"
}

private fun selectionLabel(bar: UsageBar, period: UsagePeriod): String = when (period) {
    UsagePeriod.TODAY -> "${bar.slot.label}:00 – ${(bar.slot.label.toInt() + 1).toString().padStart(2, '0')}:00"
    UsagePeriod.WEEK -> bar.slot.label
    UsagePeriod.MONTH -> "${bar.slot.label} ${monthName(LocalDate.now().monthValue)}"
}

private fun peakLine(bars: List<UsageBar>?, period: UsagePeriod): String? {
    val peak = bars?.maxByOrNull { it.totalBytes }?.takeIf { it.totalBytes > 0 } ?: return null
    val amount = NetText.bytes(peak.totalBytes)
    val tip = "Ayrıntı için bir çubuğa dokun."
    return when (period) {
        UsagePeriod.TODAY -> "En yoğun saat ${peak.slot.label}:00, $amount. $tip"
        UsagePeriod.WEEK -> "En yoğun gün ${peak.slot.label}, $amount. $tip"
        UsagePeriod.MONTH -> "En yoğun gün ${peak.slot.label} ${monthName(LocalDate.now().monthValue)}, $amount. $tip"
    }
}

private fun monthName(month: Int): String =
    listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")[month - 1]

/** What the phone is connected through right now, following the default network. */
@Composable
private fun rememberConnection(): String {
    val context = LocalContext.current
    var label by remember { mutableStateOf("Bağlantı yok") }
    DisposableEffect(Unit) {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val base = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi ile bağlı"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobil veri ile bağlı"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Kablo ile bağlı"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN ile bağlı"
                    else -> "Bağlı"
                }
                val vpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) && !base.startsWith("VPN")
                label = if (vpn) "$base · VPN açık" else base
            }

            override fun onLost(network: Network) {
                label = "Bağlantı yok"
            }
        }
        // Callbacks arrive on a binder thread; the state write is safe from there.
        connectivity.registerDefaultNetworkCallback(callback)
        onDispose { connectivity.unregisterNetworkCallback(callback) }
    }
    return label
}

private fun openUsageAccessSettings(context: Context) {
    // Samsung and recent Android open the app's own page with the package; older ones need the list.
    val direct = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, "package:${context.packageName}".toUri())
    runCatching { context.startActivity(direct) }
        .onFailure { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
}
