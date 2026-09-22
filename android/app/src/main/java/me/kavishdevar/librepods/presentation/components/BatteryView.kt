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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.Battery

@Composable
fun BatteryView(batteryList: List<Battery>, budsRes: Int, caseRes: Int) {
    val readings = SettingsBatteries.from(batteryList)
    val combined = readings.combined
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(budsRes), contentDescription = null,
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(128.dp))
            Spacer(Modifier.height(12.dp))
            if (combined != null) {
                BatteryIndicator(combined.level, combined.status, stringResource(R.string.buds))
            } else {
                BatteryIndicator(readings.left.level, readings.left.status, stringResource(R.string.left))
                BatteryIndicator(readings.right.level, readings.right.status, stringResource(R.string.right))
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(caseRes), contentDescription = null,
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(128.dp))
            Spacer(Modifier.height(12.dp))
            BatteryIndicator(readings.case.level, readings.case.status, stringResource(R.string.case_alt))
        }
    }
}
