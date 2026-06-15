package com.jjswigut.eventide.repository

import com.jjswigut.eventide.data.models.TideAlertFilter
import com.jjswigut.eventide.data.models.TideAlertPreference
import kotlinx.coroutines.flow.Flow

interface TideAlertRepository {
    fun getAlertPreferences(): Flow<List<TideAlertPreference>>

    fun getEnabledAlertPreferences(): Flow<List<TideAlertPreference>>

    suspend fun getAlertPreference(stationId: String): TideAlertPreference?

    suspend fun upsertAlertPreference(
        stationId: String,
        leadTimeMinutes: Int,
        tideFilter: TideAlertFilter,
        enabled: Boolean,
    )

    suspend fun deleteAlertPreference(stationId: String)
}
