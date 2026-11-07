package me.kavishdevar.librepods.presentation.screens.apple

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import me.kavishdevar.librepods.ui.components.SettingsImageBackdrop

/** Own smooth approximation of the native translucent glow, with its source placement. */
@Composable
internal fun BudsHomeBackdrop(dark: Boolean) {
    SettingsImageBackdrop(remember(dark) { HomeGlowPainter(dark) })
}

private class HomeGlowPainter(private val dark: Boolean) : Painter() {
    override val intrinsicSize = Size(500f, 500f)
    private var measured = Size.Unspecified
    private var glow: Brush? = null
    override fun DrawScope.onDraw() {
        if (measured != size) {
            measured = size
            // Native PNG samples: center136/202/248 at alpha58 dark /97 light.
            // Do not bundle the vendor raster or synthesize its random grain.
            val alpha = if (dark) 58 else 97
            fun ink(rgb: Long, fraction: Float) = Color(rgb).copy(alpha = alpha / 255f * fraction)
            glow = Brush.radialGradient(
                0f to ink(0xFF88CAF8, 1f),
                .4f to ink(0xFFA3D4F9, .57f),
                .75f to ink(0xFFBCE5FF, .28f),
                .9f to ink(0xFFC9FEFF, .09f),
                1f to Color.Transparent,
                center = center, radius = size.minDimension * .42f)
        }
        drawRect(glow!!)
    }
}

internal enum class BudsMenuArtwork { Sound, Touch, Accessibility, QuietSound }

/** Own functional line drawings; 24dp and1.5-unit strokes follow native menu resources. */
@Composable
internal fun BudsMenuIcon(kind: BudsMenuArtwork) {
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(24.dp)) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun path(block: Path.() -> Unit) = drawPath(Path().apply(block), ink, style = stroke)
            when (kind) {
                BudsMenuArtwork.Sound, BudsMenuArtwork.QuietSound -> {
                    path {
                        moveTo(3.5f, 9f); lineTo(7f, 9f); lineTo(12.5f, 4.5f)
                        lineTo(12.5f, 19.5f); lineTo(7f, 15f); lineTo(3.5f, 15f); close()
                    }
                    drawArc(ink, -50f, 100f, false, Offset(9f, 7f), Size(9f, 10f), style = stroke)
                    if (kind == BudsMenuArtwork.Sound)
                        drawArc(ink, -50f, 100f, false, Offset(7f, 3.5f), Size(15f, 17f), style = stroke)
                }
                BudsMenuArtwork.Accessibility -> {
                    drawCircle(ink, 2f, Offset(12f, 4.5f), style = stroke)
                    path { moveTo(4.5f, 9f); lineTo(9.5f, 9.5f); lineTo(14.5f, 9.5f); lineTo(19.5f, 9f) }
                    path { moveTo(9.5f, 9.5f); lineTo(10f, 13f); lineTo(7.5f, 21f) }
                    path { moveTo(14.5f, 9.5f); lineTo(14f, 13f); lineTo(16.5f, 21f) }
                    path { moveTo(10f, 13f); lineTo(14f, 13f) }
                }
                BudsMenuArtwork.Touch -> {
                    drawArc(ink, 140f, 240f, false, Offset(5f, 3f), Size(9f, 9f), style = stroke)
                    path {
                        moveTo(9.5f, 15f); lineTo(9.5f, 8f)
                        cubicTo(9.5f, 6.7f, 12f, 6.7f, 12f, 8f); lineTo(12f, 12f)
                        cubicTo(12f, 10.7f, 14.2f, 10.7f, 14.2f, 12f)
                        cubicTo(14.2f, 10.9f, 16.4f, 11.2f, 16.4f, 12.5f)
                        cubicTo(16.4f, 11.6f, 18.5f, 12f, 18.5f, 13.3f)
                        lineTo(18.5f, 16f); cubicTo(18.5f, 22f, 12f, 22f, 9f, 18.5f)
                        lineTo(7f, 16f); cubicTo(5.8f, 14.4f, 7.7f, 13.4f, 9.5f, 15f)
                    }
                }
            }
        }
    }
}

/** Own static status glyphs, not the vendor Lottie paths/animation. */
@Composable
internal fun BudsModeArtwork(mode: Int, selected: Boolean? = null, compact: Boolean = false) {
    val dark = MaterialTheme.colorScheme.onSurface != Color(0xFF010102)
    val ink = when (selected) {
        true -> if (dark) Color.Black else Color.White
        false -> Color(0xFFB1B1B4)
        null -> MaterialTheme.colorScheme.onSurface
    }
    Canvas(Modifier.size(if (compact) 32.dp else 44.dp)) {
        scale(size.width / 44f, size.height / 44f, pivot = Offset.Zero) {
            // Preserve a44dp source artwork slot; the own human glyph occupies28dp.
            val head = Offset(22f, 18f)
            drawCircle(ink, 4.8f, head)
            val body = Path().apply {
                moveTo(12f, 36f); cubicTo(12f, 23f, 32f, 23f, 32f, 36f); close()
            }
            if (mode != 4) drawPath(body, ink)
            val ring = Stroke(2f, cap = StrokeCap.Round)
            when (mode) {
                1, 2 -> drawArc(if (mode == 1) ink.copy(alpha = ink.alpha * .4f) else ink,
                    140f, 260f, false, Offset(11.5f, 7f), Size(21f, 23f), style = ring)
                3 -> for (i in 0..8) {
                    val angle = Math.toRadians(150.0 + i * 30.0)
                    drawCircle(ink, 1.45f, Offset(head.x + kotlin.math.cos(angle).toFloat() * 10f,
                        head.y + kotlin.math.sin(angle).toFloat() * 10f))
                }
                4 -> {
                    // Native adaptive mode uses sound bars, not the iOS sparkle symbol.
                    drawPath(Path().apply { moveTo(12f, 36f); cubicTo(12f, 27f, 18f, 25f, 22f, 25f); lineTo(22f, 36f); close() }, ink)
                    listOf(5f, 9f, 13f, 9f, 5f).forEachIndexed { i, height ->
                        val x = 25f + i * 3.2f
                        drawLine(ink, Offset(x, 31f - height / 2), Offset(x, 31f + height / 2),
                            strokeWidth = 2.1f, cap = StrokeCap.Round)
                    }
                }
            }
        }
    }
}
