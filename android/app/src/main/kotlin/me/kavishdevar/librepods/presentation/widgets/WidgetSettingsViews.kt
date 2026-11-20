package me.kavishdevar.librepods.presentation.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.ui.components.settingsBottomFadeOverlay

/**
 * The reference window shows the system wallpaper (`windowShowWallpaper`) and paints its page colour
 * around a transparent preview. This root paints everywhere except the hole view's rectangle; the
 * hole's own [CornerOverlay] rounds it, as `semSetRoundedCorners` does on the reference.
 */
internal class WidgetSettingsPage(context: Context, private val color: Int) : FrameLayout(context) {
    var hole: View? = null
    private val rect = Rect()

    init { setWillNotDraw(false) }

    override fun onDraw(canvas: Canvas) {
        val view = hole
        if (view != null && view.isLaidOut) {
            rect.set(0, 0, view.width, view.height)
            offsetDescendantRectToMyCoords(view, rect)
            canvas.save()
            canvas.clipOutRect(rect)
            canvas.drawColor(color)
            canvas.restore()
        } else canvas.drawColor(color)
    }
}

/** `semSetRoundedCorners(15)`: paints [color] outside the smooth outline, over the view's content. */
internal class CornerOverlay(private val color: Int, private val radius: Float) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = this@CornerOverlay.color }
    private var path = Path()

    override fun onBoundsChange(bounds: Rect) {
        path = WidgetSurface.smoothPath(bounds.width().toFloat(), bounds.height().toFloat(), List(4) { radius }).apply {
            fillType = Path.FillType.INVERSE_WINDING
            offset(bounds.left.toFloat(), bounds.top.toFloat())
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.clipRect(bounds)
        canvas.drawPath(path, paint)
        canvas.restore()
    }
    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

/** The vendor check stroke: three points in a [viewport] box, 1.5 units wide with round ends. */
internal class CheckDrawable(color: Int, private val viewportWidth: Float, private val viewportHeight: Float) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()

    override fun draw(canvas: Canvas) {
        val sx = bounds.width() / viewportWidth
        val sy = bounds.height() / viewportHeight
        paint.strokeWidth = 1.5f * sx
        path.reset()
        path.moveTo(bounds.left + 20.75f * sx, bounds.top + 6.791f * sy)
        path.lineTo(bounds.left + 10.282f * sx, bounds.top + 19.099f * sy)
        path.lineTo(bounds.left + 3.25f * sx, bounds.top + 10.832f * sy)
        canvas.drawPath(path, paint)
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

/** Locally drawn opacity glyph: a checkerboard disc with an outline, at the source icon's proportions. */
internal class OpacityIconDrawable(context: Context) : Drawable() {
    private val outline = context.getColor(R.color.widget_settings_opacity_outline)
    private val light = context.getColor(R.color.widget_settings_opacity_light)
    private val dark = context.getColor(R.color.widget_settings_opacity_dark)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()

    override fun draw(canvas: Canvas) {
        // In a 32-unit icon: an 11-unit disc, a 1-unit outline and 5.5-unit cells centred on the disc.
        val unit = bounds.width() / 32f
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        val radius = 10.5f * unit
        val cell = 5.5f * unit
        clip.reset()
        clip.addCircle(cx, cy, radius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clip)
        paint.style = Paint.Style.FILL
        for (i in -2 until 2) for (j in -2 until 2) {
            paint.color = if ((i + j) % 2 == 0) dark else light
            canvas.drawRect(cx + i * cell, cy + j * cell, cx + (i + 1) * cell, cy + (j + 1) * cell, paint)
        }
        canvas.restore()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = unit
        paint.color = outline
        canvas.drawCircle(cx, cy, radius, paint)
    }
    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

/** One configurable corner shape, drawn from its thumbnail geometry rather than vendor artwork. */
internal data class WidgetShapeThumbnail(val shape: Int, val corners: List<Float>, val smooth: Boolean)

internal class WidgetShapeView(context: Context, private val thumbnail: WidgetShapeThumbnail) : View(context) {
    private val fill = context.getColor(R.color.widget_settings_shape)
    private val selectedTint = context.getColor(R.color.widget_settings_shape_selected)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val check = CheckDrawable(context.getColor(R.color.widget_settings_shape_check), 25f, 26f)
    private var path = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Thumbnail corners are in the 58 × 68 dp viewport units of the source vectors.
        val unit = w / 58f
        val radii = thumbnail.corners.map { it * unit }
        path = if (thumbnail.smooth) WidgetSurface.smoothPath(w.toFloat(), h.toFloat(), radii)
        else Path().apply {
            addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()),
                floatArrayOf(radii[0], radii[0], radii[1], radii[1], radii[2], radii[2], radii[3], radii[3]),
                Path.Direction.CW)
        }
        // The 25 × 26 dp check is centred within the thumbnail.
        val cw = (25 * unit).toInt()
        val ch = (26 * unit).toInt()
        check.setBounds((w - cw) / 2, (h - ch) / 2, (w - cw) / 2 + cw, (h - ch) / 2 + ch)
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = fill
        canvas.drawPath(path, paint)
        if (isSelected) {
            // setColorFilter(selected): SRC_ATOP over the opaque thumbnail.
            paint.color = selectedTint
            canvas.drawPath(path, paint)
            check.draw(canvas)
        }
    }
}

/** NestedScrollView's SESL extra bottom fading edge, faded in or out over 300ms. */
internal class WidgetSettingsFadeFrame(context: Context, private val scroll: ScrollView, color: Int) : FrameLayout(context) {
    private val height = resources.getDimension(R.dimen.widget_settings_fade_height)
    // SeslFadingEdgeHelperImpl: the extra bottom band keeps the default 0.04 edge alpha and
    // the no-navigation-overlap curve.
    private val shader: RuntimeShader = settingsBottomFadeOverlay(color, height, .04f, floatArrayOf(.35f, 0f, .6f, 1f), 0f)
    private val paint = Paint().apply { shader = this@WidgetSettingsFadeFrame.shader }
    private var strength = 0f
    private var target = 0f
    private var animator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        scroll.setOnScrollChangeListener { _, _, _, _, _ -> update(true) }
        scroll.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> update(isLaidOut) }
    }

    private fun update(animate: Boolean) {
        val next = if (scroll.canScrollVertically(1)) 1f else 0f
        if (next == target) return
        target = next
        animator?.cancel()
        if (!animate) { setStrength(next); return }
        animator = ValueAnimator.ofFloat(strength, next).setDuration(300).apply {
            addUpdateListener { setStrength(it.animatedValue as Float) }
            start()
        }
    }

    private fun setStrength(value: Float) {
        strength = value
        shader.setFloatUniform("strength", value)
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (strength <= 0f) return
        canvas.save()
        canvas.translate(0f, getHeight() - height)
        canvas.drawRect(0f, 0f, width.toFloat(), height, paint)
        canvas.restore()
    }
}
