package com.fantopo.metacrtl.feature.map.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fantopo.metacrtl.core.model.LocationPoint
import kotlinx.coroutines.delay

@Composable
fun InAppControlsOverlay(
    isGpsActive: Boolean,
    isFullScreen: Boolean,
    pinnedLocation: LocationPoint?,
    mockedLocation: LocationPoint?,
    onTogglePlayGps: () -> Unit,
    onCenterLocation: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onDeleteLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 8.dp, bottom = 16.dp, end = 8.dp)
    ) {
        // --- Top Status and Controls ---
        val horizontalBias by animateFloatAsState(
            targetValue = if (isGpsActive) -1f else 0f, 
            animationSpec = tween(500),
            label = "bias"
        )
        val buttonWidth by animateDpAsState(
            targetValue = if (isGpsActive) 56.dp else 140.dp, 
            animationSpec = tween(500),
            label = "width"
        )
        val buttonColor by animateColorAsState(
            targetValue = if (isGpsActive) Color(0xFF1B7A43) else Color(0xFF1E1E1E), 
            animationSpec = tween(500),
            label = "color"
        )

        // Status Card Flash Animation
        var flashColor by remember { mutableStateOf(Color.Transparent) }
        var isInitial by remember { mutableStateOf(true) }
        
        LaunchedEffect(isGpsActive) {
            if (isInitial) {
                isInitial = false
                return@LaunchedEffect
            }
            if (isGpsActive) {
                flashColor = Color(0xFF1B7A43)
                delay(800)
                flashColor = Color.Transparent
            } else {
                flashColor = Color(0xFFD32F2F)
                delay(800)
                flashColor = Color.Transparent
            }
        }

        val defaultSurface = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        val cardBackgroundColor by animateColorAsState(
            targetValue = if (flashColor != Color.Transparent) flashColor else defaultSurface,
            animationSpec = tween(400),
            label = "cardBg"
        )
        val isFlashing = flashColor != Color.Transparent
        val textColor by animateColorAsState(
            targetValue = if (isFlashing) Color.White else MaterialTheme.colorScheme.onSurface,
            label = "textColor"
        )
        val dotColor = if (isGpsActive) Color(0xFF1B7A43) else Color(0xFFD32F2F)
        val controlSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Container untuk Play Button & 3 Tombol Aksi
            Box(modifier = Modifier.fillMaxWidth()) {
                
                // Play Button
                Surface(
                    onClick = onTogglePlayGps,
                    shape = CircleShape,
                    color = buttonColor,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        // Perubahan penting: verticalBias di set '1f' agar menempel ke dasar Box
                        // Sehingga posisinya sejajar dengan tombol terbawah di sebelah kanannya
                        .align(BiasAlignment(horizontalBias, 1f))
                        .height(56.dp)
                        .width(buttonWidth)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = MapIcons.PowerSettingsNew,
                            contentDescription = if (isGpsActive) "Stop Fake GPS" else "Start Fake GPS",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        AnimatedVisibility(visible = !isGpsActive) {
                            Text(
                                text = "Aktifkan", 
                                color = Color.White, 
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                // --- 3 Tombol In-App (Tersusun Vertikal di Kanan) ---
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.End,
                    // Ditempatkan di BottomEnd, agar tombol yang paling bawah (Delete)
                    // sejajar tepat dengan Play Button.
                    modifier = Modifier.align(Alignment.BottomEnd) 
                ) {
                    // Pemusat Lokasi (Center Location)
                    SmallFloatingActionButton(
                        onClick = onCenterLocation,
                        shape = RoundedCornerShape(12.dp),
                        containerColor = controlSurfaceColor,
                        contentColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(
                            imageVector = MapIcons.MyLocation,
                            contentDescription = "Center Location"
                        )
                    }

                    // Fullscreen / Normal screen
                    SmallFloatingActionButton(
                        onClick = onToggleFullScreen,
                        shape = RoundedCornerShape(12.dp),
                        containerColor = controlSurfaceColor,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullScreen) MapIcons.FullscreenExit else MapIcons.Fullscreen,
                            contentDescription = "Toggle Fullscreen"
                        )
                    }

                    // Delete button (Reset / Delete pinned marker)
                    SmallFloatingActionButton(
                        onClick = onDeleteLocation,
                        shape = RoundedCornerShape(12.dp),
                        containerColor = controlSurfaceColor,
                        contentColor = MaterialTheme.colorScheme.error,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(
                            imageVector = MapIcons.Delete,
                            contentDescription = "Delete / Reset Pin"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Status Card
            if (!isFullScreen) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(1.0f) 
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (!isFlashing) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isGpsActive) {
                                "Fantopo Mock Location is Active"
                            } else {
                                "Fantopo Mock Location Not Active"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = textColor
                            )
                        )
                    }
                }
            }
        }
    }
}
