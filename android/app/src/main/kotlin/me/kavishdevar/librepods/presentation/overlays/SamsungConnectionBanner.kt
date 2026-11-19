package me.kavishdevar.librepods.presentation.overlays

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.core.devices.Battery
import me.kavishdevar.librepods.core.devices.BatteryComponent
import me.kavishdevar.librepods.core.devices.BatteryStatus

/** Compact connection and battery summary for the Samsung theme. */
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
    val supportingColor = if (darkTheme) Color(0xFFCBCBD1) else Color(0xFF424248)
    val lowColor = if (darkTheme) Color(0xFFFF986D) else Color(0xFFB63725)
    val batteryText = buildAnnotatedString {
        labels.forEachIndexed { index, (label, level) ->
            if (index > 0) append(" | ")
            withStyle(SpanStyle(color = if (level <= 15) lowColor else supportingColor)) {
                if (label.isNotEmpty()) append("$label ")
                append("$level%")
            }
        }
    }

    // Use the settings theme's device font and the platform line metrics of a compact popup.
    val bannerTextStyle = MaterialTheme.typography.bodySmall.copy(
        letterSpacing = 0.sp,
        lineHeight = TextUnit.Unspecified,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    )

    AnimatedVisibility(transition, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
        Surface(
            // Fit the rendered backing on the reference S25, including the dialog's extra height.
            modifier = Modifier.widthIn(max = 268.dp).heightIn(min = 62.dp),
            shape = RoundedCornerShape(26.dp),
            // A neutral backing keeps text legible at the touch-through window's opacity cap.
            color = if (darkTheme) Color(0xFF454548) else Color(0xFFF2F2F4),
        ) {
            Box(Modifier.padding(horizontal = 15.dp, vertical = 10.dp), Alignment.CenterStart) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = name,
                        style = bannerTextStyle,
                        color = if (darkTheme) Color.White else Color(0xFF252528),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (batteryText.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_connection_banner_battery),
                                contentDescription = null,
                                tint = supportingColor,
                            )
                            Text(
                                text = batteryText,
                                style = bannerTextStyle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = supportingColor,
                            )
                        }
                    }
                }
            }
        }
    }
}
