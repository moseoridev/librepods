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

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.theme.sliderThumbCore
import me.kavishdevar.librepods.presentation.theme.sliderTrack

/** `dimen/sesl_seekbar_track_height`. */
private val TrackHeight = 3.dp

/** `dimen/sesl_seekbar_track_height_expand`; reached while the seekbar is pressed. */
private val ExpandedTrackHeight = 13.dp

/** `dimen/sesl_seekbar_thumb_radius`. */
private val ThumbRadius = 6.5.dp

/** `dimen/sesl_seekbar_thumb_stroke`; the thumb core is inset from the ring by this much. */
private val ThumbStroke = 2.dp

/**
 * SESL seekbar renderer: `SeslAbsSeekBar.C()` composes the `n2` track and `p2` thumb drawables.
 * The track is a round-capped stroke that widens while pressed; the thumb is a filled ring with a
 * concentric core. The widget's own 16dp horizontal padding is left to the containing row so the
 * control lines up with the other cards. `bool/sesl_seekbar_sliding_animation` is enabled upstream,
 * so the native 3dp <-> 13dp transition is kept; its exact path interpolator is not reproduced.
 * Traced under `librepodsDesignReference/buds4-settings/2026-09-26-slider/`.
 */
@Composable
internal fun BudsSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val state = remember(valueRange) { SliderState(value, valueRange = valueRange) }
    state.onValueChange = onValueChange
    state.value = value

    // Multiply rather than replace: the inactive track is a translucent role and must keep its
    // own alpha so it composites over the card surface.
    val alpha = if (enabled) 1f else .4f
    val stroke by animateDpAsState(
        targetValue = if (enabled && (pressed || state.isDragging)) ExpandedTrackHeight else TrackHeight,
        animationSpec = tween(250), label = "Buds seekbar track")
    val colors = MaterialTheme.colorScheme
    fun Color.fade() = copy(alpha = this.alpha * alpha)
    val active = colors.primary.fade()
    val inactive = colors.sliderTrack.fade()
    val core = colors.sliderThumbCore.fade()

    // The native thumb and track are measured independently; the thumb still sets the travel.
    Slider(
        state = state,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        thumb = {
            Canvas(Modifier.size(ThumbRadius * 2)) {
                // The row's inherited 16dp minimum height wins over the 13dp request, so this slot is
                // not square; the circle stays centred while the inset is measured against the radius.
                val radius = ThumbRadius.toPx()
                drawCircle(active, radius = radius)
                drawCircle(core, radius = radius - ThumbStroke.toPx())
            }
        },
        track = {
            Canvas(Modifier.fillMaxWidth().height(ExpandedTrackHeight)) {
                // n2 insets the line by half its stroke, so the round caps reach the track bounds
                // and the active segment stays under the thumb at both endpoints.
                val half = stroke.toPx() / 2f
                val centerY = size.height / 2f
                val end = size.width - half
                // The track slot is mirrored as a whole under RTL, so the canvas itself never sees a
                // flipped coordinate space; mirror the line endpoints instead, or the fill would grow
                // away from the thumb while the thumb itself travels right to left.
                val startOffset = if (layoutDirection == LayoutDirection.Rtl) end else half
                val endOffset = if (layoutDirection == LayoutDirection.Rtl) half else end
                // A zero-length round-capped line still paints a dot, so the active segment is
                // skipped at the low endpoint as the native ClipDrawable does at level 0.
                val fraction = it.coercedValueAsFraction
                drawLine(inactive, Offset(startOffset, centerY), Offset(endOffset, centerY),
                    strokeWidth = stroke.toPx(), cap = StrokeCap.Round)
                if (fraction > 0f) {
                    drawLine(active, Offset(startOffset, centerY),
                        Offset(startOffset + (endOffset - startOffset) * fraction, centerY),
                        strokeWidth = stroke.toPx(), cap = StrokeCap.Round)
                }
            }
        },
    )
}
