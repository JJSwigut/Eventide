package com.jjswigut.eventide.map

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.jjswigut.eventide.R
import com.jjswigut.eventide.data.models.TideDay
import com.jjswigut.eventide.dispatchers.Dispatcher
import com.jjswigut.eventide.map.MapAction.CloseClicked
import com.jjswigut.eventide.map.MapAction.CloseTides
import com.jjswigut.eventide.map.MapAction.GetTidesForStation
import com.jjswigut.eventide.map.MapAction.GetUserLocation
import com.jjswigut.eventide.map.MapAction.HandleMapMotion
import com.jjswigut.eventide.map.MapAction.Initialize
import com.jjswigut.eventide.map.MapAction.MapLoaded
import com.jjswigut.eventide.map.MapAction.MenuClicked
import com.jjswigut.eventide.map.MapAction.NavigateToNearestStation
import com.jjswigut.eventide.map.models.StationClusterItem
import com.jjswigut.eventide.network.utils.process
import com.jjswigut.eventide.repository.NoaaRepository
import com.jjswigut.eventide.ui.components.Action
import com.jjswigut.eventide.ui.components.MenuItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val listOfButtons = persistentListOf(
    MenuItem(
        text = "",
        iconRes = R.drawable.arrow_up,
        action = CloseClicked,
    ),
    MenuItem(
        text = "",
        iconRes = R.drawable.arrow_up,
        action = CloseClicked,
    ),
    MenuItem(
        text = "",
        iconRes = R.drawable.arrow_up,
        action = CloseClicked,
    ),
)

private val mainButton = MenuItem(
    "",
    R.drawable.menu_icon,
    MenuClicked,
)

