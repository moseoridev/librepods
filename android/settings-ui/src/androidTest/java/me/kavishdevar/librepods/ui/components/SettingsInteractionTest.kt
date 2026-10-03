package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.KeyboardOptions
import android.widget.EditText
import android.widget.ProgressBar
import android.text.method.PasswordTransformationMethod
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.drawable.Animatable
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.input.key.Key
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.launch
import org.hamcrest.Matchers.not
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun multipleChoicesKeepIndependentStateAndSingleNamedActions() {
        var first by mutableStateOf(false)
        var second by mutableStateOf(false)
        var enabled by mutableStateOf(true)
        var changes = 0
        var saves = 0
        compose.setContent {
            SettingsTheme {
                SettingsDialog(true, "Select modes", {},
                    presentation = SettingsDialogPresentation.Selection,
                    message = "Select at least two", actions = listOf(
                        SettingsDialogAction("Save", { saves++ }, enabled = first && second))) {
                    SettingsMultiChoiceRow("First mode", first, { first = it; changes++ }, enabled = enabled)
                    SettingsMultiChoiceRow("Second mode", second, { second = it; changes++ })
                }
            }
        }
        val choice = SemanticsMatcher.expectValue(SemanticsProperties.Role,
            androidx.compose.ui.semantics.Role.Checkbox)
        compose.onAllNodes(choice, useUnmergedTree = true).assertCountEquals(2)
        compose.onAllNodes(hasClickAction() and hasText("First mode"), useUnmergedTree = false).assertCountEquals(1)
        for (label in listOf("First mode", "Second mode")) {
            compose.onNodeWithText(label).performTouchInput {
                down(center); advanceEventTime(200); cancel()
            }.assertIsOff()
        }
        compose.runOnIdle { assertEquals(0, changes) }
        compose.onNodeWithText("First mode").assertIsOff().performTouchInput { click() }.assertIsOn()
        compose.onNodeWithText("Second mode").assertIsOff().performTouchInput { click() }.assertIsOn()
        compose.onNodeWithText("First mode").assertIsOn()
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        compose.onNodeWithText("First mode").performClick().assertIsOff()
        compose.onNodeWithText("Second mode").assertIsOn()
        compose.onNodeWithText("Save").assertIsNotEnabled().performClick()
        compose.runOnIdle { enabled = false }
        compose.onNodeWithText("First mode").assertIsNotEnabled().performClick()
            .performTouchInput { click() }.assertIsOff()
        compose.runOnIdle {
            assertEquals(3, changes)
            assertEquals(1, saves)
            // External state updates must not invoke a change callback.
            first = true
        }
        compose.onNodeWithText("First mode").assertIsOn()
        compose.runOnIdle { assertEquals(3, changes) }
    }

    @Test fun sheetOutsideTouchKeepsCallerStateAndDarkContentColor() {
        var dismissals = 0
        var contentColor = androidx.compose.ui.graphics.Color.Unspecified
        compose.setContent {
            SettingsTheme(darkTheme = true) {
                SettingsBottomSheet(true, { dismissals++ }) { _ ->
                    contentColor = androidx.compose.material3.LocalContentColor.current
                    Text("문의 내용", Modifier.testTag("sheet-title"))
                }
            }
        }
        compose.onAllNodes(isRoot()).filter(hasAnyDescendant(hasTestTag("sheet-title")))
            .onFirst().performTouchInput { click(Offset(10.dp.toPx(), 100.dp.toPx())) }
        compose.runOnIdle {
            assertEquals(0, dismissals)
            assertEquals(androidx.compose.ui.graphics.Color(0xFFFCFCFF), contentColor)
        }
        compose.onNodeWithTag("sheet-title").assertIsDisplayed()
    }

    @Test fun sheetDimsItsWindowAndRestoresItWhenRemoved() {
        var visible by mutableStateOf(true)
        var sheetWindow: android.view.Window? = null
        var originalAttributes: android.view.WindowManager.LayoutParams? = null
        var originalElevation = 0f
        compose.setContent {
            SettingsTheme(darkTheme = true) {
                SettingsBottomSheet(visible, { visible = false }) { _ ->
                    val view = androidx.compose.ui.platform.LocalView.current
                    if (sheetWindow == null) {
                        sheetWindow = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
                        originalAttributes = android.view.WindowManager.LayoutParams().apply {
                            copyFrom(requireNotNull(sheetWindow).attributes)
                        }
                        originalElevation = requireNotNull(sheetWindow).decorView.elevation
                    }
                    SettingsSheetTitle("설정 안내")
                    SettingsSheetBody("호출자가 제공한 내용")
                }
            }
        }
        compose.onAllNodesWithText("설정 안내").assertCountEquals(1)
        compose.onAllNodesWithText("호출자가 제공한 내용").assertCountEquals(1)
        compose.runOnIdle {
            val attributes = requireNotNull(sheetWindow).attributes
            assertTrue(attributes.dimAmount > 0f && attributes.dimAmount < 1f)
            assertTrue(attributes.flags and android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0)
            assertEquals(8 * compose.density.density, requireNotNull(sheetWindow).decorView.elevation, .001f)
            visible = false
        }
        compose.onNodeWithText("설정 안내").assertDoesNotExist()
        compose.runOnIdle {
            val attributes = requireNotNull(sheetWindow).attributes
            val original = requireNotNull(originalAttributes)
            val dimFlag = android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
            assertEquals(original.flags and dimFlag, attributes.flags and dimFlag)
            if (original.dimAmount >= 1f) assertEquals(original.dimAmount, attributes.dimAmount, .001f)
            assertEquals(originalElevation, requireNotNull(sheetWindow).decorView.elevation, .001f)
        }
    }

    @Test fun lightSheetKeepsPlatformDimmingPolicy() {
        org.junit.Assume.assumeTrue(android.os.Build.MANUFACTURER.equals("samsung", ignoreCase = true))
        compose.activity.setTheme(android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        var sheetWindow: android.view.Window? = null
        compose.setContent {
            SettingsTheme(darkTheme = false) {
                SettingsBottomSheet(true, {}) { _ ->
                    val view = androidx.compose.ui.platform.LocalView.current
                    sheetWindow = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
                    SettingsSheetTitle("밝은 안내")
                }
            }
        }
        compose.onNodeWithText("밝은 안내").assertIsDisplayed()
        compose.runOnIdle {
            val reduceTransparency = android.provider.Settings.System.getInt(
                requireNotNull(sheetWindow).context.contentResolver, "accessibility_reduce_transparency", 0) == 1
            assertEquals(if (reduceTransparency) .35f else .18f,
                requireNotNull(sheetWindow).attributes.dimAmount, .001f)
        }
    }

    @Test fun sheetOutsideDismissalOptionStillCallsTheCallerOnce() {
        var visible by mutableStateOf(true)
        var dismissals = 0
        compose.setContent {
            SettingsTheme {
                SettingsBottomSheet(visible, { dismissals++; visible = false },
                    dismissOnClickOutside = true) { _ ->
                    SettingsSheetTitle("닫을 수 있는 안내", Modifier.testTag("dismissible-sheet"))
                }
            }
        }
        compose.onAllNodes(isRoot()).filter(hasAnyDescendant(hasTestTag("dismissible-sheet")))
            .onFirst().performTouchInput { click(Offset(100.dp.toPx(), 100.dp.toPx())) }
        compose.waitUntil(timeoutMillis = 5_000) { dismissals > 0 }
        compose.onNodeWithTag("dismissible-sheet").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test fun sheetSurfaceUpdatesNativeGeometryWhenCallerDensityChanges() {
        var density by mutableStateOf(2f)
        var dark by mutableStateOf(true)
        var visible by mutableStateOf(true)
        lateinit var surface: SettingsSheetBackground
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density, 1f)) {
                SettingsTheme(darkTheme = dark) {
                    if (visible) SettingsSheetSurface { Text("Sheet content") }
                }
            }
        }
        fun assertGeometry(expectedDensity: Float, retain: Boolean) {
            onView(isAssignableFrom(SettingsSheetBackground::class.java)).check { view, error ->
                if (error != null) throw error
                val current = view as SettingsSheetBackground
                if (retain) assertTrue(surface === current) else surface = current
                assertTrue(current.isAttachedToWindow)
                assertEquals(8f * expectedDensity, current.elevation, .001f)
                val drawable = current.background
                if (drawable is android.graphics.drawable.GradientDrawable) {
                    assertEquals(26f * expectedDensity, drawable.cornerRadius, .001f)
                } else {
                    // Samsung's optional blur replaces the background drawable;
                    // inspect its actual corner data rather than internal kit state.
                    val corners = Regex("corners=\\{([^}]+)\\}")
                        .find(drawable.toString())?.groupValues?.get(1)
                    assertTrue("Unexpected background: $drawable", corners != null)
                    val radii = corners!!.split(',').map { it.trim().toFloat() }
                    assertEquals(4, radii.size)
                    radii.forEach { assertEquals(26f * expectedDensity, it, .001f) }
                }
            }
        }
        assertGeometry(2f, retain = false)
        compose.runOnIdle { density = 3.5f }
        assertGeometry(3.5f, retain = true)
        compose.runOnIdle { dark = false }
        assertGeometry(3.5f, retain = true)
        compose.runOnIdle { density = 2f }
        assertGeometry(2f, retain = true)
        compose.runOnIdle { visible = false }
        compose.onNodeWithText("Sheet content").assertDoesNotExist()
        compose.runOnIdle { assertFalse(surface.isAttachedToWindow) }
    }

    @Test fun longSheetScrollsToCallerActionAndRemovesItsNativeSurface() {
        var visible by mutableStateOf(true)
        var actions = 0
        lateinit var background: SettingsSheetBackground
        compose.setContent {
            SettingsTheme {
                SettingsBottomSheet(visible, { visible = false }) { _ ->
                    Text("문의 내용", Modifier.testTag("sheet-title"))
                    Spacer(Modifier.height(1200.dp))
                    SettingsButton({ actions++ }, Modifier.testTag("sheet-action")) { Text("보내기") }
                }
            }
        }
        onView(isAssignableFrom(SettingsSheetBackground::class.java)).check { view, error ->
            if (error != null) throw error
            background = view as SettingsSheetBackground
            assertTrue(background.isAttachedToWindow)
        }
        compose.onNodeWithTag("sheet-action").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, actions) }
        // A caller action does not implicitly dismiss or reset its content.
        compose.onNodeWithTag("sheet-action").assertIsDisplayed()
        compose.runOnIdle { visible = false }
        compose.onNodeWithTag("sheet-title").assertDoesNotExist()
        compose.runOnIdle { assertFalse(background.isAttachedToWindow) }
    }

    @Test fun loadingIndicatorKeepsOneNamedRangeAndStopsAfterRemoval() {
        var visible by mutableStateOf(true)
        lateinit var widget: ProgressBar
        compose.setContent {
            SettingsTheme {
                if (visible) SettingsLoadingIndicator(Modifier.testTag("loading").semantics {
                    contentDescription = "불러오는 중"
                })
            }
        }
        val range = SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo.Indeterminate)
        compose.onAllNodes(range).assertCountEquals(1)
        compose.onNodeWithContentDescription("불러오는 중").assert(range)
        onView(isAssignableFrom(ProgressBar::class.java)).check { view, error ->
            if (error != null) throw error
            widget = view as ProgressBar
            assertTrue(widget.isIndeterminate)
            assertTrue(widget.isAttachedToWindow)
            assertTrue(widget.indeterminateDrawable is Animatable)
        }
        compose.waitUntil(2_000) { (widget.indeterminateDrawable as Animatable).isRunning }
        compose.runOnIdle { visible = false }
        compose.onNodeWithTag("loading").assertDoesNotExist()
        compose.runOnIdle {
            assertFalse(widget.isAttachedToWindow)
            assertFalse((widget.indeterminateDrawable as Animatable).isRunning)
        }
    }

    @Test fun focusRequesterReachesEditableFieldAndEditsCallerState() {
        val state = TextFieldState("Original")
        val focus = FocusRequester()
        lateinit var focusManager: FocusManager
        var enabled by mutableStateOf(true)
        var validation by mutableStateOf<String?>(null)
        compose.setContent {
            focusManager = LocalFocusManager.current
            SettingsTheme { SettingsInputField(state, Modifier.testTag("name"), enabled = enabled,
                focusRequester = focus, errorMessage = validation,
                keyboardOptions = KeyboardOptions(showKeyboardOnFocus = false)) }
        }
        compose.runOnIdle { focus.requestFocus() }
        val editor = onView(isAssignableFrom(EditText::class.java))
        editor.check(matches(hasFocus())).perform(replaceText("Changed"))
        compose.runOnIdle { assertEquals("Changed", state.text.toString()) }
        compose.runOnIdle { focusManager.clearFocus(force = true) }
        editor.check(matches(not(hasFocus())))
        compose.runOnIdle { focus.requestFocus() }
        editor.check(matches(hasFocus()))
        compose.runOnIdle { state.edit { replace(0, length, "Caller update"); selection = TextRange(2, 5) } }
        editor.check { view, error ->
            if (error != null) throw error
            val field = view as EditText
            assertEquals("Caller update", field.text.toString())
            assertEquals(2, field.selectionStart)
            assertEquals(5, field.selectionEnd)
            field.setSelection(7)
        }
        compose.runOnIdle { assertEquals(TextRange(7), state.selection); enabled = false; validation = "Invalid value" }
        editor.check(matches(not(isEnabled())))
        editor.check { view, error ->
            if (error != null) throw error
            val info = view.createAccessibilityNodeInfo()
            assertTrue(info.isContentInvalid)
            assertEquals("Invalid value", info.error.toString())
        }
    }

    @Test fun passwordOptionsKeepMaskingAcrossCallerUpdates() {
        val state = TextFieldState("123456")
        var keyboardType by mutableStateOf(KeyboardType.Password)
        var singleLine by mutableStateOf(true)
        compose.setContent {
            SettingsTheme {
                SettingsInputField(state, singleLine = singleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, showKeyboardOnFocus = false))
            }
        }
        val editor = onView(isAssignableFrom(EditText::class.java))
        for (type in listOf(KeyboardType.Password, KeyboardType.NumberPassword)) {
            compose.runOnIdle { keyboardType = type; singleLine = true }
            for (lineMode in listOf(true, false)) {
                compose.runOnIdle {
                    singleLine = lineMode
                    state.edit { replace(0, length, if (lineMode) "654321" else "987654") }
                }
                editor.check { view, error ->
                    if (error != null) throw error
                    val field = view as EditText
                    assertTrue(field.transformationMethod is PasswordTransformationMethod)
                    assertEquals(state.text.toString(), field.text.toString())
                    assertFalse(field.transformationMethod.getTransformation(field.text, field).toString() == state.text.toString())
                    assertTrue(field.createAccessibilityNodeInfo().isPassword)
                }
            }
        }
        compose.runOnIdle { keyboardType = KeyboardType.Text; singleLine = true }
        editor.check { view, error ->
            if (error != null) throw error
            val field = view as EditText
            assertEquals(state.text.toString(), field.transformationMethod.getTransformation(field.text, field).toString())
            assertFalse(field.createAccessibilityNodeInfo().isPassword)
        }
    }

    @Test fun switchRowOwnsOneActionAndDisabledRowsCannotChangeState() {
        var checked by mutableStateOf(false)
        var masterChecked by mutableStateOf(false)
        var activeEnabled by mutableStateOf(true)
        var callbacks = 0
        compose.setContent {
            SettingsTheme {
                Column {
                    SettingsList {
                        SettingsToggle("Active", checked = checked, enabled = activeEnabled,
                            onCheckedChange = { checked = it; callbacks++ })
                        SettingsToggle("Disabled", enabled = false, onCheckedChange = { callbacks++ })
                    }
                    SettingsToggle(label = "Master", header = true, checked = masterChecked,
                        onCheckedChange = { masterChecked = it; callbacks++ })
                    SettingsToggle(label = "Disabled master", header = true, enabled = false,
                        onCheckedChange = { callbacks++ })
                }
            }
        }
        compose.onAllNodes(isToggleable()).assertCountEquals(4)
        compose.onAllNodes(isToggleable(), useUnmergedTree = true).assertCountEquals(4)
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(4)
        for (label in listOf("Active", "Master")) {
            val row = compose.onNodeWithText(label)
            // Both pointer paths retain the one row action. A cancelled switch
            // gesture must not bubble up as an extra row change.
            for (switchRegion in listOf(false, true)) {
                row.performTouchInput {
                    down(if (switchRegion) Offset(width - 38.dp.toPx(), center.y) else center)
                    advanceEventTime(160)
                    cancel()
                }.assertIsOff()
                compose.runOnIdle { assertEquals(if (label == "Active") 0 else 2, callbacks) }
            }
            row.performTouchInput { click(Offset(width - 38.dp.toPx(), center.y)) }.assertIsOn()
            row.performTouchInput { click(center) }.assertIsOff()
        }
        for (label in listOf("Disabled", "Disabled master")) {
            compose.onNodeWithText(label).assertIsNotEnabled().performClick().assertIsOff()
                .performTouchInput { click(Offset(width - 38.dp.toPx(), center.y)) }.assertIsOff()
        }
        val active = compose.onNodeWithText("Active")
        active.performTouchInput { down(Offset(width - 38.dp.toPx(), center.y)) }
        compose.runOnIdle { activeEnabled = false }
        active.performTouchInput { up() }.assertIsNotEnabled().assertIsOff()
        compose.runOnIdle {
            assertEquals(4, callbacks)
            activeEnabled = true
            // Caller state updates have no command side effect.
            checked = true
            masterChecked = true
        }
        compose.onNodeWithText("Active").assertIsOn()
        compose.onNodeWithText("Master").assertIsOn()
        compose.runOnIdle { assertEquals(4, callbacks) }
    }

    @Test fun disabledChoiceKeepsItsIndependentTrailingActionAndIconSize() {
        var choices = 0
        var actions = 0
        var style by mutableStateOf(SettingsChoiceRowStyle.Menu)
        compose.setContent {
            SettingsTheme {
                SettingsList {
                    SettingsChoiceRow("Unavailable choice", false, { choices++ }, enabled = false, style = style,
                        trailingAction = {
                            SettingsIconButton({ actions++ }, "Configure choice") {
                                Box(Modifier.size(24.dp).testTag("action-artwork"))
                            }
                        })
                }
            }
        }
        for ((index, choiceStyle) in SettingsChoiceRowStyle.entries.withIndex()) {
            compose.runOnIdle { style = choiceStyle }
            compose.onNodeWithText("Unavailable choice").assertIsNotEnabled().performClick()
            compose.onNodeWithTag("action-artwork", useUnmergedTree = true)
                .assertWidthIsEqualTo(24.dp).assertHeightIsEqualTo(24.dp)
            compose.onNodeWithContentDescription("Configure choice").assertIsEnabled()
                .assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
                .performTouchInput { click() }
            compose.runOnIdle { assertEquals(0, choices); assertEquals(index + 1, actions) }
        }
    }

    @Test fun menuRowAndCompactSwitchKeepSeparateActions() {
        var navigations = 0
        var changes = 0
        var rowEnabled by mutableStateOf(true)
        var switchEnabled by mutableStateOf(true)
        compose.setContent {
            SettingsTheme {
                SettingsList {
                    SettingsListItem(name = "Details", onClick = { navigations++ },
                        enabled = rowEnabled, homeMenu = true, trailingDivider = true,
                        leadingContent = { Box(Modifier.size(24.dp)) },
                        trailingContent = {
                            SettingsSwitch(false, { changes++ }, "Enable feature",
                                enabled = switchEnabled, compact = true)
                        })
                }
            }
        }
        compose.onNodeWithText("Details").performTouchInput { down(center); advanceEventTime(200); cancel() }
        compose.onNodeWithContentDescription("Enable feature").performTouchInput {
            down(center); advanceEventTime(200); cancel()
        }
        compose.runOnIdle { assertEquals(0, navigations); assertEquals(0, changes) }
        compose.onNodeWithText("Details").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, navigations); assertEquals(0, changes) }
        compose.onNodeWithContentDescription("Enable feature").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, navigations); assertEquals(1, changes); rowEnabled = false }
        compose.onNodeWithText("Details").assertIsNotEnabled().performClick()
        compose.onNodeWithContentDescription("Enable feature").assertIsEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, navigations); assertEquals(2, changes) }
        val switch = compose.onNodeWithContentDescription("Enable feature")
        switch.performTouchInput { down(center) }
        compose.runOnIdle { switchEnabled = false }
        switch.performTouchInput { up() }.assertIsNotEnabled().performClick()
            .performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, navigations); assertEquals(2, changes) }
    }

    @Test fun menuChoicesKeepCallerSelectionAndCancelWithoutCallbacks() {
        var selected by mutableStateOf("First")
        var enabled by mutableStateOf(true)
        var style by mutableStateOf(SettingsChoiceRowStyle.Menu)
        var callbacks = 0
        lateinit var inputMode: InputModeManager
        compose.setContent {
            inputMode = LocalInputModeManager.current
            SettingsTheme {
                SettingsList {
                    for (label in listOf("First", "Second")) {
                        SettingsChoiceRow(label, selected == label,
                            { selected = label; callbacks++ }, enabled = enabled, style = style)
                    }
                }
            }
        }
        compose.onAllNodes(isSelectable()).assertCountEquals(2)
        for (choiceStyle in SettingsChoiceRowStyle.entries) {
            compose.runOnIdle {
                style = choiceStyle; selected = "First"; enabled = true; callbacks = 0
            }
            for (label in listOf("First", "Second")) {
                compose.onNodeWithText(label).performTouchInput { down(center); advanceEventTime(200); cancel() }
                compose.runOnIdle { assertEquals("First", selected); assertEquals(0, callbacks) }
            }
            compose.onNodeWithText("Second").performTouchInput { click() }
            compose.onNodeWithText("Second").assertIsSelected()
            compose.runOnIdle { assertEquals(1, callbacks); selected = "First" }
            compose.onNodeWithText("First").assertIsSelected()
            val originalInputMode = compose.runOnIdle { inputMode.inputMode }
            try {
                compose.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
                compose.onNodeWithText("Second").performSemanticsAction(SemanticsActions.RequestFocus) { assertTrue(it()) }
                compose.onNodeWithText("Second").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
                compose.onNodeWithText("Second").assertIsSelected()
                compose.runOnIdle { assertEquals(2, callbacks); enabled = false }
                compose.onNodeWithText("First").assertIsNotEnabled().performTouchInput { click() }
                compose.onNodeWithText("First").performClick()
                compose.runOnIdle { assertEquals("Second", selected); assertEquals(2, callbacks) }
            } finally {
                InstrumentationRegistry.getInstrumentation().setInTouchMode(originalInputMode == InputMode.Touch)
            }
        }
    }

    @Test fun confirmationAndDismissalInvokeOnlyTheirOwnCallbacks() {
        var visible by mutableStateOf(true)
        var confirmed = 0
        var dismissed = 0
        compose.setContent {
            SettingsTheme {
                SettingsConfirmationDialog(visible, "Confirm action", "Details", "Confirm", "Cancel",
                    onConfirm = { confirmed++; visible = false }, onDismiss = { dismissed++; visible = false })
            }
        }
        compose.onAllNodes(hasClickAction()).assertCountEquals(2)
        for (label in listOf("Cancel", "Confirm")) {
            compose.onNodeWithText(label).performTouchInput {
                down(center)
                advanceEventTime(200)
                cancel()
            }
        }
        compose.onNodeWithText("Confirm action").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, confirmed); assertEquals(0, dismissed) }
        compose.onNodeWithText("Cancel").performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, confirmed); assertEquals(1, dismissed); visible = true }
        compose.onNodeWithText("Confirm").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, confirmed); assertEquals(1, dismissed) }
        compose.onNodeWithText("Confirm action").assertDoesNotExist()
    }

    @Test fun textEntryKeepsNamedActionsAndCallerOwnedValidation() {
        val state = TextFieldState()
        lateinit var inputMode: InputModeManager
        var requests = 0
        var visible by mutableStateOf(true)
        compose.setContent {
            SettingsTheme {
                SettingsDialog(visible, "A title that wraps within the caller's narrow dialog", { visible = false },
                    modifier = Modifier.width(240.dp).testTag("editor-dialog"),
                    presentation = SettingsDialogPresentation.TextEntry,
                    actions = listOf(SettingsDialogAction("Cancel", { visible = false }),
                        SettingsDialogAction("Save", { requests++; visible = false }, enabled = state.text.isNotBlank()))) {
                    inputMode = LocalInputModeManager.current
                    SettingsInputField(state, keyboardOptions = KeyboardOptions(showKeyboardOnFocus = false))
                }
            }
        }
        compose.onNodeWithTag("editor-dialog").assertWidthIsEqualTo(240.dp)
        compose.onNodeWithText("Save").assertHasClickAction().assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(0, requests) }
        fun assertAndroidActions(saveEnabled: Boolean) {
            compose.waitForIdle()
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val serviceInfo = automation.serviceInfo
            val originalFlags = serviceInfo.flags
            try {
                serviceInfo.flags = originalFlags and AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS.inv()
                automation.serviceInfo = serviceInfo
                val nodes = mutableListOf<AccessibilityNodeInfo>()
                fun collect(node: AccessibilityNodeInfo) {
                    nodes += node
                    for (index in 0 until node.childCount) node.getChild(index)?.let(::collect)
                }
                var root: AccessibilityNodeInfo? = null
                compose.waitUntil(3_000) {
                    root = automation.rootInActiveWindow
                    root != null
                }
                collect(requireNotNull(root))
                for (label in listOf("Cancel", "Save")) {
                    val named = nodes.filter { it.text?.toString() == label }
                    assertEquals("One Android accessibility name for $label", 1, named.size)
                    val action = named.single()
                    assertTrue("The named node owns the click", action.isClickable)
                    assertEquals(label == "Cancel" || saveEnabled, action.isEnabled)
                }
            } finally {
                serviceInfo.flags = originalFlags
                automation.serviceInfo = serviceInfo
            }
        }
        assertAndroidActions(saveEnabled = false)
        onView(isAssignableFrom(EditText::class.java)).perform(replaceText("Changed"))
        assertAndroidActions(saveEnabled = true)
        compose.onAllNodes(hasClickAction()).assertCountEquals(2)
        for (label in listOf("Cancel", "Save")) {
            compose.onNodeWithText(label).performTouchInput { down(center); advanceEventTime(200); cancel() }
            compose.runOnIdle { assertTrue(visible); assertEquals(0, requests) }
            compose.onNodeWithTag("editor-dialog").assertIsDisplayed()
        }
        compose.onNodeWithText("Save").assertIsEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals("Changed", state.text.toString()); assertEquals(1, requests) }
        compose.onNodeWithTag("editor-dialog").assertDoesNotExist()
        compose.runOnIdle { visible = true }
        compose.onNodeWithTag("editor-dialog").assertIsDisplayed()
        compose.onNodeWithText("Save").assertIsEnabled()
        onView(isAssignableFrom(EditText::class.java)).perform(replaceText("Reopened"))
        compose.onNodeWithText("Cancel").performTouchInput { click() }
        compose.onNodeWithTag("editor-dialog").assertDoesNotExist()
        compose.runOnIdle { assertEquals("Reopened", state.text.toString()); assertEquals(1, requests) }
        compose.runOnIdle { visible = true }
        compose.onNodeWithTag("editor-dialog").assertIsDisplayed()
        val originalInputMode = compose.runOnIdle { inputMode.inputMode }
        try {
            compose.runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
            compose.onNodeWithText("Save").performSemanticsAction(SemanticsActions.RequestFocus) { assertTrue(it()) }
            compose.onNodeWithText("Save").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
            compose.onNodeWithTag("editor-dialog").assertDoesNotExist()
            compose.runOnIdle { assertEquals("Reopened", state.text.toString()); assertEquals(2, requests) }
        } finally {
            // Android touch mode outlives the dialog and can affect subsequent tests.
            InstrumentationRegistry.getInstrumentation().setInTouchMode(originalInputMode == InputMode.Touch)
        }
    }

    @Test fun longDialogBodyScrollsWithoutMovingTitleOrActions() {
        compose.setContent {
            SettingsTheme {
                Box(Modifier.height(400.dp)) {
                    SettingsDialog(true, "Fixed title", {}, actions = listOf(
                        SettingsDialogAction("Cancel", {}), SettingsDialogAction("Confirm", {}),
                    )) { repeat(30) { Text("Line $it", Modifier.height(30.dp)) } }
                }
            }
        }
        val titleBounds = compose.onNodeWithText("Fixed title").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("Line 29").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Fixed title").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithText("Confirm").assertIsDisplayed().assertHasClickAction()
        assertEquals(titleBounds, compose.onNodeWithText("Fixed title").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun wideDialogLabelsStackAndDisabledActionCannotInvokeCaller() {
        val first = "Cancel this operation and return without changing anything"
        val second = "Apply the requested change after checking all the details"
        var requests = 0
        compose.setContent {
            SettingsTheme {
                SettingsDialog(true, "Actions", {}, modifier = Modifier.width(240.dp), actions = listOf(
                    SettingsDialogAction(first, { requests++ }),
                    SettingsDialogAction(second, { requests++ }, enabled = false),
                )) { Text("Details") }
            }
        }
        val firstBounds = compose.onNodeWithText(first).fetchSemanticsNode().boundsInRoot
        val secondBounds = compose.onNodeWithText(second).fetchSemanticsNode().boundsInRoot
        assertTrue("Long actions must be reachable in separate rows", secondBounds.top >= firstBounds.bottom)
        compose.onNodeWithText(second).assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(0, requests) }
        compose.onNodeWithText(first).performClick()
        compose.runOnIdle { assertEquals(1, requests) }
    }

    @Test fun verticalControlIncreasesUpwardAndKeepsValuesInRange() {
        var value by mutableFloatStateOf(0f)
        compose.setContent {
            SettingsTheme {
                Column {
                    SettingsVerticalSeekBar(value, { value = it }, -10f..10f,
                        Modifier.height(240.dp).testTag("gain"))
                    Text("Gain $value")
                }
            }
        }
        compose.onNodeWithTag("gain").performTouchInput {
            down(center); advanceEventTime(200); cancel()
        }
        compose.runOnIdle { assertEquals(0f, value) }
        compose.onNodeWithTag("gain").performTouchInput { swipeUp() }
        compose.runOnIdle { assertTrue("Dragging up must increase gain: $value", value > 0f && value <= 10f) }
    }

    @Test fun modeArtworkAndCaptionShareOneAccessibleSelectionAction() {
        var selected by mutableStateOf("Off")
        val requested = mutableListOf<String>()
        compose.setContent {
            SettingsTheme {
                SettingsModeStrip(listOf("Off", "Ambient", "Cancel"), selected,
                    onSelect = { selected = it; requested += it },
                    label = { it }, artwork = { _, _ -> })
            }
        }
        compose.onAllNodes(isSelectable()).assertCountEquals(3)
        // Cancelling a press on either hit area must not request a mode change.
        compose.onNodeWithContentDescription("Ambient").performTouchInput {
            down(Offset(center.x, 22.dp.toPx()))
            advanceEventTime(200)
            cancel()
        }.assertIsNotSelected()
        compose.onNodeWithContentDescription("Cancel").performTouchInput {
            down(Offset(center.x, height - 2f))
            advanceEventTime(200)
            cancel()
        }.assertIsNotSelected()
        compose.onNodeWithContentDescription("Off").assertIsSelected()
        compose.runOnIdle { assertTrue(requested.isEmpty()) }
        // Touch the artwork half of an option, then the caption half of another.
        compose.onNodeWithContentDescription("Ambient").performTouchInput {
            click(Offset(center.x, 22.dp.toPx()))
        }.assertIsSelected()
        compose.onNodeWithContentDescription("Cancel").performTouchInput {
            click(Offset(center.x, height - 2f))
        }.assertIsSelected()
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.runOnIdle { assertEquals(listOf("Ambient", "Cancel"), requested) }
    }

    @Test fun equalizerLabelsBelongToRangesAndDisabledGainCannotRequestChanges() {
        var enabled by mutableStateOf(true)
        var bands by mutableStateOf(listOf(
            SettingsEqualizerBand("Low", 0, "Low gain"),
            SettingsEqualizerBand("Mid", 0, "Mid gain"),
            SettingsEqualizerBand("High", 0, "High gain"),
        ))
        var callbacks = 0
        compose.setContent {
            SettingsTheme {
                SettingsEqualizer(bands, { index, value ->
                    bands = bands.mapIndexed { i, band -> if (i == index) band.copy(value = value) else band }
                    callbacks++
                }, enabled = enabled)
            }
        }
        val range = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        compose.onAllNodes(range).assertCountEquals(3)
        compose.onNodeWithContentDescription("Low gain").assert(range)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(5f) }
        compose.runOnIdle { assertEquals(5, bands[0].value); assertEquals(1, callbacks); enabled = false }
        compose.onNodeWithContentDescription("Mid gain").assert(range).assertIsNotEnabled()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(10f) }
        compose.runOnIdle { assertEquals(0, bands[1].value); assertEquals(1, callbacks) }
    }

    @Test fun equalizerPreservesCallerRangeAndSingleStepUpdates() {
        var bands by mutableStateOf(listOf(
            SettingsEqualizerBand("Low", 50, "Low level"),
            SettingsEqualizerBand("Mid", 50, "Mid level"),
            SettingsEqualizerBand("High", 50, "High level"),
        ))
        val updates = mutableListOf<Pair<Int, Int>>()
        compose.setContent {
            SettingsTheme {
                SettingsEqualizer(bands, { index, value ->
                    bands = bands.mapIndexed { i, band -> if (i == index) band.copy(value = value) else band }
                    updates += index to value
                }, valueRange = 0..100, valueLabel = { "Level $it" })
            }
        }
        val low = compose.onNodeWithContentDescription("Low level")
        low.assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo(50f, 0f..100f, 99)))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(51f) }
        compose.onNodeWithText("Level 51").assertExists()
        compose.runOnIdle { assertEquals(listOf(0 to 51), updates) }
        low.performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        low.performSemanticsAction(SemanticsActions.SetProgress) { it(100f) }
        compose.runOnIdle { assertEquals(listOf(0 to 51, 0 to 0, 0 to 100), updates) }
        // A device snapshot remains caller state and must not echo a command.
        compose.runOnIdle { bands = bands.map { it.copy(value = 49) } }
        low.assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo(49f, 0f..100f, 99)))
        compose.runOnIdle { assertEquals(3, updates.size) }
    }

    @Test fun equalizerKeyboardAndAccessibilityMoveOneIntegerStep() {
        var range by mutableStateOf(-10..10)
        var value by mutableIntStateOf(0)
        compose.setContent {
            SettingsTheme {
                SettingsEqualizer(listOf(SettingsEqualizerBand("Low", value, "Low level")),
                    { _, next -> value = next }, valueRange = range)
            }
        }
        val control = compose.onNodeWithContentDescription("Low level")
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun findControl(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.contentDescription?.toString() == "Low level") return node
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                findControl(child)?.let { return it }
            }
            return null
        }
        fun accessibilityStep(action: Int) {
            compose.waitForIdle()
            val node = findControl(requireNotNull(automation.rootInActiveWindow))
            assertTrue("The Android accessibility range must accept a step", requireNotNull(node).performAction(action))
        }
        for ((domain, initial) in listOf((-10..10) to 0, (0..100) to 50)) {
            compose.runOnIdle { range = domain; value = initial }
            control.assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(initial.toFloat(), domain.first.toFloat()..domain.last.toFloat(), domain.last - domain.first - 1)))
            control.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            control.performKeyInput { pressKey(Key.DirectionUp) }
            compose.runOnIdle { assertEquals(initial + 1, value) }
            control.performKeyInput { pressKey(Key.DirectionDown) }
            compose.runOnIdle { assertEquals(initial, value) }
            accessibilityStep(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            compose.runOnIdle { assertEquals(initial + 1, value) }
            accessibilityStep(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            compose.runOnIdle { assertEquals(initial, value) }
        }
    }

    @Test fun horizontalRowNamesItsRangeAndKeepsDisabledValues() {
        var enabled by mutableStateOf(true)
        var value by mutableFloatStateOf(0f)
        var callbacks = 0
        compose.setContent {
            SettingsTheme {
                SettingsSeekBarRow("왼쪽과 오른쪽 균형", value, {
                    value = it
                    callbacks++
                }, 0f..32f, "왼쪽", "오른쪽", enabled = enabled, steps = 31)
            }
        }
        val range = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        compose.onAllNodes(range).assertCountEquals(1)
        val control = compose.onNodeWithContentDescription("왼쪽과 오른쪽 균형")
        control.assert(range).performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
        compose.runOnIdle { assertEquals(4f, value); assertEquals(1, callbacks); enabled = false }
        control.assertIsNotEnabled()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(12f) }
        compose.runOnIdle { assertEquals(4f, value); assertEquals(1, callbacks) }
    }

    @Test fun expandableHeaderKeepsNamedNavigationAndCallerContext() {
        val scroll = ScrollState(0)
        var allowExpansion by mutableStateOf(true)
        var backCalls = 0
        compose.setContent {
            SettingsTheme {
                SettingsScaffold(modifier = Modifier.testTag("expandable-viewport"),
                    title = "Screen title", subtitle = "Left side", backLabel = "Go back",
                    showBackButton = true, onNavigateBack = { backCalls++ },
                    expandableHeader = allowExpansion, scrollState = scroll) {
                    Text("Caller content", Modifier.height(100.dp))
                }
            }
        }
        val viewport = compose.onNodeWithTag("expandable-viewport")
        fun assertNamedContent() {
            compose.onAllNodesWithText("Screen title").assertCountEquals(1)
            compose.onAllNodesWithText("Left side").assertCountEquals(1)
            compose.onAllNodesWithContentDescription("Go back").assertCountEquals(1)
            compose.onNodeWithText("Caller content").assertIsDisplayed()
        }
        assertNamedContent()
        val collapsedTitle = compose.onNodeWithText("Screen title").fetchSemanticsNode().boundsInRoot
        val origin = viewport.fetchSemanticsNode().boundsInRoot.topLeft
        val navigation = compose.onNodeWithContentDescription("Go back").fetchSemanticsNode().boundsInRoot
        viewport.performTouchInput {
            val start = Offset(width * .7f, navigation.center.y - origin.y)
            swipe(start, start + Offset(0f, 250.dp.toPx()), 600)
        }
        assertNamedContent()
        compose.runOnIdle { assertEquals(0, backCalls); assertEquals(0, scroll.value) }
        assertTrue(compose.onNodeWithText("Screen title").fetchSemanticsNode().boundsInRoot.height > collapsedTitle.height)
        compose.onNodeWithContentDescription("Go back").performTouchInput { click() }
        val expandedTitle = compose.onNodeWithText("Screen title").fetchSemanticsNode().boundsInRoot
        viewport.performTouchInput {
            swipe(expandedTitle.center - origin,
                expandedTitle.center - origin - Offset(0f, 250.dp.toPx()), 600)
        }
        assertNamedContent()
        assertEquals(collapsedTitle, compose.onNodeWithText("Screen title").fetchSemanticsNode().boundsInRoot)
        viewport.performTouchInput {
            val start = Offset(width * .7f, navigation.center.y - origin.y)
            swipe(start, start + Offset(0f, 250.dp.toPx()), 600)
        }
        compose.runOnIdle { assertEquals(1, backCalls); allowExpansion = false }
        assertNamedContent()
        compose.onNodeWithText("Screen title").assertIsDisplayed()
        assertEquals(collapsedTitle, compose.onNodeWithText("Screen title").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithContentDescription("Go back").performTouchInput { click() }
        compose.runOnIdle { assertEquals(2, backCalls) }
    }

    @Test fun hiddenHeaderLetsTouchesReachScrolledContent() {
        val scroll = ScrollState(0)
        var backCalls = 0
        var actionCalls = 0
        var contentCalls = 0
        lateinit var scope: kotlinx.coroutines.CoroutineScope
        compose.setContent {
            scope = rememberCoroutineScope()
            SettingsTheme {
                SettingsScaffold(modifier = Modifier.height(400.dp).testTag("settings-viewport"),
                    title = "Screen title", backLabel = "Go back", showBackButton = true,
                    onNavigateBack = { backCalls++ }, scrollState = scroll,
                    actionButtons = listOf({ SettingsIconButton({ actionCalls++ }, "Header action") { Text("A") } })) {
                    Box(Modifier.fillMaxWidth().height(2400.dp).clickable { contentCalls++ })
                }
            }
        }
        val viewport = compose.onNodeWithTag("settings-viewport")
        val origin = viewport.fetchSemanticsNode().boundsInRoot.topLeft
        val backPosition = compose.onNodeWithContentDescription("Go back").fetchSemanticsNode().boundsInRoot.center - origin
        val actionPosition = compose.onNodeWithContentDescription("Header action").fetchSemanticsNode().boundsInRoot.center - origin
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { scope.launch { scroll.scrollTo(1500) } }
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithContentDescription("Go back").assertDoesNotExist()
        compose.onNodeWithContentDescription("Header action").assertDoesNotExist()
        viewport.performTouchInput { click(backPosition); click(actionPosition) }
        compose.runOnIdle { assertEquals(0, backCalls); assertEquals(0, actionCalls); assertEquals(2, contentCalls) }
        compose.mainClock.autoAdvance = true
    }

    @Test fun scrolledContentKeepsOneReachableBackAction() {
        val scroll = ScrollState(0)
        var backCalls = 0
        compose.setContent {
            SettingsTheme {
                SettingsScaffold(modifier = Modifier.height(400.dp).testTag("settings-viewport"),
                    title = "Screen title", backLabel = "Go back",
                    showBackButton = true, onNavigateBack = { backCalls++ }, scrollState = scroll) {
                    repeat(30) { Text("Entry $it", Modifier.height(80.dp)) }
                }
            }
        }
        compose.onNodeWithTag("settings-viewport").assertHeightIsEqualTo(400.dp)
        compose.onNodeWithText("Entry 29").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertTrue(scroll.value > 0) }
        compose.onNodeWithText("Screen title").assertDoesNotExist()
        compose.waitUntil(timeoutMillis = 3_000) {
            compose.onAllNodesWithContentDescription("Go back").fetchSemanticsNodes().size == 1
        }
        val back = compose.onNodeWithContentDescription("Go back").assertHasClickAction()
        val viewport = compose.onNodeWithTag("settings-viewport")
        val center = back.fetchSemanticsNode().boundsInRoot.center -
            viewport.fetchSemanticsNode().boundsInRoot.topLeft
        // The floating icon's 24dp artwork must retain its expanded 48dp target.
        viewport.performTouchInput {
            click(center + Offset(-20.dp.toPx(), 0f))
            click(center + Offset(20.dp.toPx(), 0f))
        }
        compose.runOnIdle { assertEquals(2, backCalls) }
    }
}
