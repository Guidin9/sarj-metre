package com.anils.sarjmetre.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anils.sarjmetre.LiveState
import com.anils.sarjmetre.MeterEngine
import com.anils.sarjmetre.MeterState
import com.anils.sarjmetre.battery.CapacitySource
import com.anils.sarjmetre.settings.IconMode
import com.anils.sarjmetre.settings.SignMode
import com.anils.sarjmetre.settings.UnitMode
import com.anils.sarjmetre.ui.components.ActionRow
import com.anils.sarjmetre.ui.components.ControlRow
import com.anils.sarjmetre.ui.components.InsetGroup
import com.anils.sarjmetre.ui.components.LargeTitlePage
import com.anils.sarjmetre.ui.components.ListRow
import com.anils.sarjmetre.ui.components.RowSeparator
import com.anils.sarjmetre.ui.components.SegmentedControl
import com.anils.sarjmetre.ui.glass.GlassToggle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    engine: MeterEngine,
    scrollState: ScrollState,
    reduceTransparency: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = LocalAppleColors.current
    val settings = engine.settings
    val state by LiveState.state.collectAsStateWithLifecycle()
    val running by LiveState.running.collectAsStateWithLifecycle()

    var enabled by remember { mutableStateOf(settings.serviceEnabled) }
    // Picks up a stop from the notification's button.
    LaunchedEffect(running) { enabled = settings.serviceEnabled }
    var onlyWhilePlugged by remember { mutableStateOf(settings.batteryOnlyWhilePlugged) }
    var netEnabled by remember { mutableStateOf(settings.netMeterEnabled) }
    var iconMode by remember { mutableStateOf(settings.iconMode) }
    var interval by remember { mutableLongStateOf(settings.intervalMs) }
    var target by remember { mutableIntStateOf(settings.targetLevel) }
    var unitMode by remember { mutableStateOf(settings.unitMode) }
    var signMode by remember { mutableStateOf(settings.signMode) }
    var capacityText by remember { mutableStateOf(settings.capacityOverrideMah.takeIf { it > 0 }?.toString().orEmpty()) }
    var detectionRestarted by remember { mutableStateOf(false) }
    var todayReset by remember { mutableStateOf(false) }
    // A done-row reads its confirmation for a moment, then offers the action again.
    LaunchedEffect(detectionRestarted) {
        if (detectionRestarted) {
            delay(2000)
            detectionRestarted = false
        }
    }
    LaunchedEffect(todayReset) {
        if (todayReset) {
            delay(2000)
            todayReset = false
        }
    }

    LargeTitlePage(title = "Ayarlar", scrollState = scrollState) {
        InsetGroup(header = "Gösterge", footer = "Ekran kapalıyken gösterge güncellenmez, böylece pil harcamaz.") {
            ListRow {
                Column(Modifier.weight(1f)) {
                    Text("Göstergeyi çalıştır", style = AppleType.body, color = colors.label)
                    Text("Status bar ikonu ve bildirim", style = AppleType.footnote, color = colors.secondaryLabel)
                }
                Spacer(Modifier.width(12.dp))
                GlassToggle(
                    checked = { enabled },
                    onCheckedChange = { on ->
                        enabled = on
                        settings.serviceEnabled = on
                        if (on) onStart() else onStop()
                    },
                    onColor = colors.green,
                    offColor = colors.fill,
                    reduceTransparency = reduceTransparency,
                )
            }
            RowSeparator()
            ListRow {
                Column(Modifier.weight(1f)) {
                    Text("Sadece şarjdayken", style = AppleType.body, color = colors.label)
                    Text("Şarj takılı değilken mA ikonu gizlenir", style = AppleType.footnote, color = colors.secondaryLabel)
                }
                Spacer(Modifier.width(12.dp))
                GlassToggle(
                    checked = { onlyWhilePlugged },
                    onCheckedChange = { on ->
                        onlyWhilePlugged = on
                        settings.batteryOnlyWhilePlugged = on
                    },
                    onColor = colors.green,
                    offColor = colors.fill,
                    reduceTransparency = reduceTransparency,
                )
            }
            RowSeparator()
            ControlRow("Status bar'da göster") {
                SegmentedControl(
                    options = listOf(IconMode.MILLIAMPS to "Akım (mA)", IconMode.WATTS to "Güç (W)"),
                    selected = iconMode,
                    onSelect = { iconMode = it; settings.iconMode = it },
                )
            }
            RowSeparator()
            ControlRow("Güncelleme sıklığı") {
                SegmentedControl(
                    options = listOf(1000L to "1 sn", 2000L to "2 sn", 5000L to "5 sn"),
                    selected = interval,
                    onSelect = { interval = it; settings.intervalMs = it },
                )
            }
        }

        InsetGroup(
            header = "Ağ hızı",
            footer = "İndirme ve yüklemenin toplamı, mA göstergesiyle aynı sıklıkta güncellenir.",
        ) {
            ListRow {
                Column(Modifier.weight(1f)) {
                    Text("Ağ hızını göster", style = AppleType.body, color = colors.label)
                    Text("İkinci status bar ikonu", style = AppleType.footnote, color = colors.secondaryLabel)
                }
                Spacer(Modifier.width(12.dp))
                GlassToggle(
                    checked = { netEnabled },
                    onCheckedChange = { on ->
                        netEnabled = on
                        settings.netMeterEnabled = on
                    },
                    onColor = colors.green,
                    offColor = colors.fill,
                    reduceTransparency = reduceTransparency,
                )
            }
        }

        InsetGroup(
            header = "Şarj",
            footer = "Samsung'da Pil koruma açıksa aynı değeri seç; dolma süresi bu sınıra göre hesaplanır.",
        ) {
            ControlRow("Şarj sınırı") {
                SegmentedControl(
                    options = listOf(100, 95, 90, 85, 80).map { it to "%$it" },
                    selected = target,
                    onSelect = { target = it; settings.targetLevel = it },
                )
            }
        }

        InsetGroup(header = "Pil", footer = capacityFooter(state)) {
            ListRow {
                Text("Kapasite", style = AppleType.body, color = colors.label, modifier = Modifier.weight(1f))
                BasicTextField(
                    value = capacityText,
                    onValueChange = { text ->
                        capacityText = text.filter(Char::isDigit).take(5)
                        settings.capacityOverrideMah = capacityText.toIntOrNull() ?: 0
                    },
                    textStyle = AppleType.body.merge(AppleType.figures).copy(color = colors.label, textAlign = TextAlign.End),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(colors.green),
                    modifier = Modifier.widthIn(min = 64.dp, max = 120.dp),
                    decorationBox = { field ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (capacityText.isEmpty()) Text("Otomatik", style = AppleType.body, color = colors.tertiaryLabel)
                            field()
                        }
                    },
                )
                if (capacityText.isNotEmpty()) {
                    Text(" mAh", style = AppleType.body, color = colors.secondaryLabel)
                }
            }
        }

        InsetGroup(header = "Ölçüm", footer = detectionFooter(state)) {
            ControlRow("Akım birimi") {
                SegmentedControl(
                    options = listOf(UnitMode.AUTO to "Otomatik", UnitMode.MICROAMPS to "µA", UnitMode.MILLIAMPS to "mA"),
                    selected = unitMode,
                    onSelect = { unitMode = it; settings.unitMode = it },
                )
            }
            RowSeparator()
            ControlRow("Akım yönü") {
                SegmentedControl(
                    options = listOf(SignMode.AUTO to "Otomatik", SignMode.NORMAL to "Normal", SignMode.INVERTED to "Ters"),
                    selected = signMode,
                    onSelect = { signMode = it; settings.signMode = it },
                )
            }
            RowSeparator()
            ActionRow(
                if (detectionRestarted) "Tespit yeniden başladı" else "Otomatik tespiti yeniden yap",
                if (detectionRestarted) colors.secondaryLabel else colors.blue,
            ) {
                engine.resetDetection()
                detectionRestarted = true
            }
        }

        InsetGroup {
            ActionRow(
                if (todayReset) "Bugünün sayaçları sıfırlandı" else "Bugünün sayaçlarını sıfırla",
                if (todayReset) colors.secondaryLabel else colors.red,
            ) {
                engine.resetToday()
                todayReset = true
            }
        }
    }
}

private fun capacityFooter(state: MeterState?): String {
    if (state == null) return "Boş bırakırsan kapasite otomatik bulunur."
    val source = when (state.capacitySource) {
        CapacitySource.MANUAL -> "elle girilen"
        CapacitySource.COUNTER -> "pil sayacından"
        CapacitySource.DESIGN -> "fabrika değeri"
        CapacitySource.DEFAULT -> "varsayılan tahmin"
    }
    return "Hesaplarda ${state.capacityMah.roundToInt()} mAh kullanılıyor ($source). Boş bırakırsan otomatik bulunur."
}

private fun detectionFooter(state: MeterState?): String? {
    state ?: return null
    val unit = if (state.unitIsMicro) "µA" else "mA"
    val sign = when (state.signInverted) {
        null -> "Yön, telefon şarjdan çıkarılınca birkaç saniyede anlaşılır."
        true -> "Değer ters çevrilerek gösteriliyor."
        false -> "Değer olduğu gibi gösteriliyor."
    }
    return "Telefonun verdiği değer ${state.currentRaw ?: "–"}, $unit olarak okunuyor. $sign"
}
