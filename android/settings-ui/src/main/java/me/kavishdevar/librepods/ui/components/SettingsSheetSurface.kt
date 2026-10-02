package me.kavishdevar.librepods.ui.components

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.ViewOutlineProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** Current XML card, independent of its modal window and caller-owned actions. */
@Composable
fun SettingsSheetSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = MaterialTheme.colorScheme.onSurface != Color(0xFF010102)
    val density = LocalDensity.current.density
    val border = remember(dark, density) { SettingsSheetBorder(density, dark) }
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = settingsSheetMaxWidth(LocalConfiguration.current)).fillMaxWidth()
            .drawWithContent {
                drawContent()
                // The XML GradientBorderView follows the scrolling content.
                drawIntoCanvas { border.draw(it.nativeCanvas, size.width, size.height) }
            }) {
            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { SettingsSheetBackground(it) },
                update = { it.configure(dark) },
                onRelease = { SettingsWindowBlur.clear(it) },
            )
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp),
                    content = content)
            }
        }
    }
}

// sw600 density-qualified variants and sw960 all resolve to the same 400dp value.
internal fun settingsSheetMaxWidth(configuration: Configuration) =
    if (configuration.smallestScreenWidthDp >= 600) 400.dp else 360.dp

/** GradientDrawable and GradientBorderView use ordinary circular corners. */
internal class SettingsSheetBackground(context: Context) : View(context) {
    private val radius = 26f * resources.displayMetrics.density
    private var dark: Boolean? = null

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        elevation = 8f * resources.displayMetrics.density
        outlineProvider = ViewOutlineProvider.BACKGROUND
    }

    fun configure(dark: Boolean) {
        if (this.dark == dark) return
        this.dark = dark
        background = settingsDialogBackground(if (dark) 0xFF171717.toInt() else 0xFFFCFCFC.toInt(), radius)
        // The optional OEM effect shares the existing prompt fallback/cleanup path.
        SettingsWindowBlur.apply(this, dark, radius, radiusPixels = 60)
        invalidate()
    }

}

/** A drawing layer avoids putting a touch-intercepting View above the content. */
private class SettingsSheetBorder(density: Float, dark: Boolean) {
    private val radius = 26f * density
    private val half = density / 2f
    private val rgb = if (dark) 0x00A3A3A7 else 0x00FFFFFF
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density
    }
    private val frame = RectF()
    private var width = -1f
    private var height = -1f

    fun draw(canvas: Canvas, width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        if (this.width != width || this.height != height) {
            this.width = width
            this.height = height
            frame.set(half, half, width - half, height - half)
            paint.shader = LinearGradient(0f, 0f, 0f, height,
                rgb or 0x1A000000, rgb, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(frame, radius, radius, paint)
    }
}
