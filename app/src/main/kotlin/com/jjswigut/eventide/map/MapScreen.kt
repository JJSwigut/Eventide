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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.sp
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
import com.jjswigut.eventide.data.models.Weather
import com.jjswigut.eventide.map.MapAction.CloseTides
import com.jjswigut.eventide.map.MapAction.HandleMapMotion
import com.jjswigut.eventide.map.MapAction.Initialize
import com.jjswigut.eventide.ui.components.Action
import com.jjswigut.eventide.ui.components.BodyText
import com.jjswigut.eventide.ui.components.ClusterPin
import com.jjswigut.eventide.ui.components.DateHeaderText
import com.jjswigut.eventide.ui.components.EnhancedBodyText
import com.jjswigut.eventide.ui.components.FullScreenLoader
import com.jjswigut.eventide.ui.components.LargeBodyText
import com.jjswigut.eventide.ui.components.SectionTitleText
import com.jjswigut.eventide.ui.components.ShimmerLoading
import com.jjswigut.eventide.ui.components.StationPin
import com.jjswigut.eventide.ui.components.TemperatureText
import com.jjswigut.eventide.ui.components.TitleText
import com.jjswigut.eventide.ui.theme.BackgroundDark
import com.jjswigut.eventide.ui.theme.PrimaryDark
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
              .padding(bottom = 12.dp, end = 20.dp)
              .align(End)
              .size(48.dp)
              .background(
                color = PrimaryDark,
                shape = RoundedCornerShape(12.dp)
              )
              .border(
                width = 2.dp,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
              )
              .clip(shape = RoundedCornerShape(12.dp))
              .clickable {
                actionHandler(CloseTides)
              }
        ) {
            Image(
              painter = painterResource(id = R.drawable.close_icon),
              contentDescription = "Close",
              modifier = Modifier.size(28.dp)
            )
        }

      LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
          start = 16.dp,
          end = 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
            items(list) { day ->
                TideCard(day = day)
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
          .height(680.dp)
          .width(boxWidth.dp)
          .background(
            color = BackgroundDark.copy(alpha = 0.85f),
            shape = RoundedCornerShape(16.dp)
          )
          .border(
            width = 2.dp,
            color = PrimaryLight.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp)
          ),
        contentAlignment = Alignment.Center
    ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(
                    color = PrimaryLight,
                    shape = roundedShape
                  ),
              contentAlignment = Center,
            ) {
              DateHeaderText(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    text = day.date
                )
            }

          Spacer(Modifier.height(12.dp))

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .weight(0.4f)
              .background(
                color = PrimaryLight.copy(alpha = 0.9f),
                shape = roundedShape
              ),
            horizontalAlignment = Alignment.CenterHorizontally
            ) {
              SectionTitleText(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    text = "Tides"
                )

              if (day.isTidesLoading) {
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.Center
                    ) {
                      ShimmerLoading(modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp),
                      verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                      day.tides.forEach { tide ->
                        EnhancedTideRow(tide = tide)
                      }
                    }
                }
            }

          Spacer(Modifier.height(12.dp))

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .weight(0.6f)
              .background(
                color = PrimaryLight.copy(alpha = 0.95f),
                shape = roundedShape
              ),
            horizontalAlignment = Alignment.CenterHorizontally
            ) {
              SectionTitleText(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    text = "Weather"
                )

              Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .weight(1f)
                      .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    when {
                      day.isWeatherLoading -> {
                        ShimmerLoading(modifier = Modifier.fillMaxWidth())
                      }

                      day.weather != null -> {
                            EnhancedWeatherContent(weather = day.weather)
                        }

                      else -> {
                            BodyText(
                              text = "Weather unavailable",
                              textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedTideRow(
  tide: Tide,
) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Larger tide icon
        Icon(
          modifier = Modifier.size(24.dp),
            painter = painterResource(id = tide.tideValue.iconRes),
          contentDescription = null,
          tint = BackgroundDark
        )

      Spacer(Modifier.width(8.dp))

      // Time with enhanced typography
      EnhancedBodyText(
            modifier = Modifier.weight(1f),
            text = tide.time,
          textAlign = TextAlign.Start,
          fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )

      // Height with bold weight
      EnhancedBodyText(
            text = tide.height,
          textAlign = TextAlign.End,
          fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

@Composable
private fun EnhancedWeatherContent(weather: Weather) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    // Weather emoji - more compact
    androidx.compose.material3.Text(
      text = weather.getWeatherEmoji(),
      fontSize = 48.sp
    )

      // Conditions
      LargeBodyText(
        text = weather.conditions,
        textAlign = TextAlign.Center,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        maxLines = 2
      )

      // Temperature display - more compact
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Low Temperature - Cool Blue
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
              .background(
                color = androidx.compose.ui.graphics.Color(0xFF4A90E2).copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
              )
              .padding(horizontal = 20.dp, vertical = 12.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
              androidx.compose.material3.Text(
                text = "LOW",
                fontSize = 13.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = androidx.compose.ui.graphics.Color.White,
                letterSpacing = 1.5.sp
              )
              androidx.compose.material3.Text(
                text = "↓",
                fontSize = 16.sp,
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
              )
            }

              androidx.compose.material3.Text(
                text = "${weather.lowTemp}°",
                fontSize = 32.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = androidx.compose.ui.graphics.Color(0xFF1E4A6F)
              )
            }

          // High Temperature - Warm Orange
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
              .background(
                color = androidx.compose.ui.graphics.Color(0xFFFF6B35).copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
              )
              .padding(horizontal = 20.dp, vertical = 12.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
              androidx.compose.material3.Text(
                text = "HIGH",
                fontSize = 13.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = androidx.compose.ui.graphics.Color.White,
                letterSpacing = 1.5.sp
              )
              androidx.compose.material3.Text(
                text = "↑",
                fontSize = 16.sp,
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
              )
            }

            androidx.compose.material3.Text(
              text = "${weather.highTemp}°",
              fontSize = 32.sp,
              fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
              color = androidx.compose.ui.graphics.Color(0xFFC53D0F)
            )
          }
        }

      // Wind information - compact
        Row(
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth(0.85f)
            .background(
              color = PrimaryLight.copy(alpha = 0.3f),
              shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          androidx.compose.material3.Text(
            text = "Wind:",
            fontSize = 15.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = BackgroundDark
          )
          Spacer(Modifier.width(6.dp))
          EnhancedBodyText(
            text = weather.windSpeed,
            textAlign = TextAlign.Center,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            maxLines = 1
          )
        }
    }
}
