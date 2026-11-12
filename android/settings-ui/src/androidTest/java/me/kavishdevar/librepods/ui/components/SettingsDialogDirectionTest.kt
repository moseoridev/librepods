package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsDialogDirectionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun neutralTitleStartsAtRightInRtl() = checkStart(LayoutDirection.Rtl)
    @Test fun neutralTitleStartsAtLeftInLtr() = checkStart(LayoutDirection.Ltr)

    @Test fun narrowGroupChildUsesRtlStartEdge() {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SettingsGroupColumn(Modifier.width(200.dp).testTag("group-parent")) {
                    Box(Modifier.size(40.dp).testTag("narrow-child"))
                }
            }
        }
        val parent = compose.onNodeWithTag("group-parent").fetchSemanticsNode().boundsInRoot
        val child = compose.onNodeWithTag("narrow-child").fetchSemanticsNode().boundsInRoot
        assertEquals(parent.right, child.right, .5f)
        assertTrue(child.width < parent.width)
    }

    private fun checkStart(direction: LayoutDirection) {
        compose.setContent {
            SettingsTheme(darkTheme = false) {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    SettingsDialogTitle("123", Modifier.width(200.dp).background(Color.White).testTag("title"))
                }
            }
        }
        val pixels = compose.onNodeWithTag("title").captureToImage().toPixelMap()
        val counts = (0..1).map { half ->
            (0 until pixels.height).sumOf { y ->
                (half * pixels.width / 2 until (half + 1) * pixels.width / 2).count { x ->
                    pixels[x, y].red < .5f && pixels[x, y].alpha > .5f
                }
            }
        }
        val start = if (direction == LayoutDirection.Rtl) 1 else 0
        assertTrue("Title ink must follow layout direction: $counts", counts[start] > 20 && counts[1 - start] == 0)
    }
}
