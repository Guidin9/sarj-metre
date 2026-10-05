package com.anils.sarjmetre.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anils.sarjmetre.LiveState
import com.anils.sarjmetre.MeterEngine
import com.anils.sarjmetre.MeterState
import com.anils.sarjmetre.R
import com.anils.sarjmetre.battery.CapacitySource
import com.anils.sarjmetre.settings.IconMode
import com.anils.sarjmetre.settings.SignMode
import com.anils.sarjmetre.settings.UnitMode
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(engine: MeterEngine, onBack: () -> Unit, onStart: () -> Unit, onStop: () -> Unit) {
    val colors = LocalMeterColors.current
    val settings = engine.settings
    val state by LiveState.state.collectAsStateWithLifecycle()
    val running by LiveState.running.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(settings.serviceEnabled) }
    // Picks up a stop from the notification's button.
    LaunchedEffect(running) { enabled = settings.serviceEnabled }
    var iconMode by remember { mutableStateOf(settings.iconMode) }
    var interval by remember { mutableLongStateOf(settings.intervalMs) }
    var target by remember { mutableIntStateOf(settings.targetLevel) }
    var unitMode by remember { mutableStateOf(settings.unitMode) }
    var signMode by remember { mutableStateOf(settings.signMode) }
    var capacityText by remember {
        mutableStateOf(settings.capacityOverrideMah.takeIf { it > 0 }?.toString().orEmpty())
    }

    Scaffold(
        containerColor = colors.background,
        topBar = { BackBar("Ayarlar", onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Group("Gösterge", first = true) {
                SwitchRow(
                    title = "Göstergeyi çalıştır",
                    caption = "Kapatınca status bar'daki akım ve bildirim kalkar.",
                    checked = enabled,
                ) { on ->
                    enabled = on
                    settings.serviceEnabled = on
                    if (on) onStart() else onStop()
                }
                Choice(
                    label = "Status bar'da göster",
                    options = listOf(IconMode.MILLIAMPS to "Akım (mA)", IconMode.WATTS to "Güç (W)"),
                    selected = iconMode,
                ) { iconMode = it; settings.iconMode = it }
                Choice(
                    label = "Güncelleme sıklığı",
                    options = listOf(1000L to "1 sn", 2000L to "2 sn", 5000L to "5 sn"),
                    selected = interval,
                    caption = "Ekran kapalıyken gösterge güncellenmez, böylece pil harcamaz.",
                ) { interval = it; settings.intervalMs = it }
            }

            Group("Şarj") {
                Choice(
                    label = "Şarj sınırı",
                    options = listOf(100, 95, 90, 85, 80).map { it to "%$it" },
                    selected = target,
                    caption = "Samsung'da Pil koruma açıksa aynı yüzdeyi seç; dolma süresi bu sınıra göre hesaplanır.",
                ) { target = it; settings.targetLevel = it }
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { text ->
                        capacityText = text.filter(Char::isDigit).take(5)
                        settings.capacityOverrideMah = capacityText.toIntOrNull() ?: 0
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pil kapasitesi (mAh)") },
                    placeholder = { Text("Otomatik") },
                    supportingText = { Text(capacityCaption(state)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }

            Group("Ölçüm") {
                Choice(
                    label = "Akım birimi",
                    options = listOf(UnitMode.AUTO to "Otomatik", UnitMode.MICROAMPS to "µA", UnitMode.MILLIAMPS to "mA"),
                    selected = unitMode,
                    caption = state?.let {
                        "Telefonun verdiği değer ${it.currentRaw ?: "–"}, ${if (it.unitIsMicro) "µA" else "mA"} olarak okunuyor."
                    },
                ) { unitMode = it; settings.unitMode = it }
                Choice(
                    label = "Akım yönü",
                    options = listOf(SignMode.AUTO to "Otomatik", SignMode.NORMAL to "Normal", SignMode.INVERTED to "Ters"),
                    selected = signMode,
                    caption = state?.let {
                        when (it.signInverted) {
                            null -> "Yönü anlamak için telefon birkaç saniye şarjdan çıkarılmış olmalı."
                            true -> "Telefonun verdiği değer ters çevrilerek gösteriliyor."
                            false -> "Telefonun verdiği değer olduğu gibi gösteriliyor."
                        }
                    },
                ) { signMode = it; settings.signMode = it }
                TextButton(
                    onClick = {
                        engine.resetDetection()
                        scope.launch { snackbar.showSnackbar("Otomatik tespit yeniden başladı") }
                    },
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) { Text("Otomatik tespiti yeniden yap") }
            }

            Group("Bugün") {
                Text(
                    "Sayaçlar her gece yarısı kendiliğinden sıfırlanır.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                )
                TextButton(
                    onClick = {
                        engine.resetToday()
                        scope.launch { snackbar.showSnackbar("Bugünün sayaçları sıfırlandı") }
                    },
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) { Text("Bugünün sayaçlarını sıfırla") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BackBar(title: String, onBack: () -> Unit) {
    val colors = LocalMeterColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 20.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Geri", tint = colors.ink)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.ink)
    }
}

@Composable
private fun Group(title: String, first: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalMeterColors.current
    if (!first) HorizontalDivider(color = colors.hairline)
    Column(Modifier.padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.ink)
        content()
    }
}

@Composable
private fun SwitchRow(title: String, caption: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalMeterColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = 16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.ink)
            Text(caption, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Choice(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    caption: String? = null,
    onSelect: (T) -> Unit,
) {
    val colors = LocalMeterColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = colors.ink)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, text) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    icon = {},
                ) { Text(text, maxLines = 1) }
            }
        }
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        }
    }
}

private fun capacityCaption(state: MeterState?): String {
    if (state == null) return "Boş bırakırsan otomatik bulunur."
    val source = when (state.capacitySource) {
        CapacitySource.MANUAL -> "elle girilen"
        CapacitySource.COUNTER -> "pil sayacından"
        CapacitySource.DESIGN -> "fabrika değeri"
        CapacitySource.DEFAULT -> "varsayılan tahmin"
    }
    return "Hesaplarda ${state.capacityMah.roundToInt()} mAh kullanılıyor ($source). Boş bırakırsan otomatik bulunur."
}
