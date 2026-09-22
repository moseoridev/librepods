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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.theme.BudsStyle

@Composable
fun StyledToggle(
    title: String? = null,
    label: String,
    description: String? = null,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    header: Boolean = false
) {
    if (header) {
        Column {
            title?.let { BudsSectionLabel(it) }
            Row(Modifier.fillMaxWidth()
                .background(if (checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface, BudsStyle.GroupShape)
                .clip(BudsStyle.GroupShape)
                .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(horizontal = BudsStyle.RowInset), verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f).padding(vertical = 16.5.dp),
                    style = BudsStyle.RowTitle.copy(fontWeight = FontWeight.SemiBold),
                    color = (if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        .copy(alpha = if (enabled) 1f else .4f))
                Box(Modifier.padding(start = 12.dp, top = 17.dp, bottom = 17.dp)) {
                    BudsSwitch(checked, enabled)
                }
            }
            description?.let { Text(it, style = BudsStyle.RowSummary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = BudsStyle.RowInset).padding(top = BudsStyle.FooterTop)) }
        }
    } else {
        StyledList(title = title) { StyledToggle(label, description, checked, enabled, onCheckedChange) }
    }
}

@Composable
fun StyledListScope.StyledToggle(
    label: String,
    description: String? = null,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    item { index, count ->
        Row(Modifier.fillMaxWidth().heightIn(min = BudsStyle.RowMinHeight)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = BudsStyle.RowInset),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).heightIn(min = BudsStyle.RowMinHeight)
                .padding(vertical = BudsStyle.RowVerticalPadding)) {
                Text(label, style = BudsStyle.RowTitle,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .4f))
                description?.let {
                    Text(it, style = BudsStyle.RowSummary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else .4f))
                }
            }
            Spacer(Modifier.width(16.dp))
            BudsSwitch(checked = checked, enabled = enabled)
        }
        if (index + 1 < count) BudsRowDivider()
    }
}
