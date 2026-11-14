package me.kavishdevar.librepods.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsToggleLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun smallTextKeepsItsOwnInsetsWhenSwitchIsTaller() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(3f, .8f)) {
                SettingsTheme {
                    Column {
                        SettingsList(Modifier.width(300.dp).testTag("home")) {
                            SettingsToggle("작은 글자 크기", checked = false,
                                onCheckedChange = {}, homeMenu = true)
                        }
                        SettingsList(Modifier.width(300.dp).testTag("plain")) {
                            SettingsToggle("일반 글자 크기", checked = false, onCheckedChange = {})
                        }
                    }
                }
            }
        }
        val text = compose.onNodeWithText("작은 글자 크기", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val home = compose.onNodeWithTag("home").getUnclippedBoundsInRoot()
        val plain = compose.onNodeWithTag("plain").getUnclippedBoundsInRoot()
        // wm.k1.d's 14dp text insets determine the row, even when the bare
        // 20dp switch is taller than the natural title. The switch gets no extra insets.
        assertTrue((text.bottom - text.top).value < 20f)
        assertEquals(14f, text.top.value - home.top.value, .01f)
        assertEquals((text.bottom - text.top).value + 28f, (home.bottom - home.top).value, .01f)
        assertEquals((plain.bottom - plain.top).value, (home.bottom - home.top).value, .01f)
    }
}
