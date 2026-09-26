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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.theme.switchThumb

/** Decorative switch: the containing row owns the single accessible toggle action. */
@Composable
internal fun BudsSwitch(checked: Boolean, enabled: Boolean) {
    val progress by animateFloatAsState(if (checked) 1f else 0f,
        animationSpec = tween(200), label = "Buds switch")
    val colors = MaterialTheme.colorScheme
    // The track follows the checked state alone: `sesl_switch_track_off_color` (outline) gives way
    // to `sesl_switch_track_on_color` (primary) as the thumb travels. Disabling swaps both tokens
    // for their `_on_disabled_`/`_off_disabled_` members, which are those same two colours at 40%;
    // the hue therefore still reports the state rather than collapsing to a neutral grey.
    val disabledAlpha = if (enabled) 1f else .4f
    val track = lerp(colors.outline, colors.primary, progress).copy(alpha = disabledAlpha)
    // That 40% is the token's own opacity and is not the whole story: the disabled row dims the
    // finished control by a further 40%, and the two multiply. The switch therefore composites as
    // a layer rather than by fading each colour again — 0.4 x 0.4 is what puts the visible track
    // at the measured (220,231,255) over the card, or (230,230,232) where the track is the off
    // colour, and it keeps the thumb (234,240,255) a lighter disc *over* the track rather than a
    // hole through to the card. SESL measures track and padded thumb independently; their pixel
    // heights can differ at fractional densities, and the measured thumb width also sets travel.
    Layout(modifier = Modifier.graphicsLayer { alpha = disabledAlpha }, content = {
        Box(Modifier.size(32.dp, 20.dp)
            .background(track, RoundedCornerShape(50)))
        Box(Modifier.padding(2.dp).size(16.dp)
            .background(colors.switchThumb.copy(alpha = disabledAlpha), CircleShape))
    }) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val track = measurables[0].measure(loose)
        val thumb = measurables[1].measure(loose)
        val height = maxOf(track.height, thumb.height)
        layout(track.width, height) {
            track.placeRelative(0, (height - track.height) / 2)
            thumb.placeRelative((progress * (track.width - thumb.width)).toInt(), (height - thumb.height) / 2)
        }
    }
}
