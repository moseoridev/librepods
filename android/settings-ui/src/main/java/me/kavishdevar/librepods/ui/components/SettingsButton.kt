package me.kavishdevar.librepods.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import me.kavishdevar.librepods.ui.theme.SettingsStyle
import me.kavishdevar.librepods.ui.theme.interactionFeedback

enum class SettingsButtonStyle { Text, Tonal, Filled }

/** wm.k1.g / kw.t.g: pill geometry, three color styles and capped label scaling. */
@Composable
fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: SettingsButtonStyle = SettingsButtonStyle.Text,
    tint: Color = Color.Unspecified,
    containerColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (containerColor != Color.Unspecified) containerColor else when (style) {
        SettingsButtonStyle.Filled -> colors.primary
        SettingsButtonStyle.Tonal -> colors.secondaryContainer
        else -> Color.Transparent
    }
    val foreground = if (tint != Color.Unspecified) tint else when (style) {
        // wm.a0 explicitly supplies the current filled label token, overriding
        // kw.w0's generic content color.
        SettingsButtonStyle.Filled -> Color(0xFFFCFCFF)
        SettingsButtonStyle.Tonal -> colors.onSurface
        else -> LocalContentColor.current
    }
    val alpha = if (enabled) 1f else .4f
    // n1.l.d → rw.a: the shared smooth corner at 50%, rather than a circular arc.
    val shape = SettingsStyle.PillShape
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val feedback = settingsButtonFeedback(enabled, interactionSource, shape)
    // ku.i0.i caps button text scaling at 1.3. Do not alter the Activity configuration.
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f)),
        // Let text and separately rounded padding determine the visual height.
        // Compose's clickable expands the hit region without enlarging the pill.
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
        LocalContentColor provides foreground.copy(alpha = foreground.alpha * alpha),
    ) {
        Box(modifier.clickable(interactionSource = interactionSource, indication = null,
            enabled = enabled, role = Role.Button, onClick = onClick)
            .then(feedback).background(container.copy(alpha = container.alpha * alpha), shape),
            propagateMinConstraints = true) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                // The current manager's common button defaults are 15sp and 16/10dp
                // content padding (wm.k1.g → kw.w0). Screen-specific 52dp heights
                // are supplied by the caller (nm.d), through modifier.
                ProvideTextStyle(SettingsStyle.RowTitle.copy(fontSize = 15.sp,
                    // wm.a0 keeps the filled label's explicit color even when
                    // kw.t.g dims its background and generic content color.
                    color = if (style == SettingsButtonStyle.Filled) foreground else Color.Unspecified,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    textMotion = TextMotion.Animated)) { content() }
            }
        }
    }
}

/** The app supplies its own artwork and a localized action label. */
@Composable
fun SettingsIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    containerColor: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    // Keep the independently rounded padding for the natural action layout.
    SettingsIconButtonContent(onClick, contentDescription,
        modifier.background(containerColor, CircleShape)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        enabled, tint, contentPadding = 12.dp, content = content)
}

/** The same 24dp control also fits the compact header's separately measured 48dp slot. */
@Composable
internal fun SettingsIconButtonContent(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    contentPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val ink = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint
    val feedbackColor = MaterialTheme.colorScheme.interactionFeedback
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val contentScale by animateFloatAsState(if (enabled && pressed) .96f else 1f,
        tween(if (pressed) 100 else 350,
            easing = if (pressed) LinearEasing else CubicBezierEasing(.22f, .25f, 0f, 1f)),
        label = "Settings icon scale")
    val feedbackAlpha by animateFloatAsState(if (!enabled) 0f else when {
        pressed -> 1f
        focused -> .6f
        hovered -> .8f
        else -> 0f
    }, tween(if (pressed) 100 else 350,
        easing = if (pressed) LinearEasing else CubicBezierEasing(.17f, .17f, .67f, 1f)),
        label = "Settings icon feedback")
    // kw.t.u → vw.a.d / nw.s0: only the icon content scales. Feedback is
    // drawn afterwards with a rounded sum of margins and unrounded translation.
    // Keep caller parent data (align/weight) on the outer layout. Measure the
    // artwork first; the tooltip's interaction anchor then covers that layout.
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.padding(contentPadding).size(24.dp)
            .drawWithContent {
                scale(if (enabled) contentScale else 1f) { this@drawWithContent.drawContent() }
                if (enabled && feedbackAlpha > 0f) {
                    val extra = 24.dp.roundToPx().toFloat()
                    val feedbackSize = Size(size.width + extra, size.height + extra)
                    drawRoundRect(feedbackColor, topLeft = Offset(-12.dp.toPx(), -12.dp.toPx()),
                        size = feedbackSize, cornerRadius = CornerRadius(feedbackSize.minDimension / 2f),
                        alpha = feedbackAlpha)
                }
            },
            contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides ink.copy(alpha = ink.alpha * if (enabled) 1f else .4f)) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { content() }
            }
        }
        Box(Modifier.matchParentSize()) {
            // TooltipBox adds its long-click semantics and input handlers to this
            // same anchor, so accessibility exposes one named button with both actions.
            SettingsIconTooltip(contentDescription, enabled, focused,
                Modifier.fillMaxSize().clickable(interactionSource = interactionSource, indication = null,
                    enabled = enabled, role = Role.Button, onClick = onClick)
                    .semantics { this.contentDescription = contentDescription })
        }
    }
}
