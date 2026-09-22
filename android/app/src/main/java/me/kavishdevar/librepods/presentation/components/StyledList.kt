package me.kavishdevar.librepods.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import me.kavishdevar.librepods.presentation.theme.BudsStyle

@Composable
fun StyledList(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    content: @Composable StyledListScope.() -> Unit
) {
    val scope = StyledListScope()
    scope.content()
    Column(modifier) {
        title?.let { BudsSectionLabel(it) }
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, BudsStyle.GroupShape)
            .clip(BudsStyle.GroupShape)) {
            scope.items.forEachIndexed { index, item -> item(index, scope.items.size) }
        }
        description?.let {
            Text(it, style = BudsStyle.RowSummary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = BudsStyle.RowInset).padding(top = BudsStyle.FooterTop))
        }
    }
}

@Composable
internal fun BudsSectionLabel(text: String) {
    Text(text, style = BudsStyle.SectionTitle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = BudsStyle.RowInset)
            .padding(top = BudsStyle.SectionTop, bottom = BudsStyle.SectionBottom))
}

class StyledListScope {
    internal val items = mutableListOf<@Composable (Int, Int) -> Unit>()
    fun item(content: @Composable (index: Int, count: Int) -> Unit) { items += content }
}
