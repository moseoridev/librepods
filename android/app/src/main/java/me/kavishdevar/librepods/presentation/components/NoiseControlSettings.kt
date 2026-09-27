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

import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
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
    val selected = NoiseControlMode.entries.getOrNull(noiseControlModeValue - 1)
    Column {
        BudsSectionLabel(stringResource(R.string.noise_control))
        StyledList {
            item { _, _ ->
                BudsNoiseControlRow(modes, selected, { onNoiseControlModeChanged(it.ordinal + 1) })
            }
        }
    }
}

/** Home control geometry from tl.e0.T / vb.q.b / gl.s; artwork remains upstream-owned. */
@Composable
internal fun BudsNoiseControlRow(
    modes: List<NoiseControlMode>,
    selected: NoiseControlMode?,
    onSelect: (NoiseControlMode) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (NoiseControlMode) -> String = { mode ->
        stringResource(when (mode) {
            NoiseControlMode.OFF -> R.string.off
            NoiseControlMode.TRANSPARENCY -> R.string.transparency
            NoiseControlMode.ADAPTIVE -> R.string.adaptive
            NoiseControlMode.NOISE_CANCELLATION -> R.string.noise_cancellation
        })
    },
    artwork: @Composable (NoiseControlMode, Boolean) -> Unit = { mode, isSelected ->
        val icon = when (mode) {
            NoiseControlMode.OFF -> R.drawable.widget_noise_off
            NoiseControlMode.TRANSPARENCY -> R.drawable.transparency
            NoiseControlMode.ADAPTIVE -> R.drawable.adaptive
            NoiseControlMode.NOISE_CANCELLATION -> R.drawable.noise_cancellation
        }
        Icon(painterResource(icon), null, Modifier.size(30.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
) {
    if (modes.isEmpty()) return
    val width = if (LocalConfiguration.current.screenWidthDp < 589) 320.dp else 392.dp
    val captionWidth = minOf(width.value * .2f, 80f).toInt().dp
    val track = if (isSystemInDarkTheme()) Color(0xFF3E3E3E) else Color(0xFFEDEDED)
    val tick = remember(track) { GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(track.toArgb()) } }
    // The source centers a responsive strip, rather than giving each caption a share of the card.
    Box(modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.TopCenter) {
        Layout(modifier = Modifier.width(width).padding(horizontal = 10.dp).selectableGroup(), content = {
            Canvas(Modifier.fillMaxWidth().height(44.dp)) {
                val radius = 22.dp.toPx()
                if (modes.size > 1) drawLine(track, Offset(radius, size.height / 2),
                    Offset(size.width - radius, size.height / 2), strokeWidth = 6.dp.toPx())
                // The native track draws oval ticks underneath the Compose option backgrounds.
                drawIntoCanvas { canvas ->
                    modes.indices.forEach { index ->
                        val center = if (modes.size == 1) size.width / 2
                            else radius + (size.width - 2 * radius) * index / (modes.size - 1)
                        tick.setBounds((center - radius).toInt(), 0, (center + radius).toInt(), size.height.toInt())
                        tick.draw(canvas.nativeCanvas)
                    }
                }
            }
            modes.forEach { mode ->
                val isSelected = mode == selected
                Box(Modifier.width(captionWidth).selectable(isSelected, role = Role.RadioButton,
                    onClick = { if (!isSelected) onSelect(mode) })) {
                    Box(Modifier.size(44.dp).align(Alignment.TopCenter)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else track, CircleShape),
                        contentAlignment = Alignment.Center) { artwork(mode, isSelected) }
                    // `gl.s` offsets the caption with one inset on the label's own modifier —
                    // `r0.c.D(text, 0f, 52f, 0f, 0f)`, a single 52dp top padding measured from the
                    // option's top — and sizes the artwork separately, rather than stacking a 44dp
                    // box and an 8dp gap. The two are equal in exact arithmetic and differ once
                    // rounded: at density 3.75 both reach 195px, but at 2.8125 the stack rounds
                    // twice (124 + 23 = 147) where one inset rounds once (146). That one pixel,
                    // applied to a caption, propagates down the page as a global vertical offset
                    // and was the whole of the 1080p disagreement (2.224% -> 0.308%); at 3.75 the
                    // change is pixel-identical. `captionWidth` still constrains the label, so the
                    // alignment only moves it vertically.
                    Text(label(mode), modifier = Modifier.align(Alignment.TopCenter).padding(top = 52.dp),
                        style = BudsStyle.RowTitle.copy(fontSize = 12.sp),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center)
                }
            }
        }) { measurables, constraints ->
            val stripWidth = constraints.maxWidth
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val options = measurables.drop(1).map { it.measure(loose) }
            val line = measurables.first().measure(Constraints.fixed(stripWidth, 44.dp.roundToPx()))
            val height = options.maxOf { it.height }
            val diameter = 44.dp.roundToPx()
            layout(stripWidth, height) {
                line.placeRelative(0, 0)
                options.forEachIndexed { index, option ->
                    val center = if (options.size == 1) stripWidth / 2f
                        else diameter / 2f + (stripWidth - diameter) * index.toFloat() / (options.size - 1)
                    option.placeRelative((center - option.width / 2f).roundToInt(), 0)
                }
            }
        }
    }
}
