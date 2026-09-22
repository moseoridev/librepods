package me.kavishdevar.librepods.presentation.theme

import android.graphics.Matrix
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/** The three-segment smooth corner used by Buds' qu.b; a circular arc is not equivalent. */
internal data class BudsGroupShape(val radius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val half = size.minDimension / 2f
        if (half <= 0f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        val r = with(density) { radius.toPx() }.coerceIn(0f, half)
        if (r == 0f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        val ratio = r / half
        val vertex = 1f - ((ratio - .5f) / .4f).coerceIn(0f, 1f) * .13877845f
        val control = if (ratio.toDouble() > .6)
            1f + ((ratio - .6f) / .3f).coerceAtMost(1f) * .042454004f else 1f
        val reach = minOf(half / r * 100f, vertex * 128.19f)
        val first = control * 83.62f
        val normalizedCorner = floatArrayOf(
            0f, reach,
            0f, first, 4.64f, 67.45f, 13.36f, 51.16f,
            22.07f, 34.86f, 34.86f, 22.07f, 51.16f, 13.36f,
            67.45f, 4.64f, first, 0f, reach, 0f,
            reach, 0f,
        )
        // Keep scale, pivot rotation and placement separate, as in qu.b. Combining them
        // into absolute-coordinate arithmetic changes float rounding at the clipped edge.
        val transform = Matrix()
        transform.setScale(r / 100f, r / 100f)
        transform.mapPoints(normalizedCorner)
        val points = FloatArray(normalizedCorner.size)
        return Outline.Generic(Path().apply {
            moveTo(0f, r)
            for (corner in 0..3) {
                when (corner) {
                    1 -> lineTo(size.width - r, 0f)
                    2 -> lineTo(size.width, size.height - r)
                    3 -> lineTo(r, size.height)
                }
                normalizedCorner.copyInto(points)
                transform.reset()
                transform.preTranslate(-r / 2f, -r / 2f)
                transform.postRotate(corner * 90f)
                transform.postTranslate(r / 2f, r / 2f)
                transform.mapPoints(points)
                transform.setTranslate(
                    if (corner == 1 || corner == 2) size.width - r else 0f,
                    if (corner >= 2) size.height - r else 0f,
                )
                transform.mapPoints(points)
                lineTo(points[0], points[1])
                for (i in 2..14 step 6) {
                    cubicTo(points[i], points[i + 1], points[i + 2], points[i + 3], points[i + 4], points[i + 5])
                }
                lineTo(points[20], points[21])
            }
            lineTo(0f, r)
            close()
        })
    }
}
