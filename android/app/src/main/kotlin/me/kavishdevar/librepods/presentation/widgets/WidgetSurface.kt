package me.kavishdevar.librepods.presentation.widgets

import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.graphics.Outline
import android.graphics.Path
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.RemoteViews
import me.kavishdevar.librepods.R

/** One UI 9 owns the final smooth outline; other hosts and local previews still need a mask. */
internal object WidgetSurface {
    private val fallbackShapes = intArrayOf(R.drawable.widget_shape_0, R.drawable.widget_shape_1,
        R.drawable.widget_shape_2, R.drawable.widget_shape_3, R.drawable.widget_shape_4)
    private val hostShapes = intArrayOf(R.drawable.widget_host_shape_default, R.drawable.widget_host_shape_1,
        R.drawable.widget_host_shape_2, R.drawable.widget_host_shape_3, R.drawable.widget_host_shape_4)

    // One UI skips its outline for Knox/dual-app providers. Conservatively retain the local
    // outline for every non-system profile, without depending on Samsung's private profile APIs.
    private val systemUser = UserHandle.getUserHandleForUid(0)
    fun hostOwnsOutline(options: Bundle, profile: UserHandle = Process.myUserHandle()): Boolean =
        profile == systemUser && Build.VERSION.SDK_INT >= 37 &&
        options.getInt("semHostType") == 1 &&
        options.getInt("semWidgetSize") in intArrayOf(WidgetGeometry.SMALL, WidgetGeometry.WIDE,
            WidgetGeometry.MEDIUM, WidgetGeometry.LARGE)

    fun apply(views: RemoteViews, size: WidgetGeometry, shape: Int, hostOwnsOutline: Boolean) {
        val drawable = if (hostOwnsOutline) {
            // The host reads individual GradientDrawable corners when Samsung string tags are absent.
            // No corners means its default shape, rather than a custom four-corner 19dp shape.
            hostShapes[if (size.type == WidgetGeometry.MEDIUM) shape else 0]
        } else {
            if (size.short) R.drawable.widget_battery_surface
            else if (size.type == WidgetGeometry.MEDIUM) fallbackShapes[shape]
            else R.drawable.widget_controls_surface
        }
        views.setInt(R.id.widget_frame, "setBackgroundResource", drawable)
        // Set both states on every full publication: RemoteViews can reapply the same root view.
        views.setBoolean(R.id.widget_frame, "setClipToOutline", !hostOwnsOutline)
    }

    /** An Activity preview has no launcher. Reproduce its outline locally, at the preview's scale. */
    fun applyPreviewOutline(view: View, options: Bundle, source: WidgetGeometry, shape: Int, scale: Float) {
        if (!hostOwnsOutline(options)) return
        val frame = view.findViewById<View>(R.id.widget_frame)
        val density = view.resources.displayMetrics.density * scale
        val defaultRadius = options.getFloat("semShapeRadius", 24f).takeIf { it.isFinite() && it > 0f } ?: 24f
        val corners = if (source.type != WidgetGeometry.MEDIUM || shape == 0) FloatArray(4) { defaultRadius }
            else when (shape) { // Clockwise from top left.
                1 -> floatArrayOf(32f, 12f, 32f, 12f)
                2 -> floatArrayOf(12f, 32f, 12f, 32f)
                3 -> floatArrayOf(32f, 12f, 32f, 32f)
                else -> floatArrayOf(12f, 32f, 32f, 32f)
            }
        frame.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                if (source.short) outline.setRoundRect(0, 0, view.width, view.height, view.height / 2f)
                else if (view.width > 0 && view.height > 0)
                    outline.setPath(smoothPath(view.width.toFloat(), view.height.toFloat(), corners.map { it * density }))
            }
        }
        frame.clipToOutline = true
    }

    // One UI Home's SmoothRoundedCorner/SeslRoundedCorner curve. Only used in the finite settings UI.
    // Each corner has three cubic segments; unlike addRoundRect, the curve eases into the straight edge.
    private fun smoothPath(width: Float, height: Float, corners: List<Float>): Path {
        val half = minOf(width, height) / 2f
        return Path().apply {
            moveTo(width / 2f, 0f)
            for (corner in 0..3) { // Top right, bottom right, bottom left, top left.
                val radius = corners[(corner + 1) % 4].coerceIn(0f, half)
                val ratio = radius / half
                val vertex = 1f - ((ratio - .5f) / .4f).coerceIn(0f, 1f) * .13877845f
                val control = 1f + ((ratio - .6f) / .3f).coerceIn(0f, 1f) * .042454004f
                val unit = radius / 100f
                val reach = unit * 128.19f * vertex
                val first = unit * 83.62f * control
                fun point(x: Float, y: Float): Pair<Float, Float> = when (corner) {
                    0 -> width - x to y
                    1 -> width - y to height - x
                    2 -> x to height - y
                    else -> y to x
                }
                fun line(x: Float, y: Float) { val p = point(x, y); lineTo(p.first, p.second) }
                fun curve(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
                    val a = point(x1, y1); val b = point(x2, y2); val c = point(x3, y3)
                    cubicTo(a.first, a.second, b.first, b.second, c.first, c.second)
                }
                val along = if (corner % 2 == 0) width / 2f else height / 2f
                val across = if (corner % 2 == 0) height / 2f else width / 2f
                line(minOf(along, reach), 0f)
                curve(first, 0f, unit * 67.45f, unit * 4.64f, unit * 51.16f, unit * 13.36f)
                curve(unit * 34.86f, unit * 22.07f, unit * 22.07f, unit * 34.86f, unit * 13.36f, unit * 51.16f)
                curve(unit * 4.64f, unit * 67.45f, 0f, first, 0f, minOf(across, reach))
            }
            close()
        }
    }
}
