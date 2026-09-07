package com.fantopo.metacrtl.feature.map.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.fantopo.metacrtl.feature.map.ui.MapIcons

@Composable
fun FloatingOverlayWidget(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRefreshLocation: () -> Unit,
    onStopSimulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Draggable state
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(
                    if (isExpanded) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) 
                    else Color.Transparent
                )
                .padding(if (isExpanded) 8.dp else 0.dp)
        ) {
            // Main logo button (circle)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF00E676), Color(0xFF00B0FF))
                        )
                    )
                    .clickable(onClick = onToggleExpand),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MapIcons.PinDrop,
                        contentDescription = "Fantopo Logo",
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Expanded horizontal section with "refresh" and "stop" icons
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            onRefreshLocation()
                        },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = MapIcons.Refresh,
                            contentDescription = "Refresh Location",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            onStopSimulation()
                            onToggleExpand()
                        },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = MapIcons.Stop,
                            contentDescription = "Stop Simulation",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

