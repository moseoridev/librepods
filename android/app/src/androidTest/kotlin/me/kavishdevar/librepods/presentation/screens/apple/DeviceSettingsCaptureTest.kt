package me.kavishdevar.librepods.presentation.screens.apple

import android.graphics.Bitmap
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Evidence generation only. No reference-pixel or behavior-parity assertion. */
class DeviceSettingsCaptureTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun keepCaptureWindowAwake() {
        compose.runOnUiThread { compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    @Test fun captureProductPagesForVisualComparison() {
        data class Configuration(val width: Int, val dark: Boolean, val font: Float) {
            val label get() = "${width}-${if (dark) "dark" else "light"}-font${(font * 100).toInt()}"
        }
        var page by mutableStateOf(DevicePage.Home)
        var config by mutableStateOf(Configuration(320, false, 1f))
        val fixture = SettingsFixture("long_name")
        compose.setContent {
            key(page, config) {
                FixtureViewport(config.width, config.font) {
                    BudsDeviceSettings(fixture.ui, fixture, initialPage = page, darkTheme = config.dark,
                        mediaVolume = { FixtureMediaVolume() })
                }
            }
        }
        for (width in listOf(320, 360, 412, 600)) for (dark in listOf(false, true)) for (font in listOf(1f, 1.5f)) {
            val next = Configuration(width, dark, font)
            for (target in DevicePage.entries) {
                compose.runOnIdle { page = target; config = next }
                compose.onNodeWithTag("device-page-${target.name}").assertIsDisplayed()
                capture("matrix-${target.name}-${next.label}-top")
                compose.onAllNodes(hasScrollAction()).onFirst().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 10000f) }
                capture("matrix-${target.name}-${next.label}-bottom")

            }
            // Report bounded progress while the full matrix runs.
            android.util.Log.i("SettingsUiMatrix", "Completed ${next.label}")
        }
    }

    private fun capture(name: String) {
        val directory = File(context.getExternalFilesDir(null), "settings-fixtures").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            assertTrue(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

}