class MapViewModel(
    private val noaaRepository: NoaaRepository,
    private val dispatcher: Dispatcher,
    private val locationClient: FusedLocationProviderClient,
) : ViewModel() {

    private var getStationsJob: Job? = null
    private var allStations: List<StationClusterItem> = emptyList()
    private var isInitialLoad = true
    private var lastBounds: LatLngBounds? = null
    private var mapMotionDebounceJob: Job? = null

    // Debounce delay for map motion to reduce excessive station fetches
    private val MAP_MOTION_DEBOUNCE_MS = 200L

    val cameraState = CameraPositionState(
        CameraPosition(
            StartLocation,
            11f,
            0f,
            0f,
        ),
    )

    fun handleAction(action: Action) {
        viewModelScope.launch(dispatcher.io) {
            when (action) {
                is Initialize -> {
                    initialize(action.hasLocationPermission)
                }
                is MapLoaded -> {
                    onMapLoaded()
                }
                is GetUserLocation -> {
                    getUserLocation()
                }
                is GetTidesForStation -> {
                    getTidesForStation(action.stationId)
                }
                is HandleMapMotion -> {
                    handleMapMotion(action.isMoving, action.bounds)
                }
                is CloseTides -> {
                    updateViewState {
                        copy(
                            listOfTideDays = null,
                            showEmptyState = stations.isEmpty() && isMapLoaded,
                        )
                    }
                }
                is NavigateToNearestStation -> {
                    navigateToNearestStation()
                }
            }
        }
    }

    private suspend fun onMapLoaded() {
        // Wait a brief moment for initial camera setup before hiding splash
        delay(300)
        updateViewState {
            copy(isMapLoaded = true)
        }

        // Check for empty state on initial load
        if (isInitialLoad) {
            isInitialLoad = false
            viewModelScope.launch(dispatcher.main) {
                cameraState.projection?.visibleRegion?.latLngBounds?.let { bounds ->
                    // Switch back to IO for the network/db call
                    viewModelScope.launch(dispatcher.io) {
                        getStations(bounds)
                    }
                }
            }
        }
    }

    private suspend fun navigateToNearestStation() {
        val currentLocation = cameraState.position.target
        val nearest = allStations.minByOrNull { station ->
            distanceBetween(currentLocation, station.position)
        }

        nearest?.let { station ->
            // Dismiss empty state first
            updateViewState {
                copy(showEmptyState = false)
            }

            // Animate to the station (without opening tides)
            animateCameraPosition(target = station.position, zoom = 13f)
        }
    }

    private fun distanceBetween(
        pos1: LatLng,
        pos2: LatLng,
    ): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(pos2.latitude - pos1.latitude)
        val dLng = Math.toRadians(pos2.longitude - pos1.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(pos1.latitude)) * cos(Math.toRadians(pos2.latitude)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private suspend fun getTidesForStation(stationId: String) {
        // Show 7 placeholder cards immediately with loading state
        val placeholderCards = (1..7).map { dayIndex ->
            TideDay(
                date = "Loading...",
                tides = emptyList(),
                weather = null,
                isWeatherLoading = true,
                isTidesLoading = true,
            )
        }

        updateViewState {
            copy(
                listOfTideDays = placeholderCards.toImmutableList(),
                showEmptyState = false,
            )
        }

        // Fetch tides in background
        viewModelScope.launch(dispatcher.io) {
            noaaRepository.getTidesForStation(stationId).process(
                onSuccess = { tideDays ->
                    // Update with tides (weather still loading)
                    val tidesWithLoadingWeather = tideDays.map {
                        it.copy(
                            isTidesLoading = false,
                            isWeatherLoading = true,
                        )
                    }
                    updateViewState {
                        copy(listOfTideDays = tidesWithLoadingWeather.toImmutableList())
                    }

                    // Fetch weather in background
                    launch(dispatcher.io) {
                        noaaRepository.getTidesWithWeather(stationId).process(
                            onSuccess = { tideDaysWithWeather ->
                                updateViewState {
                                    copy(listOfTideDays = tideDaysWithWeather.toImmutableList())
                                }
                            },
                            onError = {
                                // Weather failed, show tides without weather
                                val tidesWithoutWeather = tideDays.map {
                                    it.copy(
                                        isTidesLoading = false,
                                        isWeatherLoading = false,
                                    )
                                }
                                updateViewState {
                                    copy(listOfTideDays = tidesWithoutWeather.toImmutableList())
                                }
                            },
                        )
                    }
                },
                onError = {
                    // todo add kermit logging
                },
            )
        }
    }

    private suspend fun handleMapMotion(isMoving: Boolean, bounds: LatLngBounds?) {
        if (isMoving) {
            // Cancel any pending debounced fetch
            mapMotionDebounceJob?.cancel()
            getStationsJob?.cancel()
            // Hide empty state while moving
            updateViewState {
                copy(showEmptyState = false)
            }
        } else {
            bounds?.let {
                // Only fetch if bounds changed significantly (more than 10% area change)
                if (shouldFetchStations(it)) {
                    // Debounce the fetch to avoid rapid successive calls
                    mapMotionDebounceJob?.cancel()
                    mapMotionDebounceJob = viewModelScope.launch(dispatcher.io) {
                        delay(MAP_MOTION_DEBOUNCE_MS)
                        lastBounds = it
                        getStations(it)
                    }
                }
            }
        }
    }

    private fun shouldFetchStations(newBounds: LatLngBounds): Boolean {
        val oldBounds = lastBounds ?: return true

        // Calculate approximate area change
        val oldArea = (oldBounds.northeast.latitude - oldBounds.southwest.latitude) *
            (oldBounds.northeast.longitude - oldBounds.southwest.longitude)
        val newArea = (newBounds.northeast.latitude - newBounds.southwest.latitude) *
            (newBounds.northeast.longitude - newBounds.southwest.longitude)

        // Only fetch if area changed by more than 10% or bounds don't overlap significantly
        val areaChangeRatio = kotlin.math.abs(newArea - oldArea) / oldArea
        return areaChangeRatio > 0.1 || !boundsOverlapSignificantly(oldBounds, newBounds)
    }

    private fun boundsOverlapSignificantly(
        bounds1: LatLngBounds,
        bounds2: LatLngBounds,
    ): Boolean {
        // Check if bounds overlap by at least 70%
        val intersectNE = LatLng(
            minOf(bounds1.northeast.latitude, bounds2.northeast.latitude),
            minOf(bounds1.northeast.longitude, bounds2.northeast.longitude),
        )
        val intersectSW = LatLng(
            maxOf(bounds1.southwest.latitude, bounds2.southwest.latitude),
            maxOf(bounds1.southwest.longitude, bounds2.southwest.longitude),
        )

        // Check if there's a valid intersection
        if (intersectNE.latitude < intersectSW.latitude ||
            intersectNE.longitude < intersectSW.longitude
        ) {
            return false
        }

        val intersectArea = (intersectNE.latitude - intersectSW.latitude) *
            (intersectNE.longitude - intersectSW.longitude)
        val bounds1Area = (bounds1.northeast.latitude - bounds1.southwest.latitude) *
            (bounds1.northeast.longitude - bounds1.southwest.longitude)

        return intersectArea / bounds1Area > 0.7
    }

    private suspend fun getStations(bounds: LatLngBounds) {
        getStationsJob = viewModelScope.launch(dispatcher.io) {
            noaaRepository.getStationsWithinBounds(bounds).process(
                onSuccess = { stations ->
                    val stationItems = stations.map { StationClusterItem(it) }
                    updateViewState {
                        copy(
                            stations = stationItems.toImmutableList(),
                            showEmptyState = stationItems.isEmpty() && isMapLoaded,
                        )
                    }
                },
                onError = {
                },
            )
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun initialize(hasLocationPermission: Boolean) = coroutineScope {
        val stationsFetch = async(dispatcher.io) { noaaRepository.fetchAndCacheStations() }

        // Set initial camera position (without animation to avoid visible jump)
        if (hasLocationPermission) {
            try {
                val location = locationClient.lastLocation.await()
                location?.let {
                    viewModelScope.launch(dispatcher.main) {
                        cameraState.position = CameraPosition(
                            location.latLng(),
                            StartZoomLevel,
                            0f,
                            0f,
                        )
                    }.join()
                }
            } catch (e: Exception) {
                // If we can't get location, camera stays at StartLocation
            }
        }

        // Wait for stations to finish fetching
        stationsFetch.await()

        // Store all stations for finding nearest
        noaaRepository.getAllStations().process(
            onSuccess = { stations ->
                allStations = stations.map { StationClusterItem(it) }
            },
            onError = {
                allStations = emptyList()
            },
        )

        updateViewState {
            copy(isLoading = false)
        }
    }

    @Suppress("MissingPermission")
    private suspend fun getUserLocation() {
        try {
            val location = locationClient.lastLocation.await()
            animateCameraPosition(location.latLng())
        } catch (e: Exception) {
            // todo add kermit logging
        }
    }

    private fun animateCameraPosition(target: LatLng, zoom: Float? = null) {
        viewModelScope.launch(dispatcher.main) {
            cameraState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition(
                        target,
                        zoom ?: StartZoomLevel,
                        0f,
                        0f,
                    ),
                ),
            )
        }
    }

    private val _viewState: MutableStateFlow<MapViewState> = MutableStateFlow(
        MapViewState(
            stations = persistentListOf(),
            isLoading = true,
            isMapLoaded = false,
            showEmptyState = false,
            listOfTideDays = null,
            menuState = MenuState(
                centerButton = mainButton,
                outsideButtons = listOfButtons,
                expanded = false,
            ),
        ),
    )

    val viewState: StateFlow<MapViewState> = _viewState.asStateFlow()

    private suspend fun updateViewState(viewState: MapViewState.() -> MapViewState) {
        _viewState.emit(viewState.invoke(this.viewState.value))
    }

    companion object {
        private const val StartZoomLevel = 11f
        private val StartLocation = LatLng(41.32797202698443, -71.98954043241064)
    }
}

data class MapViewState(
    val isLoading: Boolean,
    val isMapLoaded: Boolean,
    val showEmptyState: Boolean,
    val stations: ImmutableList<StationClusterItem>,
    val listOfTideDays: ImmutableList<TideDay>?,
    val menuState: MenuState,
)

data class MenuState(
    val expanded: Boolean,
    val centerButton: MenuItem,
    val outsideButtons: ImmutableList<MenuItem>,
)

sealed interface MapAction : Action {
    data class Initialize(val hasLocationPermission: Boolean) : MapAction
    data object MapLoaded : MapAction
    data class HandleMapMotion(val isMoving: Boolean, val bounds: LatLngBounds? = null) : MapAction
    data object GetUserLocation : MapAction
    data class GetTidesForStation(val stationId: String) : MapAction
    data object NavigateToNearestStation : MapAction

    data object CloseTides : MapAction

    data object CloseClicked : MapAction

    data object MenuClicked : MapAction
}

// todo move this to its own home
fun Location.latLng(): LatLng = LatLng(latitude, longitude)
