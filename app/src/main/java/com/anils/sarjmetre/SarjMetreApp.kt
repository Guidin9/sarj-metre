package com.anils.sarjmetre

import android.app.Application
import com.anils.sarjmetre.notification.MeterNotification
import com.anils.sarjmetre.notification.NetNotification

class SarjMetreApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MeterNotification.createChannel(this)
        NetNotification.createChannel(this)
    }
}
