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
import androidx.compose.ui.graphics.Paint
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
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
    // Keep the source's float shader length (40dp is 112.5px at 450dpi). Only the
    // recording bounds are integral: its origin must not shift the content by half a pixel.
    val bottomHeight = with(density) { bottomProfile.height.toPx() }
    val bottomRecordingHeight = ceil(bottomHeight).toInt()
    val fadeBottom = bottomFade && layerHeight > 0
    val topLayer = remember(background, topHeight) {
        EdgeLayer(background, topHeight, TopFadeMinAlpha, .38f, .54f)
    }
    val bandTop = (layerHeight - bottomRecordingHeight).toFloat()
    // Both bands run the source's shader over a recording of the content they cover, so each is
    // quantised once. A RuntimeShader is evaluated in canvas coordinates, not in the coordinates of
    // the rectangle being filled, so the recorded band is told where it sits on the canvas
    // ([EdgeLayer.draw]'s `offset`, which the shader uses for the dither sample) and which edge it
    // is (`fromBottom`).
    //
    // The bottom band also needs the page painted into the recording. Its rows are mostly empty
    // page, and the page is drawn by the scaffold *behind* this content, so an un-filled recording
    // holds alpha 0 there; the effect then returns `background.rgb * cover`, which for the dark
    // theme's #010101 is 1/255 * cover and rounds to 0 or 1 in the premultiplied buffer, recovering
    // a black or double-bright pixel instead of the page. Filling the page first makes the effect
    // read the pixel the source's layer holds, so its output is the mixed colour and stays ≥ 1.
    val page = MaterialTheme.colorScheme.background
    val bottomLayer = remember(background, page, bottomProfile, bottomHeight, bandTop) {
        EdgeLayer(background, bottomHeight, bottomProfile.edgeAlpha, bottomProfile.firstX,
            bottomProfile.secondX, offset = bandTop, fromBottom = true, page = page)
    }
    return onSizeChanged { layerHeight = it.height }
        .onGloballyPositioned { layerBottom = it.boundsInWindow().bottom }
        .then(if (layerHeight == 0) Modifier else Modifier.drawWithContent {
            drawContent()
            topLayer.draw(this, size.width.toInt(), ceil(topHeight).toInt(), 0f)
            if (fadeBottom) {
                // The band runs from its whole-pixel start to the bottom of the content, and both
                // that length and the node's origin are whole pixels now, so the content replayed
                // into the node lands on the pixels it was drawn at.
                bottomLayer.draw(this, size.width.toInt(), size.height.toInt() - bandTop.toInt(),
                    bandTop)
            }
        })
}

/**
 * The top band, rendered the way the source does it: the content is recorded into a render node and
 * a runtime shader effect reads that node's pixels, so the fade is applied in one pass and the
 * result is stored once. The node is only as tall as the band, so the offscreen buffer is the
 * band's area rather than the viewport's. [offset] is where the band starts on the canvas, which
 * the dither needs because the effect samples in the node's own coordinates while the source's
 * full-size layer sampled in canvas coordinates.
 */
private class EdgeLayer(
    background: Color,
    private val height: Float,
    edgeAlpha: Float,
    firstX: Float,
    secondX: Float,
    private val offset: Float = 0f,
    private val fromBottom: Boolean = false,
    private val page: Color? = null,
) {
    // Allocated once: the band is on the scroll path, and a paint per frame would be the only
    // per-frame allocation left in this modifier.
    private val pagePaint = page?.let { Paint().apply { color = it } }
    private val shader = RuntimeShader(EdgeMath + EdgeMain).apply {
        setFloatUniform("background", background.red, background.green, background.blue, background.alpha)
        setFloatUniform("fadeHeight", height)
        setFloatUniform("recordingHeight", ceil(height))
        setFloatUniform("lowAlpha", edgeAlpha)
        // Preserve the native shader's runtime inputs and float grouping at color
        // boundaries, including its zero/one Bézier coordinates and dither strength.
        setFloatUniform("innerAlpha", 1f)
        setFloatUniform("curvePoints", firstX, 0f, secondX, 1f)
        setFloatUniform("ditherStrength", 3f)
        setFloatUniform("ditherOffset", 0f, offset)
        setIntUniform("fromBottom", if (fromBottom) 1 else 0)
        setIntUniform("customXfer", if (Build.VERSION.SDK_INT >= 36) 1 else 0)
    }
    private val node = RenderNode("fade").apply {
        setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "content"))
    }
    private var recordedWidth = 0
    private var recordedHeight = 0

    fun draw(scope: ContentDrawScope, width: Int, bandHeight: Int, bandTop: Float) {
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
            // A band whose rows are mostly empty page must see that page: it is drawn by the
            // scaffold behind this content, so the recording is transparent there and the effect's
            // premultiplied output would collapse to black. Fill it first, inside the recording
            // canvas -- drawing through the scope would use the scope's own canvas.
            pagePaint?.let { paint ->
                it.drawRect(0f, 0f, width.toFloat(), bandHeight.toFloat(), paint)
            }
            // The band shows the content rows it covers, so replay the content under its top edge.
            it.translate(0f, -bandTop)
            scope.drawContent()
            it.restore()
        }
        context.canvas = previous
        node.endRecording()
        scope.drawIntoCanvas {
            it.nativeCanvas.save()
            it.nativeCanvas.translate(0f, bandTop)
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
    uniform float recordingHeight;
    uniform float lowAlpha;
    uniform float innerAlpha;
    uniform float4 curvePoints;
    uniform float ditherStrength;
    uniform float2 ditherOffset;
    uniform int fromBottom;
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
 * The shared entry point for both edges, following the source's own: read the content, leave it
 * alone outside the band, otherwise mix it towards the background by `1 - alpha` and return that.
 * With `customXfer == 1` the output is the final unpremultiplied colour, which the compositor
 * stores once; older platforms expect the premultiplied form, as the source does.
 *
 * The dither is sampled at the node's own position plus [ditherOffset], so the pattern lands on the
 * same pixels the source's full-size layer would have used. `fromBottom` mirrors the distance, so
 * the bottom band fades towards its inner edge as the top one does.
 */
private const val EdgeMain = """
    half4 main(float2 coord) {
        half4 contentColor = content.eval(coord);
        // The node origin stays integral. Bottom distance uses its recording extent, while
        // fadeHeight retains the source's fractional curve length.
        float distance = fromBottom == 1 ? recordingHeight - coord.y : coord.y;
        if (distance >= fadeHeight) return contentColor;
        if (distance < 0.0) distance = 0.0;
        float alpha = fadeAlpha(distance / fadeHeight, lowAlpha, curvePoints);
        if (ditherStrength > 0.0) alpha += noise(coord + ditherOffset);
        alpha = clamp(alpha, 0.0, 1.0);
        half4 faded = mix(contentColor, background, 1.0 - alpha);
        if (customXfer == 0) return half4(faded.rgb * faded.a, faded.a);
        return half4(faded.rgb, faded.a);
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
