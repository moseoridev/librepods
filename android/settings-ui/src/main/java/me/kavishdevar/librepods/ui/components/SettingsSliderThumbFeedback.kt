package me.kavishdevar.librepods.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.interactionFeedback

/** wm.n0 / e4.c: .98 content-only thumb scaling with an unscaled 6dp circular margin. */
@Composable
internal fun settingsSliderThumbFeedback(
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val color = MaterialTheme.colorScheme.interactionFeedback
    val thumbScale by animateFloatAsState(
        if (enabled && (pressed || dragged)) .98f else 1f,
        tween(if (pressed || dragged) 100 else 350,
            easing = if (pressed || dragged) LinearEasing else CubicBezierEasing(.22f, .25f, 0f, 1f)),
        label = "Settings slider thumb scale")
    val feedbackAlpha by animateFloatAsState(
        if (!enabled) 0f else when {
            pressed -> 1f
            focused -> .8f
            hovered -> .6f
            dragged -> 1f
            else -> 0f
        },
        tween(if (pressed || dragged) 100 else 350,
            easing = if (pressed || dragged) LinearEasing else CubicBezierEasing(.17f, .17f, .67f, 1f)),
        label = "Settings slider feedback")
    return Modifier.drawWithContent {
        scale(if (enabled) thumbScale else 1f) { this@drawWithContent.drawContent() }
        if (enabled && feedbackAlpha > 0f) {
            // nw.n0 rounds the summed margins but translates by the float leading margin.
            val extra = 12.dp.roundToPx().toFloat()
            val feedbackSize = Size(size.width + extra, size.height + extra)
            drawRoundRect(color, topLeft = Offset(-6.dp.toPx(), -6.dp.toPx()),
                size = feedbackSize, cornerRadius = CornerRadius(feedbackSize.minDimension / 2f),
                alpha = feedbackAlpha)
        }
    }
}
