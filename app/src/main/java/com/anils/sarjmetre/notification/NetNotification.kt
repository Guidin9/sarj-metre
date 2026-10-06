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

/**
 * The network speed notification, built the same reusing way as the current one. Next to the current
 * meter it is a second status bar icon of its own; off the charger it takes the meter's foreground place.
 */
class NetNotification(context: Context) {
    val staticIcon: Icon = Icon.createWithResource(context, R.drawable.ic_stat_net)
    private val plainBuilder = newBuilder(context).setColor(context.getColor(R.color.brand))

    // In the foreground place it takes the meter's colorized card and stop button too: the colorized
    // foreground notification is what Android 16 never bundles (see MeterNotification). It has a builder
    // of its own because a builder keeps the actions of an earlier build even after they are removed.
    private val foregroundBuilder = newBuilder(context)
        .setColor(context.getColor(R.color.notification_card))
        .setColorized(true)
        .addAction(MeterNotification.stopAction(context))

    // Fresh time on every post keeps it near the top of the shade, like the current meter.
    fun build(icon: Icon, content: MeterContent, foreground: Boolean): Notification =
        (if (foreground) foregroundBuilder else plainBuilder)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(icon)
            .setContentTitle(content.title)
            .setContentText(content.today)
            .build()

    private fun newBuilder(context: Context) = Notification.Builder(context, CHANNEL_ID)
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
