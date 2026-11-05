package me.kavishdevar.librepods.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.contentFade

/** Foreground effects across the viewport before the scaffold applies its content padding.
 *
 * The current source draws top and bottom in separate layers (`pw.g`). Top is 56dp with a
 * .12 edge and cubic (.38, 0, .54, 1). Bottom selects its profile from navigation-bar overlap
 * (`pw.a/f/p`): 40dp/.2 without overlap, 92dp/.04 for three-button overlap, or 100dp/0 with
 * a taskbar. Both use 3/255 spatial dithering. The screen reports whether more content remains
 * below; fitting pages and the end of a list have no bottom layer.
 */
@Composable
internal fun Modifier.settingsContentFade(bottomFade: Boolean = false): Modifier {
    val background = MaterialTheme.colorScheme.contentFade
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val topHeight = with(density) { TopFadeHeight.toPx() }
    val navigationInset = WindowInsets.navigationBars.getBottom(density)
    var layerHeight by remember { mutableIntStateOf(0) }
    var layerBottom by remember { mutableStateOf(0f) }
    val bottomProfile = remember(context, configuration, navigationInset, layerBottom) {
        val resolver = context.contentResolver
        val threeButton = try {
            Settings.Secure.getInt(resolver, "navigation_mode", 0) != 2
        } catch (_: SecurityException) { true }
        val taskbar = try {
            Settings.Global.getInt(resolver, "sem_task_bar_available", 0) == 1
        } catch (_: SecurityException) { false }
        val navigationTop = context.resources.displayMetrics.heightPixels - navigationInset
        if (navigationInset > 0 && threeButton && layerBottom > navigationTop) {
            if (taskbar) BottomFadeProfile.Taskbar else BottomFadeProfile.ThreeButton
        } else BottomFadeProfile.NoOverlap
    }
    val bottomHeight = with(density) { bottomProfile.height.toPx() }
    val topEffect = remember(background, topHeight, layerHeight) {
        edgeEffect(background, layerHeight, topHeight, TopFadeMinAlpha, .38f, .54f, top = true)
    }
    val fadeBottom = bottomFade && layerHeight > 0
    val bottomEffect = remember(background, bottomProfile, bottomHeight, layerHeight, fadeBottom) {
        if (fadeBottom) edgeEffect(background, layerHeight, bottomHeight, bottomProfile.edgeAlpha,
            bottomProfile.firstX, bottomProfile.secondX, top = false) else null
    }
    return onSizeChanged { layerHeight = it.height }
        .onGloballyPositioned { layerBottom = it.boundsInWindow().bottom }
        .graphicsLayer {
            renderEffect = topEffect
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .then(if (bottomEffect != null) Modifier.graphicsLayer {
            renderEffect = bottomEffect
            compositingStrategy = CompositingStrategy.Offscreen
        } else Modifier)
}

/** Cache effects between measurement/configuration changes, with no animation clock or polling. */
private fun edgeEffect(background: Color, layerHeight: Int, height: Float, edgeAlpha: Float,
    firstX: Float, secondX: Float, top: Boolean): androidx.compose.ui.graphics.RenderEffect {
    val shader = RuntimeShader(EdgeMath + if (top) TopEdgeMain else BottomEdgeMain).apply {
        setFloatUniform("background", background.red, background.green, background.blue, background.alpha)
        setFloatUniform("layerHeight", layerHeight.toFloat())
        setFloatUniform("fadeHeight", height)
        setFloatUniform("edgeAlpha", edgeAlpha)
        // Preserve the native shader's runtime inputs and float grouping at color
        // boundaries, including its zero/one Bézier coordinates and dither strength.
        setFloatUniform("innerAlpha", 1f)
        setFloatUniform("curvePoints", firstX, 0f, secondX, 1f)
        setFloatUniform("ditherStrength", 3f)
        if (!top) setFloatUniform("bottomOffset", 0f)
        setIntUniform("customXfer", if (Build.VERSION.SDK_INT >= 36) 1 else 0)
    }
    return RenderEffect.createRuntimeShaderEffect(shader, "page").asComposeRenderEffect()
}

private enum class BottomFadeProfile(val height: Dp, val edgeAlpha: Float, val firstX: Float, val secondX: Float) {
    NoOverlap(40.dp, .2f, .35f, .4f),
    ThreeButton(92.dp, .04f, .46f, .58f),
    Taskbar(100.dp, 0f, .8f, .35f),
}

// Locally authored shared math with separate edge entry points, as in pw.e/pw.g.
private const val EdgeMath = """
    uniform shader page;
    uniform float4 background;
    uniform float layerHeight;
    uniform float fadeHeight;
    uniform float edgeAlpha;
    uniform float innerAlpha;
    uniform float4 curvePoints;
    uniform float ditherStrength;
    uniform int customXfer;
    float curve(float t, float x1, float y1, float x2, float y2) {
        if (t <= 0.0) return 0.0;
        if (t >= 1.0) return 1.0;
        float s = t;
        for (int i = 0; i < 6; i++) {
            float u = 1.0 - s;
            float u2 = u * u;
            float s2 = s * s;
            float x = 3.0 * u2 * s * x1 + 3.0 * u * s2 * x2 + s2 * s;
            float derivative = 3.0 * u2 * x1 + 6.0 * u * s * (x2 - x1) + 3.0 * s2 * (1.0 - x2);
            if (abs(derivative) < 0.0001) break;
            s = clamp(s - (x - t) / derivative, 0.0, 1.0);
            if (abs(x - t) < 0.0001) break;
        }
        float u = 1.0 - s;
        float u2 = u * u;
        float s2 = s * s;
        float s3 = s2 * s;
        return 3.0 * u2 * s * y1 + 3.0 * u * s2 * y2 + s3;
    }
    float noise(float2 p) {
        float seed = fract(sin(dot(abs(p), float2(12.9898, 78.233))) * 43758.5453123);
        float signedSeed = seed * 2.0 - 1.0;
        float shaped = max(-1.0, signedSeed * inversesqrt(abs(signedSeed)));
        return (shaped - sign(signedSeed)) * (ditherStrength / 255.0);
    }
    half4 fade(half4 pixel, float2 position, float distance) {
        float alpha = mix(edgeAlpha, innerAlpha, curve(distance / fadeHeight,
            curvePoints.x, curvePoints.y, curvePoints.z, curvePoints.w));
        if (ditherStrength > 0.0) alpha += noise(position);
        alpha = clamp(alpha, 0.0, 1.0);
        half4 faded = mix(pixel, background, 1.0 - alpha);
        if (customXfer == 0) return half4(faded.rgb * faded.a, faded.a);
        return half4(faded.rgb, faded.a);
    }
"""

private const val TopEdgeMain = """
    half4 main(float2 position) {
        half4 pixel = page.eval(position);
        if (position.y >= fadeHeight) return pixel;
        return fade(pixel, position, position.y);
    }
"""

private const val BottomEdgeMain = """
    uniform float bottomOffset;
    half4 main(float2 position) {
        half4 pixel = page.eval(position);
        float bottom = layerHeight - bottomOffset;
        float top = bottom - fadeHeight;
        if (position.y < top || position.y > bottom) return pixel;
        return fade(pixel, position, bottom - position.y);
    }
"""

/**
 * Whether the current screen's content has more below the fold, published by the scaffold.
 *
 * The original keeps the fading edge in the scaffold and the scroll state in the screen, wiring the
 * two together at the current call site (`wm.b2` reads `ScrollState.c()`). This is that same wire: the
 * scaffold owns it, a scrolling screen reports into it, and any other screen leaves it false — and
 * so gets no bottom edge, which is exactly the gating the original applies.
 *
 * The holder outlives the screen, so a report is tied to the identity of the reporter that made it
 * and cleared when that reporter goes away; without that, navigating from a scrolled list to a
 * fixed-height screen would leave the edge on over content that has nothing below it. The identity
 * is what makes the clear safe: navigation can compose the incoming screen before the outgoing one
 * is disposed, and an unguarded clear would then wipe the value the new screen had just set.
 */
internal class SettingsScrollState {
    private var owner: Any? = null
    internal var canScrollForward by mutableStateOf(false)
        private set

    internal fun report(owner: Any, value: Boolean) {
        this.owner = owner
        canScrollForward = value
    }

    /** Clears only if [owner] is still the reporter whose value is showing. */
    internal fun clear(owner: Any) {
        if (this.owner !== owner) return
        this.owner = null
        canScrollForward = false
    }
}

/** The enclosing [SettingsScaffold]'s scroll holder; null outside one. */
internal val LocalSettingsScrollState = staticCompositionLocalOf<SettingsScrollState?> { null }

/**
 * Reports a screen's scroll position to the enclosing scaffold, so the bottom fading edge turns on
 * exactly while there is content below. Call from a scrolling screen's composition with the state's
 * own `canScrollForward`, which both [ScrollState] and `LazyListState` expose:
 * `SettingsScrollReporter(listState.canScrollForward)`. A screen that does not scroll does not call it.
 */
@Composable
fun SettingsScrollReporter(canScrollForward: Boolean) {
    val holder = LocalSettingsScrollState.current ?: return
    // One reporter instance per call site within one screen; keying the effect on it, rather than on
    // the boolean, keeps the owner stable as the value changes and distinct across screens.
    val reporter = remember(holder) { Any() }
    holder.report(reporter, canScrollForward)
    DisposableEffect(holder, reporter) { onDispose { holder.clear(reporter) } }
}

/** Top edge: alpha rises from [TopFadeMinAlpha] to 1 across this height. */
private val TopFadeHeight = 56.dp
private const val TopFadeMinAlpha = .12f
