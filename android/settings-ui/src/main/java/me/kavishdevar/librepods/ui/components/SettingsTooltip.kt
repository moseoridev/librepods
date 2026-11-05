package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider
import me.kavishdevar.librepods.ui.theme.SettingsStyle

/** kw.t.J/K, kw.j2: a descriptive label; the caller owns its anchor and visibility. */
@Composable
fun SettingsTooltip(text: String, modifier: Modifier = Modifier) {
    val light = MaterialTheme.colorScheme.onSurface == Color(0xFF010102)
    val background = if (light) Color(0xFFFCFCFF) else Color(0xFF4D4D52)
    val foreground = if (light) Color(0xFF252528) else Color(0xFFFAFAFF)
    val density = LocalDensity.current
    val shape = SettingsStyle.PillShape
    Box(modifier.background(background, shape)
        .shadow(8.dp, shape, clip = true, spotColor = Color.Black.copy(alpha = .15f))
        .padding(horizontal = 18.dp, vertical = 10.dp)) {
        // kw.c → kw.d3 caps only the label, leaving the surface's dp geometry intact.
        CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f))) {
            Text(text, style = TextStyle(fontSize = 16.sp,
                fontWeight = FontWeight.Normal, color = foreground, textAlign = TextAlign.Center))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsIconTooltip(
    text: String,
    enabled: Boolean,
    focused: Boolean,
    modifier: Modifier,
) {
    val density = LocalDensity.current
    val position = remember(density) {
        with(density) {
            SettingsTooltipPosition(6.dp.roundToPx(), 10.dp.roundToPx(),
                50.dp.roundToPx(), 24.dp.roundToPx())
        }
    }
    val state = rememberTooltipState()
    // The clickable shares TooltipBox's anchor instead of being its descendant.
    // Its focus target precedes Material's own listener, so observe the same
    // interaction source and put Escape handling before that focus target.
    LaunchedEffect(enabled, focused) {
        if (enabled && focused) state.show(MutatePriority.PreventUserInput) else state.dismiss()
    }
    val anchorModifier = Modifier.onPreviewKeyEvent {
        if (enabled && state.isVisible && it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
            state.dismiss()
            true
        } else false
    }.then(modifier)
    TooltipBox(positionProvider = position, tooltip = { SettingsTooltip(text) },
        state = state, modifier = anchorModifier, focusable = false, enableUserInput = enabled) {}
}

/** kw.k2: prefer below the visual anchor, then clamp to the source window margins. */
private class SettingsTooltipPosition(
    private val gap: Int,
    private val side: Int,
    private val bottom: Int,
    private val iconSize: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        // The visual icon stays centered at 24dp, including when a caller makes
        // the button wider/taller. Position the label from that core, not its hit area.
        val left = anchorBounds.left + (anchorBounds.width - iconSize) / 2
        val top = anchorBounds.top + (anchorBounds.height - iconSize) / 2
        val anchor = IntRect(left, top, left + iconSize, top + iconSize)
        val x = anchor.left + anchor.width / 2 - popupContentSize.width / 2
        val y = if (anchor.bottom + popupContentSize.height + gap <= windowSize.height)
            anchor.bottom + gap else anchor.top - popupContentSize.height - gap
        // Oversized caller text can exceed the available window; keep clamping well-defined.
        return IntOffset(x.coerceIn(side, (windowSize.width - side - popupContentSize.width).coerceAtLeast(side)),
            y.coerceIn(0, (windowSize.height - bottom - popupContentSize.height).coerceAtLeast(0)))
    }
}
