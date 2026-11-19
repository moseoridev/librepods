package me.kavishdevar.librepods.presentation.screens.apple

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.icons.LocalIcons
import me.kavishdevar.librepods.ui.components.drawSettingsModeTravel
import me.kavishdevar.librepods.ui.theme.SettingsStyle
import kotlin.math.roundToInt

/** One strip for both states, so the artwork, labels and thumb move with their slots. */
@Composable
internal fun BudsAdaptiveModeStrip(
    modes: List<Int>,
    selected: Int?,
    strength: Int,
    enabled: Boolean,
    darkTheme: Boolean,
    onSelect: (Int) -> Unit,
    onStrengthChange: (Int) -> Unit,
) {
    val expanded = selected == 4
    val expansion = animateFloatAsState(
        if (expanded) 1f else 0f,
        spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = .001f),
        label = "Adaptive slot width",
    )
    val track = if (darkTheme) Color(0xFF3E3E3E) else Color(0xFFEDEDED)
    val accent = MaterialTheme.colorScheme.primary
    val selectedIndex = modes.indexOf(selected)
    var previousIndex by remember(modes) { mutableIntStateOf(selectedIndex) }
    var movingFrom by remember(modes) { mutableIntStateOf(-1) }
    val movement = remember(modes) { Animatable(1f) }
    LaunchedEffect(selectedIndex, modes) {
        movingFrom = previousIndex
        previousIndex = selectedIndex
        if (movingFrom in modes.indices && selectedIndex in modes.indices &&
            movingFrom != selectedIndex && modes[movingFrom] != 4 && !expanded) {
            movement.snapTo(0f)
            movement.animateTo(1f, tween(500, easing = CubicBezierEasing(.33f, 0f, .67f, 1f)))
        } else movement.snapTo(1f)
    }
    val pageInset = SettingsStyle.pageInset()
    val stripInset = if (pageInset == 10.dp) 20.dp else pageInset

    Box(Modifier.fillMaxWidth().padding(horizontal = stripInset, vertical = 16.dp), Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 412.dp).fillMaxWidth().selectableGroup()) {
            val captionWidth = ((maxWidth.value - 20f) * .2f).toInt().coerceIn(44, 80).dp
            Canvas(Modifier.matchParentSize()) {
                val radius = 44.dp.roundToPx() / 2f
                val inset = 10.dp.roundToPx() + radius
                val y = radius
                drawLine(track.copy(alpha = (1f - expansion.value).coerceIn(0f, 1f)),
                    Offset(inset, y), Offset(size.width - inset, y), strokeWidth = 6.dp.toPx())
                if (movingFrom in modes.indices && selectedIndex in modes.indices &&
                    movingFrom != selectedIndex && movement.value < 1f && expansion.value == 0f) {
                    val step = (size.width - 2 * inset) / modes.lastIndex
                    val direction = if (selectedIndex > movingFrom) 1f else -1f
                    val start = inset + step * movingFrom + direction * radius
                    val end = inset + step * selectedIndex - direction * radius
                    val rtl = layoutDirection == LayoutDirection.Rtl
                    drawSettingsModeTravel(accent, if (rtl) size.width - start else start,
                        if (rtl) size.width - end else end, y, movement.value)
                }
            }
            Layout(
                modifier = Modifier.fillMaxWidth(),
                content = {
                    for (mode in modes) {
                        val label = modeLabel(mode)
                        val active = mode == selected
                        val discColor by animateColorAsState(
                            if (active) MaterialTheme.colorScheme.primary else track, label = label
                        )
                        val input = when {
                            mode == 4 && expanded -> Modifier
                            mode == 1 && expanded -> Modifier.clearAndSetSemantics {}
                            else -> Modifier.selectable(
                                selected = active,
                                enabled = enabled,
                                role = Role.RadioButton,
                                interactionSource = null,
                                indication = null,
                                onClick = { if (!active) onSelect(mode) },
                            ).semantics { contentDescription = label }
                        }
                        Column(
                            input.then(if (mode == 1) Modifier.graphicsLayer {
                                alpha = (1f - expansion.value).coerceIn(0f, 1f)
                            } else Modifier),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (mode == 4) {
                                AdaptiveNoiseSlider(
                                    strength, expanded, enabled, active, expansion, track, discColor, onStrengthChange,
                                )
                            } else {
                                Box(Modifier.size(44.dp).background(discColor, CircleShape),
                                    contentAlignment = Alignment.Center) {
                                    BudsModeArtwork(mode, active)
                                }
                            }
                            Text(label, Modifier.requiredWidth(captionWidth).padding(top = 8.dp).clearAndSetSemantics {},
                                style = SettingsStyle.RowTitle.copy(fontSize = 12.sp),
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
            ) { measurables, constraints ->
                val fraction = expansion.value.coerceIn(0f, 1f)
                val width = constraints.maxWidth
                val disc = 44.dp.roundToPx()
                val start = 10.dp.roundToPx() + disc / 2f
                val end = width - start
                val normalCaptionWidth = captionWidth.roundToPx()
                val adaptiveWidth = lerp(disc, (end - start - disc - 8.dp.toPx()).roundToInt().coerceAtLeast(disc), fraction)
                val placeables = measurables.mapIndexed { index, measurable ->
                    val optionWidth = if (modes[index] == 4) adaptiveWidth else normalCaptionWidth
                    measurable.measure(Constraints.fixedWidth(optionWidth.coerceAtMost(width)))
                }
                layout(width, constraints.constrainHeight(placeables.maxOf { it.height })) {
                    placeables.forEachIndexed { index, placeable ->
                        val collapsedCenter = lerp(start, end, index.toFloat() / modes.lastIndex)
                        val expandedCenter = when (modes[index]) {
                            1, 3 -> start
                            2 -> end
                            else -> width / 2f
                        }
                        val center = lerp(collapsedCenter, expandedCenter, fraction)
                        placeable.placeRelative((center - placeable.width / 2f).roundToInt(), 0)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdaptiveNoiseSlider(
    strength: Int,
    expanded: Boolean,
    enabled: Boolean,
    active: Boolean,
    expansion: State<Float>,
    trackColor: Color,
    discColor: Color,
    onStrengthChange: (Int) -> Unit,
) {
    val label = stringResource(R.string.customize_adaptive_audio)
    val icons = LocalIcons.current
    val ink = MaterialTheme.colorScheme.onSurface
    val thumb = remember { SliderState(50f, trackRange = 0f..100f) }
    // The same thumb stays centred when collapsed, then travels along the growing capsule.
    thumb.value = lerp(50f, strength.toFloat(), expansion.value.coerceIn(0f, 1f))
    Box(Modifier.fillMaxWidth().height(44.dp).background(trackColor, SettingsStyle.PillShape)) {
        Row(
            Modifier.matchParentSize().padding(horizontal = 12.dp).graphicsLayer {
                alpha = ((expansion.value - .35f) / .65f).coerceIn(0f, 1f)
            },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icons.CircleDotted, null, Modifier.size(16.dp), tint = ink.copy(alpha = .7f))
            Icon(icons.Circle, null, Modifier.size(16.dp), tint = ink.copy(alpha = .7f))
        }
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Slider(
                state = thumb,
                onValueChange = { if (expanded && enabled) onStrengthChange(it.roundToInt()) },
                enabled = expanded && enabled,
                modifier = Modifier.matchParentSize().then(
                    if (expanded) Modifier.semantics { contentDescription = label }
                    else Modifier.clearAndSetSemantics {}
                ),
                thumb = {
                    Box(Modifier.size(40.dp).background(discColor, CircleShape), Alignment.Center) {
                        BudsModeArtwork(mode = 4, selected = active)
                    }
                },
                track = { Box(Modifier.fillMaxWidth().height(44.dp)) },
            )
        }
    }
}
