package com.fantopo.metacrtl.feature.map.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fantopo.metacrtl.core.model.LocationPoint
import com.fantopo.metacrtl.core.model.MapStyleMode
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

@Composable
fun MapSurfaceView(
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    pinnedLocation: LocationPoint?,
    mockedLocation: LocationPoint?,
    isGpsActive: Boolean,
    isFullScreen: Boolean,
    hasLocationPermission: Boolean,
    zoomLevel: Float,
    mapStyles: Set<MapStyleMode>,
    onLocationPinned: (LocationPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val mapType = when {
        mapStyles.contains(MapStyleMode.HYBRID) -> MapType.HYBRID
        else -> MapType.NORMAL
    }

    val mapProperties = remember(mapType, mapStyles, hasLocationPermission) {
        MapProperties(
            mapType = mapType,
            isTrafficEnabled = mapStyles.contains(MapStyleMode.TRAFFIC),
            isMyLocationEnabled = hasLocationPermission
        )
    }
    
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings,
            onMapClick = { latLng ->
                val newPoint = pinnedLocation?.copy(
                    latitude = latLng.latitude,
                    longitude = latLng.longitude
                ) ?: LocationPoint(latitude = latLng.latitude, longitude = latLng.longitude)
                onLocationPinned(newPoint)
            }
        ) {
            if (pinnedLocation != null) {
                Marker(
                    state = MarkerState(position = LatLng(pinnedLocation.latitude, pinnedLocation.longitude)),
                    title = "Pinned Location",
                    snippet = if (isGpsActive) "Mocking GPS..." else "GPS OFF"
                )
            }
        }
    }
}
