package com.jjswigut.eventide.alerts

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jjswigut.eventide.MainActivity
import com.jjswigut.eventide.R
import com.jjswigut.eventide.alerts.TideAlertScheduler.Companion.KEY_EVENT_TIME_MILLIS
import com.jjswigut.eventide.alerts.TideAlertScheduler.Companion.KEY_LEAD_TIME_MINUTES
import com.jjswigut.eventide.alerts.TideAlertScheduler.Companion.KEY_STATION_ID
import com.jjswigut.eventide.alerts.TideAlertScheduler.Companion.KEY_STATION_NAME
import com.jjswigut.eventide.alerts.TideAlertScheduler.Companion.KEY_TIDE_HEIGHT
import com.jjswigut.eventide.alerts.TideAlertScheduler.Companion.KEY_TIDE_TIME
import com.jjswigut.eventide.alerts.TideAlertScheduler.Companion.KEY_TIDE_TYPE
import com.jjswigut.eventide.repository.TideAlertRepository
import org.koin.core.context.GlobalContext

class TideAlertNotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val stationId = inputData.getString(KEY_STATION_ID) ?: return Result.failure()
        val stationName = inputData.getString(KEY_STATION_NAME) ?: stationId
        val tideType = inputData.getString(KEY_TIDE_TYPE) ?: "Tide"
        val tideTime = inputData.getString(KEY_TIDE_TIME).orEmpty()
        val tideHeight = inputData.getString(KEY_TIDE_HEIGHT).orEmpty()
        val leadTimeMinutes = inputData.getInt(KEY_LEAD_TIME_MINUTES, DEFAULT_LEAD_TIME_MINUTES)
        val eventTimeMillis = inputData.getLong(KEY_EVENT_TIME_MILLIS, 0L)
        val koin = GlobalContext.get()
        val alertRepository = koin.get<TideAlertRepository>()
        val scheduler = koin.get<TideAlertScheduler>()

        val alert = alertRepository.getAlertPreference(stationId)
        if (alert == null || !alert.enabled) {
            scheduler.cancel(stationId)
            return Result.success()
        }

        if (canPostNotifications()) {
            showNotification(
                stationId = stationId,
                stationName = stationName,
                tideType = tideType,
                tideTime = tideTime,
                tideHeight = tideHeight,
                leadTimeMinutes = leadTimeMinutes,
            )
        }

        scheduler.schedule(
            stationId = stationId,
            minEventTimeMillis = eventTimeMillis + ONE_MINUTE_MILLIS,
        )

        return Result.success()
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        stationId: String,
        stationName: String,
        tideType: String,
        tideTime: String,
        tideHeight: String,
        leadTimeMinutes: Int,
    ) {
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            stationId.hashCode(),
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = "$tideType tide at $stationName"
        val body = "$tideTime, $tideHeight. Alert set $leadTimeMinutes minutes ahead."
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_eventide)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(stationId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "tide_alerts"
        const val CHANNEL_NAME = "Tide alerts"
        private const val DEFAULT_LEAD_TIME_MINUTES = 30
        private const val ONE_MINUTE_MILLIS = 60 * 1000L
    }
}
