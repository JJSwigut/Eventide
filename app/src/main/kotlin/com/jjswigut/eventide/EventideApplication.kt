package com.jjswigut.eventide

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.jjswigut.eventide.alerts.TideAlertNotificationWorker
import com.jjswigut.eventide.di.initKoin

class EventideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            TideAlertNotificationWorker.CHANNEL_ID,
            TideAlertNotificationWorker.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Alerts before selected high and low tides."
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
