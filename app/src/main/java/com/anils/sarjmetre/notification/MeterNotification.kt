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

object MeterNotification {
    const val ID = 1
    private const val CHANNEL_ID = "live_meter"

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
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun staticIcon(context: Context): Icon = Icon.createWithResource(context, R.drawable.ic_stat_bolt)

    fun build(context: Context, icon: Icon, content: MeterContent?): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            context,
            1,
            Intent(context, MeterService::class.java).setAction(MeterService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setColor(context.getColor(R.color.brand))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setLocalOnly(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openApp)
            .addAction(Notification.Action.Builder(staticIcon(context), "Durdur", stop).build())
        if (content == null) {
            builder.setContentTitle("Şarj Metre").setContentText("Ölçüm başlıyor…")
        } else {
            builder.setContentTitle(content.title)
                .setContentText(content.today)
                .setStyle(Notification.BigTextStyle().bigText(content.today + "\n" + content.details))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }
}
