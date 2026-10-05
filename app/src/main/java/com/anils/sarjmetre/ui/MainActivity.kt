package com.anils.sarjmetre.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.anils.sarjmetre.MeterEngine
import com.anils.sarjmetre.R
import com.anils.sarjmetre.service.MeterService
import com.anils.sarjmetre.ui.glass.GlassTab
import com.anils.sarjmetre.ui.glass.GlassTabBar
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

class MainActivity : ComponentActivity() {
    // The meter runs either way; without this permission its notification is just hidden.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { MeterService.start(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val engine = MeterEngine.get(this)
        if (savedInstanceState == null && engine.settings.serviceEnabled) startMeter()
        setContent {
            SarjMetreTheme {
                val colors = LocalAppleColors.current
                val reduceTransparency = rememberReduceTransparency()
                // Everything in the content layer is captured here so the glass tab bar can refract it.
                val backdrop = rememberLayerBackdrop()
                var tab by rememberSaveable { mutableIntStateOf(0) }
                val meterScroll = rememberScrollState()
                val netScroll = rememberScrollState()
                val settingsScroll = rememberScrollState()
                BackHandler(enabled = tab != 0) { tab = 0 }

                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop),
                    ) {
                        when (tab) {
                            0 -> MeterScreen(
                                scrollState = meterScroll,
                                intervalMs = engine.settings.intervalMs,
                                onStart = {
                                    engine.settings.serviceEnabled = true
                                    startMeter()
                                },
                            )
                            1 -> NetScreen(
                                scrollState = netScroll,
                                history = engine.net.history,
                                netEnabled = engine.settings.netMeterEnabled,
                            )
                            else -> SettingsScreen(
                                engine = engine,
                                scrollState = settingsScroll,
                                reduceTransparency = reduceTransparency,
                                onStart = { startMeter() },
                                onStop = { MeterService.stop(this@MainActivity) },
                            )
                        }
                    }

                    GlassTabBar(
                        selectedTabIndex = { tab },
                        onTabSelected = { tab = it },
                        backdrop = backdrop,
                        tabsCount = 3,
                        accentColor = colors.green,
                        containerColor = colors.glassSurface,
                        selectionColor = colors.label.copy(alpha = 0.1f),
                        reduceTransparency = reduceTransparency,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp)
                            .width(300.dp),
                    ) {
                        GlassTab(selected = tab == 0, onClick = { tab = 0 }) {
                            Icon(painterResource(R.drawable.ic_bolt_fill), contentDescription = null, tint = colors.label, modifier = Modifier.size(24.dp))
                            Text("Batarya", style = AppleType.tabLabel, color = colors.label)
                        }
                        GlassTab(selected = tab == 1, onClick = { tab = 1 }) {
                            Icon(painterResource(R.drawable.ic_stat_net), contentDescription = null, tint = colors.label, modifier = Modifier.size(24.dp))
                            Text("Ağ", style = AppleType.tabLabel, color = colors.label)
                        }
                        GlassTab(selected = tab == 2, onClick = { tab = 2 }) {
                            Icon(painterResource(R.drawable.ic_gear_fill), contentDescription = null, tint = colors.label, modifier = Modifier.size(24.dp))
                            Text("Ayarlar", style = AppleType.tabLabel, color = colors.label)
                        }
                    }
                }
            }
        }
    }

    private fun startMeter() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            MeterService.start(this)
        }
    }
}
