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
            val radius = size.minDimension *
                if (dark) DARK_GLOW_RADIUS_FRACTION else LIGHT_GLOW_RADIUS_FRACTION
            val stops: Array<Pair<Float, Color>> =
                if (dark) DARK_GLOW_STOPS else LIGHT_GLOW_STOPS
            glow = Brush.radialGradient(
                *stops,
                center = Offset(size.width * GLOW_CENTER_X_FRACTION,
                    size.height * GLOW_CENTER_Y_FRACTION),
                radius = radius)
        }
        drawRect(glow!!)
    }

    private companion object {
        // The native backdrop is a 500x500 RGBA raster (res/Rw.png light, res/CO.png dark),
        // not a gradient. Its glow is not centred in the bitmap: the alpha-weighted centroid
        // of both assets sits at (249.76,241.06), i.e. just below the horizontal axis. Sampling
        // stops about that point is what makes the visible band match.
        const val GLOW_CENTER_X_FRACTION = 249.76f / 500f
        const val GLOW_CENTER_Y_FRACTION = 241.06f / 500f

        // Radius at which the measured alpha reaches zero: 209px light, 207px dark.
        const val LIGHT_GLOW_RADIUS_FRACTION = 209f / 500f
        const val DARK_GLOW_RADIUS_FRACTION = 207f / 500f

        // Straight-alpha stops every 5% of the measured radius, averaged over each annulus.
        // Measured peak alpha is 104 light / 62 dark. The assets keep a low-amplitude random
        // grain (+-5/255) that is not synthesised here; only the smooth radial field is
        // reproduced.
        val LIGHT_GLOW_STOPS: Array<Pair<Float, Color>> = arrayOf(
            0.00f to Color(0x7CC1F3).copy(alpha = 0.4039f),
            0.05f to Color(0x7DC2F3).copy(alpha = 0.3963f),
            0.10f to Color(0x81C5F3).copy(alpha = 0.3788f),
            0.15f to Color(0x84C7F3).copy(alpha = 0.3599f),
            0.20f to Color(0x88C9F3).copy(alpha = 0.3392f),
            0.25f to Color(0x8CCBF3).copy(alpha = 0.3213f),
            0.30f to Color(0x90CEF3).copy(alpha = 0.3010f),
            0.35f to Color(0x94D0F3).copy(alpha = 0.2812f),
            0.40f to Color(0x99D3F4).copy(alpha = 0.2575f),
            0.45f to Color(0x9DD5F5).copy(alpha = 0.2349f),
            0.50f to Color(0xA1D7F7).copy(alpha = 0.2121f),
            0.55f to Color(0xA6D9F8).copy(alpha = 0.1869f),
            0.60f to Color(0xABDBF9).copy(alpha = 0.1640f),
            0.65f to Color(0xB0DDFA).copy(alpha = 0.1391f),
            0.70f to Color(0xB4E0FB).copy(alpha = 0.1168f),
            0.75f to Color(0xBAE2FC).copy(alpha = 0.0927f),
            0.80f to Color(0xBEE4FD).copy(alpha = 0.0718f),
            0.85f to Color(0xC2E7FE).copy(alpha = 0.0501f),
            0.90f to Color(0xC5E8FE).copy(alpha = 0.0318f),
            0.95f to Color(0xC5E6FE).copy(alpha = 0.0128f),
            1.00f to Color(0xC5E6FE).copy(alpha = 0f),
        )

        val DARK_GLOW_STOPS: Array<Pair<Float, Color>> = arrayOf(
            0.00f to Color(0x7DC4F2).copy(alpha = 0.2392f),
            0.05f to Color(0x7DC2F3).copy(alpha = 0.2378f),
            0.10f to Color(0x81C5F3).copy(alpha = 0.2272f),
            0.15f to Color(0x84C7F3).copy(alpha = 0.2157f),
            0.20f to Color(0x87C9F3).copy(alpha = 0.2044f),
            0.25f to Color(0x8BCCF3).copy(alpha = 0.1926f),
            0.30f to Color(0x90CEF3).copy(alpha = 0.1816f),
            0.35f to Color(0x93D0F3).copy(alpha = 0.1698f),
            0.40f to Color(0x98D2F4).copy(alpha = 0.1556f),
            0.45f to Color(0x9CD4F5).copy(alpha = 0.1422f),
            0.50f to Color(0xA1D7F6).copy(alpha = 0.1272f),
            0.55f to Color(0xA6D9F8).copy(alpha = 0.1134f),
            0.60f to Color(0xAADBF9).copy(alpha = 0.0997f),
            0.65f to Color(0xAFDDFA).copy(alpha = 0.0848f),
            0.70f to Color(0xB4DFFB).copy(alpha = 0.0713f),
            0.75f to Color(0xB8E1FC).copy(alpha = 0.0581f),
            0.80f to Color(0xBEE4FD).copy(alpha = 0.0442f),
            0.85f to Color(0xC0E4FD).copy(alpha = 0.0322f),
            0.90f to Color(0xC6E6FD).copy(alpha = 0.0212f),
            0.95f to Color(0xC6E6FD).copy(alpha = 0.0095f),
            1.00f to Color(0xC6E6FD).copy(alpha = 0f),
        )
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
