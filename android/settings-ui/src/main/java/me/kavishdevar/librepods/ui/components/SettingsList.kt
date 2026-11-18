package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import me.kavishdevar.librepods.ui.theme.SettingsStyle

/**
 * Enable [clipEdgePaint] for lists whose edge rows span the group corners and whose content
 * stays inside the outline. Edge backgrounds and interaction feedback must apply the local
 * group clip through [settingsEdgeBackground] and [settingsRowFeedback].
 */
@Composable
fun SettingsList(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    clipEdgePaint: Boolean = false,
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
        val groupSize = if (clipEdgePaint && count > 1) {
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
                    val first = index == 0
                    val edge = remember(groupSize, first) { SettingsGroupEdge(groupSize, first) }
                    Column(Modifier.fillMaxWidth()) {
                        CompositionLocalProvider(LocalSettingsGroupEdge provides edge) {
                            item(index, count)
                        }
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

internal class SettingsGroupEdge(private val groupSize: State<IntSize>, private val first: Boolean) {
    fun path(scope: CacheDrawScope): Path? = with(scope) {
        val group = groupSize.value
        // Preserve the outline of the whole group, aligned to this edge row.
        val outline = SettingsStyle.GroupShape.createOutline(
            Size(group.width.toFloat(), group.height.toFloat()), layoutDirection, this
        )
        (outline as? Outline.Generic)?.let {
            Path().apply {
                addPath(it.path, Offset(0f, if (first) 0f else size.height - group.height))
            }
        }
    }
}

internal val LocalSettingsGroupEdge = staticCompositionLocalOf<SettingsGroupEdge?> { null }

/** Paints a row background within its containing list's edge clip, when enabled. */
@Composable
fun Modifier.settingsEdgeBackground(color: Color, shape: Shape): Modifier {
    val edge = LocalSettingsGroupEdge.current ?: return background(color, shape)
    val fill by rememberUpdatedState(color)
    return drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = edge.path(this)
        onDrawBehind {
            if (path == null) drawOutline(outline, fill)
            else clipRect { clipPath(path) { drawOutline(outline, fill) } }
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
