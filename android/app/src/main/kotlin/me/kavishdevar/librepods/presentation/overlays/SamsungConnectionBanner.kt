package me.kavishdevar.librepods.presentation.overlays

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.core.devices.Battery
import me.kavishdevar.librepods.core.devices.BatteryComponent
import me.kavishdevar.librepods.core.devices.BatteryStatus

/** Samsung Easy Setup `battery_small_popup` replica: device name over a connected-state battery line. */
@Composable
internal fun SamsungConnectionBanner(
    name: String,
    batteries: Set<Battery>,
    darkTheme: Boolean,
    cardState: ProximityCardState,
    onClosed: () -> Unit,
) {
    val visible = cardState == ProximityCardState.VISIBLE
    val transition = remember { MutableTransitionState(false).apply { targetState = visible } }

    LaunchedEffect(visible) { transition.targetState = visible }
    LaunchedEffect(transition.isIdle, transition.currentState) {
        if (transition.isIdle && !transition.currentState) onClosed()
    }

    val levels = remember(batteries) {
        batteries.filter { it.level in 0..100 && it.status != BatteryStatus.DISCONNECTED }
            .associate { it.component to it.level }
    }
    val left = levels[BatteryComponent.LEFT]
    val right = levels[BatteryComponent.RIGHT]
    val labels = buildList {
        if (left != null && left == right) {
            add(stringResource(R.string.connection_banner_both) to left)
        } else {
            if (left != null) add(stringResource(R.string.left) to left)
            if (right != null) add(stringResource(R.string.right) to right)
        }
        levels[BatteryComponent.HEADSET]?.let { add("" to it) }
        levels[BatteryComponent.CASE]?.let { add(stringResource(R.string.case_alt) to it) }
    }
    // Easy Setup 11.1.24.8 `battery_small_popup`: title `sesl_text1_light` / night `sesl_text2_dark`,
    // battery line `sesl_edit_text_hint_color_light` in both themes.
    val titleColor = if (darkTheme) Color(0xFFE9E9EC) else Color(0xFF010102)
    val batteryColor = Color(0xFF8C8C91)
    val batteryText = labels.joinToString(" | ") { (label, level) ->
        if (label.isEmpty()) "$level%" else "$label $level%"
    }

    // The popup sizes its text in dp (`battery_small_popup_title_font_size` 15dp,
    // `battery_small_popup_text_font_size` 11dp), so font scale does not grow it.
    val density = LocalDensity.current
    val baseStyle = MaterialTheme.typography.bodySmall.copy(
        letterSpacing = 0.sp,
        lineHeight = TextUnit.Unspecified,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    )
    val titleStyle = with(density) {
        baseStyle.copy(fontSize = 15.dp.toSp(), fontWeight = FontWeight(600), color = titleColor)
    }
    val batteryStyle = with(density) {
        baseStyle.copy(fontSize = 11.dp.toSp(), fontWeight = FontWeight(400), color = batteryColor)
    }

    AnimatedVisibility(transition, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
        Surface(
            // Layout: 268dp max width, 22dp start/end and 12dp top padding. The visible
            // backing measured 187px (62.33dp) tall on the S25, taller than the 58dp layout.
            modifier = Modifier.widthIn(max = 268.dp).height(BannerHeight),
            shape = RoundedCornerShape(26.dp),
            // Easy Setup blurs `?android:colorBackground` behind the popup. App overlays
            // cannot blur, so this is the rendered dark backing measured on the S25 capture.
            // Light is unmeasured.
            color = if (darkTheme) Color(0xFF3A3A3A) else Color(0xFFF2F2F4),
        ) {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp)) {
                Text(
                    text = name,
                    style = titleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 224.dp).padding(bottom = 2.dp),
                )
                if (batteryText.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ConnectedIndicator(Modifier.size(14.dp))
                        Text(
                            text = batteryText,
                            style = batteryStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 203.dp).padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

private val BannerHeight = 62.333.dp

/**
 * Our own drawing of the popup's connected indicator: a 14dp amber disc with a diagonal
 * two-link chain. Colors and the glyph's 3–10.7dp extent were measured from the reference;
 * no vendor resource is packaged.
 */
@Composable
private fun ConnectedIndicator(modifier: Modifier) {
    Canvas(modifier) {
        val u = size.width / 14f
        drawCircle(Color(0xFFFFB45B))
        val stroke = Stroke(width = 0.9f * u, cap = StrokeCap.Round)
        rotate(-45f) {
            val linkLength = 5.0f * u
            val linkWidth = 3.1f * u
            val radius = CornerRadius(linkWidth / 2)
            val cx = center.x
            val cy = center.y
            for (sign in listOf(-1f, 1f)) {
                val offset = sign * 1.65f * u
                drawRoundRect(
                    color = Color(0xFF010102),
                    topLeft = Offset(cx + offset - linkLength / 2, cy - linkWidth / 2),
                    size = Size(linkLength, linkWidth),
                    cornerRadius = radius,
                    style = stroke,
                )
            }
        }
    }
}
