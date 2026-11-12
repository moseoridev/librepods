package me.kavishdevar.librepods.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import me.kavishdevar.librepods.ui.theme.contentFade
import me.kavishdevar.librepods.ui.theme.interactionFeedback
import me.kavishdevar.librepods.ui.theme.sliderThumbCore
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsThemeModeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lightRolesSurviveCallerTextColorOverride() = checkRoles(false,
        Color(0xFFF1F1F3), Color(0xFFFCFCFF), Color(0x1A000000))

    @Test fun darkRolesSurviveLightLookingCallerTextColor() = checkRoles(true,
        Color(0xFF010101), Color(0xFF010102), Color(0x33FFFFFF))

    private fun checkRoles(dark: Boolean, fade: Color, core: Color, feedback: Color) {
        var actual = emptyList<Color>()
        compose.setContent {
            SettingsTheme(darkTheme = dark) {
                MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
                    onSurface = if (dark) Color(0xFF010102) else Color.Red)) {
                    val colors = MaterialTheme.colorScheme
                    val roles = listOf(colors.contentFade, colors.sliderThumbCore, colors.interactionFeedback)
                    SideEffect { actual = roles }
                }
            }
        }
        compose.runOnIdle { assertEquals(listOf(fade, core, feedback), actual) }
    }
}
