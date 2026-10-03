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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.SettingsStyle

@Composable
fun SettingsListItem(
    modifier: Modifier = Modifier,
    title: String? = null,
    name: String,
    onClick: (() -> Unit)?,
    description: String? = null,
    descriptionIsState: Boolean = false,
    height: Dp = SettingsStyle.RowMinHeight,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    homeMenu: Boolean = false,
    trailingDivider: Boolean = false,
) {
    SettingsList(modifier = modifier, title = title) {
        item { _, _ ->
            SettingsListRow(name, onClick, description, enabled, height = height,
                accentDescription = descriptionIsState,
                leadingContent = leadingContent, trailingContent = trailingContent,
                homeMenu = homeMenu, trailingDivider = trailingDivider)
        }
    }
}

@Composable
fun SettingsListScope.SettingsListItem(
    modifier: Modifier = Modifier,
    name: String,
    onClick: (() -> Unit)? = null,
    description: String? = null,
    descriptionIsState: Boolean = false,
    enabled: Boolean = onClick != null,
    selected: Boolean? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    homeMenu: Boolean = false,
    trailingDivider: Boolean = false,
) {
    item { index, count ->
        SettingsListRow(name, onClick, description, enabled, modifier, selected = selected,
            accentDescription = descriptionIsState,
            leadingContent = leadingContent, trailingContent = trailingContent,
            homeMenu = homeMenu, trailingDivider = trailingDivider)
        if (index + 1 < count) SettingsRowDivider(
            startInset = if (homeMenu && leadingContent != null) SettingsStyle.HomeMenuDividerInset
            else SettingsStyle.DividerInset)
    }
}


@Composable
fun SettingsRowDivider(startInset: Dp = SettingsStyle.DividerInset) {
    HorizontalDivider(Modifier.padding(start = startInset, end = SettingsStyle.DividerInset),
        thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun SettingsListRow(
    name: String,
    onClick: (() -> Unit)?,
    description: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = SettingsStyle.RowMinHeight,
    selected: Boolean? = null,
    accentDescription: Boolean = false,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    homeMenu: Boolean = false,
    trailingDivider: Boolean = false,
) {
    val canClick = onClick != null
    val interactionSource = remember { MutableInteractionSource() }
    Row(modifier.fillMaxWidth().heightIn(min = maxOf(height, SettingsStyle.RowMinHeight))
        .then(if (canClick) Modifier.clickable(enabled = enabled,
            interactionSource = interactionSource, indication = null,
            role = if (selected != null) Role.RadioButton else Role.Button,
            onClick = onClick) else Modifier)
        .then(settingsRowFeedback(canClick && enabled, interactionSource))
        .then(if (selected != null) Modifier.semantics { this.selected = selected } else Modifier)
        // wm.k1.c dims the completed row, including artwork and trailing content.
        // Read-only content has no unavailable action and retains its full strength.
        .graphicsLayer { alpha = if (canClick && !enabled) .4f else 1f }
        .padding(horizontal = SettingsStyle.RowInset),
        verticalAlignment = Alignment.CenterVertically) {
        leadingContent?.let { it(); Spacer(Modifier.width(18.dp)) }
        // wm.k1.d applies the 28dp minimum before padding the text column by 14dp.
        // An icon row shares this structure; putting that padding around the entire
        // row incorrectly grows a one-line Home item and also pads its artwork.
        Column(Modifier.weight(1f).heightIn(min = SettingsStyle.RowMinHeight)
            .padding(vertical = SettingsStyle.RowVerticalPadding)) {
            Text(name, style = SettingsStyle.RowTitle,
                color = MaterialTheme.colorScheme.onSurface)
            description?.let {
                Text(it, style = SettingsStyle.RowSummary, color = SettingsStyle.summaryColor(accentDescription, canClick, enabled = true))
            }
        }
        if (trailingContent != null) {
            if (trailingDivider) {
                // wm.y0: a 22dp rule followed by one separately measured 8dp gap.
                val outline = MaterialTheme.colorScheme.outlineVariant
                VerticalDivider(Modifier.height(22.dp), thickness = 1.dp,
                    color = outline)
                Spacer(Modifier.width(8.dp))
            } else Spacer(Modifier.width(12.dp))
            trailingContent()
        } else if (selected == true) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
