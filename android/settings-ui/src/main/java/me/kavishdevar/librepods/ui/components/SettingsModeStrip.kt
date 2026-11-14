package me.kavishdevar.librepods.ui.components

import me.kavishdevar.librepods.ui.theme.settingsIsDarkTheme
import android.graphics.drawable.GradientDrawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import me.kavishdevar.librepods.ui.theme.SettingsStyle

/** Source-derived mode strip. The caller supplies stable values, labels, artwork and state. */
@Composable
fun <T> SettingsModeStrip(
    modes: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    artwork: @Composable (T, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    stripWidth: Dp? = null,
    captionWidth: Dp? = null,
) {
    if (modes.isEmpty()) return
    val track = if (settingsIsDarkTheme()) Color(0xFF3E3E3E) else Color(0xFFEDEDED)
    val tick = remember(track) { GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(track.toArgb()) } }
    val selectedIndex = modes.indexOf(selected)
    var previousIndex by remember(modes) { mutableIntStateOf(selectedIndex) }
    var movingFrom by remember(modes) { mutableIntStateOf(selectedIndex) }
    val movement = remember(modes) { Animatable(1f) }
    LaunchedEffect(selectedIndex, modes) {
        movingFrom = previousIndex
        previousIndex = selectedIndex
        if (movingFrom in modes.indices && selectedIndex in modes.indices && movingFrom != selectedIndex) {
            // NoiseControlSeekBar.setProgressWithAnimation; interrupted moves restart
            // at the previous target, just as the source's current-index field does.
            movement.snapTo(0f)
            movement.animateTo(1f, tween(500, easing = CubicBezierEasing(.33f, 0f, .67f, 1f)))
        } else movement.snapTo(1f)
    }
    val movingColor = MaterialTheme.colorScheme.primary
    // l1.j case 2 converts the default 10dp page inset to 20dp for this strip.
    val pageInset = SettingsStyle.pageInset()
    val stripInset = if (pageInset == 10.dp) 20.dp else pageInset
    BoxWithConstraints(modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.TopStart) {
        val density = LocalDensity.current
        val capStripWidth = LocalConfiguration.current.screenWidthDp >= 589
        // Measure the inset from the screen edge before subtracting the caller's rounded
        // page padding. The positive combined anchor rounds half pixels upward: at 360/600dpi
        // the native first disc starts at x91/x151, not x90/x150.
        val sideInsetPx = with(density) { (pageInset + stripInset).roundToPx() } -
            with(density) { pageInset.roundToPx() }
        val availableWidthPx = constraints.maxWidth - sideInsetPx * 2
        // bm.t / rl.r1.a cap the outer strip at 412dp from screenWidthDp 589 upward.
        val widthPx = with(density) {
            (stripWidth?.roundToPx() ?: if (capStripWidth) {
                minOf(availableWidthPx, 412.dp.roundToPx())
            } else availableWidthPx).coerceAtLeast(0)
        }
        val width = with(density) { widthPx.toDp() }
        val fixedStrip = stripWidth != null
        // bm.t passes the strip's inner width to rl.r1.c; rl.q1 truncates its 20%
        // caption width to integer dp. This is independent of the display's width.
        val captionBasis = if (fixedStrip) width.value else (width.value - 20f).coerceAtLeast(0f)
        val measuredCaptionWidth = captionWidth ?: minOf(captionBasis * .2f, 80f).toInt().dp
        Layout(modifier = Modifier.fillMaxWidth(), content = {
            // rl.q1/rl.h1/rl.l1 use ConstraintLayout anchors for both the disc and
            // fixed-width Text. Retain that solver's measure/place boundaries, including
            // negative first-caption coordinates at fractional display densities.
            ConstraintLayout(Modifier.width(width).padding(horizontal = 10.dp).selectableGroup()) {
                val options = modes.indices.map { createRef() to createRef() }
                val background = createRef()
                Canvas(Modifier.fillMaxWidth().height(44.dp).constrainAs(background) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                }) {
                    // The native background reads the tick drawable's intrinsic 44dp width in
                    // integer pixels, then halves it for the first/last centres and oval bounds.
                    // At density 2.8125 that is 124 / 2, rather than 22dp = 61.875px.
                    val radius = 44.dp.roundToPx() / 2f
                    if (modes.size > 1) drawLine(track, Offset(radius, size.height / 2),
                        Offset(size.width - radius, size.height / 2), strokeWidth = 6.dp.toPx())
                    // The native track draws oval ticks underneath the Compose option backgrounds.
                    drawIntoCanvas { canvas ->
                        modes.indices.forEach { index ->
                            val center = if (modes.size == 1) size.width / 2
                                else radius + (size.width - 2 * radius) * index / (modes.size - 1)
                            // Current native onDraw rounds the start once and adds the
                            // intrinsic diameter, rather than truncating both edges.
                            val left = (center - radius).roundToInt()
                            val top = (size.height / 2f - radius).roundToInt()
                            tick.setBounds(left, top, left + 44.dp.roundToPx(), top + 44.dp.roundToPx())
                            tick.draw(canvas.nativeCanvas)
                        }
                    }
                    if (modes.size > 1 && movingFrom in modes.indices && selectedIndex in modes.indices &&
                        movingFrom != selectedIndex && movement.value < 1f) {
                        val step = (size.width - 2 * radius) / (modes.size - 1)
                        val direction = if (selectedIndex > movingFrom) 1f else -1f
                        val start = radius + step * movingFrom + direction * radius
                        val end = radius + step * selectedIndex - direction * radius
                        val distance = kotlin.math.abs(end - start)
                        val travelDirection = if (end >= start) 1f else -1f
                        val maximumLength = 2 * distance / 3
                        val travelled = (distance + maximumLength) * movement.value
                        val head = start + travelDirection * minOf(travelled, distance)
                        val length = if (travelled >= distance) maximumLength - (travelled - distance)
                            else minOf(travelled, maximumLength)
                        drawLine(movingColor, Offset(head - travelDirection * length, size.height / 2),
                            Offset(head, size.height / 2), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
                modes.forEachIndexed { index, mode ->
                    val isSelected = mode == selected
                    val captionLabel = label(mode)
                    // The source constrains artwork and its fixed-width Text independently.
                    val (disc, caption) = options[index]
                    Box(Modifier.size(44.dp).constrainAs(disc) {
                        when {
                            modes.size == 1 -> {
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                            }
                            index == 0 -> start.linkTo(parent.start)
                            index == modes.lastIndex -> end.linkTo(parent.end)
                            else -> {
                                start.linkTo(options[index - 1].first.end)
                                end.linkTo(options[index + 1].first.start)
                            }
                        }
                        top.linkTo(background.top)
                        bottom.linkTo(background.bottom)
                    }
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else track, CircleShape)
                        .clearAndSetSemantics {},
                        contentAlignment = Alignment.Center) { artwork(mode, isSelected) }
                    // rl.q1 sets width on Text itself. A loose Text inside a centered Box
                    // rounds its intrinsic width and alignment separately, changing glyph edges.
                    // Preserve the single 52dp top inset instead of rounding 44+8 separately.
                    Text(captionLabel, modifier = Modifier.width(measuredCaptionWidth)
                        .padding(top = 52.dp).constrainAs(caption) {
                            start.linkTo(disc.start)
                            end.linkTo(disc.end)
                            top.linkTo(disc.top)
                        }.clearAndSetSemantics {},
                        style = SettingsStyle.RowTitle.copy(fontSize = 12.sp),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center)
                    // One option owns both artwork and caption input, without changing their
                    // independently measured native anchors or adding duplicate talkback actions.
                    val hitArea = createRef()
                    Box(Modifier.width(maxOf(measuredCaptionWidth, 44.dp)).constrainAs(hitArea) {
                        start.linkTo(disc.start)
                        end.linkTo(disc.end)
                        top.linkTo(disc.top)
                        bottom.linkTo(caption.bottom)
                        height = Dimension.fillToConstraints
                    // rl.q1 passes a null indication for both the disc and caption.
                    // Keep selection/focus input without a rectangular default ripple.
                    }.selectable(isSelected,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, role = Role.RadioButton,
                        onClick = { if (!isSelected) onSelect(mode) })
                        .semantics { contentDescription = captionLabel })
                }
            }
        }) { measurables, constraints ->
            val strip = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
            layout(constraints.maxWidth, strip.height) {
                val left = if (stripWidth == null && widthPx == availableWidthPx) sideInsetPx
                    else ((constraints.maxWidth - strip.width) / 2f).roundToInt()
                strip.placeRelative(left, 0)
            }
        }
    }
}
