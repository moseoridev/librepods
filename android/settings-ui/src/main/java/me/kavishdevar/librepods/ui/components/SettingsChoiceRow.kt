package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.SettingsStyle

/** Menu uses wm.k1.L/c/d; Control uses wm.k1.E's independently padded radio/icon row. */
enum class SettingsChoiceRowStyle { Menu, Control }

@Composable
fun SettingsListScope.SettingsChoiceRow(name: String, selected: Boolean, onClick: () -> Unit,
    description: String? = null, enabled: Boolean = true, leadingContent: (@Composable () -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    divider: Boolean = true, style: SettingsChoiceRowStyle = SettingsChoiceRowStyle.Menu,
    // Control's optional app-icon surface stays opaque while its artwork dims.
    leadingIconBackdrop: Boolean = false) {
    require(!leadingIconBackdrop || (style == SettingsChoiceRowStyle.Control && leadingContent != null)) {
        "leadingIconBackdrop requires a Control row with leadingContent"
    }
    item { index, count ->
        val control = style == SettingsChoiceRowStyle.Control
        val dim = if (enabled) 1f else .4f
        val contentAlpha = if (control) Modifier.graphicsLayer { alpha = dim } else Modifier
        val interactionSource = remember { MutableInteractionSource() }
        Row(Modifier.fillMaxWidth().selectable(selected, enabled = enabled, role = Role.RadioButton,
            interactionSource = interactionSource, indication = if (control) LocalIndication.current else null,
            onClick = onClick)
            .then(if (control) Modifier else settingsRowFeedback(enabled, interactionSource))
            .heightIn(min = SettingsStyle.RowMinHeight)
            .padding(horizontal = SettingsStyle.RowInset)
            .then(if (control) Modifier.padding(vertical = SettingsStyle.RowVerticalPadding) else Modifier),
            verticalAlignment = Alignment.CenterVertically) {
            val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            // Control adds a disabled layer around the already disabled radio token.
            Canvas(Modifier.size(32.dp).then(contentAlpha)) {
                val ink = color.copy(alpha = dim)
                drawCircle(ink, size.width * .28125f, style = Stroke(size.width * if (selected) .0625f else .046875f))
                if (selected) drawCircle(ink, size.width * .15625f)
            }
            Spacer(Modifier.width(if (control) 14.dp else 18.dp))
            leadingContent?.let {
                if (control) {
                    val backdrop = if (leadingIconBackdrop) {
                        val shape = RoundedCornerShape(11.dp)
                        Modifier.size(28.dp).clip(shape).border(.1.dp, Color(0xFFE0E0E0), shape)
                            .background(Color.White).padding(.8.dp)
                    } else Modifier
                    Box(backdrop.then(contentAlpha)) { it() }
                } else it()
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)
                .then(if (control) Modifier else Modifier.padding(vertical = SettingsStyle.RowVerticalPadding))) {
                Text(name, modifier = contentAlpha, style = SettingsStyle.RowTitle,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (control) 1f else dim))
                description?.let { Text(it, modifier = contentAlpha, style = SettingsStyle.RowSummary,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = if (control) 1f else dim)) }
            }
            trailingAction?.let {
                Spacer(Modifier.width(16.dp))
                androidx.compose.material3.VerticalDivider(Modifier.height(22.dp),
                    thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.width(12.dp))
                // The 28dp visual slot must not squeeze an independent 48dp action.
                // Native gear bounds are 28dp; its enabled target extends beyond
                // that slot even when the surrounding choice is disabled.
                Box(Modifier.size(28.dp).wrapContentSize(unbounded = true).then(contentAlpha),
                    contentAlignment = Alignment.Center) { it() }
            }
        }
        if (divider && index + 1 < count) SettingsRowDivider(
            startInset = if (control) SettingsStyle.HomeMenuDividerInset else SettingsStyle.DividerInset)
    }
}
