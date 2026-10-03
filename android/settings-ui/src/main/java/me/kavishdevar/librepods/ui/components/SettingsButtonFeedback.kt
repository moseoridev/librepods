package me.kavishdevar.librepods.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.scale
import me.kavishdevar.librepods.ui.theme.interactionFeedback

/** kw.t.g → kw.o → vw.a.c: shared by ordinary and Compose-dialog actions. */
@Composable
internal fun settingsButtonFeedback(
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    shape: Shape,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val feedbackColor = MaterialTheme.colorScheme.interactionFeedback
    val scale by animateFloatAsState(if (enabled && pressed) .96f else 1f,
        tween(if (pressed) 100 else 350,
            easing = if (pressed) LinearEasing else CubicBezierEasing(.22f, .25f, 0f, 1f)),
        label = "Settings button scale")
    val feedbackAlpha by animateFloatAsState(if (!enabled) 0f else when {
        pressed -> 1f
        focused -> .8f
        hovered -> .6f
        else -> 0f
    }, tween(if (pressed) 100 else 350,
        easing = if (pressed) LinearEasing else CubicBezierEasing(.17f, .17f, .67f, 1f)),
        label = "Settings button feedback")
    return Modifier.drawWithContent {
        scale(if (enabled) scale else 1f) {
            this@drawWithContent.drawContent()
            if (enabled && feedbackAlpha > 0f) drawOutline(
                shape.createOutline(size, layoutDirection, this), feedbackColor,
                alpha = feedbackAlpha)
        }
    }
}
