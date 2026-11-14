package me.kavishdevar.librepods.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.math.ceil
import kotlin.math.roundToInt

class SettingsScaffoldTitleLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun smallTitleKeepsFullToolbarCenter() = checkTitle(.8f)

    @Test fun defaultTitleRetainsItsFullLineHeight() = checkTitle(1f)

    private fun checkTitle(fontScale: Float) {
        val density = Density(2.625f, fontScale)
        var statusTop = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                statusTop = WindowInsets.statusBars.getTop(density)
                SettingsTheme {
                    SettingsScaffold(title = "헤드 제스처", backLabel = "Back", showBackButton = true) { Box {} }
                }
            }
        }
        val title = compose.onNodeWithText("헤드 제스처", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        // kw.t.D's 64dp row is positioned by two separately measured 4dp
        // padding/-4dp offset pairs. A 48dp approximation misses an odd-height
        // title by one pixel at this density, while even-height titles can match.
        assertEquals(with(density) { ceil(28.sp.toPx()).toInt() }.toFloat(), title.height, .01f)
        val expected = with(density) {
            statusTop + 2 * (4.dp.roundToPx() + (-4).dp.roundToPx()) +
                ((64.dp.roundToPx() - title.height) / 2f).roundToInt()
        }
        assertEquals(expected.toFloat(), title.top, .01f)
    }
}
