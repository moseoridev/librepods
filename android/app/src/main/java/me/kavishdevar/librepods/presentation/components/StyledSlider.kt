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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers.GREEN_DOMINATED_EXAMPLE
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import kotlin.math.abs

@SuppressLint("UnrememberedMutableState")
@Composable
fun StyledSlider(
    label: String? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    backdrop: Backdrop = rememberLayerBackdrop(),
    snapPoints: List<Float> = emptyList(),
    snapThreshold: Float = 0.05f,
    startIcon: String? = null,
    endIcon: String? = null,
    startLabel: String? = null,
    endLabel: String? = null,
    independent: Boolean = false,
    description: String? = null,
    enabled: Boolean = true,
    index: Int = 0,
    count: Int = 1
) {
    val defaultShape = when {
        count == 1 -> RoundedCornerShape(24.dp)

        index == 0 -> RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 8.dp,
            bottomEnd = 8.dp
        )

        index == count - 1 -> RoundedCornerShape(
            topStart = 8.dp,
            topEnd = 8.dp,
            bottomStart = 24.dp,
            bottomEnd = 24.dp
        )

        else -> RoundedCornerShape(8.dp)
    }

    Column {
        label?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmallEmphasized,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 12.dp)
            )
        }
        SegmentedListItem(
            shapes = ListItemDefaults.shapes().copy(
                shape = defaultShape,
                pressedShape = RoundedCornerShape(24.dp),
                selectedShape = RoundedCornerShape(24.dp),
                hoveredShape = RoundedCornerShape(24.dp),
            ),
            onClick = {},
            enabled = enabled,
            modifier = Modifier.heightIn(min = 58.dp),
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (startLabel != null || endLabel != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            startLabel?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            endLabel?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            },
            supportingContent = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        startIcon?.let {
                            Text(it, fontFamily = FontFamily(Font(R.font.sf_pro)))
                            Spacer(Modifier.width(12.dp))
                        }

                        Slider(
                            modifier = Modifier.weight(1f),
                            value = value,
                            onValueChange = { newValue ->
                                val snapped =
                                    if (snapPoints.isNotEmpty()) {
                                        snapIfClose(
                                            newValue,
                                            snapPoints,
                                            snapThreshold
                                        )
                                    } else {
                                        newValue
                                    }

                                onValueChange(snapped)
                            },
                            valueRange = valueRange,
                            enabled = enabled
                        )

                        endIcon?.let {
                            Spacer(Modifier.width(12.dp))
                            Text(it, fontFamily = FontFamily(Font(R.font.sf_pro)))
                        }
                    }
                }
            }
        )

        if (index + 1 != count) {
            Spacer(
                modifier = Modifier.height(2.dp)
            )
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
