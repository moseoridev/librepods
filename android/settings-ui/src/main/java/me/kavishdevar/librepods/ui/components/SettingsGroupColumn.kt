package me.kavishdevar.librepods.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density
import me.kavishdevar.librepods.ui.theme.SettingsStyle

/**
 * Stacks a settings page's groups with the source's spacing rule.
 *
 * The reference screens do not put a uniform gap between every group. An ordinary group
 * follows the previous one after an explicit 20dp spacer, but a group that opens with a
 * section label takes no spacer: the label's own 16dp top padding is the whole lead. The
 * device battery row is similar in the other direction — it carries 14dp of bottom padding
 * and the next group starts immediately.
 *
 * A plain `Column(verticalArrangement = spacedBy(GroupSpacing))` cannot express that; it
 * adds the gap at every boundary. That put 20dp on top of the label's 16dp (36dp where the
 * source has 16dp) and 20dp under the battery row (34dp where the source has 14dp).
 *
 * Groups that already carry their own lead or trail space declare it with
 * [ownLeadingSpace] / [ownTrailingSpace]; their boundary then takes no gap. Titled
 * [SettingsList]s declare their lead automatically.
 */
@Composable
fun SettingsGroupColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val gap = SettingsStyle.GroupSpacing.roundToPx()
        val spacing = measurables.map { it.parentData as? SelfSpaced ?: SelfSpaced(false, false) }
        // Match Column: children measure against a zero minimum so a wrap-content child
        // (a pill button, for example) keeps its natural width instead of filling the page.
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(childConstraints) }
        var cursor = 0
        val positions = IntArray(placeables.size)
        placeables.forEachIndexed { index, placeable ->
            // The first group has no predecessor; the caller's own top inset owns that edge.
            if (index > 0 && !(spacing[index].leading || spacing[index - 1].trailing)) cursor += gap
            positions[index] = cursor
            cursor += placeable.height
        }
        // A page column spans the width its parent gives it; a wrap-content parent still
        // gets the widest group rather than a forced full width.
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth
        else (placeables.maxOfOrNull { it.width } ?: 0).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = cursor.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            placeables.forEachIndexed { index, placeable -> placeable.placeRelative(0, positions[index]) }
        }
    }
}

private data class SelfSpaced(val leading: Boolean, val trailing: Boolean)

private object OwnLeadingSpace : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any =
        (parentData as? SelfSpaced ?: SelfSpaced(false, false)).copy(leading = true)
}

private object OwnTrailingSpace : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any =
        (parentData as? SelfSpaced ?: SelfSpaced(false, false)).copy(trailing = true)
}

/** Declares that this group brings its own leading space inside [SettingsGroupColumn]. */
fun Modifier.ownLeadingSpace(): Modifier = this then OwnLeadingSpace

/** Declares that this group brings its own trailing space inside [SettingsGroupColumn]. */
fun Modifier.ownTrailingSpace(): Modifier = this then OwnTrailingSpace
