package me.kavishdevar.librepods.presentation.screens.apple

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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

    private fun capture(name: String) {
        val directory = File(context.getExternalFilesDir(null), "settings-fixtures").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            assertTrue(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
