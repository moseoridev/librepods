package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.SettingsStyle

/** wm.k1.L/c/d → wm.e0 / kw.t.A: native menu row with a radio leading slot. */
@Composable
fun SettingsListScope.SettingsChoiceRow(name: String, selected: Boolean, onClick: () -> Unit,
    description: String? = null, enabled: Boolean = true, leadingContent: (@Composable () -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    divider: Boolean = true) {
    item { index, count ->
        Row(Modifier.fillMaxWidth().selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = SettingsStyle.RowMinHeight)
            .padding(horizontal = SettingsStyle.RowInset), verticalAlignment = Alignment.CenterVertically) {
            val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Canvas(Modifier.size(32.dp)) {
                val ink = color.copy(alpha = if (enabled) 1f else .4f)
                drawCircle(ink, size.width * .28125f, style = Stroke(size.width * if (selected) .0625f else .046875f))
                if (selected) drawCircle(ink, size.width * .15625f)
            }
            Spacer(Modifier.width(18.dp))
            leadingContent?.let { it(); Spacer(Modifier.width(16.dp)) }
            Column(Modifier.weight(1f).padding(vertical = SettingsStyle.RowVerticalPadding)) {
                Text(name, style = SettingsStyle.RowTitle,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .4f))
                description?.let { Text(it, style = SettingsStyle.RowSummary,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = if (enabled) 1f else .4f)) }
            }
            trailingAction?.let {
                Spacer(Modifier.width(16.dp))
                androidx.compose.material3.VerticalDivider(Modifier.height(22.dp),
                    thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.width(12.dp))
                // The 28dp visual slot must not squeeze an independent 48dp action.
                // Native gear bounds are 28dp; its enabled target extends beyond
                // that slot even when the surrounding choice is disabled.
                Box(Modifier.size(28.dp).wrapContentSize(unbounded = true),
                    contentAlignment = Alignment.Center) { it() }
            }
        }
        if (divider && index + 1 < count) SettingsRowDivider()
    }
}
