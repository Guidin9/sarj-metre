package com.anils.sarjmetre.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.anils.sarjmetre.R
import com.anils.sarjmetre.ui.MainActivity

/** The network speed notification: a second status bar icon next to the current one, built the same reusing way. */
class NetNotification(context: Context) {
    val staticIcon: Icon = Icon.createWithResource(context, R.drawable.ic_stat_net)
    private val builder = Notification.Builder(context, CHANNEL_ID)
        .setColor(context.getColor(R.color.brand))
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

    // Fresh time on every post keeps it near the top of the shade, like the current meter.
    fun build(icon: Icon, content: MeterContent): Notification =
        builder.setWhen(System.currentTimeMillis()).setSmallIcon(icon).setContentTitle(content.title).setContentText(content.today).build()

    companion object {
        const val ID = 2
        private const val CHANNEL_ID = "net_speed"

        fun createChannel(context: Context) {
            // Same reasoning as the current meter's channel: DEFAULT keeps the icon visible, and it never makes a sound.
            val channel = NotificationChannel(CHANNEL_ID, "Ağ hızı göstergesi", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Status bar'daki anlık ağ hızı"
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.deleteNotificationChannel("net_speed_silent") // Left over from a trial build.
            manager.createNotificationChannel(channel)
        }
    }
}
