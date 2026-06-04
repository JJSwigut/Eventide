package com.jjswigut.eventide.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.jjswigut.eventide.map.MapAction.HandleMapMotion
import com.jjswigut.eventide.map.MapAction.Initialize
import com.jjswigut.eventide.map.MapAction.MapLoaded
import com.jjswigut.eventide.map.MapAction.NavigateToNearestStation
import com.jjswigut.eventide.map.components.StationInfoRow
import com.jjswigut.eventide.ui.components.ClusterPin
import com.jjswigut.eventide.ui.components.EmptyStateOverlay
import com.jjswigut.eventide.ui.components.SplashScreen
import com.jjswigut.eventide.ui.components.StationPin
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = koinViewModel(),
    hasLocationPermission: Boolean,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val mapProperties by remember(hasLocationPermission) {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = hasLocationPermission,
            ),
        )
    }

    val mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
            ),
        )
    }

    LaunchedEffect(Unit) {
        viewModel.handleAction(Initialize(hasLocationPermission))
    }

    LaunchedEffect(viewModel.cameraState.isMoving) {
        viewModel.handleAction(
            HandleMapMotion(
                isMoving = viewModel.cameraState.isMoving,
                bounds = viewModel.cameraState.latLngBounds,
            ),
        )
    }

    // Memoize cluster and item click handlers to avoid recreating them on every recomposition
    val onClusterClick: (Cluster<*>) -> Boolean = remember {
        { cluster ->
            scope.launch {
                viewModel.cameraState.animate(cluster.clusterZoomCameraUpdate())
            }
            false
        }
    }

    val onClusterItemClick = remember {
        { item: com.jjswigut.eventide.map.models.StationClusterItem ->
            viewModel.handleAction(
                com.jjswigut.eventide.map.MapAction.GetTidesForStation(item.station.id),
            )
            true
        }
    }

    Box {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = viewModel.cameraState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapLoaded = {
                viewModel.handleAction(MapLoaded)
            },
        ) {
            EventideClustering(
                items = viewState.stations,
                onClusterClick = onClusterClick,
                onClusterItemClick = onClusterItemClick,
                onClusterItemInfoWindowClick = {},
                onClusterItemInfoWindowLongClick = {},
                clusterContent = { cluster ->
                    ClusterPin(cluster.size.toString())
                },
                clusterItemContent = { item ->
                    StationPin(name = item.station.name)
                },
            )
        }

        SplashScreen(visible = !viewState.isMapLoaded)

        EmptyStateOverlay(
            visible = viewState.showEmptyState,
            onNavigateToNearest = {
                viewModel.handleAction(NavigateToNearestStation)
            },
        )

        viewState.listOfTideDays?.let { tideDays ->
            StationInfoRow(
                modifier = Modifier.align(Alignment.Center),
                list = tideDays,
                actionHandler = viewModel::handleAction,
            )
        }
    }
}

private const val CLUSTER_ZOOM_PADDING = 150

fun Cluster<*>.clusterZoomCameraUpdate(): CameraUpdate {
    val latLngBoundsBuilder = LatLngBounds.Builder()
    items.forEach { latLngBoundsBuilder.include(it.position) }
    val bounds = latLngBoundsBuilder.build()
    return CameraUpdateFactory.newLatLngBounds(bounds, CLUSTER_ZOOM_PADDING)
}

val CameraPositionState.latLngBounds: LatLngBounds?
    get() = this.projection?.visibleRegion?.latLngBounds
