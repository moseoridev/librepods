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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import me.kavishdevar.librepods.ui.theme.switchThumb
import me.kavishdevar.librepods.ui.theme.interactionFeedback
import me.kavishdevar.librepods.ui.theme.SettingsStyle

/** Separate pointer feedback while the containing row owns accessibility and keyboard input. */
@Composable
internal fun SettingsRowSwitch(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    startInset: Dp = 5.5.dp,
    visualEnabled: Boolean = enabled,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val currentChecked by rememberUpdatedState(checked)
    val currentChange by rememberUpdatedState(onCheckedChange)
    SettingsSwitchVisual(checked, visualEnabled, dimControl = false, startInset = startInset,
        interactionSource = interactionSource, feedbackEnabled = enabled,
        modifier = Modifier.hoverable(interactionSource, enabled)
            .pointerInput(enabled, interactionSource) {
                if (enabled) detectTapGestures(onPress = { position ->
                    val press = PressInteraction.Press(position)
                    interactionSource.emit(press)
                    var finished = false
                    try {
                        val released = tryAwaitRelease()
                        interactionSource.emit(if (released) PressInteraction.Release(press)
                            else PressInteraction.Cancel(press))
                        finished = true
                    } finally {
                        // Disposal/disable can cancel this coroutine before it emits
                        // the terminal interaction. Never leave the thumb held.
                        if (!finished) interactionSource.tryEmit(PressInteraction.Cancel(press))
                    }
                }, onTap = { currentChange(!currentChecked) })
            })
}

/** ww.s.a: .8 content-only thumb scaling and an unscaled 5.5dp feedback margin. */
@Composable
private fun settingsSwitchThumbFeedback(enabled: Boolean, source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val hovered by source.collectIsHoveredAsState()
    val color = MaterialTheme.colorScheme.interactionFeedback
    val contentScale by animateFloatAsState(if (enabled && pressed) .8f else 1f,
        tween(if (pressed) 100 else 350,
            easing = if (pressed) LinearEasing else CubicBezierEasing(.22f, .25f, 0f, 1f)),
        label = "Settings switch thumb scale")
    val alpha by animateFloatAsState(if (!enabled) 0f else when {
        pressed -> 1f
        focused -> .8f
        hovered -> .6f
        else -> 0f
    }, tween(if (pressed) 100 else 350,
        easing = if (pressed) LinearEasing else CubicBezierEasing(.17f, .17f, .67f, 1f)),
        label = "Settings switch thumb feedback")
    return Modifier.drawWithContent {
        scale(if (enabled) contentScale else 1f) { this@drawWithContent.drawContent() }
        if (enabled && alpha > 0f) {
            val extra = 11.dp.roundToPx().toFloat()
            val feedbackSize = Size(size.width + extra, size.height + extra)
            drawRoundRect(color, topLeft = Offset(-5.5.dp.toPx(), -5.5.dp.toPx()),
                size = feedbackSize, cornerRadius = CornerRadius(feedbackSize.minDimension / 2f),
                alpha = alpha)
        }
    }
}

/** Decorative switch: the containing row owns the single accessible toggle action. */
@Composable
internal fun SettingsSwitchVisual(
    checked: Boolean,
    enabled: Boolean,
    dimControl: Boolean = true,
    startInset: Dp = 5.5.dp,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    feedbackEnabled: Boolean = enabled,
) {
    val progress by animateFloatAsState(if (checked) 1f else 0f,
        animationSpec = tween(200), label = "Settings switch")
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
    // wm.y0 adds 5.5dp horizontal padding to its row-owned switch slot. The
    // decorative control stays centered within the row's natural text height.
    // x0.e uses the theme's default .96 FeedbackScaling pill on the switch core;
    // ww.s adds a second, independently scaled thumb effect inside that core.
    val coreFeedback = if (interactionSource == null) Modifier else
        settingsButtonFeedback(feedbackEnabled, interactionSource, SettingsStyle.PillShape)
    val thumbFeedback = if (interactionSource == null) Modifier else
        settingsSwitchThumbFeedback(feedbackEnabled, interactionSource)
    Layout(modifier = modifier.graphicsLayer { alpha = if (dimControl) disabledAlpha else 1f }
        .padding(start = startInset, end = 5.5.dp).then(coreFeedback), content = {
        Box(Modifier.size(32.dp, 20.dp)
            .background(track, RoundedCornerShape(50)))
        Box(Modifier.padding(2.dp).size(16.dp)
            .then(thumbFeedback)
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
