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

package me.kavishdevar.librepods.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import me.kavishdevar.librepods.ui.theme.SettingsStyle
import me.kavishdevar.librepods.ui.theme.sliderThumbCore
import me.kavishdevar.librepods.ui.theme.sliderTrack
import kotlin.math.roundToInt

/** `dimen/sesl_seekbar_track_height`. */
private val TrackHeight = 3.dp

/** `dimen/sesl_seekbar_track_height_expand`; reached while the seekbar is pressed. */
private val ExpandedTrackHeight = 13.dp

/** `dimen/sesl_seekbar_thumb_radius`. */
private val ThumbRadius = 6.5.dp

/** `dimen/sesl_seekbar_thumb_stroke`; the thumb core is inset from the ring by this much. */
private val ThumbStroke = 2.dp

/** The current Compose slider and the earlier narrow widget have distinct geometry. */
enum class SettingsSeekBarStyle { Standard, Slim }

// Both slots are measured before either is placed. Keep the full native track width
// available to the thumb's placement without a state write or another composition.
private class SettingsSeekBarMeasurement { var trackWidth = 0 }

/**
 * Caller-owned horizontal value control. Standard follows the current `wm.k1.D → kw.t.E`
 * Compose renderer: a full-width 14dp track, 20dp thumb and 22dp measured viewport.
 * Steps describe selectable intervals; tickCount controls the independently drawn markers.
 * Slim retains the 3dp track / 13dp thumb of the earlier `SeslAbsSeekBar.C` renderer.
 * Gesture, keyboard and accessibility handling are supplied by Compose Slider.
 */
@Composable
fun SettingsSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: SettingsSeekBarStyle = SettingsSeekBarStyle.Standard,
    steps: Int = 0,
    tickCount: Int = 0,
    activeTrackColor: Color = Color.Unspecified,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    require(steps >= 0) { "steps must be nonnegative" }
    require(tickCount >= 0) { "tickCount must be nonnegative" }
    if (style == SettingsSeekBarStyle.Slim) {
        SettingsSlimSeekBar(value, onValueChange, valueRange, modifier, enabled, steps,
            onValueChangeFinished, activeTrackColor)
        return
    }
    SettingsStandardSeekBar(value, onValueChange, valueRange, modifier, enabled, steps, tickCount,
        onValueChangeFinished, if (enabled) 1f else .4f, activeTrackColor)
}

@Composable
private fun SettingsStandardSeekBar(
    value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier, enabled: Boolean, steps: Int, tickCount: Int,
    onValueChangeFinished: (() -> Unit)?, contentAlpha: Float, activeTrackColor: Color,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val measurement = remember { SettingsSeekBarMeasurement() }
    val dragged by interactionSource.collectIsDraggedAsState()
    val state = remember(valueRange, steps) {
        SliderState(value, steps = steps, valueRange = valueRange)
    }
    // Compose's SetProgress semantics can be invoked on a disabled node as well.
    state.onValueChange = { if (enabled) onValueChange(it) }
    state.onValueChangeFinished = onValueChangeFinished?.let { { if (enabled) it() } }
    state.value = value
    val trackHeight by animateDpAsState(
        // ww.m / kw.d2: pressing alone retains the 14dp track.
        if (enabled && dragged) 22.dp else 14.dp,
        tween(350, easing = CubicBezierEasing(.22f, .25f, 0f, 1f)),
        label = "Settings slider track")
    val colors = MaterialTheme.colorScheme
    val thumbFeedback = settingsSliderThumbFeedback(enabled, interactionSource)
    val progressColor = if (activeTrackColor == Color.Unspecified) colors.primary else activeTrackColor
    // wm.k1.D dims the completed content once; preserve the translucent track's own alpha.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Slider(state = state, enabled = enabled, interactionSource = interactionSource,
            modifier = modifier.then(if (contentAlpha < 1f) Modifier.graphicsLayer { alpha = contentAlpha } else Modifier),
            thumb = {
                // wm.n0: native rounded surface, 1dp elevation, 2dp border and core fill.
                Box(Modifier.size(20.dp).layout { measurable, constraints ->
                    val thumb = measurable.measure(constraints)
                    layout(thumb.width, thumb.height) {
                        // nw.i0 truncates the travelled distance; Material rounds it.
                        // Correct the artwork in relative coordinates so RTL mirrors
                        // the same rule, without changing Slider's input/semantics.
                        val distance = (measurement.trackWidth - thumb.width).coerceAtLeast(0) *
                            state.coercedValueAsFraction
                        thumb.placeRelative(distance.toInt() - distance.roundToInt(), 0)
                    }
                }.hoverable(interactionSource, enabled).then(thumbFeedback).shadow(1.dp, CircleShape)
                    .border(2.dp, colors.primary, CircleShape)
                    .background(colors.sliderThumbCore, CircleShape))
            },
            track = {
                // Material Slider measures its track between thumb centers. The source track
                // covers the full control; extend this drawing slot by the thumb diameter while
                // retaining the measured width and native thumb travel (width - diameter).
                Box(Modifier.fillMaxWidth().height(22.dp).layout { measurable, constraints ->
                    val diameter = 20.dp.roundToPx()
                    val content = measurable.measure(constraints.offset(horizontal = diameter))
                    measurement.trackWidth = content.width
                    layout(content.width - diameter, content.height) {
                        content.placeRelative(-diameter / 2, 0)
                    }
                }, contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxWidth().height(trackHeight).clip(CircleShape)
                        .background(colors.sliderTrack).drawWithContent {
                            drawContent()
                            val fraction = it.coercedValueAsFraction
                            val width = size.width * fraction
                            drawRect(progressColor,
                                topLeft = Offset(if (layoutDirection == LayoutDirection.Rtl)
                                    size.width - width else 0f, 0f),
                                size = androidx.compose.ui.geometry.Size(width, size.height))
                        })
                    // wm.m0 draws 8dp markers with 4dp horizontal padding, independently
                    // from selectable steps. The active thumb covers a marker at an endpoint.
                    if (tickCount > 0) Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        repeat(tickCount) {
                            Box(Modifier.padding(horizontal = 4.dp).size(8.dp)
                                .background(colors.outline, CircleShape))
                        }
                    }
                }
            })
    }
}

