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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryStatus

@Suppress("UNUSED_PARAMETER") // Keep the upstream call signature; charge state no longer starts an animation.
@Composable
fun BatteryIndicator(
    batteryPercentage: Int?,
    status: Int,
    prefix: String = "",
    previousCharging: Boolean = false,
) {
    val reading = SettingsBattery.from(batteryPercentage?.let { Battery(0, it, status) })
    val charging = reading.status == BatteryStatus.CHARGING || reading.status == BatteryStatus.OPTIMIZED_CHARGING
    val value = reading.level?.let { "$it%" } ?: "—"
    val spokenValue = reading.level?.let { "$it%" } ?: stringResource(R.string.battery_unknown)
    val spokenCharge = when (reading.status) {
        BatteryStatus.CHARGING -> stringResource(R.string.battery_charging)
        BatteryStatus.OPTIMIZED_CHARGING -> stringResource(R.string.battery_optimized_charging)
        else -> ""
    }
    val description = listOf(prefix, spokenValue, spokenCharge).filter { it.isNotEmpty() }.joinToString(", ")
    val color = if (reading.level != null && reading.level <= 20) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Column(Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(22.dp, 12.dp)) {
                val stroke = 1.dp.toPx()
                val bodyWidth = size.width - 2.dp.toPx()
                drawRoundRect(color, Offset(stroke / 2, stroke / 2), Size(bodyWidth - stroke, size.height - stroke),
                    CornerRadius(2.dp.toPx()), style = Stroke(stroke))
                drawRect(color, Offset(bodyWidth, size.height / 3), Size(2.dp.toPx(), size.height / 3))
                reading.level?.takeIf { it > 0 }?.let {
                    val inset = 2.dp.toPx()
                    drawRect(color, Offset(inset, inset), Size((bodyWidth - 2 * inset) * it / 100f, size.height - 2 * inset))
                }
            }
            if (charging) Icon(painterResource(R.drawable.ic_settings_charging), contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(listOf(prefix, value).filter { it.isNotEmpty() }.joinToString(" "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
        if (reading.status == BatteryStatus.OPTIMIZED_CHARGING) {
            Text(stringResource(R.string.battery_optimized_charging),
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
