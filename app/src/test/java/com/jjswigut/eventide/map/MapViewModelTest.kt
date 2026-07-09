package com.jjswigut.eventide.map

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLngBounds
import com.jjswigut.eventide.alerts.TideAlertScheduler
import com.jjswigut.eventide.data.models.MarineConditions
import com.jjswigut.eventide.data.models.MarineObservation
import com.jjswigut.eventide.data.models.Station
import com.jjswigut.eventide.data.models.Tide
import com.jjswigut.eventide.data.models.TideAlertFilter
import com.jjswigut.eventide.data.models.TideAlertPreference
import com.jjswigut.eventide.data.models.TideDay
import com.jjswigut.eventide.data.models.TideValue
import com.jjswigut.eventide.data.models.Weather
import com.jjswigut.eventide.dispatchers.Dispatcher
import com.jjswigut.eventide.network.utils.Either
import com.jjswigut.eventide.repository.FavoritesRepository
import com.jjswigut.eventide.repository.NoaaRepository
import com.jjswigut.eventide.repository.TideAlertRepository
import com.jjswigut.eventide.settings.AppSettings
import com.jjswigut.eventide.settings.SettingsRepository
import com.jjswigut.eventide.settings.TempUnit
import com.jjswigut.eventide.settings.TideUnit
import com.jjswigut.eventide.settings.TimeFormat
import com.jjswigut.eventide.utils.GenericError
import com.jjswigut.eventide.utils.UnknownError
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `switching stations ignores stale tide weather and marine completions`() = runTest(testDispatcher) {
        val repository = FakeNoaaRepository()
        val viewModel = viewModel(repository)

        viewModel.handleAction(MapAction.GetTidesForStation("A"))
        advanceUntilIdle()
        viewModel.handleAction(MapAction.GetTidesForStation("B"))
        advanceUntilIdle()

        repository.completeTides("A", Either.success(listOf(tideDay("A tides"))))
        repository.completeWeather("A", Either.success(listOf(weather("A weather"))))
        repository.completeMarine("A", Either.success(marine("A marine")))
        advanceUntilIdle()

        repository.completeTides("B", Either.success(listOf(tideDay("B tides"))))
        repository.completeWeather("B", Either.success(listOf(weather("B weather"))))
        repository.completeMarine("B", Either.success(marine("B marine")))
        advanceUntilIdle()

        val state = viewModel.viewState.value
        assertEquals("B tides", state.listOfTideDays?.first()?.date)
        assertEquals("B weather", state.listOfTideDays?.first()?.weather?.date)
        assertEquals("B marine", state.marineConditions?.observations?.first()?.label)
    }

    @Test
    fun `closing station details invalidates in flight completions`() = runTest(testDispatcher) {
        val repository = FakeNoaaRepository()
        val viewModel = viewModel(repository)

        viewModel.handleAction(MapAction.GetTidesForStation("A"))
        advanceUntilIdle()
        viewModel.handleAction(MapAction.CloseTides)
        advanceUntilIdle()

        repository.completeTides("A", Either.success(listOf(tideDay("A tides"))))
        repository.completeWeather("A", Either.success(listOf(weather("A weather"))))
        repository.completeMarine("A", Either.success(marine("A marine")))
        advanceUntilIdle()

        val state = viewModel.viewState.value
        assertNull(state.listOfTideDays)
        assertNull(state.marineConditions)
        assertFalse(state.isMarineConditionsLoading)
    }

    @Test
    fun `marine result can render when tide request fails`() = runTest(testDispatcher) {
        val repository = FakeNoaaRepository()
        val viewModel = viewModel(repository)

        viewModel.handleAction(MapAction.GetTidesForStation("A"))
        advanceUntilIdle()

        repository.completeMarine("A", Either.success(marine("A marine")))
        repository.completeTides("A", Either.failure(UnknownError()))
        repository.completeWeather("A", Either.failure(UnknownError()))
        advanceUntilIdle()

        val state = viewModel.viewState.value
        assertEquals("Tides unavailable", state.listOfTideDays?.first()?.date)
        assertEquals("A marine", state.marineConditions?.observations?.first()?.label)
        assertFalse(state.isMarineConditionsLoading)
    }

    @Test
    fun `weather failure keeps successful tides and clears weather loading`() = runTest(testDispatcher) {
        val repository = FakeNoaaRepository()
        val viewModel = viewModel(repository)

        viewModel.handleAction(MapAction.GetTidesForStation("A"))
        advanceUntilIdle()

        repository.completeTides("A", Either.success(listOf(tideDay("A tides"))))
        repository.completeWeather("A", Either.failure(UnknownError()))
        repository.completeMarine("A", Either.failure(UnknownError()))
        advanceUntilIdle()

        val day = viewModel.viewState.value.listOfTideDays?.first()
        assertEquals("A tides", day?.date)
        assertNull(day?.weather)
        assertFalse(day?.isWeatherLoading ?: true)
    }

    private fun viewModel(repository: FakeNoaaRepository): MapViewModel {
        return MapViewModel(
            noaaRepository = repository,
            favoritesRepository = FakeFavoritesRepository(),
            tideAlertRepository = FakeTideAlertRepository(),
            tideAlertScheduler = mockk<TideAlertScheduler>(relaxed = true),
            settingsRepository = FakeSettingsRepository(),
            dispatcher = TestDispatcherProvider(testDispatcher),
            locationClient = mockk<FusedLocationProviderClient>(relaxed = true),
        )
    }

    private fun tideDay(date: String): TideDay {
        return TideDay(
            date = date,
            tides = listOf(
                Tide(
                    time = "1:14am",
                    tideValue = TideValue.High,
                    height = "3.94ft",
                ),
            ),
        )
    }

    private fun weather(date: String): Weather {
        return Weather(
            date = date,
            highTemp = 78,
            lowTemp = 62,
            conditions = "Sunny",
            iconUrl = "https://api.weather.gov/icons/test",
            windSpeed = "5 mph",
        )
    }

    private fun marine(label: String): MarineConditions {
        return MarineConditions(
            observations = listOf(
                MarineObservation(label = label, value = "64.1°F"),
            ),
        )
    }
}

