/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/


package me.kavishdevar.librepods.presentation.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.StyledButton
import me.kavishdevar.librepods.presentation.components.StyledToggle
import me.kavishdevar.librepods.presentation.components.StyledList
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.services.ServiceManager
import me.kavishdevar.librepods.utils.HeadTracking
import kotlin.math.abs

@Composable
fun HeadTrackingScreen(viewModel: AirPodsViewModel, navigateToPurchase: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    HeadGesturesContent(state, viewModel::setHeadGesturesEnabled, navigateToPurchase) {
        DisposableEffect(viewModel) {
            viewModel.startHeadTracking()
            onDispose { viewModel.stopHeadTracking() }
        }
        HeadGestureDiagnostics { ServiceManager.getService()?.testHeadGestures() }
    }
}

@Composable
fun HeadGesturesContent(state: AirPodsUiState, onEnabled: (Boolean) -> Unit,
                        navigateToPurchase: () -> Unit, diagnostics: @Composable () -> Unit) {
    var testing by remember { mutableStateOf(false) }
    SettingsDetailColumn {
        StyledToggle(label = stringResource(R.string.head_gestures), checked = state.headGesturesEnabled, header = true,
            onCheckedChange = onEnabled, enabled = state.isPremium || state.headGesturesEnabled,
            description = stringResource(R.string.head_gestures_details))
        StyledList {
            // Upstream supports fixed nod/shake call gestures, not configurable notification actions.
            StyledListItem(name = stringResource(R.string.answer_call),
                description = stringResource(R.string.gesture_up_down))
            StyledListItem(name = stringResource(R.string.decline_call),
                description = stringResource(R.string.gesture_side_side))
        }
        if (!state.isPremium) StyledButton(onClick = navigateToPurchase, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.unlock_advanced_features))
        }
        StyledListItem(name = stringResource(if (testing) R.string.close_gesture_test else R.string.try_head_gestures),
            onClick = { testing = !testing }, enabled = state.isLocallyConnected)
        if (testing && state.isLocallyConnected) diagnostics()
    }
}

@Composable
private fun HeadGestureDiagnostics(test: suspend () -> Boolean?) {
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var completed by remember { mutableStateOf(false) }
    Text(stringResource(R.string.gesture_velocity), style = MaterialTheme.typography.titleSmall)
    Plot()
    StyledButton(onClick = {
        running = true; completed = false
        scope.launch {
            try { result = test(); completed = true } finally { running = false }
        }
    }, enabled = !running, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.try_head_gestures))
    }
    if (running || completed) Text(stringResource(when {
        running -> R.string.shake_your_head_or_nod
        result == true -> R.string.gesture_yes_detected
        result == false -> R.string.gesture_no_detected
        else -> R.string.gesture_test_unavailable
    }), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Plot() {
    val acceleration by HeadTracking.acceleration.collectAsState()
    val maxPoints = 100
    val points = remember { mutableStateListOf<Pair<Float, Float>>() }
    val darkTheme = isSystemInDarkTheme()

    var maxAbs by remember { mutableFloatStateOf(1000f) }

    LaunchedEffect(acceleration) {
        points.add(Pair(acceleration.horizontal, acceleration.vertical))
        if (points.size > maxPoints) {
            points.removeAt(0)
        }

        val currentMax = points.maxOf { maxOf(abs(it.first), abs(it.second)) }
        maxAbs = maxOf(currentMax * 1.2f, 1000f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp)
    ) {
        val horizontalColor = MaterialTheme.colorScheme.primary
        val verticalColor = MaterialTheme.colorScheme.onPrimary

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val height = size.height
                val xScale = width / maxPoints
                val yScale = (height - 40.dp.toPx()) / (maxAbs * 2)
                val zeroY = height / 2

                val gridColor = if (darkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)

                for (i in 0..maxPoints step 10) {
                    val x = i * xScale
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val gridStep = maxAbs / 4
                for (value in (-maxAbs.toInt()..maxAbs.toInt()) step gridStep.toInt()) {
                    val y = zeroY - value * yScale
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                drawLine(
                    color = if (darkTheme) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f),
                    start = Offset(0f, zeroY),
                    end = Offset(width, zeroY),
                    strokeWidth = 1.5f.dp.toPx()
                )

                if (points.size > 1) {
                    for (i in 0 until points.size - 1) {
                        val x1 = i * xScale
                        val x2 = (i + 1) * xScale

                        drawLine(
                            color = horizontalColor,
                            start = Offset(x1, zeroY - points[i].first * yScale),
                            end = Offset(x2, zeroY - points[i + 1].first * yScale),
                            strokeWidth = 2.dp.toPx()
                        )

                        drawLine(
                            color = verticalColor,
                            start = Offset(x1, zeroY - points[i].second * yScale),
                            end = Offset(x2, zeroY - points[i + 1].second * yScale),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        color = if (darkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                        textSize = 12.sp.toPx()
                        textAlign = Paint.Align.RIGHT
                    }

                    drawText("${maxAbs.toInt()}", 30.dp.toPx(), 20.dp.toPx(), paint)
                    drawText("0", 30.dp.toPx(), height/2, paint)
                    drawText("-${maxAbs.toInt()}", 30.dp.toPx(), height - 10.dp.toPx(), paint)
                }

                val legendY = 15.dp.toPx()
                val textOffsetY = legendY + 5.dp.toPx() / 2

                drawCircle(horizontalColor, 5.dp.toPx(), Offset(width - 150.dp.toPx(), legendY))
                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        color = if (darkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                        textSize = 12.sp.toPx()
                        textAlign = Paint.Align.LEFT
                    }
                    drawText("Horizontal", width - 140.dp.toPx(), textOffsetY, paint)
                }

                drawCircle(verticalColor, 5.dp.toPx(), Offset(width - 70.dp.toPx(), legendY))
                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        color = if (darkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                        textSize = 12.sp.toPx()
                        textAlign = Paint.Align.LEFT
                    }
                    drawText("Vertical", width - 60.dp.toPx(), textOffsetY, paint)
                }
            }
        }
    }
}
