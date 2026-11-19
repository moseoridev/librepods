package me.kavishdevar.librepods.presentation.overlays

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.core.devices.Battery
import me.kavishdevar.librepods.core.devices.BatteryComponent
import me.kavishdevar.librepods.core.devices.BatteryStatus
import me.kavishdevar.librepods.data.app.OverlayBackgroundColor
import me.kavishdevar.librepods.data.app.ProximityCardSettings
import me.kavishdevar.librepods.data.app.resolve
import me.kavishdevar.librepods.presentation.components.primitives.MaterialButtonStyle
import me.kavishdevar.librepods.presentation.components.primitives.StyledButton
import me.kavishdevar.librepods.presentation.icons.LocalIcons
import kotlin.math.roundToInt

@SuppressLint("LocalContextResourcesRead", "LocalContextConfigurationRead")
@Composable
internal fun SamsungProximityCardContent(
    name: String,
    batteries: Set<Battery>,
    settings: ProximityCardSettings,

    @DrawableRes primaryImageRes: Int,
    @RawRes videoResId: Int,
    cardState: ProximityCardState,
    onConnect: () -> Unit,
    onDismiss: () -> Unit,
    onClosed: () -> Unit,
    airPodsConnected: Boolean,
) {
    val visibilityState = remember {
        MutableTransitionState(false).apply { targetState = cardState == ProximityCardState.VISIBLE }
    }
    val offsetY = remember { Animatable(0f) }

    LaunchedEffect(cardState) {
        if (cardState == ProximityCardState.VISIBLE) offsetY.snapTo(0f)
        visibilityState.targetState = cardState == ProximityCardState.VISIBLE
    }

    LaunchedEffect(visibilityState.isIdle, visibilityState.currentState) {
        if (visibilityState.isIdle && !visibilityState.currentState) {
            onClosed()
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val colors = MaterialTheme.colorScheme
    val backgroundColor = if (settings.backgroundColor is OverlayBackgroundColor.Default) {
        if (colors.surface.luminance() < .5f) Color(0xFF252528) else Color(0xFFFCFCFF)
    } else settings.backgroundColor.resolve()
    val panelColor = backgroundColor.copy(alpha = settings.backgroundAlpha)

    val darkTheme = backgroundColor.luminance() < 0.5f
    val textColor = if (settings.backgroundColor is OverlayBackgroundColor.Default) if (darkTheme) Color.White else Color.Black else contentColorFor(backgroundColor)

    val context = LocalContext.current

    val videoContext = remember(context, darkTheme) {
        val config = Configuration(context.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (darkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        context.createConfigurationContext(config)
    }

    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            val newOffset = (offsetY.value + delta).coerceAtLeast(0f)
            offsetY.snapTo(newOffset)
        }
    }

    val density = LocalDensity.current

    val heightDp = with(density) { settings.height.toDp() }
    val radius = if (settings.borderRadius == 78f) 26.dp else with(density) { settings.borderRadius.toDp() }
    val shape = RoundedCornerShape(radius)

    AnimatedVisibility(visibilityState, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = heightDp)
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = draggableState,
                    onDragStopped = { velocity ->
                        coroutineScope.launch {
                            if (offsetY.value > 120f || velocity > 1000f) {
                                onDismiss()
                            } else {
                                offsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    }
                )
                .clip(shape)
                .then(
                    if (settings.borderWidth > 0f) {
                        Modifier.border(settings.borderWidth.dp, textColor.copy(0.2f), shape)
                    } else {
                        Modifier
                    }
                ),
            color = panelColor,
            shape = shape
        ) {
            Box {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = TextAlign.Center,
                    )
                    SamsungConnectionBatteries(
                        batteries, primaryImageRes, videoResId, settings.useVideo,
                        backgroundColor, textColor, videoContext,
                    )
                    AnimatedVisibility(settings.showConnectButton && !airPodsConnected) {
                        StyledButton(
                            onClick = onConnect,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            materialButtonStyle = MaterialButtonStyle.Outlined
                        ) {
                            Text(
                                text = stringResource(R.string.connect),
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor
                            )
                        }
                    }
                }
                if (settings.showCloseButton) {
                    Box(Modifier.matchParentSize()) {
                        Icon(
                            imageVector = LocalIcons.current.Close,
                            contentDescription = stringResource(R.string.hide_prox_card),
                            tint = textColor,
                            modifier = Modifier.align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onDismiss)
                                .padding(11.dp),
                        )
                    }
                }
            }
        }
    }
}

// Easy Setup's battery_reconnection sheet uses one centred device image and a
// compact strip of readings below it. The AirPods imagery is already bundled.
@Composable
private fun SamsungConnectionBatteries(
    batteries: Set<Battery>,
    @DrawableRes primaryImageRes: Int,
    @RawRes videoResId: Int,
    useVideo: Boolean,
    backgroundColor: Color,
    textColor: Color,
    videoContext: android.content.Context,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val artwork = Modifier.width(132.dp).height(101.dp)
        if (useVideo) {
            androidx.compose.runtime.CompositionLocalProvider(LocalContext provides videoContext) {
                ConnectionVideo(resource = videoResId, fallback = primaryImageRes,
                    loadingColor = backgroundColor, modifier = artwork)
            }
        } else {
            Image(painterResource(primaryImageRes), contentDescription = null, modifier = artwork)
        }
        val readings = batteries.filter { it.level in 0..100 && it.status != BatteryStatus.DISCONNECTED }
            .sortedBy { it.component.ordinal }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            readings.forEachIndexed { index, battery ->
                if (index > 0) VerticalDivider(Modifier.height(21.dp))
                val label = stringResource(when (battery.component) {
                    BatteryComponent.LEFT -> R.string.left
                    BatteryComponent.RIGHT -> R.string.right
                    BatteryComponent.CASE -> R.string.case_alt
                    BatteryComponent.HEADSET -> R.string.airpods_label
                })
                Row(
                    Modifier.weight(1f).padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val low = battery.level <= 15
                    val fill = if (low) Color(0xFFD04A02) else Color(0xFF45BF10)
                    Canvas(Modifier.size(14.dp, 20.dp)) {
                        val cap = 2.dp.toPx()
                        val bodyHeight = size.height - cap
                        val inset = 2.dp.toPx()
                        val corner = CornerRadius(2.dp.toPx())
                        drawRect(fill.copy(alpha = .35f), Offset(size.width / 3f, 0f), Size(size.width / 3f, cap))
                        drawRoundRect(fill.copy(alpha = .28f), Offset(0f, cap), Size(size.width, bodyHeight), corner)
                        val level = (bodyHeight - inset * 2) * battery.level / 100f
                        if (level > 0f) drawRoundRect(fill, Offset(inset, size.height - inset - level),
                            Size(size.width - inset * 2, level), CornerRadius(1.dp.toPx()))
                    }
                    Column {
                        Text(label, color = textColor.copy(alpha = .75f), fontSize = 12.sp, lineHeight = 14.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${battery.level}%", color = textColor, fontSize = 16.sp, lineHeight = 20.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (battery.status == BatteryStatus.CHARGING || battery.status == BatteryStatus.OPTIMIZED_CHARGING) {
                                Icon(LocalIcons.current.Bolt, stringResource(R.string.charging_label),
                                    Modifier.size(10.dp), tint = textColor)
                            }
                        }
                    }
                }
            }
        }
    }
}
