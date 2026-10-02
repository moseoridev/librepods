package me.kavishdevar.librepods.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsButtonInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun buttonStylesKeepOneActionAndSupportTouchKeyboardAndCancellation() {
        var enabled by mutableStateOf(true)
        var style by mutableStateOf(SettingsButtonStyle.Text)
        var clicks = 0
        lateinit var inputMode: InputModeManager
        compose.setContent {
            SettingsTheme {
                inputMode = LocalInputModeManager.current
                SettingsButton({ clicks++ }, Modifier.testTag("action"), enabled, style) {
                    Text("Action")
                }
            }
        }
        val button = compose.onNodeWithTag("action")
        for ((index, variant) in SettingsButtonStyle.entries.withIndex()) {
            compose.runOnIdle { style = variant; enabled = true }
            compose.onAllNodes(hasClickAction()).assertCountEquals(1)
            button.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .assertIsEnabled().performTouchInput { down(center); cancel() }
            compose.runOnIdle { assertEquals(index * 2, clicks) }
            button.performTouchInput { click(center) }
            compose.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
            button.performSemanticsAction(SemanticsActions.RequestFocus) { assertTrue(it()) }
            button.assertIsFocused()
            button.performKeyInput { pressKey(Key.Enter) }
            compose.runOnIdle { assertEquals(index * 2 + 2, clicks); enabled = false }
            button.assertIsNotEnabled().performClick()
            button.performTouchInput { click(center) }
            compose.runOnIdle { assertEquals(index * 2 + 2, clicks) }
        }
    }
}
