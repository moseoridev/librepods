package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Accessible switch. A compact control keeps its natural artwork size inside a row
 * with a separate action; toggleable still supplies the platform's expanded touch region.
 * Ordinary grouped toggles use SettingsToggle's single row action.
 */
@Composable
fun SettingsSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    contentDescription: String, modifier: Modifier = Modifier, enabled: Boolean = true,
    compact: Boolean = false) {
    Box(modifier.then(if (compact) Modifier else Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp))
        .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
        .semantics { this.contentDescription = contentDescription }, contentAlignment = Alignment.Center) {
        // The surrounding menu row applies its own disabled alpha once.
        SettingsSwitchVisual(checked, enabled, dimControl = !compact)
    }
}
