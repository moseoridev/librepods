package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt
import me.kavishdevar.librepods.ui.theme.SettingsStyle

/** Labels are supplied by the caller; no preset, device or protocol is part of this module. */
data class SettingsEqualizerBand(val label: String, val value: Int, val contentDescription: String)

/**
 * om.h.q / a1.b1 case 14 supplies the graph geometry and default signed gain labels.
 * Callers own the numeric domain: LibrePods uses 0..100 without reducing its steps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsEqualizer(bands: List<SettingsEqualizerBand>, onValueChange: (index: Int, value: Int) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    valueRange: IntRange = -10..10,
    valueLabel: (Int) -> String = { if (it > 0) "+$it" else "$it" },
) {
    require(!valueRange.isEmpty()) { "valueRange must not be empty" }
    if (bands.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    // The source divides 13sp by fontScale; the band labels are effectively 13dp.
    CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
        Row(modifier.fillMaxWidth().height(350.dp).padding(start = 32.dp, end = 32.dp, top = 19.dp, bottom = 26.dp)) {
            bands.forEachIndexed { index, band ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val value = band.value.coerceIn(valueRange)
                    // mq.d.c.d inherits the optional device family through xw.e / fx.b.
                    val labelStyle = SettingsStyle.RowSummary.copy(fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF929295))
                    Text(valueLabel(value), style = labelStyle, maxLines = 1)
                    Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
                        Column(Modifier.fillMaxSize().padding(vertical = 6.dp)) {
                            HorizontalDivider(thickness = 1.dp, color = colors.secondaryContainer)
                            repeat(10) {
                                Spacer(Modifier.weight(1f))
                                HorizontalDivider(thickness = 1.dp, color = colors.secondaryContainer)
                            }
                        }
                        SettingsVerticalSeekBar(value.toFloat(), onValueChange = {
                            val next = it.roundToInt().coerceIn(valueRange)
                            if (next != value) onValueChange(index, next)
                        }, valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                            steps = (valueRange.last.toLong() - valueRange.first - 1).coerceAtLeast(0).toInt(),
                            enabled = enabled, label = band.contentDescription,
                            modifier = Modifier.fillMaxHeight())
                    }
                    Text(band.label, style = labelStyle, modifier = Modifier.padding(1.dp), maxLines = 1)
                }
            }
        }
    }
}
