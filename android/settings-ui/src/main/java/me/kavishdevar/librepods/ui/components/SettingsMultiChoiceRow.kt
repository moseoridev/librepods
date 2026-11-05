package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.ui.theme.SettingsStyle
import me.kavishdevar.librepods.ui.theme.interactionFeedback

/** Independent caller-owned choices, with ww.c/e's circular selection artwork. */
@Composable
fun SettingsMultiChoiceRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val alpha = if (enabled) 1f else .4f
    val interactionSource = remember { MutableInteractionSource() }
    // One named toggle target; the decorative circle creates no duplicate action.
    Row(modifier.fillMaxWidth().toggleable(checked, enabled = enabled,
        role = Role.Checkbox, interactionSource = interactionSource, indication = null,
        onValueChange = onCheckedChange)
        // ww.c.a → vw.a.b: .98 content-only, unscaled default Full pill.
        .then(settingsRowFeedback(enabled, interactionSource, colors.interactionFeedback,
            SettingsStyle.PillShape))
        .padding(start = 16.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        val ink = (if (checked) colors.primary else colors.outline).copy(alpha = alpha)
        Canvas(Modifier.size(32.dp)) {
            drawCircle(ink, size.width * .28125f,
                style = Stroke(size.width * if (checked) .0625f else .046875f))
            if (checked) drawCircle(ink, size.width * .15625f)
        }
        Spacer(Modifier.width(14.dp))
        // bm.r replaces LocalTextStyle with a color-only style. Its text resolves
        // to the platform default family at 14sp, rather than the page's sec family.
        Text(label, Modifier.weight(1f), style = SettingsStyle.RowTitle.copy(
            fontFamily = FontFamily.Default, fontSize = 14.sp),
            color = colors.onSurface.copy(alpha = alpha))
    }
}
