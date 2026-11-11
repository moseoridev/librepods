package me.kavishdevar.librepods.ui.components

import android.graphics.RenderEffect
import android.graphics.RenderNode
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    // reference Home shows that step in both bands, and only where the band has a nonzero mix.
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
    val topLayer = remember(background, topHeight) {
        EdgeLayer(background, topHeight, TopFadeMinAlpha, .38f, .54f)
    }
    val bandTop = layerHeight - bottomHeight
    // A RuntimeShader is evaluated in canvas coordinates, not in the coordinates of the rectangle
    // being filled, so the bottom band has to be told where it starts. Without that, its rows
    // arrive as ~1000px past its own height, the curve reads them as past the inner edge, and the
    // whole band paints as a flat 80% cover over the content instead of a fade.
    val bottomBrush = remember(background, bottomProfile, bottomHeight, bandTop) {
        bandBrush(background, bottomHeight, bottomProfile.edgeAlpha, bottomProfile.firstX,
            bottomProfile.secondX, bandTop = bandTop)
    }
    // The two bands are rendered by different means, because their surroundings differ.
    //
    // The top band sits over page and glyphs alone, so it can be done the way the source does it:
    // record the rows it covers into a layer as tall as the band, run the source's shader over that
    // layer and draw the result once. That reproduces the source's single quantisation, and measured
    // against the Manager it takes the band from 5,614 unequal pixels to 157 and from 12 to 0 over
    // three steps.
    //
    // The bottom band cannot use that path. Its rows are not all app pixels: the last ones sit under
    // the system navigation bar, which no layer recorded here contains, so the effect sees nothing
    // to fade and those rows come out darker than the Manager's (321 pixels over three steps against
    // 29 for a plain overlay). Painting a translucent band over the composited content leaves the
    // system's pixels in place and matches there.
    return onSizeChanged { layerHeight = it.height }
        .onGloballyPositioned { layerBottom = it.boundsInWindow().bottom }
        .then(if (layerHeight == 0) Modifier else Modifier.drawWithContent {
            drawContent()
            topLayer.draw(this, size.width.toInt(), topHeight.toInt(), 0)
            if (fadeBottom) {
                drawRect(bottomBrush,
                    topLeft = Offset(0f, size.height - bottomHeight),
                    size = Size(size.width, bottomHeight))
            }
        })
}

/**
 * Paints one fade band as a translucent overlay. A [ShaderBrush] is shaded in the coordinates of
 * the rectangle it fills, so the distance from the edge comes from the band's height, not from its
 * place on screen. The source blends the page *towards* the background by `1 - alpha`: at the edge
 * the page is 88% covered. Painting the background on top therefore uses `1 - alpha`, not `alpha`.
 *
 * A runtime shader's output is *premultiplied*, so the RGB must be scaled by the cover it returns.
 * Returning the straight colour instead makes the compositor divide it back by the alpha, and a
 * near-white background then saturates to white at the edge -- which dark backgrounds hide because
 * 0x01 / 0.88 still rounds to 0x01.
 */
private fun bandBrush(background: Color, height: Float, edgeAlpha: Float, firstX: Float,
    secondX: Float, bandTop: Float): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader = RuntimeShader(EdgeMath + BandMain).apply {
        setFloatUniform("background", background.red, background.green, background.blue, background.alpha)
        setFloatUniform("fadeHeight", height)
        setFloatUniform("bandTop", bandTop)
        setFloatUniform("lowAlpha", edgeAlpha)
        // Preserve the native shader's runtime inputs and float grouping at color
        // boundaries, including its zero/one Bézier coordinates and dither strength.
        setFloatUniform("innerAlpha", 1f)
        setFloatUniform("curvePoints", firstX, 0f, secondX, 1f)
        setFloatUniform("ditherStrength", 3f)
    }
}

/**
 * The top band, rendered the way the source does it: the content is recorded into a render node and
 * a runtime shader effect reads that node's pixels, so the fade is applied in one pass and the
 * result is stored once. The node is only as tall as the band, so the offscreen buffer is the
 * band's area rather than the viewport's. The band starts at the top of the viewport, so the node's
 * own coordinates are the canvas coordinates the source's dither is sampled at.
 */
