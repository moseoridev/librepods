package me.kavishdevar.librepods.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density

class SettingsExpandedHeaderBackgroundTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun darkPartialExpansionExposesPageBackground() = checkBackground(true)
    @Test fun lightPartialExpansionExposesPageBackground() = checkBackground(false)

    @Test fun uncoveredRoundingRemainderUsesHostBackground() {
        var startY = 0f
        var travel = 0f
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(3f, 1f)) {
                val density = LocalDensity.current
                startY = WindowInsets.statusBars.getTop(density) + with(density) { 32.dp.toPx() }
                travel = LocalViewConfiguration.current.touchSlop + with(density) { 80.dp.toPx() } + .75f
                Box(Modifier.fillMaxSize().background(Color.Magenta).testTag("host")) {
                    SettingsTheme(darkTheme = false) {
                        SettingsScaffold(Modifier.fillMaxSize().testTag("scaffold"),
                            title = "Header", backLabel = "Back", expandableHeader = true) { Box {} }
                    }
                }
            }
        }
        val scaffold = compose.onNodeWithTag("scaffold")
        scaffold.performTouchInput {
            down(Offset(center.x, startY))
            moveBy(Offset(0f, travel), delayMillis = 100)
        }
        // hl.e rounds bottom padding but truncates translation independently.
        // A fractional drag exposes the host at the lower edge, not a component color.
        val image = compose.onNodeWithTag("host").captureToImage().toPixelMap()
        assertEquals(Color.Magenta, image[image.width / 2, image.height - 1])
        scaffold.performTouchInput { up() }
    }

    private fun checkBackground(dark: Boolean) {
        var expected = Color.Unspecified
        var startY = 0f
        var travel = 0f
        compose.setContent {
            val density = LocalDensity.current
            startY = WindowInsets.statusBars.getTop(density) + with(density) { 32.dp.toPx() }
            travel = with(density) { 80.dp.toPx() }
            SettingsTheme(darkTheme = dark) {
                expected = MaterialTheme.colorScheme.surfaceContainer
                SettingsScaffold(Modifier.fillMaxSize().testTag("scaffold"),
                    title = "Header", backLabel = "Back", expandableHeader = true) { Box {} }
            }
        }
        val scaffold = compose.onNodeWithTag("scaffold")
        val title = compose.onNodeWithText("Header", useUnmergedTree = true)
        val initialTop = title.fetchSemanticsNode().boundsInRoot.top
        // Hold the gesture below the crossfade midpoint, where the large title
        // is transparent and the translated scaffold exposes the host page.
        scaffold.performTouchInput {
            down(Offset(center.x, startY))
            moveBy(Offset(0f, travel), delayMillis = 100)
        }
        assertTrue(title.fetchSemanticsNode().boundsInRoot.top > initialTop + travel / 2f)
        val image = scaffold.captureToImage().toPixelMap()
        assertEquals(expected, image[1, 1])
        scaffold.performTouchInput { up() }
    }
}
