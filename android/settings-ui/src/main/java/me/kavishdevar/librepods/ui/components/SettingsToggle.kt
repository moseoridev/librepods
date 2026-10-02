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

package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.SettingsStyle

@Composable
fun SettingsToggle(
    title: String? = null,
    label: String,
    description: String? = null,
    descriptionIsState: Boolean = false,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    header: Boolean = false
) {
    if (header) {
        Column {
            title?.let { SettingsSectionLabel(it) }
            // wm.k1.y → u1.q: a 52dp centered master row. Allow caller text
            // to grow beyond that minimum instead of clipping enlarged labels.
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp)
                .graphicsLayer { alpha = if (enabled) 1f else .4f }
                .background(if (checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface, SettingsStyle.GroupShape)
                .clip(SettingsStyle.GroupShape)
                .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(horizontal = SettingsStyle.RowInset), verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f),
                    style = SettingsStyle.RowTitle.copy(fontWeight = FontWeight.SemiBold),
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                // Each native 17.5/5.5dp inset rounds once. The complete disabled
                // card fades once; it retains enabled switch colors underneath.
                SettingsSwitchVisual(checked, enabled = true, dimControl = false, startInset = 17.5.dp)
            }
            description?.let { Text(it, style = SettingsStyle.RowSummary,
                color = SettingsStyle.summaryColor(descriptionIsState, canClick = true, enabled = enabled),
                modifier = Modifier.padding(horizontal = SettingsStyle.RowInset).padding(top = SettingsStyle.FooterTop)) }
        }
    } else {
        SettingsList(title = title) { SettingsToggle(label, description, descriptionIsState, checked, enabled, onCheckedChange) }
    }
}

@Composable
fun SettingsListScope.SettingsToggle(
    label: String,
    description: String? = null,
    descriptionIsState: Boolean = false,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    trailingDivider: Boolean = false,
    minHeight: Dp = SettingsStyle.RowMinHeight,
    homeMenu: Boolean = false,
) {
    item { index, count ->
        val rowPadding = if (homeMenu) Modifier.padding(horizontal = SettingsStyle.RowInset,
            vertical = SettingsStyle.HomeMenuVerticalPadding)
        else Modifier.padding(horizontal = SettingsStyle.RowInset)
        val textPadding = if (homeMenu) Modifier else Modifier.padding(vertical = SettingsStyle.RowVerticalPadding)
        Row(Modifier.fillMaxWidth().heightIn(min = maxOf(minHeight, SettingsStyle.RowMinHeight))
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            // wm.k1.c dims the finished row once, including the trailing control.
            .graphicsLayer { alpha = if (enabled) 1f else .4f }
            .then(rowPadding),
            verticalAlignment = Alignment.CenterVertically) {
            // wm.k1.c constrains the row, not the natural text inside it.
            Column(Modifier.weight(1f).then(textPadding),
                verticalArrangement = if (homeMenu) Arrangement.Center else Arrangement.Top) {
                Text(label, style = SettingsStyle.RowTitle,
                    color = MaterialTheme.colorScheme.onSurface)
                description?.let {
                    Text(it, style = SettingsStyle.RowSummary, color = SettingsStyle.summaryColor(descriptionIsState, canClick = true, enabled = true))
                }
            }
            // wm.y0 selects its rule by the caller's switch presentation style,
            // independently of checked/enabled state, then adds one 8dp gap.
            if (trailingDivider) {
                val outline = MaterialTheme.colorScheme.outlineVariant
                VerticalDivider(Modifier.height(22.dp), thickness = 1.dp,
                    color = outline)
            }
            Spacer(Modifier.width(8.dp))
            SettingsSwitchVisual(checked = checked, enabled = enabled, dimControl = false)
        }
        if (index + 1 < count) SettingsRowDivider()
    }
}
