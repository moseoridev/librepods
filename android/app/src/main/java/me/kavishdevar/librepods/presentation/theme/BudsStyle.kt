package me.kavishdevar.librepods.presentation.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** Shared geometry. Screen-specific Buds renderer parity is tracked in android-settings.md. */
internal object BudsStyle {
    val RowTitle = TextStyle(fontFamily = BudsFontFamily, fontSize = 17.sp)
    val RowSummary = TextStyle(fontFamily = BudsFontFamily, fontSize = 13.sp)
    val SectionTitle = RowSummary.copy(fontWeight = FontWeight.SemiBold)
    val CompactTitle = RowTitle.copy(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
    val CompactSubtitle = RowSummary.copy(fontWeight = FontWeight.SemiBold)
    val ExpandedTitle = RowTitle.copy(fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
    val GroupRadius = 26.dp
    val GroupShape = BudsGroupShape(GroupRadius)
    val RowInset = 18.dp
    val RowVerticalPadding = 12.dp
    // The source constrains the row before padding its text, not the whole row to 48dp.
    // Compose's clickable/toggleable still expand the touch target to the platform minimum.
    val RowMinHeight = 28.dp
    val GroupSpacing = 20.dp
    val SectionTop = 16.dp
    val SectionBottom = 8.dp
    val FooterTop = 14.dp
    val DividerInset = 16.dp

    /** gm.g2 -> m3.i.x: responsive page width, independent of row text insets. */
    @Composable
    fun pageInset() = LocalConfiguration.current.let { configuration ->
        val width = configuration.screenWidthDp
        val fraction = when {
            width >= 990 -> .75f
            width >= 589 && configuration.screenHeightDp > 411 -> .86f
            else -> 1f
        }
        ((width - (width * fraction).toInt()) / 2).let { if (it == 0) 10 else it }.dp
    }
}
