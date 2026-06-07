package com.jjswigut.eventide.alerts

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class TideAlertScheduler(
    context: Context,
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun schedule(stationId: String, minEventTimeMillis: Long = 0L, delayMillis: Long = 0L) {
        val request = OneTimeWorkRequestBuilder<TideAlertScheduleWorker>()
            .setConstraints(networkConstraints)
            .setInputData(
                workDataOf(
                    KEY_STATION_ID to stationId,
                    KEY_MIN_EVENT_TIME_MILLIS to minEventTimeMillis,
                ),
            )
            .setInitialDelay(delayMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            scheduleWorkName(stationId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun scheduleNotification(
        stationId: String,
        stationName: String,
        tideType: String,
        tideTime: String,
        tideHeight: String,
        leadTimeMinutes: Int,
        eventTimeMillis: Long,
        delayMillis: Long,
    ) {
        val request = OneTimeWorkRequestBuilder<TideAlertNotificationWorker>()
            .setInputData(
                workDataOf(
                    KEY_STATION_ID to stationId,
                    KEY_STATION_NAME to stationName,
                    KEY_TIDE_TYPE to tideType,
                    KEY_TIDE_TIME to tideTime,
                    KEY_TIDE_HEIGHT to tideHeight,
                    KEY_LEAD_TIME_MINUTES to leadTimeMinutes,
                    KEY_EVENT_TIME_MILLIS to eventTimeMillis,
                ),
            )
            .setInitialDelay(delayMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            notificationWorkName(stationId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(stationId: String) {
        workManager.cancelUniqueWork(scheduleWorkName(stationId))
        workManager.cancelUniqueWork(notificationWorkName(stationId))
    }

    companion object {
        const val KEY_STATION_ID = "station_id"
        const val KEY_STATION_NAME = "station_name"
        const val KEY_TIDE_TYPE = "tide_type"
        const val KEY_TIDE_TIME = "tide_time"
        const val KEY_TIDE_HEIGHT = "tide_height"
        const val KEY_LEAD_TIME_MINUTES = "lead_time_minutes"
        const val KEY_EVENT_TIME_MILLIS = "event_time_millis"
        const val KEY_MIN_EVENT_TIME_MILLIS = "min_event_time_millis"

        private val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        private fun scheduleWorkName(stationId: String) = "tide-alert-schedule-$stationId"

        private fun notificationWorkName(stationId: String) = "tide-alert-notification-$stationId"
    }
}
