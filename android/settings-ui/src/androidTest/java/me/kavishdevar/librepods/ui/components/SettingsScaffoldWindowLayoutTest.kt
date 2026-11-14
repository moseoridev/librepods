package me.kavishdevar.librepods.ui.components

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

class SettingsScaffoldWindowLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun shortPixelWindowUsesCompactRow() = checkWindow(579, 360, 56, 0)
    @Test fun tallPixelWindowIgnoresShortDpHeight() = checkWindow(580, 360, 64, 0)
    @Test fun qualifiedPaddingKeepsItsIndependentOffset() = checkWindow(1080, 442, 64, 4)

    @Test fun floatingNavigationKeepsIndependentPadding() {
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                orientation = Configuration.ORIENTATION_LANDSCAPE
                screenWidthDp = 1265
                screenHeightDp = 584
            }
            CompositionLocalProvider(LocalConfiguration provides configuration,
                LocalDensity provides Density(1.85f, 1f)) {
                SettingsTheme {
                    SettingsScaffold(title = "Toolbar", backLabel = "Back", showBackButton = true,
                        navigationIcon = { Box(Modifier.size(24.dp)) },
                        scrollState = rememberScrollState(20)) {
                        Box(Modifier.fillMaxWidth().height(3000.dp))
                    }
                }
            }
        }
        val button = compose.onNodeWithContentDescription("Back").fetchSemanticsNode().boundsInRoot
        // kw.m1 / wm.l1.i -> im.e(case9) -> wm.k1.v: round(24*1.85) +
        // 2*round(12*1.85) = 88px. The separately measured 48dp slot is 89px.
        assertEquals(88f, button.width, .01f)
        assertEquals(88f, button.height, .01f)
    }

    private fun checkWindow(heightPx: Int, heightDp: Int, rowDp: Int, paddingDp: Int) {
        val density = Density(2f, 1f)
        var statusTop = 0
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                orientation = Configuration.ORIENTATION_LANDSCAPE
                screenWidthDp = 800
                screenHeightDp = heightDp
            }
            val window = object : WindowInfo {
                override val isWindowFocused = true
                override val containerSize = IntSize(1600, heightPx)
            }
            CompositionLocalProvider(LocalConfiguration provides configuration,
                LocalDensity provides density, LocalWindowInfo provides window) {
                statusTop = WindowInsets.statusBars.getTop(density)
                SettingsTheme {
                    SettingsScaffold(title = "Toolbar", backLabel = "Back",
                        scrollState = rememberScrollState()) {
                        Box(Modifier.fillMaxWidth().height(10.dp).testTag("content"))
                    }
                }
            }
        }
        val title = compose.onNodeWithText("Toolbar", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val content = compose.onNodeWithTag("content").fetchSemanticsNode().boundsInRoot
        val expectedTitle = with(density) {
            statusTop + 2 * (paddingDp.dp.roundToPx() + (-2).dp.roundToPx()) +
                ((rowDp.dp.roundToPx() - title.height) / 2f).roundToInt()
        }
        val expectedContent = with(density) { statusTop + rowDp.dp.roundToPx() + 2 * paddingDp.dp.roundToPx() }
        assertEquals(expectedTitle.toFloat(), title.top, .01f)
        assertEquals(expectedContent.toFloat(), content.top, .01f)
    }
}
