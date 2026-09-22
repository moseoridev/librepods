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
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.NoiseControlMode
import me.kavishdevar.librepods.presentation.theme.BudsStyle

@Composable
fun NoiseControlSettings(
    showOffListeningMode: Boolean,
    noiseControlModeValue: Int,
    onNoiseControlModeChanged: (Int) -> Unit
) {
    val modes = buildList {
        if (showOffListeningMode) add(NoiseControlMode.OFF)
        add(NoiseControlMode.TRANSPARENCY)
        add(NoiseControlMode.ADAPTIVE)
        add(NoiseControlMode.NOISE_CANCELLATION)
    }
    // An absent/invalid UiState value must not select a default mode.
    val selected = NoiseControlMode.entries.getOrNull(noiseControlModeValue - 1)
    val dark = isSystemInDarkTheme()
    val track = if (dark) Color(0xFF3E3E3E) else Color(0xFFEDEDED)
    val active = if (dark) Color(0xFFFCFCFF) else Color(0xFF252528)
    Column {
        BudsSectionLabel(stringResource(R.string.noise_control))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(BudsStyle.GroupRadius))
            .background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 16.dp)) {
            // Buds home gl.s: 44dp circles, captions at 52dp; native background uses a 6dp line.
            Canvas(Modifier.fillMaxWidth().height(48.dp)) {
                val halfCell = size.width / (modes.size * 2)
                drawLine(track, Offset(halfCell, size.height / 2),
                    Offset(size.width - halfCell, size.height / 2), strokeWidth = 6.dp.toPx())
            }
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                modes.forEach { mode ->
                    val label = stringResource(when (mode) {
                        NoiseControlMode.OFF -> R.string.off
                        NoiseControlMode.TRANSPARENCY -> R.string.transparency
                        NoiseControlMode.ADAPTIVE -> R.string.adaptive
                        NoiseControlMode.NOISE_CANCELLATION -> R.string.noise_cancellation
                    })
                    val icon = when (mode) {
                        NoiseControlMode.OFF -> R.drawable.widget_noise_off
                        NoiseControlMode.TRANSPARENCY -> R.drawable.transparency
                        NoiseControlMode.ADAPTIVE -> R.drawable.adaptive
                        NoiseControlMode.NOISE_CANCELLATION -> R.drawable.noise_cancellation
                    }
                    val isSelected = selected == mode
                    Column(Modifier.weight(1f).selectable(selected = isSelected, role = Role.RadioButton,
                        onClick = { if (!isSelected) onNoiseControlModeChanged(mode.ordinal + 1) }),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.height(48.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(44.dp).background(if (isSelected) active else track, CircleShape),
                                contentAlignment = Alignment.Center) {
                                Icon(painterResource(icon), contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(30.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp))
                    }
                }
            }
        }
    }
}
