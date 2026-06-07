package com.jjswigut.eventide.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jjswigut.eventide.alerts.TideAlertScheduler.Companion.KEY_MIN_EVENT_TIME_MILLIS
import com.jjswigut.eventide.alerts.TideAlertScheduler.Companion.KEY_STATION_ID
import com.jjswigut.eventide.data.models.Tide
import com.jjswigut.eventide.data.models.TideAlertFilter
import com.jjswigut.eventide.data.models.TideDay
import com.jjswigut.eventide.data.models.TideValue
import com.jjswigut.eventide.network.utils.Either
import com.jjswigut.eventide.repository.FavoritesRepository
import com.jjswigut.eventide.repository.NoaaRepository
import com.jjswigut.eventide.repository.TideAlertRepository
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

class TideAlertScheduleWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val stationId = inputData.getString(KEY_STATION_ID) ?: return Result.failure()
        val koin = GlobalContext.get()
        val alertRepository = koin.get<TideAlertRepository>()
        val noaaRepository = koin.get<NoaaRepository>()
        val favoritesRepository = koin.get<FavoritesRepository>()
        val scheduler = koin.get<TideAlertScheduler>()

        val alert = alertRepository.getAlertPreference(stationId)
        if (alert == null || !alert.enabled) {
            scheduler.cancel(stationId)
            return Result.success()
        }

        val tidesResult = noaaRepository.getTidesForStation(stationId)
        if (tidesResult !is Either.Success) {
            return Result.retry()
        }

        val minEventTime = inputData.getLong(KEY_MIN_EVENT_TIME_MILLIS, 0L)
            .takeIf { it > 0L }
            ?.toLocalDateTime()
            ?: LocalDateTime.MIN
        val now = LocalDateTime.now()
        val event = tidesResult.value.toTideAlertEvents()
            .filter { it.tideValue.matches(alert.tideFilter) }
            .firstOrNull { it.dateTime.isAfter(now) && it.dateTime.isAfter(minEventTime) }

        if (event == null) {
            scheduler.schedule(
                stationId = stationId,
                minEventTimeMillis = now.toEpochMillis(),
                delayMillis = SIX_HOURS_MILLIS,
            )
            return Result.success()
        }

        val stationName = favoritesRepository.getFavorites()
            .first()
            .firstOrNull { it.id == stationId }
            ?.name
            ?: stationId
        val notificationAt = event.dateTime.minusMinutes(alert.leadTimeMinutes.toLong())
        val delayMillis = Duration.between(now, notificationAt).toMillis().coerceAtLeast(0L)

        scheduler.scheduleNotification(
            stationId = stationId,
            stationName = stationName,
            tideType = event.tideValue.name,
            tideTime = event.displayTime,
            tideHeight = event.height,
            leadTimeMinutes = alert.leadTimeMinutes,
            eventTimeMillis = event.dateTime.toEpochMillis(),
            delayMillis = delayMillis,
        )

        return Result.success()
    }

    private fun List<TideDay>.toTideAlertEvents(): List<TideAlertEvent> {
        return flatMap { tideDay ->
            val date = LocalDate.parse(tideDay.date, dayFormatter)
            tideDay.tides.mapNotNull { tide -> tide.toAlertEvent(date) }
        }.sortedBy { it.dateTime }
    }

    private fun Tide.toAlertEvent(date: LocalDate): TideAlertEvent? {
        val localTime = runCatching {
            LocalTime.parse(time.uppercase(Locale.US), timeFormatter)
        }.getOrNull() ?: return null

        return TideAlertEvent(
            dateTime = LocalDateTime.of(date, localTime),
            displayTime = time,
            tideValue = tideValue,
            height = height,
        )
    }

    private fun TideValue.matches(filter: TideAlertFilter): Boolean {
        return when (filter) {
            TideAlertFilter.High -> this == TideValue.High
            TideAlertFilter.Low -> this == TideValue.Low
            TideAlertFilter.Both -> true
        }
    }

    private fun Long.toLocalDateTime(): LocalDateTime {
        return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    private fun LocalDateTime.toEpochMillis(): Long {
        return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private data class TideAlertEvent(
        val dateTime: LocalDateTime,
        val displayTime: String,
        val tideValue: TideValue,
        val height: String,
    )

    companion object {
        private const val SIX_HOURS_MILLIS = 6 * 60 * 60 * 1000L
        private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")
        private val timeFormatter = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("h:mma")
            .toFormatter(Locale.US)
    }
}
