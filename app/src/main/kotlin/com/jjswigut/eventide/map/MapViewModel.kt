package com.jjswigut.eventide.map

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
import com.jjswigut.eventide.map.MapAction.MenuClicked
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val listOfButtons = persistentListOf(
    MenuItem(
        text = "", iconRes = R.drawable.arrow_up, action = CloseClicked
    ),
    MenuItem(
        text = "", iconRes = R.drawable.arrow_up, action = CloseClicked
    ),
    MenuItem(
        text = "", iconRes = R.drawable.arrow_up, action = CloseClicked
    ),
)

private val mainButton = MenuItem(
    "", R.drawable.menu_icon, MenuClicked
)
class MapViewModel(
    private val noaaRepository: NoaaRepository,
    private val dispatcher: Dispatcher,
    private val locationClient: FusedLocationProviderClient,
) : ViewModel() {

    private var getStationsJob: Job? = null

    val cameraState = CameraPositionState(
        CameraPosition(
            LatLng(0.0, 0.0),
            11f,
            0f,
            0f,
        )
    )

    fun handleAction(action: Action) {
        viewModelScope.launch(dispatcher.io) {
            when (action) {
                is Initialize -> {
                    initialize(action.hasLocationPermission)
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
                        copy(listOfTideDays = null)
                    }
                }
            }
        }
    }

    private suspend fun getTidesForStation(stationId: String) {
        noaaRepository.getTidesForStation(stationId).process(
            onSuccess = { tideDays ->
                updateViewState {
                    copy(
                        listOfTideDays = tideDays.toImmutableList()
                    )
                }
            },
            onError = {
                //todo add kermit logging
            }
        )
    }

    private suspend fun handleMapMotion(isMoving: Boolean, bounds: LatLngBounds?) {
        if (isMoving) {
            getStationsJob?.cancel()
        } else {
            bounds?.let {
                getStations(it)
            }
        }
    }

    private suspend fun getStations(bounds: LatLngBounds) {
        getStationsJob = viewModelScope.launch(dispatcher.io) {
            noaaRepository.getStationsWithinBounds(bounds).process(
                onSuccess = { stations ->
                    updateViewState {
                        copy(
                            stations = stations.map { StationClusterItem(it) }
                                .toImmutableList(),
                        )
                    }
                },
                onError = {

                }
            )
        }
    }

    private suspend fun initialize(hasLocationPermission: Boolean) = coroutineScope {
        async(dispatcher.io) { noaaRepository.fetchAndCacheStations() }.await()

        if (hasLocationPermission) {
            getUserLocation()
        } else {
            animateCameraPosition(target = StartLocation)
        }

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
            //todo add kermit logging
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
                    )
                )
            )
        }
    }

    private val _viewState: MutableStateFlow<MapViewState> = MutableStateFlow(
        MapViewState(
            stations = persistentListOf(),
            isLoading = true,
            listOfTideDays = null,
            menuState = MenuState(
                centerButton = mainButton,
                outsideButtons = listOfButtons,
                expanded = false,
            )
        )
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
    val stations: ImmutableList<StationClusterItem>,
    val listOfTideDays: ImmutableList<TideDay>?,
    val menuState: MenuState
)

data class MenuState(
    val expanded: Boolean,
    val centerButton: MenuItem,
    val outsideButtons: ImmutableList<MenuItem>
)

sealed interface MapAction : Action {
    data class Initialize(val hasLocationPermission: Boolean) : MapAction
    data class HandleMapMotion(val isMoving: Boolean, val bounds: LatLngBounds? = null) : MapAction
    data object GetUserLocation : MapAction
    data class GetTidesForStation(val stationId: String) : MapAction

    data object CloseTides : MapAction

    data object CloseClicked : MapAction

    data object MenuClicked : MapAction
}

// todo move this to its own home
fun Location.latLng(): LatLng = LatLng(latitude, longitude)
