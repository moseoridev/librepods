package me.kavishdevar.librepods.ui.components

import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.text
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

/** XML sheet title; the caller owns its text, placement and actions. */
@Composable
fun SettingsSheetTitle(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    SettingsSheetText(text, false, modifier, color)
}

/** XML sheet body; place it 12dp below the title for the source message layout. */
@Composable
fun SettingsSheetBody(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    SettingsSheetText(text, true, modifier, color)
}

@Composable
private fun SettingsSheetText(text: String, body: Boolean, modifier: Modifier, color: Color) {
    val dark = MaterialTheme.colorScheme.onSurface != Color(0xFF010102)
    val defaultColor = if (body) {
        if (dark) Color(0xFFEAEAEB) else Color(0xFF252528)
    } else {
        if (dark) Color(0xFFFAFAFF) else Color(0xFF010102)
    }
    val tint = if (color == Color.Unspecified) defaultColor else color
    val density = LocalDensity.current
    val fontPixels = with(density) { (if (body) 14.sp else 17.sp).toPx().roundToInt().toFloat() }
    val locales = LocalConfiguration.current.locales
    // TextView's style-based family lookup and platform text layout differ from
    // Compose's weight-based resolver. No font or vendor resources are bundled.
    val typeface = remember(body) { Typeface.create("sec", if (body) Typeface.NORMAL else Typeface.BOLD) }
    AndroidView(modifier = modifier.semantics { this.text = AnnotatedString(text) },
        factory = { TextView(it).apply {
            includeFontPadding = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        } },
        update = {
            it.text = text
            it.typeface = typeface
            it.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontPixels)
            it.setTextColor(tint.toArgb())
            it.textLocales = locales
        })
}
