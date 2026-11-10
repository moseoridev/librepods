package me.kavishdevar.librepods.ui.components

import android.graphics.RuntimeShader
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.draw.drawWithContent
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
    // Both bands fade towards `indicator_background_color`, which the source passes as the top and
    // bottom colour of the same call (`wm.b2` -> `pw.e.d(..., t(indicator_background_color), ...)`).
    // It is one step darker than the page colour (#F1F1F3 vs #F3F3F5, #010101 vs #010102); the
    // reference Home shows that step in both bands, and only where the band has cover, which is
    // what an overlay painted in the wrong colour looks like.
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
    val fadeBottom = bottomFade && layerHeight > 0
    val bandTop = layerHeight - bottomHeight
    val topBrush = remember(background, topHeight) {
        bandBrush(background, topHeight, TopFadeMinAlpha, .38f, .54f, bottom = false, bandTop = 0f)
    }
    // A RuntimeShader is evaluated in canvas coordinates, not in the coordinates of the rectangle
    // being filled, so the bottom band has to be told where it starts. Without that, its rows
    // arrive as ~1000px past its own height, the curve reads them as past the inner edge, and the
    // whole band paints as a flat 80% cover over the content instead of a fade.
    val bottomBrush = remember(background, bottomProfile, bottomHeight, bandTop) {
        bandBrush(background, bottomHeight, bottomProfile.edgeAlpha, bottomProfile.firstX,
            bottomProfile.secondX, bottom = true, bandTop = bandTop)
    }
    // The edges are painted as overlay bands rather than as render effects over the whole viewport.
    // A render effect forces the entire layer -- and, layered twice, the entire layer again --
    // through an offscreen buffer every frame, which cost most of the GPU time of a scroll frame;
    // the bands are a couple of hundred rows of pixels instead. Both bands paint the background at
    // the shader's alpha over the content, which is the same blend the effect applied.
    return onSizeChanged { layerHeight = it.height }
        .onGloballyPositioned { layerBottom = it.boundsInWindow().bottom }
        .then(if (layerHeight == 0) Modifier else Modifier.drawWithContent {
            drawContent()
            drawRect(topBrush, size = Size(size.width, topHeight))
            if (fadeBottom) drawRect(bottomBrush,
                topLeft = Offset(0f, size.height - bottomHeight),
                size = Size(size.width, bottomHeight))
        })
}

/**
 * A shading brush for one fade band. A [ShaderBrush] is shaded in the coordinates of the rectangle
 * it fills, so [bottom] only flips which end is the edge. The shader compiles when the brush is
 * created, so hold it in `remember` rather than building one per frame.
 */
private fun bandBrush(background: Color, height: Float, edgeAlpha: Float, firstX: Float,
    secondX: Float, bottom: Boolean, bandTop: Float): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader = RuntimeShader(EdgeMath + BandMain).apply {
        setFloatUniform("background", background.red, background.green, background.blue, background.alpha)
        setFloatUniform("fadeHeight", height)
        // A RuntimeShader sees canvas coordinates, so the band's origin is an input of its own.
        setFloatUniform("bandTop", bandTop)
        setFloatUniform("lowAlpha", edgeAlpha)
        // Preserve the native shader's runtime inputs and float grouping at color
        // boundaries, including its zero/one Bézier coordinates and dither strength.
        setFloatUniform("innerAlpha", 1f)
        setFloatUniform("curvePoints", firstX, 0f, secondX, 1f)
        setFloatUniform("ditherStrength", 3f)
        setIntUniform("fromBottom", if (bottom) 1 else 0)
    }
}

private enum class BottomFadeProfile(val height: Dp, val edgeAlpha: Float, val firstX: Float, val secondX: Float) {
    NoOverlap(40.dp, .2f, .35f, .4f),
    ThreeButton(92.dp, .04f, .46f, .58f),
    Taskbar(100.dp, 0f, .8f, .35f),
}

// Locally authored shared math for both edges, as in pw.e/pw.g.
private const val EdgeMath = """
    uniform float4 background;
    uniform float fadeHeight;
    uniform float bandTop;
    uniform float lowAlpha;
    uniform float innerAlpha;
    uniform float4 curvePoints;
    uniform float ditherStrength;
    uniform int fromBottom;
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
    // The parameters must not be named `curve`: one would shadow the function of that name.
    float fadeAlpha(float distance, float height, float low, float4 curvePts) {
        return mix(low, innerAlpha, curve(distance / height,
            curvePts.x, curvePts.y, curvePts.z, curvePts.w));
    }
"""

/**
 * Paints one fade band as a translucent overlay. A [ShaderBrush] is shaded in the coordinates of
 * the rectangle it fills, so the distance from the edge follows [fromBottom], not the band's place
 * on screen. The alpha is written straight into the colour, so no offscreen layer is needed.
 *
 * The source blends the page *towards* the background by `1 - alpha`: at the edge the page is 88%
 * covered. Painting the background on top instead therefore uses `1 - alpha`, not `alpha`.
 *
 * A runtime shader's output is *premultiplied*, so the RGB must be scaled by the alpha it returns.
 * Returning the straight colour instead makes the compositor divide it back by the alpha, and a
 * near-white background then saturates to white at the edge — which dark backgrounds hide because
 * 0x01 / 0.88 still rounds to 0x01.
 */
private const val BandMain = """
    half4 main(float2 position) {
        float y = position.y - bandTop;
        float distance = fromBottom == 1 ? fadeHeight - y : y;
        float alpha = fadeAlpha(distance, fadeHeight, lowAlpha, curvePoints);
        if (ditherStrength > 0.0) alpha += noise(position);
        float cover = 1.0 - clamp(alpha, 0.0, 1.0);
        return half4(background.rgb * cover, cover);
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
