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

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop

enum class MaterialButtonStyle { Tonal, Normal, Outlined, Filled }

@Suppress("UNUSED_PARAMETER") // Upstream callers retain their signatures; no glass rendering remains.
@Composable
fun StyledButton(
    onClick: () -> Unit,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    maxScale: Float = 0.1f,
    enabled: Boolean = true,
    materialButtonStyle: MaterialButtonStyle = MaterialButtonStyle.Tonal,
    content: @Composable RowScope.() -> Unit,
) {
    val buttonModifier = modifier.heightIn(min = 48.dp)
    when (materialButtonStyle) {
        MaterialButtonStyle.Filled -> Button(onClick, modifier = buttonModifier, enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = surfaceColor, contentColor = tint), content = content)
        MaterialButtonStyle.Tonal -> FilledTonalButton(onClick, modifier = buttonModifier, enabled = enabled,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = surfaceColor, contentColor = tint), content = content)
        MaterialButtonStyle.Outlined -> OutlinedButton(onClick, modifier = buttonModifier, enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(containerColor = surfaceColor, contentColor = tint), content = content)
        MaterialButtonStyle.Normal -> TextButton(onClick, modifier = buttonModifier, enabled = enabled,
            colors = ButtonDefaults.textButtonColors(containerColor = surfaceColor, contentColor = tint), content = content)
    }
}
