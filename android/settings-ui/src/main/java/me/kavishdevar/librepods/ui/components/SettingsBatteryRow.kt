package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Source battery layout: one primary group and an optional secondary meter.
 * The caller chooses combined, separate or missing readings and supplies content.
 * No battery normalization, charging policy or artwork belongs to this row.
 */
@Composable
fun SettingsBatteryRow(
    primaryMeters: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    secondaryMeter: (@Composable () -> Unit)? = null,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Row(Modifier.widthIn(max = 280.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Row(Modifier.weight(primaryMeters.size.coerceAtLeast(1).toFloat()),
                horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Top) {
                primaryMeters.forEach { meter ->
                    // Preserve the weighted width through the slot. Wrapping a
                    // natural-width column changes independent ring/label rounding.
                    Box(Modifier.weight(1f), propagateMinConstraints = true) { meter() }
                }
            }
            if (secondaryMeter != null) Row(Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                Spacer(Modifier.width(3.dp))
                Box(Modifier.weight(1f), propagateMinConstraints = true) { secondaryMeter() }
            }
        }
    }
}
