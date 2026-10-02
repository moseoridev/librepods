package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.kavishdevar.librepods.ui.theme.SettingsStyle

/** Group surface for content that is not a list. Does not own padding, state or gestures. */
@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = SettingsStyle.GroupShape, color = MaterialTheme.colorScheme.surface) {
        Column(content = content)
    }
}
