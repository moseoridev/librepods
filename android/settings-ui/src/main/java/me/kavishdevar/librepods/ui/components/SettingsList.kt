package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import me.kavishdevar.librepods.ui.theme.SettingsStyle

@Composable
fun SettingsList(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    content: @Composable SettingsListScope.() -> Unit
) {
    val scope = SettingsListScope()
    scope.content()
    Column(modifier) {
        title?.let { SettingsSectionLabel(it) }
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, SettingsStyle.GroupShape)
            .clip(SettingsStyle.GroupShape)) {
            scope.items.forEachIndexed { index, item -> item(index, scope.items.size) }
        }
        description?.let {
            Text(it, style = SettingsStyle.RowSummary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = SettingsStyle.RowInset).padding(top = SettingsStyle.FooterTop))
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
