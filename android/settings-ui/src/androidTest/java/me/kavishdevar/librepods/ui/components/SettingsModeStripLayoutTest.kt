package me.kavishdevar.librepods.ui.components

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsModeStripLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun wideStripKeepsTheSourceOuterCap() {
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                screenWidthDp = 800
                screenHeightDp = 800
            }
            CompositionLocalProvider(LocalConfiguration provides configuration,
                LocalDensity provides Density(1f, 1f)) {
                SettingsTheme {
                    SettingsModeStrip(listOf(0, 1, 2, 3), 0, {}, label = { "Mode $it" },
                        modifier = Modifier.width(701.dp), artwork = { mode, _ ->
                            Box(Modifier.fillMaxSize().testTag("mode-$mode"))
                        })
                }
            }
        }
        val first = compose.onNodeWithTag("mode-0", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val last = compose.onNodeWithTag("mode-3", useUnmergedTree = true).getUnclippedBoundsInRoot()
        // bm.t caps the outer width at rl.r1.a=412dp. Its two 10dp
        // insets leave 392dp for the discs, including their 44dp diameter.
        assertEquals(155f, first.left.value, .01f)
        assertEquals(348f, (last.left + last.right - first.left - first.right).value / 2f, .01f)
    }
}
