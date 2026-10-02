package me.kavishdevar.librepods.ui.components

import android.graphics.Canvas
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.FrameLayout
import android.view.ContextThemeWrapper
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import me.kavishdevar.librepods.ui.theme.interactionFeedback

/** A public platform TextView retains the original scrolling-text draw coordinates. */
@Composable
internal fun SettingsTextEntryAction(label: String, enabled: Boolean, onClick: () -> Unit,
    contentColor: Color, measurement: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val color = MaterialTheme.colorScheme.interactionFeedback
    val density = LocalDensity.current
    val padding = with(density) { 4.dp.roundToPx() }
    val inset = with(density) { 4.dp.toPx().toInt() }
    val radius = with(density) { 26.dp.toPx() }
    val scale by animateFloatAsState(if (enabled && pressed) .96f else 1f,
        tween(if (pressed) 100 else 350,
            easing = if (pressed) LinearEasing else CubicBezierEasing(.22f, .25f, 0f, 1f)),
        label = "Settings text entry action scale")
    val active = enabled && (pressed || focused || hovered)
    val alpha by animateFloatAsState(if (!enabled) 0f else when {
        pressed -> 1f
        hovered -> .6f
        focused -> .8f
        else -> 0f
    }, tween(if (active) 100 else 350,
        easing = if (active) LinearEasing else CubicBezierEasing(.17f, .17f, .67f, 1f)),
        label = "Settings text entry action background")
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        val textPaint = rememberDialogTextPaint()
        Box(Modifier.clickable(interactionSource = interactionSource, indication = null,
            enabled = enabled, role = Role.Button, onClick = onClick)
            // The overlaid AndroidView occludes measurement descendants in Android's
            // accessibility tree. Keep the name on the node that owns the action.
            .semantics { text = AnnotatedString(label) }, propagateMinConstraints = true) {
            measurement()
            AndroidView(modifier = Modifier.matchParentSize(), factory = { context ->
                ActionLabelFrame(context)
            }, update = { frame ->
                val view = frame.label
                view.applyTextPaint(textPaint)
                view.setTextColor(contentColor.toArgb())
                if (view.paddingLeft != padding || view.paddingRight != padding)
                    view.setPadding(padding, 0, padding, 0)
                if (view.text.toString() != label) view.text = label
                view.alpha = if (enabled) 1f else 1f - .6f
                view.scaleX = if (enabled) scale else 1f
                view.scaleY = if (enabled) scale else 1f
                (view.background as TextEntryActionBackground).update(inset, radius,
                    if (enabled) color.copy(alpha = color.alpha * alpha).toArgb() else 0)
            })
        }
    }
}

/** Retain ViewGroup's child RenderNode clipping at the transformed button bounds. */
private class ActionLabelFrame(context: android.content.Context) : FrameLayout(context) {
    val label = ActionLabelView(context)
    init {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        clipChildren = true
        clipToPadding = true
        addView(label, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
}

private class ActionLabelView(context: android.content.Context) : TextView(
    ContextThemeWrapper(context, android.R.style.Theme_Material_Light_NoActionBar),
) {
    init {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        isFocusable = false
        isClickable = false
        setSingleLine(true)
        ellipsize = TextUtils.TruncateAt.END
        gravity = Gravity.CENTER
        includeFontPadding = true
        setFallbackLineSpacing(true)
        if (Build.VERSION.SDK_INT >= 35) {
            setUseBoundsForWidth(true)
            setLocalePreferredLineHeightForMinimumUsed(false)
        }
        background = TextEntryActionBackground()
        setLineSpacing(0f, 1f)
    }

    private var appliedPaint: TextPaint? = null
    fun applyTextPaint(textPaint: TextPaint) {
        if (appliedPaint === textPaint) return
        // Use the setters to invalidate TextView's cached layout when caller fonts change.
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textPaint.textSize)
        typeface = textPaint.typeface
        textLocales = textPaint.textLocales
        paint.set(textPaint)
        appliedPaint = textPaint
        requestLayout()
        invalidate()
    }
}

private class TextEntryActionBackground : Drawable() {
    // Retain the original opaque mask and SRC_IN tint compositing.
    // FrameLayout separately supplies the source View bounds clipping.
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    private var tint: Int? = null
    private var inset = 0
    private var radius = 0f
    private var path = android.graphics.Path()
    fun update(inset: Int, radius: Float, color: Int) {
        if (this.inset != inset || this.radius != radius) {
            this.inset = inset
            this.radius = radius
            rebuild()
        }
        if (tint != color) {
            tint = color
            paint.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_IN)
            invalidateSelf()
        }
    }
    override fun onBoundsChange(bounds: android.graphics.Rect) { rebuild() }
    private fun rebuild() {
        path = textEntryActionPath(inset.toFloat(), (bounds.width() - inset).toFloat(),
            bounds.height().toFloat(), radius).asAndroidPath()
    }
    override fun draw(canvas: Canvas) { canvas.drawPath(path, paint) }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** Framework smooth GradientDrawable coordinates, including its integer InsetDrawable bounds. */
private fun textEntryActionPath(left: Float, right: Float, height: Float, radius: Float): Path {
    val path = Path()
    val width = right - left
    if (width <= 0f || height <= 0f) return path
    val halfWidth = width / 2f
    val halfHeight = height / 2f
    val half = minOf(halfWidth, halfHeight)
    val r = radius.coerceIn(0f, half)
    val ratio = r / half
    val vertex = 1f - ((ratio - .5f) / .4f).coerceIn(0f, 1f) * .13877845f
    val control = if (ratio.toDouble() > .6)
        1f + ((ratio - .6f) / .3f).coerceAtMost(1f) * .042454004f else 1f
    val unit = r / 100f
    val reach = (128.19f * unit) * vertex
    val first = (83.62f * unit) * control
    val a = unit * 67.45f
    val b = unit * 4.64f
    val c = unit * 51.16f
    val d = unit * 13.36f
    val e = unit * 34.86f
    val f = unit * 22.07f
    // Keep absolute-coordinate arithmetic in framework order rather than
    // rotating a shared corner template.
    return path.apply {
        moveTo(left + halfWidth, 0f)
        lineTo(left + maxOf(halfWidth, width - reach), 0f)
        cubicTo(right - first, 0f, right - a, b, right - c, d)
        cubicTo(right - e, f, right - f, e, right - d, c)
        cubicTo(right - b, a, right, first, right, minOf(halfHeight, reach))
        lineTo(right, maxOf(halfHeight, height - reach))
        cubicTo(right, height - first, right - b, height - a, right - d, height - c)
        cubicTo(right - f, height - e, right - e, height - f, right - c, height - d)
        cubicTo(right - a, height - b, right - first, height,
            left + maxOf(halfWidth, width - reach), height)
        lineTo(left + minOf(halfWidth, reach), height)
        cubicTo(left + first, height, left + a, height - b, left + c, height - d)
        cubicTo(left + e, height - f, left + f, height - e, left + d, height - c)
        cubicTo(left + b, height - a, left, height - first, left, maxOf(halfHeight, height - reach))
        lineTo(left, minOf(halfHeight, reach))
        cubicTo(left, first, left + b, a, left + d, c)
        cubicTo(left + f, e, left + e, f, left + c, d)
        cubicTo(left + a, b, left + first, 0f, left + minOf(halfWidth, reach), 0f)
        close()
    }
}
