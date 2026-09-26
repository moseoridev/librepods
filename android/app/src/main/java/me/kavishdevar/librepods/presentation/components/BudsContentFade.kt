package me.kavishdevar.librepods.presentation.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Foreground content effect; its layer spans the viewport, before scaffold padding.
 *
 * The two edges have separate sources. The top is a linear ramp over [TopFadeHeight] starting at
 * [TopFadeMinAlpha]. The bottom reproduces the One UI fading edge, whose stops (`ou.g` via
 * `m3.i.Q`) are total height 70dp, a lower zone 18dp and the alpha 0.24 that the profile reaches
 * where that zone ends: alpha falls linearly from 1 to 0.24 across the 52dp above the zone, then
 * keeps falling through the zone to 0 at the very edge. The floor is a waypoint on the way down,
 * not a plateau — the original does fade the last stretch to nothing, and the measured profile
 * agrees (the vendor's own last text row is drawn at 0.12 alpha, half of 0.24). Pass [bottomFade]
 * only where the content actually scrolls; the original enables the bottom stops from the scroll
 * state and disables them otherwise.
 */
@Composable
internal fun Modifier.budsContentFade(bottomFade: Boolean = false): Modifier {
    val background = MaterialTheme.colorScheme.background
    val density = LocalDensity.current
    val topHeight = with(density) { TopFadeHeight.toPx() }
    val bottomHeight = with(density) { BottomFadeHeight.toPx() }
    val bottomRamp = with(density) { BottomFadeRamp.toPx() }
    // The shader needs the layer height to measure distance from the bottom edge.
    var layerHeight by remember { mutableIntStateOf(0) }
    val fadeBottom = bottomFade && layerHeight > 0
    val effect = remember(background, topHeight, bottomHeight, bottomRamp, layerHeight, fadeBottom) {
        // Locally authored fade. Keep the shader/effect across redraws; no frame clock.
        val shader = RuntimeShader("""
            uniform shader page;
            uniform float4 background;
            uniform float topHeight;
            uniform float2 topOpacity;
            uniform float3 bottom;
            uniform float bottomMinAlpha;
            uniform int bottomOn;
            half4 main(float2 position) {
                half4 pixel = page.eval(position);
                float alpha = 1.0;
                if (position.y < topHeight) {
                    alpha = mix(topOpacity.x, topOpacity.y, clamp(position.y / topHeight, 0.0, 1.0));
                }
                if (bottomOn == 1) {
                    // bottom = (layer height, edge height, ramp height); measure up from the edge.
                    float d = bottom.x - position.y;
                    float edge;
                    if (d <= 0.0) edge = 0.0;
                    else if (d >= bottom.y) edge = 1.0;
                    else if (d <= bottom.z) edge = bottomMinAlpha * (d / bottom.z);
                    else edge = mix(bottomMinAlpha, 1.0, (d - bottom.z) / (bottom.y - bottom.z));
                    alpha = min(alpha, edge);
                }
                return mix(pixel, background, 1.0 - alpha);
            }
        """.trimIndent()).apply {
            setFloatUniform("background", background.red, background.green, background.blue, background.alpha)
            setFloatUniform("topHeight", topHeight)
            setFloatUniform("topOpacity", TopFadeMinAlpha, 1f)
            setFloatUniform("bottom", layerHeight.toFloat(), bottomHeight, bottomRamp)
            setFloatUniform("bottomMinAlpha", BottomFadeMinAlpha)
            setIntUniform("bottomOn", if (fadeBottom) 1 else 0)
        }
        RenderEffect.createRuntimeShaderEffect(shader, "page").asComposeRenderEffect()
    }
    return onSizeChanged { layerHeight = it.height }
        .graphicsLayer {
            renderEffect = effect
            compositingStrategy = CompositingStrategy.Offscreen
        }
}

/**
 * Whether the current screen's content has more below the fold, published by the scaffold.
 *
 * The original keeps the fading edge in the scaffold and the scroll state in the screen, wiring the
 * two together at the call site (`gm.w2` reads `ScrollState.d()`). This is that same wire: the
 * scaffold owns it, a scrolling screen reports into it, and any other screen leaves it false — and
 * so gets no bottom edge, which is exactly the gating the original applies.
 */
class BudsScrollState {
    internal var canScrollForward by mutableStateOf(false)
}

/** The enclosing [StyledScaffold]'s scroll holder; null outside one. */
val LocalBudsScrollState = staticCompositionLocalOf<BudsScrollState?> { null }

/**
 * Reports a screen's scroll position to the enclosing scaffold, so the bottom fading edge turns on
 * exactly while there is content below. Call from a scrolling screen's composition with the state's
 * own `canScrollForward`, which both [ScrollState] and `LazyListState` expose:
 * `BudsScrollReporter(listState.canScrollForward)`. A screen that does not scroll does not call it.
 *
 * The holder outlives the screen, so the report is cleared when the screen leaves: without that,
 * navigating from a scrolled list to a fixed-height screen would leave the edge on over content
 * that has nothing below it. Screens that never report are unaffected — the value starts false.
 */
@Composable
fun BudsScrollReporter(canScrollForward: Boolean) {
    val holder = LocalBudsScrollState.current ?: return
    holder.canScrollForward = canScrollForward
    DisposableEffect(holder) { onDispose { holder.canScrollForward = false } }
}

/** Top edge: alpha rises from [TopFadeMinAlpha] to 1 across this height. */
private val TopFadeHeight = 56.dp
private const val TopFadeMinAlpha = .12f

/** Bottom edge: total height of the One UI fading edge (`ou.g.b`). */
private val BottomFadeHeight = 70.dp

/** Bottom edge: the zone below the 0.24 waypoint, running from there down to nothing. */
private val BottomFadeRamp = 18.dp

/** Bottom edge: the alpha at [BottomFadeRamp] from the edge, halfway down the final fall. */
private const val BottomFadeMinAlpha = .24f
