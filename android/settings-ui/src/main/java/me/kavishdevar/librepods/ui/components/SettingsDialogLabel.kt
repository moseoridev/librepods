package me.kavishdevar.librepods.ui.components

import androidx.compose.ui.unit.LayoutDirection
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.BoringLayout
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextUtils
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.ui.theme.SettingsStyle
import kotlin.math.roundToInt

/** Intrinsic action measurement; its parent owns the accessible label and the platform view draws it. */
@Composable
internal fun SettingsDialogActionMeasure(label: String, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val paint = rememberDialogTextPaint()
    val direction = dialogTextDirection()
    val platformLayout = remember(label, paint, direction) {
        dialogTextLayout(label, paint, ScrollingTextWidth, Layout.Alignment.ALIGN_CENTER, direction)
    }
    Text(label, modifier.height(with(density) { platformLayout.height.toDp() }).clearAndSetSemantics {}.drawWithContent {
        // Keep measurement without a second visual or accessible copy of the label.
    }, maxLines = 1, softWrap = false, style = SettingsStyle.RowTitle.copy(
        fontSize = with(density) { paint.textSize.toSp() }, fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center, textMotion = TextMotion.Animated,
        platformStyle = PlatformTextStyle(includeFontPadding = true)))
}

@Composable
internal fun SettingsDialogTitle(title: String, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val paint = rememberDialogTextPaint()
    val direction = dialogTextDirection()
    BoxWithConstraints(modifier) {
        val width = constraints.maxWidth
        val layout = remember(title, paint, width, direction) {
            val single = dialogTextLayout(title, paint, width, Layout.Alignment.ALIGN_NORMAL, direction)
            if (single.getEllipsisCount(0) > 0)
                dialogTextLayout(title, paint, width, Layout.Alignment.ALIGN_NORMAL, direction, maxLines = 2)
            else single
        }
        Text(title, Modifier.fillMaxWidth().height(with(density) { layout.height.toDp() })
            .drawWithContent { drawIntoCanvas { layout.draw(it.nativeCanvas) } },
            style = SettingsStyle.RowTitle.copy(fontSize = with(density) { paint.textSize.toSp() },
                fontWeight = FontWeight.SemiBold, platformStyle = PlatformTextStyle(includeFontPadding = true)))
    }
}

@Composable
internal fun rememberDialogTextPaint(): TextPaint {
    val density = LocalDensity.current
    val locales = LocalConfiguration.current.locales
    val resolvedTypeface = LocalFontFamilyResolver.current.resolve(
        SettingsStyle.RowTitle.fontFamily, FontWeight.SemiBold).value as Typeface
    val textColor = LocalContentColor.current
    val fontPixels = with(density) {
        val resourcePixels = 17.sp.toPx().roundToInt().toFloat()
        if (fontScale > 1.3f) resourcePixels / fontScale * 1.3f else resourcePixels
    }
    return remember(resolvedTypeface, fontPixels, textColor, locales) {
        TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            typeface = resolvedTypeface
            textSize = fontPixels
            color = textColor.toArgb()
            textLocales = locales
            isLinearText = true
            isSubpixelText = true
        }
    }
}

// TextView.getTextDirectionHeuristic: default first-strong follows the view's layout direction.
@Composable
private fun dialogTextDirection(): TextDirectionHeuristic =
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) TextDirectionHeuristics.FIRSTSTRONG_RTL
    else TextDirectionHeuristics.FIRSTSTRONG_LTR

private fun dialogTextLayout(text: String, paint: TextPaint, width: Int, alignment: Layout.Alignment,
    direction: TextDirectionHeuristic, maxLines: Int = 1): Layout {
    if (Build.VERSION.SDK_INT >= 35) return Layout.Builder(text, 0, text.length, paint, width)
        .setAlignment(alignment).setTextDirectionHeuristic(direction)
        .setFontPaddingIncluded(true).setFallbackLineSpacingEnabled(true)
        .setUseBoundsForWidth(true).setShiftDrawingOffsetForStartOverhang(false)
        .setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END).setEllipsizedWidth(width).build()
    val metrics = BoringLayout.isBoring(text, paint, direction, true, null)
    return if (metrics != null && metrics.width <= width)
        BoringLayout.make(text, paint, width, alignment, metrics, true, TextUtils.TruncateAt.END, width, true)
    else StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(alignment).setTextDirection(direction).setIncludePad(true).setUseLineSpacingFromFallbacks(true)
        .setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END).setEllipsizedWidth(width).build()
}

private const val ScrollingTextWidth = 1 shl 20
