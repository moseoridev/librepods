package me.kavishdevar.librepods.ui.components

import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
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
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsButtonInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun standaloneTextActionIsReadableInDarkThemeAndKeepsCallerTint() {
        var tint by mutableStateOf(Color.Unspecified)
        compose.setContent {
            SettingsTheme(darkTheme = true) {
                Box(Modifier.background(MaterialTheme.colorScheme.background)) {
                    SettingsButton({}, Modifier.testTag("standalone-action"), tint = tint) {
                        Text("Reset")
                    }
                }
            }
        }
        fun visiblePixels(matches: (Color) -> Boolean): Int {
            val pixels = compose.onNodeWithTag("standalone-action").captureToImage().toPixelMap()
            return (0 until pixels.height).sumOf { y ->
                (0 until pixels.width).count { x -> matches(pixels[x, y]) }
            }
        }
        assertTrue(visiblePixels { it.red > .8f && it.green > .8f && it.blue > .8f } > 40)
        compose.runOnIdle { tint = Color.Red }
        assertTrue(visiblePixels { it.red > .8f && it.green < .2f && it.blue < .2f } > 40)
    }

    @Test fun iconButtonPreservesCallerAlignmentWeightAndFullHitArea() {
        var clicks = 0
        compose.setContent {
            SettingsTheme {
                Column {
                    Box(Modifier.size(160.dp).testTag("alignment-parent")) {
                        SettingsIconButton({}, "Aligned", Modifier.align(Alignment.BottomEnd)) {
                            Box(Modifier.size(24.dp))
                        }
                    }
                    Row(Modifier.width(240.dp)) {
                        SettingsIconButton({ clicks++ }, "Weighted", Modifier.weight(1f)) {
                            Box(Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(80.dp))
                    }
                }
            }
        }
        val parent = compose.onNodeWithTag("alignment-parent").fetchSemanticsNode().boundsInRoot
        val aligned = compose.onNodeWithContentDescription("Aligned").fetchSemanticsNode().boundsInRoot
        assertEquals(parent.right, aligned.right, .5f)
        assertEquals(parent.bottom, aligned.bottom, .5f)
        compose.onNodeWithContentDescription("Weighted")
            .assertWidthIsEqualTo(160.dp).assertHeightIsEqualTo(48.dp)
            .performTouchInput { click(Offset(1f, center.y)) }
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun iconButtonExposesClickAndHelpOnOneNamedActionAndSupportsKeyboard() {
        var clicks = 0
        lateinit var inputMode: InputModeManager
        compose.setContent {
            SettingsTheme {
                inputMode = LocalInputModeManager.current
                SettingsIconButton({ clicks++ }, "Action help") { Box(Modifier.size(24.dp)) }
            }
        }
        val action = compose.onNodeWithContentDescription("Action help")
        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick)).assertCountEquals(1)
        action.assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performSemanticsAction(SemanticsActions.OnLongClick) { assertTrue(it()) }
        compose.onNodeWithText("Action help").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, clicks) }
        compose.mainClock.advanceTimeBy(2000)
        compose.onNodeWithText("Action help").assertDoesNotExist()
        compose.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
        action.performSemanticsAction(SemanticsActions.RequestFocus) { assertTrue(it()) }
        action.assertIsFocused()
        compose.onNodeWithText("Action help").assertIsDisplayed()
        action.performKeyInput { pressKey(Key.Escape) }
        compose.onNodeWithText("Action help").assertDoesNotExist()
        action.performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun iconTooltipConsumesLongPressAndDisappearsWhenDisabledOrRemoved() {
        var enabled by mutableStateOf(true)
        var visible by mutableStateOf(true)
        var clicks = 0
        compose.setContent {
            SettingsTheme {
                if (visible) SettingsIconButton({ clicks++ }, "Action help", enabled = enabled) {
                    Box(Modifier.size(24.dp))
                }
            }
        }
        val action = compose.onNodeWithContentDescription("Action help")
        action.assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        action.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithText("Action help").assertIsDisplayed()
        action.performTouchInput { up() }
        compose.runOnIdle { assertEquals(0, clicks); enabled = false }
        compose.onNodeWithText("Action help").assertDoesNotExist()
        action.assertIsNotEnabled().performTouchInput { longClick() }
        compose.onNodeWithText("Action help").assertDoesNotExist()
        compose.runOnIdle { enabled = true }
        action.performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, clicks) }
        action.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithText("Action help").assertIsDisplayed()
        action.performTouchInput { cancel() }
        // The standard tooltip retains a brief press for its minimum display duration.
        compose.mainClock.advanceTimeBy(2000)
        compose.onNodeWithText("Action help").assertDoesNotExist()
        action.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithText("Action help").assertIsDisplayed()
        compose.runOnIdle { visible = false }
        compose.onRoot().performTouchInput { cancel() }
        compose.onNodeWithText("Action help").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

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
