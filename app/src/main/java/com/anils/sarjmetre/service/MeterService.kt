package com.anils.sarjmetre.service

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.anils.sarjmetre.BuildConfig
import com.anils.sarjmetre.LiveState
import com.anils.sarjmetre.MeterEngine
import com.anils.sarjmetre.MeterState
import com.anils.sarjmetre.net.NetText
import com.anils.sarjmetre.notification.MeterNotification
import com.anils.sarjmetre.notification.MeterText
import com.anils.sarjmetre.notification.NetNotification
import com.anils.sarjmetre.notification.PostGate
import com.anils.sarjmetre.notification.StatusIconRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Keeps the live meter in the status bar. Samples on a timer while the screen is on; with the
 * screen off it only listens to battery broadcasts, so today's totals keep counting for free.
 * The timer slows down while the phone is warm, and the icon is only redrawn when it would change.
 * When enabled, the same timer drives a second icon with the network speed.
 */
class MeterService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null
    private lateinit var engine: MeterEngine
    private lateinit var notifications: NotificationManager
    private lateinit var notification: MeterNotification
    private lateinit var renderer: StatusIconRenderer
    private lateinit var power: PowerManager
    private lateinit var netNotification: NetNotification
    private val gate = PostGate(Pacing.ICON_THRESHOLD_MA, Pacing.TEXT_EVERY_MS)
    private val netGate = PostGate(thresholdMa = 0, textEveryMs = Pacing.TEXT_EVERY_MS)
    private var netActive = false
    private var batteryIntent: Intent? = null
    private var receiversRegistered = false
    private var screenOn = true
    private var thermalStatus = PowerManager.THERMAL_STATUS_NONE
    private var thermalListener: Any? = null
    private var updates = 0

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val old = batteryIntent
            batteryIntent = intent
            // With the screen off broadcasts are the only updates, and they keep today's totals counting.
            val urgent = Pacing.isUrgent(old?.plugged(), old?.status(), intent.plugged(), intent.status())
            if (ticker == null || urgent) update()
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
                pauseNet()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        engine = MeterEngine.get(this)
        notifications = getSystemService(NotificationManager::class.java)
        notification = MeterNotification(this)
        netNotification = NetNotification(this)
        renderer = StatusIconRenderer((ICON_DP * resources.displayMetrics.density).roundToInt())
        power = getSystemService(PowerManager::class.java)
        if (!enterForeground()) return

        LiveState.setRunning(true)
        screenOn = power.isInteractive
        listenToThermalStatus()
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
        if (netActive) notifications.cancel(NetNotification.ID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (thermalListener as? PowerManager.OnThermalStatusChangedListener)?.let(power::removeThermalStatusListener)
        }
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
        return try {
            ServiceCompat.startForeground(this, MeterNotification.ID, notification.build(notification.staticIcon, null), type)
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
                updateNet()
                update().currentMa?.let(LiveState::appendSample)
                delay(Pacing.intervalMs(engine.settings.intervalMs, thermalStatus))
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    /** Samsung reports MODERATE and up when the phone gets warm; the timer then slows down. */
    private fun listenToThermalStatus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        thermalStatus = power.currentThermalStatus
        val listener = PowerManager.OnThermalStatusChangedListener { thermalStatus = it }
        power.addThermalStatusListener(listener)
        thermalListener = listener
    }

    private fun update(): MeterState {
        val state = engine.sample(batteryIntent)
        LiveState.publish(state)
        if (screenOn) post(state)
        if (BuildConfig.DEBUG && updates++ % LOG_EVERY == 0) log(state)
        return state
    }

    private fun post(state: MeterState) {
        // The notification text shows the same held current as the icon.
        val shown = state.copy(currentMa = gate.iconMa(state.currentMa, state.isPlugged))
        val content = MeterText.content(shown, engine.settings.iconMode)
        if (!gate.shouldPost(content, SystemClock.elapsedRealtime())) return
        val icon = Icon.createWithBitmap(renderer.render(content.iconValue, content.iconUnit))
        notifications.notify(MeterNotification.ID, notification.build(icon, content))
    }

    private fun postStatic() {
        gate.reset()
        val content = LiveState.state.value?.let { MeterText.content(it, engine.settings.iconMode) }
        notifications.notify(MeterNotification.ID, notification.build(notification.staticIcon, content))
    }

    private fun updateNet() {
        if (!engine.settings.netMeterEnabled) {
            if (netActive) stopNet()
            return
        }
        val state = engine.net.sample() ?: return
        netActive = true
        if (engine.net.todayDue()) refreshNetToday()
        LiveState.publishNet(state)
        val content = NetText.content(state)
        if (!netGate.shouldPost(content, SystemClock.elapsedRealtime())) return
        val icon = Icon.createWithBitmap(renderer.render(content.iconValue, content.iconUnit))
        notifications.notify(NetNotification.ID, netNotification.build(icon, content))
    }

    /** Today's use is a query into the system's statistics, so it runs on a background thread. */
    private fun refreshNetToday() {
        engine.net.setToday(LiveState.net.value?.today) // Not due again while this one runs.
        scope.launch {
            val today = withContext(Dispatchers.IO) { engine.net.history.today() }
            engine.net.setToday(today)
        }
    }

    /** Screen off: the speed would only average over the gap later, so restart it and show a static icon. */
    private fun pauseNet() {
        engine.net.pause()
        netGate.reset()
        val state = LiveState.net.value ?: return
        if (netActive) notifications.notify(NetNotification.ID, netNotification.build(netNotification.staticIcon, NetText.pausedContent(state)))
    }

    private fun stopNet() {
        engine.net.pause()
        netGate.reset()
        netActive = false
        notifications.cancel(NetNotification.ID)
        LiveState.publishNet(null)
    }

    private fun Intent.plugged() = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

    private fun Intent.status() = getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)

    /** Raw values for checking unit/sign detection on a new phone (debug builds): adb logcat -s SarjMetre */
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
