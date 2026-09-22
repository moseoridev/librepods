package me.kavishdevar.librepods.presentation.components

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
import me.kavishdevar.librepods.presentation.theme.BudsStyle

/** Buds gm.t1.B row and ku.k2/m2.d radio geometry; no extracted drawable. */
@Composable
fun StyledListScope.BudsChoiceRow(name: String, selected: Boolean, onClick: () -> Unit,
    description: String? = null, enabled: Boolean = true, leadingContent: (@Composable () -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    divider: Boolean = false) {
    item { _, _ ->
        Row(Modifier.fillMaxWidth().selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Canvas(Modifier.size(32.dp)) {
                val ink = color.copy(alpha = if (enabled) 1f else .4f)
                drawCircle(ink, size.width * .28125f, style = Stroke(size.width * if (selected) .0625f else .046875f))
                if (selected) drawCircle(ink, size.width * .15625f)
            }
            Spacer(Modifier.width(14.dp))
            leadingContent?.let { it(); Spacer(Modifier.width(16.dp)) }
            Column(Modifier.weight(1f)) {
                Text(name, style = BudsStyle.RowTitle,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .4f))
                description?.let { Text(it, style = BudsStyle.RowSummary,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = if (enabled) 1f else .4f)) }
            }
            trailingAction?.let {
                Spacer(Modifier.width(16.dp))
                androidx.compose.material3.VerticalDivider(Modifier.height(22.dp),
                    thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { it() }
            }
        }
        if (divider) androidx.compose.material3.HorizontalDivider(
            Modifier.padding(start = 62.dp, end = 16.dp), thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant)
    }
}
