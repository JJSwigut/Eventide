package com.jjswigut.eventide.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.jjswigut.eventide.StationsDb
import com.jjswigut.eventide.data.models.TideAlertFilter
import com.jjswigut.eventide.data.models.TideAlertPreference
import com.jjswigut.eventide.db.toAlertPreferenceModel
import com.jjswigut.eventide.dispatchers.Dispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TideAlertRepositoryImpl(
    private val stationsDb: StationsDb,
    private val dispatcher: Dispatcher,
) : TideAlertRepository {

    override fun getAlertPreferences(): Flow<List<TideAlertPreference>> {
        return stationsDb.tidealertsQueries.selectAllAlertPreferences()
            .asFlow()
            .mapToList(dispatcher.io)
            .map { it.toAlertPreferenceModel() }
    }

    override fun getEnabledAlertPreferences(): Flow<List<TideAlertPreference>> {
        return stationsDb.tidealertsQueries.selectEnabledAlertPreferences()
            .asFlow()
            .mapToList(dispatcher.io)
            .map { it.toAlertPreferenceModel() }
    }

    override suspend fun getAlertPreference(stationId: String): TideAlertPreference? {
        return stationsDb.tidealertsQueries.selectAlertPreferenceByStationId(stationId)
            .executeAsOneOrNull()
            ?.let { listOf(it).toAlertPreferenceModel().firstOrNull() }
    }

    override suspend fun upsertAlertPreference(
        stationId: String,
        leadTimeMinutes: Int,
        tideFilter: TideAlertFilter,
        enabled: Boolean,
    ) {
        stationsDb.tidealertsQueries.upsertAlertPreference(
            station_id = stationId,
            lead_time_minutes = leadTimeMinutes.toLong(),
            tide_filter = tideFilter.storageValue,
            enabled = if (enabled) 1L else 0L,
            updated_at = System.currentTimeMillis(),
        )
    }

    override suspend fun deleteAlertPreference(stationId: String) {
        stationsDb.tidealertsQueries.deleteAlertPreference(stationId)
    }
}
