package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import me.kavishdevar.librepods.ui.theme.SettingsStyle

/**
 * Enable [clipOnlyEdgeRows] when the first and last items span the full height of the group
 * corners, and each item draws within its own bounds. Keep the complete group clip otherwise.
 */
@Composable
fun SettingsList(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    clipOnlyEdgeRows: Boolean = false,
    content: @Composable SettingsListScope.() -> Unit
) {
    val scope = SettingsListScope()
    scope.content()
    // A title already opens with the label's 16dp top padding, so inside a
    // SettingsGroupColumn this group must not also take the ordinary 20dp group gap.
    // Parent data is inert under any other parent.
    Column(modifier.then(if (title != null) Modifier.ownLeadingSpace() else Modifier)) {
        title?.let { SettingsSectionLabel(it) }
        val count = scope.items.size
        val groupSize = if (clipOnlyEdgeRows && count > 1) {
            remember { mutableStateOf(IntSize.Zero) }
        } else {
            null
        }
        val groupClip = if (groupSize == null) {
            Modifier.clip(SettingsStyle.GroupShape)
        } else {
            Modifier.clipToBounds().onSizeChanged { groupSize.value = it }
        }
        Column(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, SettingsStyle.GroupShape)
                .then(groupClip)
        ) {
            scope.items.forEachIndexed { index, item ->
                if (groupSize != null && (index == 0 || index == count - 1)) {
                    // An item can emit its row and divider as separate children.
                    Column(Modifier.fillMaxWidth().clipToGroupEdge(groupSize, first = index == 0)) {
                        item(index, count)
                    }
                } else {
                    item(index, count)
                }
            }
        }
        description?.let {
            Text(it, style = SettingsStyle.RowSummary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = SettingsStyle.RowInset).padding(top = SettingsStyle.FooterTop))
        }
    }
}

private fun Modifier.clipToGroupEdge(groupSize: State<IntSize>, first: Boolean): Modifier =
    drawWithCache {
        val group = groupSize.value
        val outline = SettingsStyle.GroupShape.createOutline(
            Size(group.width.toFloat(), group.height.toFloat()), layoutDirection, this
        )
        // Use the full group's outline so the edge pixels match at any row and window size.
        val path = (outline as? Outline.Generic)?.let {
            Path().apply {
                addPath(it.path, Offset(0f, if (first) 0f else size.height - group.height))
            }
        }
        onDrawWithContent {
            if (path == null) {
                drawContent()
            } else {
                clipRect { clipPath(path) { this@onDrawWithContent.drawContent() } }
            }
        }
    }

@Composable
fun SettingsSectionLabel(text: String) {
    Text(text, style = SettingsStyle.SectionTitle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = SettingsStyle.RowInset)
            .padding(top = SettingsStyle.SectionTop, bottom = SettingsStyle.SectionBottom))
}

class SettingsListScope {
    internal val items = mutableListOf<@Composable (Int, Int) -> Unit>()
    fun item(content: @Composable (index: Int, count: Int) -> Unit) { items += content }
}
