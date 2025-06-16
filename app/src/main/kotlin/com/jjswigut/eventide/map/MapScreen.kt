package com.jjswigut.eventide.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.jjswigut.eventide.R
import com.jjswigut.eventide.data.models.Tide
import com.jjswigut.eventide.data.models.TideDay
import com.jjswigut.eventide.map.MapAction.CloseTides
import com.jjswigut.eventide.map.MapAction.HandleMapMotion
import com.jjswigut.eventide.map.MapAction.Initialize
import com.jjswigut.eventide.ui.components.Action
import com.jjswigut.eventide.ui.components.BodyText
import com.jjswigut.eventide.ui.components.ClusterPin
import com.jjswigut.eventide.ui.components.FullScreenLoader
import com.jjswigut.eventide.ui.components.StationPin
import com.jjswigut.eventide.ui.components.TitleText
import com.jjswigut.eventide.ui.theme.BackgroundDark
import com.jjswigut.eventide.ui.theme.PrimaryLight
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
            )
        )
    }

    val mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
            )
        )
    }

    LaunchedEffect(viewModel.cameraState.isMoving) {
        viewModel.handleAction(
            HandleMapMotion(
                isMoving = viewModel.cameraState.isMoving,
                bounds = viewModel.cameraState.latLngBounds
            )
        )
    }

    Box {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = viewModel.cameraState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapLoaded = {
                viewModel.handleAction(Initialize(hasLocationPermission))
            }
        ) {
            EventideClustering(
                items = viewState.stations,
                onClusterClick = { cluster ->
                    scope.launch {
                        viewModel.cameraState.animate(cluster.clusterZoomCameraUpdate())
                    }
                    false
                },
                onClusterItemClick = {
                    viewModel.handleAction(MapAction.GetTidesForStation(it.station.id))
                    true // Return true to consume the event and prevent default info window
                },
                onClusterItemInfoWindowClick = {},
                onClusterItemInfoWindowLongClick = {},
                clusterContent = {
                    ClusterPin(it.size.toString())
                },
                clusterItemContent = {
                    StationPin(name = it.station.name)
                }
            )
        }

        FullScreenLoader(visible = viewState.isLoading)

        viewState.listOfTideDays?.let { tideDays ->
            StationInfoRow(
                modifier = Modifier.align(Companion.Center),
                list = tideDays,
                actionHandler = viewModel::handleAction
            )
        }
    }
}


private const val CLUSTER_ZOOM_PADDING = 150
fun Cluster<*>.clusterZoomCameraUpdate(): CameraUpdate {
    val latLngBoundsBuilder = LatLngBounds.Builder()

    this.items.forEach {
        latLngBoundsBuilder.include(it.position)
    }
    val bounds = latLngBoundsBuilder.build()

    return CameraUpdateFactory.newLatLngBounds(bounds, CLUSTER_ZOOM_PADDING)
}

val CameraPositionState.latLngBounds: LatLngBounds?
    get() = this.projection?.visibleRegion?.latLngBounds


@Composable
fun StationInfoRow(
    modifier: Modifier,
    list: List<TideDay>,
    actionHandler: (Action) -> Unit
) {
    Column(modifier = modifier) {
        Box(
            contentAlignment = Center,
            modifier = Modifier
                .padding(bottom = 8.dp, end = 16.dp)
                .align(End)
                .size(32.dp)
                .background(shape = RoundedCornerShape(8.dp), color = PrimaryLight)
                .clip(shape = RoundedCornerShape(8.dp))
                .clickable {
                    actionHandler(CloseTides)
                }) {
            Image(
                painter = painterResource(id = R.drawable.close_icon), contentDescription = null,
            )
        }
        LazyRow {
            item {
                Spacer(Modifier.width(16.dp))
            }
            items(list) { day ->
                TideCard(day = day)
                Spacer(Modifier.width(16.dp))
            }
        }
    }
}

val roundedShape = RoundedCornerShape(12.dp)

@Composable
private fun TideCard(
    day: TideDay
) {
    val boxWidth = LocalConfiguration.current.screenWidthDp - 48
    Box(
        modifier = Modifier
            .size(height = 250.dp, width = boxWidth.dp)
            .background(color = BackgroundDark.copy(alpha = .7f), shape = roundedShape)
            .border(color = PrimaryLight, width = 4.dp, shape = roundedShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(color = PrimaryLight, shape = roundedShape),
                contentAlignment = Companion.Center,
            ) {
                TitleText(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    text = day.date
                )
            }

            Spacer(Modifier.padding(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color = PrimaryLight, shape = roundedShape),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TitleText(text = "Tides")

                    day.tides.forEach { tide ->
                        TideRow(
                            modifier = Modifier.weight(1f),
                            tide = tide
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color = PrimaryLight, shape = roundedShape),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TitleText(text = "Weather")

                    BodyText(text = "Weather info coming soon!", textAlign = TextAlign.Center)

                }
            }
        }
    }
}

@Composable
private fun TideRow(
    modifier: Modifier,
    tide: Tide
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.weight(.5f, fill = false),
            painter = painterResource(id = tide.tideValue.iconRes),
            contentDescription = null
        )
        BodyText(
            modifier = Modifier.weight(1f),
            text = tide.time,
            textAlign = TextAlign.End
        )
        BodyText(
            modifier = Modifier.weight(1f),
            text = tide.height,
            textAlign = TextAlign.End
        )
    }
}