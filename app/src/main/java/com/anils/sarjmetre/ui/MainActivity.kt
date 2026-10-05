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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.anils.sarjmetre.MeterEngine
import com.anils.sarjmetre.service.MeterService

private enum class Screen { Main, Settings }

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
                var screen by rememberSaveable { mutableStateOf(Screen.Main) }
                // The dial's self-test sweep plays once per launch, not on every return from settings.
                var dialSwept by rememberSaveable { mutableStateOf(false) }
                BackHandler(enabled = screen == Screen.Settings) { screen = Screen.Main }
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        val forward = targetState == Screen.Settings
                        (slideInHorizontally { if (forward) it else -it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { if (forward) -it / 4 else it } + fadeOut())
                    },
                    label = "screen",
                ) { current ->
                    when (current) {
                        Screen.Main -> MainScreen(
                            dialSwept = dialSwept,
                            onDialSwept = { dialSwept = true },
                            onStart = {
                                engine.settings.serviceEnabled = true
                                startMeter()
                            },
                            onOpenSettings = { screen = Screen.Settings },
                        )
                        Screen.Settings -> SettingsScreen(
                            engine = engine,
                            onBack = { screen = Screen.Main },
                            onStart = { startMeter() },
                            onStop = { MeterService.stop(this@MainActivity) },
                        )
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
