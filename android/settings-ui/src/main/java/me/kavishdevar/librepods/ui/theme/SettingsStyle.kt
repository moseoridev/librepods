package me.kavishdevar.librepods.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration

/** Shared geometry. Screen-specific reference renderer parity is tracked in android-settings.md. */
object SettingsStyle {
    val RowTitle = TextStyle(fontFamily = SettingsFontFamily, fontSize = 17.sp)
    val RowSummary = TextStyle(fontFamily = SettingsFontFamily, fontSize = 13.sp)
    val SectionTitle = RowSummary.copy(fontWeight = FontWeight.SemiBold)
    val CompactTitle = RowTitle.copy(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
    val CompactSubtitle = RowSummary.copy(fontWeight = FontWeight.SemiBold)
    val ExpandedTitle = RowTitle.copy(fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
    val GroupRadius = 26.dp
    val GroupShape = SettingsGroupShape(GroupRadius)
    // n1.l.d: 50% smooth corners clamp to half the shorter axis at any button height.
    val PillShape = SettingsGroupShape(Dp.Infinity)
    val RowInset = 18.dp
    // 9.0.10.1704: wm.k1.d pads the text column by 14dp; leading controls
    // stay outside that padding (wm.e0). This also keeps multi-line rows natural.
    val RowVerticalPadding = 14.dp
    val HomeMenuVerticalPadding = 14.dp
    // The source constrains the row before padding its text, not the whole row to 48dp.
    // Compose's clickable/toggleable still expand the touch target to the platform minimum.
    val RowMinHeight = 28.dp
    val GroupSpacing = 20.dp
    val SectionTop = 16.dp
    val SectionBottom = 8.dp
    val FooterTop = 14.dp
    val DividerInset = 16.dp
    // en.v1.i -> en.o0(case 1) gives the Home list divider 62dp start / 16dp end
    // padding. The start matches stock captures at 450, 480, 560 and 600 dpi.
    val HomeMenuDividerInset = 62.dp

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

    /**
     * gm.t1.G passes no colour for the 13sp supporting line, so it inherits the row's
     * content colour. The vendor screens therefore split two ways: a supporting line that
     * reports state (the current mode, on/off) renders in the accent, while one that explains
     * what the control does stays in the neutral supporting colour.
     *
     * A row dims only when it is both clickable and disabled — a control that is visible but
     * temporarily locked out. A row with no `onClick` is read-only rather than unavailable, so its
     * value keeps full strength even though callers pass `enabled = false`; taking the flag alone
     * would dim every read-only row in the app.
     */
    @Composable
    fun summaryColor(isState: Boolean, canClick: Boolean, enabled: Boolean): Color {
        val alpha = if (enabled || !canClick) 1f else .4f
        return if (isState) MaterialTheme.colorScheme.secondary.copy(alpha = alpha)
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
    }
}
