package me.kavishdevar.librepods.presentation.screens.apple

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.platform.app.InstrumentationRegistry
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.core.bluetooth.aacp.types.ControlCommandIdentifier as Command
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class DeviceSettingsScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(id: Int) = context.getString(id)

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

    @Test fun callChoiceUsesIosCoupledMappingAndVersionPreservesRawValue() {
        val fixture = SettingsFixture()
        var page by mutableStateOf(DevicePage.Mute)
        compose.setContent {
            key(page) { BudsDeviceSettings(fixture.ui, fixture, initialPage = page, mediaVolume = { FixtureMediaVolume() }) }
        }
        compose.onNodeWithText(text(R.string.press_twice)).performClick()
        compose.runOnIdle {
            assertArrayEquals(byteArrayOf(0, 2), fixture.ui.state.controlStates[Command.CALL_MANAGEMENT_CONFIG])
            page = DevicePage.Version
        }
        compose.onNodeWithText("9A350").assertIsDisplayed()
        compose.onNodeWithText("9442784").assertIsDisplayed()
        capture("version")
    }

    @Test fun everyProductPageRendersAndReturnsToItsParent() {
        var page by mutableStateOf(DevicePage.Home)
        var large by mutableStateOf(false)
        val fixture = SettingsFixture("long_name")
        compose.setContent {
            key(page, large) {
                FixtureViewport(if (large) 320 else 360, if (large) 1.5f else 1f) {
                    BudsDeviceSettings(fixture.ui, fixture, initialPage = page, darkTheme = large,
                        mediaVolume = { FixtureMediaVolume() })
                }
            }
        }
        for (enlarged in listOf(false, true)) for (target in DevicePage.entries) {
            compose.runOnIdle { page = target; large = enlarged }
            compose.onNodeWithTag("device-page-${target.name}").assertIsDisplayed()
            capture("page-${target.name}-${if (enlarged) "320-dark-font150" else "360-light"}-top")
            compose.onAllNodes(hasScrollAction()).onFirst().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 10000f) }
            capture("page-${target.name}-${if (enlarged) "320-dark-font150" else "360-light"}-bottom")
            target.parent?.let { parent ->
                compose.onNodeWithContentDescription(text(R.string.back)).performClick()
                compose.onNodeWithTag("device-page-${parent.name}").assertIsDisplayed()
            }
        }
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

    private fun capture(name: String) {
        val directory = File(context.getExternalFilesDir(null), "settings-fixtures").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            assertTrue(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
