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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.presentation.theme.BudsStyle

@Suppress("UNUSED_PARAMETER") // One UI places the supporting text below the title.
@Composable
fun StyledListItem(
    modifier: Modifier = Modifier,
    title: String? = null,
    name: String,
    onClick: (() -> Unit)?,
    description: String? = null,
    height: Dp = BudsStyle.RowMinHeight,
    enabled: Boolean = true,
    orientation: ListItemOrientation = ListItemOrientation.Horizontal,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    StyledList(modifier = modifier, title = title) {
        item { _, _ ->
            BudsListRow(name, onClick, description, enabled, height = height,
                leadingContent = leadingContent, trailingContent = trailingContent)
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun StyledListScope.StyledListItem(
    modifier: Modifier = Modifier,
    name: String,
    onClick: (() -> Unit)? = null,
    description: String? = null,
    enabled: Boolean = onClick != null,
    orientation: ListItemOrientation = ListItemOrientation.Horizontal,
    selected: Boolean? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    item { index, count ->
        BudsListRow(name, onClick, description, enabled, modifier, selected = selected,
            leadingContent = leadingContent, trailingContent = trailingContent)
        if (index + 1 < count) BudsRowDivider()
    }
}

enum class ListItemOrientation { Horizontal, Vertical }

@Composable
internal fun BudsRowDivider() {
    HorizontalDivider(Modifier.padding(horizontal = BudsStyle.DividerInset),
        thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun BudsListRow(
    name: String,
    onClick: (() -> Unit)?,
    description: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = BudsStyle.RowMinHeight,
    selected: Boolean? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val canClick = onClick != null
    Row(modifier.fillMaxWidth().heightIn(min = maxOf(height, BudsStyle.RowMinHeight))
        .then(if (canClick) Modifier.clickable(enabled = enabled,
            role = if (selected != null) Role.RadioButton else Role.Button,
            onClick = onClick) else Modifier)
        .then(if (selected != null) Modifier.semantics { this.selected = selected } else Modifier)
        .padding(horizontal = BudsStyle.RowInset),
        verticalAlignment = Alignment.CenterVertically) {
        leadingContent?.let { it(); Spacer(Modifier.width(18.dp)) }
        Column(Modifier.weight(1f).heightIn(min = BudsStyle.RowMinHeight)
            .padding(vertical = BudsStyle.RowVerticalPadding)) {
            Text(name, style = BudsStyle.RowTitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled || !canClick) 1f else .4f))
            description?.let {
                Text(it, style = BudsStyle.RowSummary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled || !canClick) 1f else .4f))
            }
        }
        if (trailingContent != null) {
            Spacer(Modifier.width(12.dp)); trailingContent()
        } else if (selected == true) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