private class EdgeLayer(
    background: Color,
    private val height: Float,
    edgeAlpha: Float,
    firstX: Float,
    secondX: Float,
) {
    private val shader = RuntimeShader(EdgeMath + TopEdgeMain).apply {
        setFloatUniform("background", background.red, background.green, background.blue, background.alpha)
        setFloatUniform("fadeHeight", height)
        setFloatUniform("lowAlpha", edgeAlpha)
        // Preserve the native shader's runtime inputs and float grouping at color
        // boundaries, including its zero/one Bézier coordinates and dither strength.
        setFloatUniform("innerAlpha", 1f)
        setFloatUniform("curvePoints", firstX, 0f, secondX, 1f)
        setFloatUniform("ditherStrength", 3f)
        setIntUniform("customXfer", if (Build.VERSION.SDK_INT >= 36) 1 else 0)
    }
    private val node = RenderNode("fade").apply {
        setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "content"))
    }
    private var recordedWidth = 0
    private var recordedHeight = 0

    fun draw(scope: ContentDrawScope, width: Int, bandHeight: Int, offset: Int) {
        if (bandHeight <= 0) return
        if (width != recordedWidth || bandHeight != recordedHeight) {
            recordedWidth = width
            recordedHeight = bandHeight
            node.setPosition(0, 0, width, bandHeight)
        }
        val recording = node.beginRecording(width, bandHeight)
        val composeCanvas = androidx.compose.ui.graphics.Canvas(recording)
        val context = scope.drawContext
        val previous = context.canvas
        context.canvas = composeCanvas
        scope.drawIntoCanvas {
            it.save()
            it.clipRect(0f, 0f, width.toFloat(), bandHeight.toFloat())
            // The band shows the content rows it covers, so replay the content under its top edge.
            it.translate(0f, -offset.toFloat())
            scope.drawContent()
            it.restore()
        }
        context.canvas = previous
        node.endRecording()
        scope.drawIntoCanvas {
            it.nativeCanvas.save()
            it.nativeCanvas.translate(0f, offset.toFloat())
            it.nativeCanvas.drawRenderNode(node)
            it.nativeCanvas.restore()
        }
    }
}

private enum class BottomFadeProfile(val height: Dp, val edgeAlpha: Float, val firstX: Float, val secondX: Float) {
    NoOverlap(40.dp, .2f, .35f, .4f),
    ThreeButton(92.dp, .04f, .46f, .58f),
    Taskbar(100.dp, 0f, .8f, .35f),
}

// Locally authored shared math for both edges, as in pw.e/pw.g.
private const val EdgeMath = """
    uniform shader content;
    uniform float4 background;
    uniform float fadeHeight;
    uniform float bandTop;
    uniform float lowAlpha;
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
    // The parameters must not be named `curve`: one would shadow the function of that name.
    float fadeAlpha(float t, float low, float4 curvePts) {
        return mix(low, innerAlpha, curve(t, curvePts.x, curvePts.y, curvePts.z, curvePts.w));
    }
"""

/**
 * The two edge entry points, following the source's own: read the content, leave it alone outside
 * the band, otherwise mix it towards the background by `1 - alpha` and return that. With
 * `customXfer == 1` the output is the final unpremultiplied colour, which the compositor stores
 * once; older platforms expect the premultiplied form, as the source does. The dither is sampled at
 * the node's own position plus [originY], so the pattern lands on the same pixels the source's
 * full-size layer would have used.
 */
private const val TopEdgeMain = """
    half4 main(float2 coord) {
        half4 contentColor = content.eval(coord);
        if (coord.y >= fadeHeight) return contentColor;
        float alpha = fadeAlpha(coord.y / fadeHeight, lowAlpha, curvePoints);
        if (ditherStrength > 0.0) alpha += noise(coord);
        alpha = clamp(alpha, 0.0, 1.0);
        half4 faded = mix(contentColor, background, 1.0 - alpha);
        if (customXfer == 0) return half4(faded.rgb * faded.a, faded.a);
        return half4(faded.rgb, faded.a);
    }
"""
private const val BandMain = """
    half4 main(float2 position) {
        float y = position.y - bandTop;
        float distance = fadeHeight - y;
        float alpha = fadeAlpha(distance / fadeHeight, lowAlpha, curvePoints);
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
 * own `canScrollForward`, which both [ScrollState] and [LazyListState] expose:
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
