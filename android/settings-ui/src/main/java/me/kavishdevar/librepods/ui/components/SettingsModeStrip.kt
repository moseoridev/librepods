package me.kavishdevar.librepods.ui.components

import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
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
    val track = if (MaterialTheme.colorScheme.onSurface != Color(0xFF010102)) Color(0xFF3E3E3E) else Color(0xFFEDEDED)
    val tick = remember(track) { GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(track.toArgb()) } }
    // l1.j case 2 converts the default 10dp page inset to 20dp for this strip.
    val pageInset = SettingsStyle.pageInset()
    val stripInset = if (pageInset == 10.dp) 20.dp else pageInset
    BoxWithConstraints(modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.TopStart) {
        val density = LocalDensity.current
        // The source constrains each side separately. Its negative anchor values use
        // Math.round, so a half-pixel inset resolves toward the start at 600 dpi.
        val sideInsetPx = -with(density) { (-stripInset.toPx()).roundToInt() }
        val availableWidthPx = constraints.maxWidth - sideInsetPx * 2
        val widthPx = with(density) {
            (stripWidth?.roundToPx() ?: minOf(availableWidthPx, 392.dp.roundToPx())).coerceAtLeast(0)
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
                    }.selectable(isSelected, role = Role.RadioButton,
                        onClick = { if (!isSelected) onSelect(mode) })
                        .semantics { contentDescription = captionLabel })
                }
            }
        }) { measurables, constraints ->
            val strip = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
            layout(constraints.maxWidth, strip.height) {
                val left = if (stripWidth == null && widthPx == availableWidthPx) sideInsetPx
                    else (constraints.maxWidth - strip.width) / 2
                strip.placeRelative(left, 0)
            }
        }
    }
}
