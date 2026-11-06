package me.kavishdevar.librepods.presentation.screens.apple

import android.graphics.Bitmap
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.click as clickEditor
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.platform.app.InstrumentationRegistry
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.core.bluetooth.aacp.types.ControlCommandIdentifier as Command
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.After
import org.junit.Before
import java.io.File

class DeviceSettingsScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Before fun keepTestWindowAwake() {
        compose.runOnUiThread { compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(id: Int) = context.getString(id)
    @After fun dismissTestKeyboard() { closeSoftKeyboard() }

    @Test fun batteryArtworkStaysAboveAlignedMetersAcrossViewports() {
        data class Viewport(val width: Int?, val dark: Boolean, val font: Float)
        var viewport by mutableStateOf(Viewport(null, true, 1.5f))
        val fixture = SettingsFixture("charging")
        compose.setContent {
            key(viewport) {
                FixtureViewport(viewport.width, viewport.font) {
                    BudsDeviceSettings(fixture.ui, fixture, darkTheme = viewport.dark, mediaVolume = { FixtureMediaVolume() })
                }
            }
        }
        for (width in listOf(null, 320, 360, 412, 600)) for (dark in listOf(true, false)) {
            compose.runOnIdle { viewport = Viewport(width, dark, if (dark) 1.5f else 1f) }
            val primaryArt = compose.onNodeWithTag("battery-primary-art").fetchSemanticsNode().boundsInRoot
            val caseArt = compose.onNodeWithTag("battery-case-art").fetchSemanticsNode().boundsInRoot
            val left = compose.onNodeWithTag("battery-LEFT").fetchSemanticsNode().boundsInRoot
            val right = compose.onNodeWithTag("battery-RIGHT").fetchSemanticsNode().boundsInRoot
            val case = compose.onNodeWithTag("battery-CASE").fetchSemanticsNode().boundsInRoot
            assertTrue("art overlaps ring at $viewport", primaryArt.bottom < left.top && caseArt.bottom < case.top)
            assertEquals(primaryArt.center.x, (left.center.x + right.center.x) / 2, 1.5f)
            assertEquals(caseArt.center.x, case.center.x, 1.5f)
            assertTrue(left.right <= right.left && right.right <= case.left)
            capture("charging-${width}-${if (dark) "dark-font150" else "light"}")
        }
    }

    @Test fun zeroIsDistinctFromMissingAndHeadsetHasNoCase() {
        var fixture by mutableStateOf(SettingsFixture("zero_unknown"))
        compose.setContent {
            key(fixture) { BudsDeviceSettings(fixture.ui, fixture, mediaVolume = { FixtureMediaVolume() }) }
        }
        compose.onNodeWithTag("battery-LEFT").assertContentDescriptionEquals("${text(R.string.left)} 0%")
        compose.onNodeWithTag("battery-RIGHT").assertContentDescriptionEquals("${text(R.string.right)} —")
        compose.onNodeWithTag("battery-CASE").assertContentDescriptionEquals("${text(R.string.case_alt)} —")
        capture("zero-unknown")
        compose.runOnIdle { fixture = SettingsFixture("headset") }
        compose.onNodeWithTag("battery-HEADSET").assertIsDisplayed()
        compose.onNodeWithTag("battery-LEFT").assertDoesNotExist()
        compose.onNodeWithTag("battery-CASE").assertDoesNotExist()
        compose.onNodeWithTag("battery-case-art").assertDoesNotExist()
        capture("headset")
    }

    @Test fun requestedModeIsNotShownAsAcknowledgedAndDisconnectedDoesNotSend() {
        var fixture by mutableStateOf(SettingsFixture("pending"))
        compose.setContent {
            key(fixture) { BudsDeviceSettings(fixture.ui, fixture, mediaVolume = { FixtureMediaVolume() }) }
        }
        val noise = compose.onNodeWithContentDescription(text(R.string.noise_cancellation))
        noise.assertIsNotSelected().performClick().assertIsNotSelected()
        compose.runOnIdle {
            assertArrayEquals(byteArrayOf(2), fixture.ui.state.controlStates[Command.LISTENING_MODE])
            assertFalse(fixture.ui.state.confirmedControlStates.containsKey(Command.LISTENING_MODE))
            fixture = SettingsFixture("disconnected")
        }
        noise.performClick()
        compose.runOnIdle { assertTrue(fixture.events.isEmpty()) }
    }

    @Test fun callChoicesRemainCoupledAcrossBothPages() {
        val fixture = SettingsFixture()
        var page by mutableStateOf(DevicePage.Mute)
        compose.setContent {
            key(page) { BudsDeviceSettings(fixture.ui, fixture, initialPage = page, mediaVolume = { FixtureMediaVolume() }) }
        }
        compose.onNodeWithText(text(R.string.press_twice)).performClick()
        compose.runOnIdle {
            assertArrayEquals(byteArrayOf(0, 2), fixture.ui.state.controlStates[Command.CALL_MANAGEMENT_CONFIG])
            page = DevicePage.EndCall
        }
        compose.onNodeWithText(text(R.string.press_once)).assertIsSelected()
        compose.onNodeWithText(text(R.string.press_twice)).performClick()
        compose.runOnIdle { page = DevicePage.Mute }
        compose.onNodeWithText(text(R.string.press_once)).assertIsSelected()
    }

    @Test fun screenshotNavigationRowsReachTheirRealDestinations() {
        var start by mutableStateOf(DevicePage.Home)
        var iteration by mutableIntStateOf(0)
        val fixture = SettingsFixture()
        compose.setContent {
            key(iteration) { BudsDeviceSettings(fixture.ui, fixture, initialPage = start, mediaVolume = { FixtureMediaVolume() }) }
        }
        val routes = listOf(
            Triple(DevicePage.Home, R.string.audio_routing, DevicePage.Audio),
            Triple(DevicePage.Home, R.string.controls_gestures, DevicePage.Controls),
            Triple(DevicePage.Home, R.string.accessibility, DevicePage.Accessibility),
            Triple(DevicePage.Home, R.string.battery, DevicePage.Battery),
            Triple(DevicePage.Home, R.string.version, DevicePage.Version),
            Triple(DevicePage.Home, R.string.name, DevicePage.Name),
            Triple(DevicePage.Audio, R.string.equalizer, DevicePage.Equalizer),
            Triple(DevicePage.Audio, R.string.microphone_mode, DevicePage.Microphone),
            Triple(DevicePage.Audio, R.string.automatic_connection, DevicePage.Routing),
            Triple(DevicePage.Controls, R.string.left, DevicePage.Left),
            Triple(DevicePage.Controls, R.string.right, DevicePage.Right),
            Triple(DevicePage.Controls, R.string.mute_unmute, DevicePage.Mute),
            Triple(DevicePage.Controls, R.string.hang_up, DevicePage.EndCall),
            Triple(DevicePage.Controls, R.string.head_gestures, DevicePage.Head),
        )
        for ((source, label, destination) in routes) {
            compose.runOnIdle { start = source; iteration++ }
            compose.onNodeWithText(text(label)).performScrollTo().performClick()
            compose.onNodeWithTag("device-page-${destination.name}").assertIsDisplayed()
            compose.onNodeWithContentDescription(text(R.string.back)).performClick()
            compose.onNodeWithTag("device-page-${source.name}").assertIsDisplayed()
        }
    }

    @Test fun equalizerUsesExclusiveChoicesAndCannotResetWhileDisconnected() {
        val fixture = SettingsFixture()
        compose.setContent { BudsDeviceSettings(fixture.ui, fixture, initialPage = DevicePage.Equalizer, mediaVolume = { FixtureMediaVolume() }) }
        compose.onNodeWithText(text(R.string.recommended)).assertIsSelected()
        compose.onNodeWithText(text(R.string.custom)).performClick().assertIsSelected()
        compose.onNodeWithText(text(R.string.recommended)).assertIsNotSelected()
        compose.runOnIdle { fixture.setCustomEq(10, 20, 30); fixture.disconnect(); fixture.events.clear() }
        compose.onNodeWithText(text(R.string.reset)).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.custom)).performScrollTo().assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(listOf(10, 20, 30), fixture.ui.state.customEq.let { listOf(it.low, it.mid, it.high) })
            assertTrue(fixture.events.isEmpty())
        }
    }

    @Test fun longPressChoicesStayIndependentAndPreserveOtherBits() {
        val fixture = SettingsFixture().apply { setControlCommand(Command.LISTENING_MODE_CONFIGS, 0xa5); events.clear() }
        compose.setContent { BudsDeviceSettings(fixture.ui, fixture, initialPage = DevicePage.Left, mediaVolume = { FixtureMediaVolume() }) }
        compose.onNodeWithText(text(R.string.off)).performScrollTo().assertIsOn().performClick().assertIsOff()
        compose.onNodeWithText(text(R.string.transparency)).performScrollTo().assertIsOn().performClick().assertIsOff()
        compose.onNodeWithText(text(R.string.adaptive)).performScrollTo().performClick().assertIsOn()
        compose.runOnIdle {
            assertEquals(0xa8, fixture.ui.state.controlStates[Command.LISTENING_MODE_CONFIGS]!!.first().toInt() and 255)
            assertEquals(3, fixture.events.size)
        }
    }

    @Test fun renameValidatesBlankInputAndSavesKoreanWithKeyboardOpen() {
        val fixture = SettingsFixture()
        lateinit var host: View
        compose.setContent {
            host = LocalView.current
            FixtureViewport(320, 1.5f) {
                BudsDeviceSettings(fixture.ui, fixture, initialPage = DevicePage.Name, mediaVolume = { FixtureMediaVolume() })
            }
        }
        fun keyboardVisible(): Boolean {
            var visible = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                visible = host.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
            }
            return visible
        }
        val editor = onView(isAssignableFrom(EditText::class.java))
        editor.perform(clickEditor(), replaceText("   "))
        compose.onNodeWithText(text(R.string.widget_save)).assertIsNotEnabled()
        val name = "검증용 AirPods " + "긴 이름 ".repeat(12).trim()
        editor.perform(replaceText("  $name  "))
        compose.waitUntil(5000) { keyboardVisible() }
        capture("interaction-name-keyboard")
        compose.onNodeWithText(text(R.string.widget_save)).performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithTag("device-page-Home").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(name, fixture.ui.metadata.name)
            assertEquals(listOf("rename: $name"), fixture.events.toList())
        }
        compose.waitUntil(5000) { !keyboardVisible() }
    }

    @Test fun keyboardBackDismissesBeforeLeavingAndUnsavedNameIsDiscarded() {
        val fixture = SettingsFixture()
        lateinit var host: View
        compose.setContent {
            host = LocalView.current
            BudsDeviceSettings(fixture.ui, fixture, initialPage = DevicePage.Name, mediaVolume = { FixtureMediaVolume() })
        }
        var keyboard = false
        fun refreshKeyboard(): Boolean {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                keyboard = host.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
            }
            return keyboard
        }
        onView(isAssignableFrom(EditText::class.java)).perform(clickEditor(), replaceText("저장하지 않은 이름"))
        compose.waitUntil(5000) { refreshKeyboard() }
        pressBack()
        compose.waitUntil(5000) { !refreshKeyboard() }
        compose.onNodeWithTag("device-page-Name").assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.back)).performClick()
        compose.onNodeWithTag("device-page-Home").assertIsDisplayed()
        compose.runOnIdle { assertEquals("AirPods UI Fixture", fixture.ui.metadata.name); assertTrue(fixture.events.isEmpty()) }
    }

    @Test fun horizontalSlidersDragWithoutMixingMediaAndToneCommands() {
        val fixture = SettingsFixture()
        compose.setContent { BudsDeviceSettings(fixture.ui, fixture, mediaVolume = { FixtureMediaVolume() }) }
        val media = compose.onNodeWithContentDescription(text(R.string.media_volume_label))
        media.performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        media.performTouchInput { swipe(center, centerRight, 300) }
        capture("interaction-media-after-drag")
        val mediaProgress = media.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
        compose.runOnIdle {
            assertTrue("Media progress after drag: $mediaProgress", mediaProgress >= 14f)
            assertTrue(fixture.events.isEmpty())
            assertEquals(50, fixture.ui.state.controlStates[Command.CHIME_VOLUME]!!.first().toInt())
        }
        compose.onNodeWithText(text(R.string.accessibility)).performScrollTo().performClick()
        val tone = compose.onNodeWithContentDescription(text(R.string.tone_volume))
        tone.performScrollTo().performTouchInput { swipe(center, centerRight, 300) }
        compose.onNodeWithContentDescription(text(R.string.back)).performClick()
        compose.waitUntil(3000) { fixture.ui.state.controlStates[Command.CHIME_VOLUME]!!.first().toInt() >= 95 }
        compose.runOnIdle { assertTrue(fixture.events.all { it.startsWith("CHIME_VOLUME:") }) }
    }

    @Test fun equalizerTouchAndResetKeepThreeBandStateAcrossNavigation() {
        val fixture = SettingsFixture()
        compose.setContent { BudsDeviceSettings(fixture.ui, fixture, initialPage = DevicePage.Equalizer, mediaVolume = { FixtureMediaVolume() }) }
        compose.onNodeWithText(text(R.string.custom)).performClick()
        val low = compose.onNodeWithContentDescription(text(R.string.eq_low))
        low.performScrollTo().performTouchInput { swipe(center, topCenter, 300) }
        compose.waitUntil(3000) { fixture.ui.state.customEq.low > 80 }
        compose.onNodeWithContentDescription(text(R.string.eq_mid)).performSemanticsAction(SemanticsActions.SetProgress) { it(20f) }
        compose.onNodeWithContentDescription(text(R.string.eq_high)).performSemanticsAction(SemanticsActions.SetProgress) { it(80f) }
        compose.waitUntil(3000) { fixture.ui.state.customEq.mid == 20 && fixture.ui.state.customEq.high == 80 }
        compose.onNodeWithText(text(R.string.reset)).performScrollTo().performClick()
        compose.onNodeWithContentDescription(text(R.string.back)).performClick()
        compose.waitUntil(3000) { fixture.ui.state.customEq.let { it.low == 50 && it.mid == 50 && it.high == 50 } }
        compose.onNodeWithTag("device-page-Audio").assertIsDisplayed()
        compose.runOnIdle { assertEquals("EQ: 50 50 50", fixture.events.last()) }
    }

    @Test fun scrollingDoesNotActivateRowsAndTouchOpensTheIntendedChoice() {
        val fixture = SettingsFixture()
        compose.setContent { BudsDeviceSettings(fixture.ui, fixture, mediaVolume = { FixtureMediaVolume() }) }
        compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
        compose.onNodeWithTag("device-page-Home").assertIsDisplayed()
        compose.runOnIdle { assertTrue(fixture.events.isEmpty()) }
        compose.onNodeWithText(text(R.string.audio_routing)).performScrollTo().performTouchInput { click() }
        compose.onNodeWithTag("device-page-Audio").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.microphone_mode)).performScrollTo().performTouchInput { click() }
        compose.onNodeWithText(text(R.string.microphone_always_right)).performTouchInput { click() }
        compose.runOnIdle {
            assertArrayEquals(byteArrayOf(1), fixture.ui.state.controlStates[Command.MIC_MODE])
            assertEquals(listOf("MIC_MODE: 1"), fixture.events.toList())
        }
    }

    private fun capture(name: String) {
        val directory = File(context.getExternalFilesDir(null), "settings-fixtures").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            assertTrue(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

}
