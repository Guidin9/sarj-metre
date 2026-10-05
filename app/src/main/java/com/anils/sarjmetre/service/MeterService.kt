package com.anils.sarjmetre.service

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.anils.sarjmetre.LiveState
import com.anils.sarjmetre.MeterEngine
import com.anils.sarjmetre.MeterState
import com.anils.sarjmetre.notification.MeterContent
import com.anils.sarjmetre.notification.MeterNotification
import com.anils.sarjmetre.notification.MeterText
import com.anils.sarjmetre.notification.StatusIconRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Keeps the live meter in the status bar. Samples on a timer while the screen is on; with the
 * screen off it only listens to battery broadcasts, so today's totals keep counting for free.
 */
class MeterService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null
    private lateinit var engine: MeterEngine
    private lateinit var notifications: NotificationManager
    private lateinit var renderer: StatusIconRenderer
    private var batteryIntent: Intent? = null
    private var receiversRegistered = false
    private var screenOn = true
    private var lastPosted: MeterContent? = null
    private var updates = 0

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            batteryIntent = intent
            update()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            screenOn = intent.action == Intent.ACTION_SCREEN_ON
            if (screenOn) {
                startTicker()
            } else {
                stopTicker()
                // Nothing updates while the screen is off, so don't leave a stale number on the lock screen or AOD.
                postStatic()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        engine = MeterEngine.get(this)
        notifications = getSystemService(NotificationManager::class.java)
        renderer = StatusIconRenderer((ICON_DP * resources.displayMetrics.density).roundToInt())
        if (!enterForeground()) return

        LiveState.setRunning(true)
        screenOn = getSystemService(PowerManager::class.java).isInteractive
        // Registering returns the sticky battery intent and also delivers it to onReceive right away.
        batteryIntent = ContextCompat.registerReceiver(
            this, batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(this, screenReceiver, screenFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiversRegistered = true
        if (screenOn) startTicker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            engine.settings.serviceEnabled = false
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        if (receiversRegistered) {
            unregisterReceiver(batteryReceiver)
            unregisterReceiver(screenReceiver)
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        LiveState.setRunning(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun enterForeground(): Boolean {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        val notification = MeterNotification.build(this, MeterNotification.staticIcon(this), null)
        return try {
            ServiceCompat.startForeground(this, MeterNotification.ID, notification, type)
            true
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException when the system restarts us in the background
            // and the app has no battery optimization exemption.
            Log.w(TAG, "Could not enter the foreground", e)
            stopSelf()
            false
        }
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                update()
                delay(engine.settings.intervalMs)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun update() {
        val state = engine.sample(batteryIntent)
        LiveState.publish(state)
        if (screenOn) post(state)
        if (updates++ % LOG_EVERY == 0) log(state)
    }

    private fun post(state: MeterState) {
        val content = MeterText.content(state, engine.settings.iconMode)
        if (content == lastPosted) return
        lastPosted = content
        val icon = Icon.createWithBitmap(renderer.render(content.iconValue, content.iconUnit))
        notifications.notify(MeterNotification.ID, MeterNotification.build(this, icon, content))
    }

    private fun postStatic() {
        lastPosted = null
        val content = LiveState.state.value?.let { MeterText.content(it, engine.settings.iconMode) }
        notifications.notify(MeterNotification.ID, MeterNotification.build(this, MeterNotification.staticIcon(this), content))
    }

    /** Raw values for checking unit/sign detection on a new phone: adb logcat -s SarjMetre */
    private fun log(state: MeterState) {
        Log.d(
            TAG,
            "raw=${state.currentRaw} counter=${state.counterRaw} mA=${state.currentMa} level=${state.level} " +
                "status=${state.status} plugged=${state.plugged} mV=${state.voltageMv} T=${state.temperatureC} " +
                "micro=${state.unitIsMicro} inverted=${state.signInverted} " +
                "capacity=${state.capacityMah.roundToInt()}(${state.capacitySource}) " +
                "toFull=${state.minutesToFull} systemToFull=${state.systemMinutesToFull} toEmpty=${state.minutesToEmpty}",
        )
    }

    companion object {
        const val ACTION_STOP = "com.anils.sarjmetre.action.STOP"
        private const val TAG = "SarjMetre"
        private const val ICON_DP = 24
        private const val LOG_EVERY = 10

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, MeterService::class.java))
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Service start refused", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MeterService::class.java))
        }
    }
}