private class TestDispatcherProvider(
    private val dispatcher: CoroutineDispatcher,
) : Dispatcher {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

private class FakeNoaaRepository : NoaaRepository {
    private val tides = mutableMapOf<String, CompletableDeferred<Either<List<TideDay>, GenericError>>>()
    private val weather = mutableMapOf<String, CompletableDeferred<Either<List<Weather>, GenericError>>>()
    private val marine = mutableMapOf<String, CompletableDeferred<Either<MarineConditions, GenericError>>>()

    override suspend fun fetchAndCacheStations() = Unit

    override suspend fun getAllStations(): Either<List<Station>, GenericError> = Either.success(emptyList())

    override suspend fun getStationsWithinBounds(bounds: LatLngBounds): Either<List<Station>, GenericError> =
        Either.success(emptyList())

    override suspend fun getTidesForStation(stationID: String): Either<List<TideDay>, GenericError> =
        tides.getOrPut(stationID) { CompletableDeferred() }.await()

    override suspend fun getTidesWithWeather(stationID: String): Either<List<TideDay>, GenericError> =
        getTidesForStation(stationID)

    override suspend fun getWeatherForStation(stationID: String): Either<List<Weather>, GenericError> =
        weather.getOrPut(stationID) { CompletableDeferred() }.await()

    override suspend fun getMarineConditionsForStation(stationID: String): Either<MarineConditions, GenericError> =
        marine.getOrPut(stationID) { CompletableDeferred() }.await()

    override suspend fun getTidesImmediately(
        stationID: String,
        onWeatherUpdate: (List<TideDay>) -> Unit,
    ): Either<List<TideDay>, GenericError> = getTidesForStation(stationID)

    fun completeTides(stationId: String, result: Either<List<TideDay>, GenericError>) {
        tides.getOrPut(stationId) { CompletableDeferred() }.complete(result)
    }

    fun completeWeather(stationId: String, result: Either<List<Weather>, GenericError>) {
        weather.getOrPut(stationId) { CompletableDeferred() }.complete(result)
    }

    fun completeMarine(stationId: String, result: Either<MarineConditions, GenericError>) {
        marine.getOrPut(stationId) { CompletableDeferred() }.complete(result)
    }
}

private class FakeFavoritesRepository : FavoritesRepository {
    private val favorites = MutableStateFlow<List<Station>>(emptyList())

    override suspend fun addFavorite(station: Station) = Unit

    override suspend fun removeFavorite(id: String) = Unit

    override fun isFavorite(id: String): Flow<Boolean> = flowOf(false)

    override fun getFavorites(): Flow<List<Station>> = favorites
}

private class FakeTideAlertRepository : TideAlertRepository {
    override fun getAlertPreferences(): Flow<List<TideAlertPreference>> = flowOf(emptyList())

    override fun getEnabledAlertPreferences(): Flow<List<TideAlertPreference>> = flowOf(emptyList())

    override suspend fun getAlertPreference(stationId: String): TideAlertPreference? = null

    override suspend fun upsertAlertPreference(
        stationId: String,
        leadTimeMinutes: Int,
        tideFilter: TideAlertFilter,
        enabled: Boolean,
    ) = Unit

    override suspend fun deleteAlertPreference(stationId: String) = Unit
}

private class FakeSettingsRepository : SettingsRepository {
    override val settings: Flow<AppSettings> = flowOf(AppSettings())

    override suspend fun setTideUnit(tideUnit: TideUnit) = Unit

    override suspend fun setTempUnit(tempUnit: TempUnit) = Unit

    override suspend fun setTimeFormat(timeFormat: TimeFormat) = Unit

    override suspend fun setHomeStationId(stationId: String?) = Unit
}
