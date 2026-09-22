package me.kavishdevar.librepods.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.kavishdevar.librepods.presentation.theme.BudsStyle

/** App scaffold owns system insets; detail content grows and scrolls with the font size. */
@Composable
internal fun SettingsDetailColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState()).padding(horizontal = BudsStyle.pageInset()).padding(bottom = BudsStyle.GroupSpacing),
        verticalArrangement = Arrangement.spacedBy(BudsStyle.GroupSpacing), content = content)
}
