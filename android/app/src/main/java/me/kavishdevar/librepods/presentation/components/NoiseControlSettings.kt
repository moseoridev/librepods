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
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
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
    onNoiseControlModeChanged: (Int) -> Unit,
    showSectionLabel: Boolean = true,
    conversationAwarenessChecked: Boolean? = null,
    conversationAwarenessEnabled: Boolean = true,
    onConversationAwarenessChanged: (Boolean) -> Unit = {},
) {
    val modes = buildList {
        if (showOffListeningMode) add(NoiseControlMode.OFF)
        add(NoiseControlMode.TRANSPARENCY)
        add(NoiseControlMode.ADAPTIVE)
        add(NoiseControlMode.NOISE_CANCELLATION)
    }
    val selected = NoiseControlMode.entries.getOrNull(noiseControlModeValue - 1)
    Column {
        if (showSectionLabel) BudsSectionLabel(stringResource(R.string.noise_control))
        StyledList {
            item { _, _ ->
                BudsNoiseControlRow(modes, selected, { onNoiseControlModeChanged(it.ordinal + 1) })
                if (conversationAwarenessChecked != null) BudsRowDivider()
            }
            if (conversationAwarenessChecked != null) {
                StyledToggle(
                    label = stringResource(R.string.conversational_awareness),
                    checked = conversationAwarenessChecked,
                    enabled = conversationAwarenessEnabled,
                    onCheckedChange = onConversationAwarenessChanged,
                    trailingDivider = true,
                    homeMenu = true,
                )
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
    stripWidth: Dp? = null,
    captionWidth: Dp? = null,
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
    val track = if (isSystemInDarkTheme()) Color(0xFF3E3E3E) else Color(0xFFEDEDED)
    val tick = remember(track) { GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(track.toArgb()) } }
    // The Home strip follows the card width. Its content starts after the same 18dp
    // inset used by rows on either side. The caption width is calculated separately
    // from the display width by the source: min(screenWidthDp * .2f, 80f), truncated
    // to integer dp before measurement. Keeping it independent of the constrained
    // strip also preserves the density-dependent Korean line break at 384dp width.
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    BoxWithConstraints(modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.TopCenter) {
        val width = stripWidth ?: minOf(maxWidth - BudsStyle.RowInset * 2, 392.dp)
        val measuredCaptionWidth = captionWidth ?: minOf(screenWidthDp * .2f, 80f).toInt().dp
        Layout(modifier = Modifier.width(width).padding(horizontal = 10.dp).selectableGroup(), content = {
            Canvas(Modifier.fillMaxWidth().height(44.dp)) {
                // The native background reads the tick drawable's intrinsic 44dp width in
                // integer pixels, then halves it for the first/last centres and oval bounds.
                // At density 2.8125 that is 124 / 2, rather than 22dp = 61.875px.
                val radius = 44.dp.roundToPx() / 2f
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
                // Artwork and label are placed independently below: the source hangs the 44dp disc
                // on the track's own SpaceBetween slots and gives the label its own box, and at
                // 3.75 the two sequences round apart by a pixel.
                Box(Modifier.size(44.dp)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else track, CircleShape),
                    contentAlignment = Alignment.Center) { artwork(mode, isSelected) }
                Box(Modifier.width(measuredCaptionWidth).selectable(isSelected, role = Role.RadioButton,
                    onClick = { if (!isSelected) onSelect(mode) })) {
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
            // Content is exactly [track, (disc, caption) * n]; the pairing below depends on that
            // order, so any child added to the content lambda must keep the disc/caption couplet
            // intact or be appended after all pairs. The two are measured and placed separately
            // because the source does not nest them: the disc rides the track's own slots, and the
            // caption sits in its own box. Nesting the disc inside the caption box rounds twice —
            // box origin, then the centring inside it — which is what put our first disc a pixel
            // off at 3.75 while 2.8125 and 3.5 agreed.
            val line = measurables.first().measure(Constraints.fixed(stripWidth, 44.dp.roundToPx()))
            val discs = ArrayList<Placeable>(measurables.size / 2)
            val captions = ArrayList<Placeable>(measurables.size / 2)
            measurables.drop(1).forEachIndexed { index, measurable ->
                val placeable = measurable.measure(loose)
                if (index % 2 == 0) discs.add(placeable) else captions.add(placeable)
            }
            val height = captions.maxOf { it.height }
            val diameter = 44.dp.roundToPx()
            // `gl.s` lays the discs out with Arrangement.SpaceBetween, which accumulates the
            // fractional gap as a float and rounds each slot once, so at 3.75 the slot origins are
            // 0/320/639/959 rather than the 0/283/602/922 a per-slot centre would give. Both are
            // exact in dp; they differ only where roundToInt sees a fraction, which is why 2.8125
            // and 3.5 already agreed and 3.75 did not.
            val discLefts = IntArray(discs.size)
            if (discs.size == 1) {
                // Unreachable from the only caller (which always passes three or four modes), and
                // deliberately not SpaceBetween's own single-child rule, which is position 0. The
                // guard exists to keep the divisor below non-zero if that ever changes.
                discLefts[0] = (stripWidth - diameter) / 2
            } else {
                val space = (stripWidth - diameter * discs.size).toFloat() / (discs.size - 1)
                var position = 0f
                for (i in discs.indices) {
                    discLefts[i] = position.roundToInt()
                    position += diameter + space
                }
            }
            layout(stripWidth, height) {
                line.placeRelative(0, 0)
                discs.forEachIndexed { index, disc -> disc.placeRelative(discLefts[index], 0) }
                captions.forEachIndexed { index, caption ->
                    val center = if (captions.size == 1) stripWidth / 2f
                        else diameter / 2f + (stripWidth - diameter) * index.toFloat() / (captions.size - 1)
                    // The caption is centred on the disc above it and is wider than it, so for the
                    // first option the centre constraint is over-satisfied and resolves to a
                    // negative local x: at 3.75 the slack is (165 - 240) / 2 = -37.5. The source
                    // lays this out through ConstraintLayout, whose solver writes that coordinate
                    // as Math.round(-37.5) = -37 and then recovers the frame by adding a half and
                    // truncating toward zero, so -36.5 becomes -36. Rounding the half once, as
                    // Compose places it, lands on -37 instead — one pixel left of the source. The
                    // second truncation is a no-op for a positive coordinate, which is why the
                    // three captions that wrap to the full 64dp cap are already exact and only the
                    // narrow one disagrees; and why it disagrees at 3.75, 2.8125 and 3.5 alike,
                    // since all three leave the same negative half. Keyed on the sign rather than
                    // on the option, so it stays correct if the strip or the labels change.
                    val left = center - caption.width / 2f
                    val rounded = left.roundToInt()
                    caption.placeRelative(if (left < 0f) rounded + 1 else rounded, 0)
                }
            }
        }
    }
}
