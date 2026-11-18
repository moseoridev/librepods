package me.kavishdevar.librepods.presentation.screens.apple

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.icons.LocalIcons
import me.kavishdevar.librepods.ui.theme.SettingsStyle
import kotlin.math.roundToInt

/** The selected Adaptive segment expands between the other modes, as in the Apple control. */
@Composable
internal fun BudsAdaptiveModeStrip(
    modes: List<Int>,
    strength: Int,
    enabled: Boolean,
    darkTheme: Boolean,
    onSelect: (Int) -> Unit,
    onStrengthChange: (Int) -> Unit,
) {
    val track = if (darkTheme) Color(0xFF3E3E3E) else Color(0xFFEDEDED)
    val captionStyle = SettingsStyle.RowTitle.copy(fontSize = 12.sp)
    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 20.dp), Alignment.TopCenter) {
        Row(Modifier.widthIn(max = 392.dp).fillMaxWidth()) {
            // The expanded Adaptive control occupies the space of the Off option.
            for (mode in modes) {
                if (mode == 1) continue
                val label = modeLabel(mode)
                if (mode == 4) {
                    Column(Modifier.weight(3f), horizontalAlignment = Alignment.CenterHorizontally) {
                        AdaptiveNoiseSlider(strength, enabled, track, onStrengthChange)
                        Text(label, Modifier.padding(top = 8.dp).clearAndSetSemantics {},
                            style = captionStyle, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    }
                } else {
                    Column(
                        Modifier.weight(1f).selectable(
                            selected = false, enabled = enabled, role = Role.RadioButton,
                            interactionSource = remember { MutableInteractionSource() }, indication = null,
                            onClick = { onSelect(mode) },
                        ).semantics { contentDescription = label },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(44.dp).background(track, CircleShape),
                            contentAlignment = Alignment.Center) { BudsModeArtwork(mode, false) }
                        Text(label, Modifier.padding(top = 8.dp).clearAndSetSemantics {},
                            style = captionStyle, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

/** The active mode is the slider thumb. The end marks show the range of noise cancellation. */
@Composable
private fun AdaptiveNoiseSlider(
    strength: Int,
    enabled: Boolean,
    trackColor: Color,
    onStrengthChange: (Int) -> Unit,
) {
    val label = stringResource(R.string.customize_adaptive_audio)
    val colors = MaterialTheme.colorScheme
    val icons = LocalIcons.current
    val sliderState = remember { SliderState(strength.toFloat(), trackRange = 0f..100f) }
    sliderState.value = strength.toFloat()
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(44.dp)
            .then(if (enabled) Modifier else Modifier.graphicsLayer { alpha = .4f })
            .background(trackColor, SettingsStyle.PillShape),
    ) {
        Row(Modifier.matchParentSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icons.CircleDotted, contentDescription = null, modifier = Modifier.size(16.dp),
                tint = colors.onSurface.copy(alpha = .7f))
            Icon(icons.Circle, contentDescription = null, modifier = Modifier.size(16.dp),
                tint = colors.onSurface.copy(alpha = .7f))
        }
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Slider(
                state = sliderState,
                onValueChange = { if (enabled) onStrengthChange(it.roundToInt()) },
                enabled = enabled,
                modifier = Modifier.matchParentSize().semantics { contentDescription = label },
                thumb = {
                    Box(Modifier.size(40.dp).background(colors.primary, CircleShape),
                        contentAlignment = Alignment.Center) {
                        BudsModeArtwork(mode = 4, selected = true, compact = true)
                    }
                },
                track = { Box(Modifier.fillMaxWidth().height(44.dp)) },
            )
        }
    }
}
