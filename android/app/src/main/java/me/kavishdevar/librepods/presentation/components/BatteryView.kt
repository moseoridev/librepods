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

package me.kavishdevar.librepods.presentation.components

import android.graphics.Paint
import android.graphics.RectF
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryStatus

@Composable
fun BatteryView(batteryList: List<Battery>, budsRes: Int, caseRes: Int) {
    val readings = SettingsBatteries.from(batteryList)
    val combined = readings.combined
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(budsRes), contentDescription = null,
            contentScale = ContentScale.Fit, modifier = Modifier.size(120.dp, 80.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            val count = if (combined != null) 2 else 3
            val slotMinWidth = minOf(138.5.dp, (maxWidth - 3.dp * (count - 1)) / count)
            Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (combined != null) {
                    BatteryMeter(combined, stringResource(R.string.left) + "•" + stringResource(R.string.right),
                        budsRes, slotMinWidth)
                } else {
                    BatteryMeter(readings.left, stringResource(R.string.left), budsRes, slotMinWidth)
                    BatteryMeter(readings.right, stringResource(R.string.right), budsRes, slotMinWidth)
                }
                BatteryMeter(readings.case, stringResource(R.string.case_alt), caseRes, slotMinWidth)
            }
        }
    }
}

@Composable
private fun BatteryMeter(reading: SettingsBattery, name: String, artwork: Int, minWidth: Dp) {
    val level = reading.level
    val charging = reading.status == BatteryStatus.CHARGING || reading.status == BatteryStatus.OPTIMIZED_CHARGING
    val value = level?.let {
        if (Locale.getDefault().language == "tr") " %$it" else " $it%"
    } ?: "—"
    val spokenValue = level?.let { "$it%" } ?: stringResource(R.string.battery_unknown)
    val chargeLabel = when (reading.status) {
        BatteryStatus.CHARGING -> stringResource(R.string.battery_charging)
        BatteryStatus.OPTIMIZED_CHARGING -> stringResource(R.string.battery_optimized_charging)
        else -> ""
    }
    val description = listOf(name, spokenValue, chargeLabel).filter { it.isNotEmpty() }.joinToString(", ")
    val progressColor = when {
        level != null && level <= 20 -> MaterialTheme.colorScheme.error
        isSystemInDarkTheme() -> Color(0xFF22CA61)
        else -> Color(0xFF26E26D)
    }
    val density = LocalDensity.current
    val textSize = with(density) { 12.dp.toSp() }
    val stroke = with(density) { 7.dp.toPx() }
    val diameter = with(density) { 50.dp.toPx() }
    val arc = remember(stroke, diameter) {
        RectF(stroke / 2f, stroke / 2f, diameter - stroke / 2f, diameter - stroke / 2f)
    }
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }
    // The manager measures a minimum-width slot, then centers its content at its natural width.
    // A fixed-width Column rounds the outer and inner centers differently at some densities.
    Column(Modifier.widthIn(min = minWidth).widthIn(min = 92.dp)
        .wrapContentSize(Alignment.Center).clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(50.dp, 37.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                paint.strokeWidth = stroke
                drawIntoCanvas { canvas ->
                    paint.color = android.graphics.Color.argb(102, 202, 202, 202)
                    canvas.nativeCanvas.drawArc(arc, 147.73f, 244.54f, false, paint)
                    if (level != null) {
                        paint.color = progressColor.toArgb()
                        canvas.nativeCanvas.drawArc(arc, 147.73f, 244.54f * level / 100f, false, paint)
                    }
                }
            }
            Image(painterResource(artwork), contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-4).dp).size(30.dp, 18.dp))
        }
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
                fontSize = textSize, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(2.dp))
            Text(value, fontFamily = FontFamily.Default, fontSize = textSize,
                color = MaterialTheme.colorScheme.onSurface)
        }
        if (charging) {
            Text(chargeLabel, fontFamily = FontFamily.Default, fontSize = textSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
