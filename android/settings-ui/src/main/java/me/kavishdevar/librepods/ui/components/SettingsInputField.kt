package me.kavishdevar.librepods.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import me.kavishdevar.librepods.ui.theme.SettingsStyle
import kotlin.math.roundToInt

/** Resource-derived field around a platform editor; text and validation remain caller-owned. */
@Composable
fun SettingsInputField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    errorMessage: String? = null,
    reserveSupportingSpace: Boolean = true,
    cursorColor: Color = Color.Unspecified,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    selectionColor: Color = Color.Unspecified,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val platformCursorColor = remember(context, configuration) {
        // The application-context editor resolves the platform's dynamic 600 role.
        Color(context.resources.getColor(android.R.color.system_accent1_600, context.theme))
    }
    val dark = colors.onSurface != Color(0xFF010102)
    val foreground = (if (dark) Color(0xFFFAFAFF) else Color(0xFF010102))
        .copy(alpha = if (enabled) 1f else .4f)
    val density = LocalDensity.current
    // XML measures the helper's 30dp margin and editor's 24dp margin separately.
    val helperStart = with(density) { (30.dp.roundToPx() - 24.dp.roundToPx()).toDp() }
    val locales = configuration.locales
    // The original EditText enables locale-preferred minimum metrics on API 35+.
    val minimumLinePixels = remember(density, locales) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.SANS_SERIF
            textSize = with(density) { 17.sp.toPx().roundToInt().toFloat() }
            textLocales = locales
        }
        val metrics = Paint.FontMetricsInt()
        if (Build.VERSION.SDK_INT >= 35) paint.getFontMetricsIntForLocale(metrics)
        else paint.getFontMetricsInt(metrics)
        (metrics.bottom - metrics.top).coerceAtLeast(0)
    }
    val fieldStyle = SettingsStyle.RowTitle.copy(fontFamily = FontFamily.SansSerif,
        platformStyle = PlatformTextStyle(includeFontPadding = true), color = foreground)
    val rule = if (errorMessage != null) {
        if (dark) Color(0xFFF76F68) else Color(0xFFDB332A)
    } else if (dark) Color(0xFFFAFAFA) else Color(0xFF252525)
    Column(modifier.fillMaxWidth()) {
        SettingsInputEditor(state, placeholder = placeholder, singleLine = singleLine,
            enabled = enabled, focusRequester = focusRequester, keyboardOptions = keyboardOptions,
            foreground = foreground,
            hint = colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else .4f),
            cursor = if (cursorColor == Color.Unspecified) platformCursorColor else cursorColor,
            selection = if (selectionColor == Color.Unspecified) platformCursorColor.copy(alpha = .4f)
                else selectionColor,
            rule = rule, errorMessage = errorMessage,
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = with(density) { (minimumLinePixels + 10.dp.toPx().toInt() +
                    7.dp.toPx().toInt() + 4.dp.roundToPx()).toDp() })
                .then(if (errorMessage != null) Modifier.semantics { error(errorMessage) } else Modifier))
        if (errorMessage != null) Text(errorMessage,
            style = fieldStyle.copy(fontSize = with(density) { 12.sp.toPx().roundToInt().toSp() },
                lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified, lineHeightStyle = null), color = rule,
            modifier = Modifier.padding(start = helperStart).heightIn(min = 26.dp))
        else if (reserveSupportingSpace) Spacer(Modifier.height(26.dp))
    }
}
