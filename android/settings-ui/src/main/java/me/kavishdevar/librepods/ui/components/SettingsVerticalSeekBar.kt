package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderState
import androidx.compose.material3.VerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.sliderThumbCore
import me.kavishdevar.librepods.ui.theme.sliderTrack

/**
 * Caller-owned gain control. om.h.q / kw.i2 / e4.c supply the 3dp track,
 * 6dp end padding and 13dp thumb. Give the control a bounded height.
 * Compose VerticalSlider supplies gestures, keyboard input and range semantics.
 */
@Composable
fun SettingsVerticalSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    steps: Int = 0,
) {
    val colors = MaterialTheme.colorScheme
    val state = remember(valueRange, steps) { SliderState(value, steps = steps, valueRange = valueRange) }
    state.value = value
    state.onValueChange = { if (enabled) onValueChange(it) }
    Box(modifier.width(13.dp).then(if (enabled) Modifier else Modifier.graphicsLayer { alpha = .4f }),
        contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            // Keep Material's interaction layer and its thumb-height-based travel, but
            // measure artwork independently of Material's 16dp width minimum.
            VerticalSlider(state = state, enabled = enabled, reverseDirection = true,
                modifier = Modifier.fillMaxHeight().then(label?.let {
                    Modifier.semantics { contentDescription = it }
                } ?: Modifier),
                thumb = { Box(Modifier.size(13.dp)) },
                track = { Box(Modifier.width(13.dp).fillMaxHeight()) })
        }
        Layout(modifier = Modifier.matchParentSize().padding(vertical = 6.dp), content = {
            Canvas(Modifier.width(3.dp).fillMaxHeight().clip(CircleShape)) {
                drawRect(colors.sliderTrack)
                val progress = state.coercedValueAsFraction * size.height
                if (progress > 0f) drawRect(colors.primary,
                    topLeft = Offset(0f, size.height - progress),
                    size = androidx.compose.ui.geometry.Size(size.width, progress))
            }
        }) { measurables, constraints ->
            val track = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
            // kw.i2 rotates the source track by 180 degrees (s2.z.u, rotation bit 256).
            // Direct bottom-up drawing uses the equivalent mirrored center placement;
            // an odd remaining width therefore puts the extra pixel on the right.
            layout(constraints.maxWidth, constraints.maxHeight) {
                track.placeRelative((constraints.maxWidth - track.width) / 2, 0)
            }
        }
        Layout(modifier = Modifier.matchParentSize(), content = {
            Box(Modifier.size(13.dp).shadow(1.dp, CircleShape)
                .border(2.dp, colors.primary, CircleShape)
                .background(colors.sliderThumbCore, CircleShape))
        }) { measurables, constraints ->
            val thumb = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val x = Alignment.Center.align(IntSize(thumb.width, thumb.height), IntSize(width, height), layoutDirection).x
            // nw.j0 reverses the fraction, then truncates the travelled distance.
            val travel = (height - thumb.height).coerceAtLeast(0)
            val y = (travel * (1f - state.coercedValueAsFraction)).toInt()
            layout(width, height) { thumb.placeRelative(x, y) }
        }
    }
}
