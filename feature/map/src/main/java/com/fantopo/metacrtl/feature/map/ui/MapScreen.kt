package com.fantopo.metacrtl.feature.map.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fantopo.metacrtl.feature.map.dialog.FavoriteLocationDialog
import com.fantopo.metacrtl.feature.map.dialog.HistoryLocationDialog
import com.fantopo.metacrtl.feature.map.dialog.ProviderServiceDialog
import com.fantopo.metacrtl.feature.map.dialog.SaveLocationDialog
import com.fantopo.metacrtl.feature.map.dialog.SettingsDialog
import com.fantopo.metacrtl.feature.map.model.DialogType
import com.fantopo.metacrtl.feature.map.overlay.FloatingOverlayWidget
import com.fantopo.metacrtl.feature.map.viewmodel.MapViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(uiState.pinnedLocation?.latitude ?: -6.2088, uiState.pinnedLocation?.longitude ?: 106.8456),
            uiState.zoomLevel
        )
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        }
    )

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && uiState.pinnedLocation == null) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(location.latitude, location.longitude),
                                    uiState.zoomLevel
                                )
                            )
                        }
                    }
                }
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }

    val handleCenterLocation: () -> Unit = {
        if (hasLocationPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(location.latitude, location.longitude),
                                    uiState.zoomLevel
                                )
                            )
                        }
                    } else {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(uiState.pinnedLocation?.latitude ?: -6.2088, uiState.pinnedLocation?.longitude ?: 106.8456),
                                    uiState.zoomLevel
                                )
                            )
                        }
                    }
                }.addOnFailureListener {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(uiState.pinnedLocation?.latitude ?: -6.2088, uiState.pinnedLocation?.longitude ?: 106.8456),
                                uiState.zoomLevel
                            )
                        )
                    }
                }
            } catch (e: SecurityException) {
                coroutineScope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(uiState.pinnedLocation?.latitude ?: -6.2088, uiState.pinnedLocation?.longitude ?: 106.8456),
                            uiState.zoomLevel
                        )
                    )
                }
            }
        } else {
            coroutineScope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(uiState.pinnedLocation?.latitude ?: -6.2088, uiState.pinnedLocation?.longitude ?: 106.8456),
                        uiState.zoomLevel
                    )
                )
            }
        }
    }

    LaunchedEffect(uiState.messageSnackbar) {
        uiState.messageSnackbar?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onDismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Full-screen map rendering surface
            MapSurfaceView(
                cameraPositionState = cameraPositionState,
                pinnedLocation = uiState.pinnedLocation,
                mockedLocation = uiState.mockedLocation,
                isGpsActive = uiState.isGpsActive,
                isFullScreen = uiState.isFullScreen,
                hasLocationPermission = hasLocationPermission,
                zoomLevel = uiState.zoomLevel,
                mapStyles = uiState.settings.mapStyles,
                onLocationPinned = { point ->
                    viewModel.onPinLocation(point)
                }
            )

            // 2. Floating mode overlay concept (when floating mode setting is active and mock is active)
            if (uiState.settings.isFloatingMode && uiState.isGpsActive) {
                FloatingOverlayWidget(
                    isExpanded = uiState.isFloatingExpanded,
                    onToggleExpand = { viewModel.onToggleFloatingOverlay() },
                    onRefreshLocation = { viewModel.onRefreshLocation() },
                    onStopSimulation = { viewModel.onStopGps() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                )
            }

            // 3. In-app controls overlay (play/off on left, center, fullscreen, delete on right)
            InAppControlsOverlay(
                isGpsActive = uiState.isGpsActive,
                isFullScreen = uiState.isFullScreen,
                pinnedLocation = uiState.pinnedLocation,
                mockedLocation = uiState.mockedLocation,
                onTogglePlayGps = { viewModel.onTogglePlayGps() },
                onCenterLocation = handleCenterLocation,
                onToggleFullScreen = { viewModel.onToggleFullScreen() },
                onDeleteLocation = { viewModel.onDeleteLocation() }
            )

            // 4. Bottom Menu Bar
            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                BottomMenuBar(
                    activeDialog = uiState.activeDialog,
                    onOpenDialog = { dialog -> viewModel.onOpenDialog(dialog) }
                )
            }

            // 5. Active Dialogs
            when (uiState.activeDialog) {
                DialogType.SAVE -> {
                    SaveLocationDialog(
                        initialPoint = uiState.pinnedLocation,
                        onDismiss = { viewModel.onDismissDialog() },
                        onSave = { name, lat, lng ->
                            viewModel.onSaveLocation(name, lat, lng)
                        }
                    )
                }
                DialogType.FAVORITE -> {
                    FavoriteLocationDialog(
                        favorites = uiState.favoriteLocations,
                        currentPoint = uiState.pinnedLocation,
                        onDismiss = { viewModel.onDismissDialog() },
                        onMoveTo = { lat, lng, name ->
                            viewModel.onMoveToLocation(lat, lng, name)
                        },
                        onEditFavorite = { updated ->
                            viewModel.onUpdateFavorite(updated)
                        },
                        onDeleteFavorite = { id ->
                            viewModel.onDeleteFavorite(id)
                        },
                        onAddFavorite = { name, lat, lng ->
                            viewModel.onAddFavorite(name, lat, lng)
                        }
                    )
                }
                DialogType.HISTORY -> {
                    HistoryLocationDialog(
                        historyEntries = uiState.historyEntries,
                        onDismiss = { viewModel.onDismissDialog() },
                        onMoveTo = { lat, lng, name ->
                            viewModel.onMoveToLocation(lat, lng, name)
                        },
                        onSaveToSaved = { name, lat, lng ->
                            viewModel.onSaveLocation(name, lat, lng)
                        },
                        onDeleteHistory = { id ->
                            viewModel.onDeleteHistory(id)
                        },
                        onClearAllHistory = {
                            viewModel.onClearHistory()
                        }
                    )
                }
                DialogType.SETTINGS -> {
                    SettingsDialog(
                        settings = uiState.settings,
                        onDismiss = { viewModel.onDismissDialog() },
                        onUpdateSettings = { newSettings ->
                            viewModel.onUpdateSettings(newSettings)
                        },
                        onToggleMapStyle = { style ->
                            viewModel.onToggleMapStyle(style)
                        }
                    )
                }
                DialogType.PROVIDER_SERVICE -> {
                    ProviderServiceDialog(
                        selectedProvider = uiState.selectedProvider,
                        onDismiss = { viewModel.onDismissDialog() },
                        onSelectProvider = { provider ->
                            viewModel.onSelectProvider(provider)
                        }
                    )
                }
                null -> {}
            }
        }
    }
}
