package com.jjswigut.eventide.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "eventide_settings")

class SettingsRepositoryImpl(
    context: Context,
) : SettingsRepository {
    private val dataStore = context.applicationContext.settingsDataStore

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                tideUnit = preferences[TIDE_UNIT_KEY]?.toTideUnit() ?: TideUnit.Feet,
                tempUnit = preferences[TEMP_UNIT_KEY]?.toTempUnit() ?: TempUnit.Fahrenheit,
                timeFormat = preferences[TIME_FORMAT_KEY]?.toTimeFormat() ?: TimeFormat.TwelveHour,
                homeStationId = preferences[HOME_STATION_ID_KEY],
            )
        }

    override suspend fun setTideUnit(tideUnit: TideUnit) {
        dataStore.edit { preferences ->
            preferences[TIDE_UNIT_KEY] = tideUnit.storageValue
        }
    }

    override suspend fun setTempUnit(tempUnit: TempUnit) {
        dataStore.edit { preferences ->
            preferences[TEMP_UNIT_KEY] = tempUnit.storageValue
        }
    }

    override suspend fun setTimeFormat(timeFormat: TimeFormat) {
        dataStore.edit { preferences ->
            preferences[TIME_FORMAT_KEY] = timeFormat.storageValue
        }
    }

    override suspend fun setHomeStationId(stationId: String?) {
        dataStore.edit { preferences ->
            if (stationId == null) {
                preferences.remove(HOME_STATION_ID_KEY)
            } else {
                preferences[HOME_STATION_ID_KEY] = stationId
            }
        }
    }

    companion object {
        private val TIDE_UNIT_KEY = stringPreferencesKey("tide_unit")
        private val TEMP_UNIT_KEY = stringPreferencesKey("temp_unit")
        private val TIME_FORMAT_KEY = stringPreferencesKey("time_format")
        private val HOME_STATION_ID_KEY = stringPreferencesKey("home_station_id")
    }
}