/** Title, value control and endpoint labels from `wm.k1.D`; the caller supplies the card. */
@Composable
fun SettingsSeekBarRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    steps: Int = 0,
    tickCount: Int = 0,
    activeTrackColor: Color = Color.Transparent,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = SettingsStyle.RowInset)
        .padding(bottom = 12.dp).then(if (enabled) Modifier else Modifier.graphicsLayer { alpha = .4f })) {
        Column(Modifier.heightIn(min = SettingsStyle.RowMinHeight)
            .padding(vertical = SettingsStyle.RowVerticalPadding)) {
            Text(title, style = SettingsStyle.RowTitle, color = MaterialTheme.colorScheme.onSurface)
        }
        // The row owns its opacity so a disabled control is not dimmed a second time.
        require(steps >= 0 && tickCount >= 0) { "steps and tickCount must be nonnegative" }
        SettingsStandardSeekBar(value, onValueChange, valueRange,
            Modifier.fillMaxWidth().semantics { contentDescription = title }, enabled,
            steps, tickCount, onValueChangeFinished, contentAlpha = 1f, activeTrackColor)
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(startLabel, style = SettingsStyle.RowSummary, color = MaterialTheme.colorScheme.primary)
            Text(endLabel, style = SettingsStyle.RowSummary, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SettingsSlimSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier,
    enabled: Boolean,
    steps: Int,
    onValueChangeFinished: (() -> Unit)?,
    activeTrackColor: Color,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val state = remember(valueRange, steps) { SliderState(value, steps = steps, valueRange = valueRange) }
    state.onValueChange = { if (enabled) onValueChange(it) }
    state.onValueChangeFinished = onValueChangeFinished?.let { { if (enabled) it() } }
    state.value = value

    // Multiply rather than replace: the inactive track is a translucent role and must keep its
    // own alpha so it composites over the card surface.
    val alpha = if (enabled) 1f else .4f
    val stroke by animateDpAsState(
        targetValue = if (enabled && (pressed || state.isDragging)) ExpandedTrackHeight else TrackHeight,
        animationSpec = tween(250), label = "Settings seekbar track")
    val colors = MaterialTheme.colorScheme
    fun Color.fade() = copy(alpha = this.alpha * alpha)
    val thumb = colors.primary.fade()
    val active = (if (activeTrackColor == Color.Unspecified) colors.primary else activeTrackColor).fade()
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
                drawCircle(thumb, radius = radius)
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
