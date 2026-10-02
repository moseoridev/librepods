package me.kavishdevar.librepods.ui.components

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.sliderTrack
import me.kavishdevar.librepods.ui.theme.SettingsFontFamily
import kotlin.math.atan

/** Display-only arc meter. Normalize missing/combined readings in the caller. */
@Composable
fun SettingsBatteryMeter(level: Int?, name: String, value: String, description: String,
    progressColor: Color, maxWidth: Dp, chargeLabel: String = "",
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.sliderTrack,
    nameContent: (@Composable () -> Unit)? = null,
    artwork: @Composable () -> Unit) {
    val density = LocalDensity.current
    val textSize = with(density) { 12.dp.toSp() }
    val lineHeight = with(density) { 18.dp.toSp() }
    // h4.i.d trims both outer line edges; newer Compose defaults leave them.
    val labelStyle = TextStyle(fontFamily = SettingsFontFamily, fontSize = textSize,
        lineHeight = lineHeight, lineHeightStyle = LineHeightStyle(
            LineHeightStyle.Alignment.Proportional, LineHeightStyle.Trim.Both))
    val stroke = with(density) { 8.dp.toPx() }
    val diameter = with(density) { 50.dp.toPx() }
    val arc = remember(stroke, diameter) {
        RectF(stroke / 2f, stroke / 2f, diameter - stroke / 2f, diameter - stroke / 2f)
    }
    // wm.m1 reduces the arc for the round end caps; the source angles describe
    // the outer opening rather than the final stroke's center-line endpoints.
    val capAngle = Math.toDegrees(atan(stroke / 2f / ((diameter - stroke) / 2f)).toDouble()).toFloat()
    val startAngle = 147.73f + capAngle
    val sweepAngle = (244.54f - 2f * capAngle).coerceAtLeast(0f)
    // wm.l1.a first converts the clamped percentage to the outer arc angle;
    // wm.m1 then normalizes it to the cap-adjusted sweep. Preserve both float
    // operations: multiplying the sweep by the percentage first rounds differently.
    val outerProgressAngle = ((level ?: 0).toFloat().coerceIn(0f, 100f) / 100f) * 244.54f
    val progressSweepAngle = ((outerProgressAngle / 244.54f) * sweepAngle).coerceIn(0f, sweepAngle)
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }
    // rl.r.d keeps its incoming weighted width. Do not insert wrapContentSize:
    // centering a natural-width column rounds the ring and label independently.
    Column(modifier.widthIn(max = maxWidth).widthIn(max = 92.dp)
        .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(50.dp, 37.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                paint.strokeWidth = stroke
                drawIntoCanvas { canvas ->
                    paint.color = trackColor.toArgb()
                    canvas.nativeCanvas.drawArc(arc, startAngle, sweepAngle, false, paint)
                    if (progressSweepAngle > 0f) {
                        paint.color = progressColor.toArgb()
                        canvas.nativeCanvas.drawArc(arc, startAngle, progressSweepAngle, false, paint)
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomCenter).offset(y = (-4).dp).size(30.dp, 18.dp)) { artwork() }
        }
        // Current rl.r.d separates the 37dp ring slot and label by 3dp.
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProvideTextStyle(labelStyle.copy(fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)) {
                if (nameContent == null) Text(name) else nameContent()
            }
            Spacer(Modifier.width(2.dp))
            Text(value, style = labelStyle,
                color = MaterialTheme.colorScheme.onSurface)
        }
        if (chargeLabel.isNotEmpty()) {
            Text(chargeLabel, style = labelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
