/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.presentation.components

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers.GREEN_DOMINATED_EXAMPLE
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.theme.BudsStyle
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import kotlin.math.abs

@SuppressLint("UnrememberedMutableState")
@Composable
fun StyledSlider(
    label: String? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    snapPoints: List<Float> = emptyList(),
    snapThreshold: Float = 0.05f,
    startIcon: String? = null,
    endIcon: String? = null,
    startLabel: String? = null,
    endLabel: String? = null,
    @Suppress("UNUSED_PARAMETER") // Upstream callers retain their signatures; grouping is by card.
    independent: Boolean = false,
    description: String? = null,
    enabled: Boolean = true
) {
    val alpha = if (enabled) 1f else .4f
    StyledList(title = label) {
        item { _, _ ->
            Column(Modifier.fillMaxWidth()
                .padding(horizontal = BudsStyle.RowInset)
                .padding(vertical = BudsStyle.RowVerticalPadding)) {
                description?.let {
                    Text(it, style = BudsStyle.RowSummary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                }

                if (startLabel != null || endLabel != null) {
                    Row(Modifier.fillMaxWidth()) {
                        startLabel?.let {
                            Text(it, style = BudsStyle.RowSummary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                        }

                        Spacer(Modifier.weight(1f))

                        endLabel?.let {
                            Text(it, style = BudsStyle.RowSummary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    startIcon?.let {
                        Text(it, fontFamily = FontFamily(Font(R.font.sf_pro)),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                        Spacer(Modifier.width(12.dp))
                    }

                    BudsSeekBar(
                        value = value,
                        onValueChange = { newValue ->
                            onValueChange(
                                if (snapPoints.isNotEmpty()) {
                                    snapIfClose(newValue, snapPoints, snapThreshold)
                                } else {
                                    newValue
                                }
                            )
                        },
                        valueRange = valueRange,
                        modifier = Modifier.weight(1f),
                        enabled = enabled
                    )

                    endIcon?.let {
                        Spacer(Modifier.width(12.dp))
                        Text(it, fontFamily = FontFamily(Font(R.font.sf_pro)),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                    }
                }
            }
        }
    }
}

private fun snapIfClose(value: Float, points: List<Float>, threshold: Float = 0.05f): Float {
    val nearest = points.minByOrNull { abs(it - value) } ?: value
    return if (abs(nearest - value) <= threshold) nearest else value
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, wallpaper = GREEN_DOMINATED_EXAMPLE)
@Composable
fun StyledSliderPreview() {
    val a = remember { mutableFloatStateOf(0.5f) }
    LibrePodsTheme() {
        StyledScaffold(
            title = "test",
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(72.dp))
                StyledSlider(
                    value = a.floatValue,
                    onValueChange = {
                        a.floatValue = it
                    },
                    valueRange = 0f..2f,
                    snapPoints = listOf(1f),
                    snapThreshold = 0.1f,
                    independent = true,
                    startIcon = "A",
                    endIcon = "B",
                )
                StyledSlider(
                    label = "Small label",
                    description = "This is a somewhat long descriptionRes",
                    value = a.floatValue,
                    onValueChange = {
                        a.floatValue = it
                    },
                    valueRange = 0f..2f,
                    snapPoints = listOf(1f),
                    snapThreshold = 0.1f,
                    independent = true,
                    startIcon = "A",
                    endIcon = "B",
                )
            }
        }
    }
}
