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
import androidx.compose.material3.VerticalDivider
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
    descriptionIsState: Boolean = false,
    height: Dp = BudsStyle.RowMinHeight,
    enabled: Boolean = true,
    orientation: ListItemOrientation = ListItemOrientation.Horizontal,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    homeMenu: Boolean = false,
    trailingDivider: Boolean = false,
) {
    StyledList(modifier = modifier, title = title) {
        item { _, _ ->
            BudsListRow(name, onClick, description, enabled, height = height,
                accentDescription = descriptionIsState,
                leadingContent = leadingContent, trailingContent = trailingContent,
                homeMenu = homeMenu, trailingDivider = trailingDivider)
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
    descriptionIsState: Boolean = false,
    enabled: Boolean = onClick != null,
    orientation: ListItemOrientation = ListItemOrientation.Horizontal,
    selected: Boolean? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    homeMenu: Boolean = false,
    trailingDivider: Boolean = false,
) {
    item { index, count ->
        BudsListRow(name, onClick, description, enabled, modifier, selected = selected,
            accentDescription = descriptionIsState,
            leadingContent = leadingContent, trailingContent = trailingContent,
            homeMenu = homeMenu, trailingDivider = trailingDivider)
        if (index + 1 < count) BudsRowDivider(
            startInset = if (homeMenu && leadingContent != null) BudsStyle.HomeMenuDividerInset
            else BudsStyle.DividerInset)
    }
}

enum class ListItemOrientation { Horizontal, Vertical }

@Composable
internal fun BudsRowDivider(startInset: Dp = BudsStyle.DividerInset) {
    HorizontalDivider(Modifier.padding(start = startInset, end = BudsStyle.DividerInset),
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
    accentDescription: Boolean = false,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    homeMenu: Boolean = false,
    trailingDivider: Boolean = false,
) {
    val canClick = onClick != null
    // The Home menu wraps its 28dp content in 14dp vertical padding. Rounding
    // these three pieces separately differs from rounding one 56dp minimum.
    val rowPadding = if (homeMenu) Modifier.padding(horizontal = BudsStyle.RowInset,
        vertical = BudsStyle.HomeMenuVerticalPadding)
    else Modifier.padding(horizontal = BudsStyle.RowInset)
    val textPadding = if (homeMenu) Modifier else Modifier.padding(vertical = BudsStyle.RowVerticalPadding)
    Row(modifier.fillMaxWidth().heightIn(min = maxOf(height, BudsStyle.RowMinHeight))
        .then(if (canClick) Modifier.clickable(enabled = enabled,
            role = if (selected != null) Role.RadioButton else Role.Button,
            onClick = onClick) else Modifier)
        .then(if (selected != null) Modifier.semantics { this.selected = selected } else Modifier)
        .then(rowPadding),
        verticalAlignment = Alignment.CenterVertically) {
        leadingContent?.let { it(); Spacer(Modifier.width(18.dp)) }
        Column(Modifier.weight(1f).heightIn(min = BudsStyle.RowMinHeight).then(textPadding),
            verticalArrangement = if (homeMenu) Arrangement.Center else Arrangement.Top) {
            Text(name, style = BudsStyle.RowTitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled || !canClick) 1f else .4f))
            description?.let {
                Text(it, style = BudsStyle.RowSummary, color = BudsStyle.summaryColor(accentDescription, canClick, enabled))
            }
        }
        if (trailingContent != null) {
            if (trailingDivider) {
                // The Home gesture switch uses the same 22dp rule and 16dp gap as a
                // grouped switch row; reserve that space inside the trailing slot.
                val outline = MaterialTheme.colorScheme.outlineVariant
                VerticalDivider(Modifier.height(22.dp), thickness = 1.dp,
                    color = outline.copy(alpha = outline.alpha *
                        (if (enabled || !canClick) 1f else .4f)))
                Spacer(Modifier.width(16.dp))
            } else Spacer(Modifier.width(12.dp))
            trailingContent()
        } else if (selected == true) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
