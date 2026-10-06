package com.anils.sarjmetre.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import com.anils.sarjmetre.R
import com.anils.sarjmetre.service.MeterService
import com.anils.sarjmetre.ui.MainActivity

/**
 * Builds the meter notification. One builder, its PendingIntents and its action are made once and
 * reused, so a tick only sets the changing parts instead of asking the system for PendingIntents again.
 */
class MeterNotification(context: Context) {
    val staticIcon: Icon = Icon.createWithResource(context, R.drawable.ic_stat_bolt)
    private val bigText = Notification.BigTextStyle()
    private val builder = Notification.Builder(context, CHANNEL_ID)
        // Android 16 bundles an app's notifications, and a bundle shows a single static icon in the
        // status bar. A colorized foreground service notification is exempt, so this one and the
        // network meter each keep their own live icon. Dark gray keeps the card close to a plain one.
        .setColor(context.getColor(R.color.notification_card))
        .setColorized(true)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setLocalOnly(true)
        .setCategory(Notification.CATEGORY_STATUS)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        .addAction(stopAction(context))
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            }
        }

    /**
     * Takes the foreground notification's place off the charger while the network meter is off.
     * Its channel is the lowest importance, so it shows no status bar icon.
     */
    val quiet: Notification = Notification.Builder(context, QUIET_CHANNEL_ID)
        .setSmallIcon(staticIcon)
        .setContentTitle("Live Meter")
        .setContentText("Şarj takılınca gösterge açılır")
        .setOngoing(true)
        .setShowWhen(false)
        .setLocalOnly(true)
        .setCategory(Notification.CATEGORY_STATUS)
        .addAction(stopAction(context))
        .build()

    fun build(icon: Icon, content: MeterContent?): Notification {
        // The shade orders notifications of equal importance by this time, newest on top. A reused
        // builder would keep the service's start time and sink below everything posted since.
        builder.setWhen(System.currentTimeMillis()).setSmallIcon(icon)
        if (content == null) {
            builder.setContentTitle("Live Meter").setContentText("Ölçüm başlıyor…").setStyle(null)
        } else {
            builder.setContentTitle(content.title)
                .setContentText(content.today)
                .setStyle(bigText.bigText(content.today + "\n" + content.details))
        }
        return builder.build()
    }

    companion object {
        const val ID = 1
        private const val CHANNEL_ID = "live_meter"
        private const val QUIET_CHANNEL_ID = "background"

        /** Stops the whole service; every notification that holds the foreground place carries it. */
        fun stopAction(context: Context): Notification.Action = Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_stat_bolt),
            "Durdur",
            PendingIntent.getService(
                context,
                1,
                Intent(context, MeterService::class.java).setAction(MeterService.ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()

        fun createChannel(context: Context) {
            // DEFAULT rather than LOW: some Android versions hide status bar icons of silent channels.
            // Sound and vibration are off, so it still never makes a noise.
            val channel = NotificationChannel(CHANNEL_ID, "Canlı şarj göstergesi", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Status bar'daki anlık mA göstergesi"
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val quiet = NotificationChannel(QUIET_CHANNEL_ID, "Arka plan", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Şarj dışında ve ağ göstergesi kapalıyken uygulamayı açık tutar"
                setShowBadge(false)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(channel, quiet))
        }
    }
}
