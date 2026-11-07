package me.kavishdevar.librepods.presentation.screens.apple

import android.graphics.Bitmap
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.view.WindowCompat
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
        val requestedPage = InstrumentationRegistry.getArguments().getString("capturePage")
        val pages = requestedPage?.let { listOf(DevicePage.valueOf(it)) } ?: DevicePage.entries
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
            compose.runOnUiThread {
                WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            // The system redraws its bar icons on a later traversal than this call. Capture
            // immediately after a dark→light switch and the first frame can still show the
            // previous icon contrast, which is a capture artifact, not product rendering.
            compose.waitForIdle()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            android.os.SystemClock.sleep(250)
            for (target in pages) {
                val previousPage = page
                compose.runOnIdle { page = target; config = next }
                compose.onNodeWithTag("device-page-${target.name}").assertIsDisplayed()
                if (target == DevicePage.Name || previousPage == DevicePage.Name) {
                    // Compose idle excludes platform Dialog window enter/exit animations.
                    // Keep system animations unchanged and capture after that transition.
                    compose.waitForIdle()
                    android.os.SystemClock.sleep(400)
                }
                capture("matrix-${target.name}-${next.label}-top")
                if (target != DevicePage.Name) {
                    val scroll = compose.onAllNodes(hasScrollAction()).onFirst()
                    var frame = 0
                    while (true) {
                        val node = scroll.fetchSemanticsNode()
                        val axis = node.config[SemanticsProperties.VerticalScrollAxisRange]
                        val before = compose.runOnIdle { axis.value() }
                        if (compose.runOnIdle { axis.maxValue() - before } < 1f) break
                        // Overlap viewports so long-page middle content is not skipped.
                        scroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, node.boundsInRoot.height * .7f) }
                        val after = compose.runOnIdle { axis.value() }
                        check(after > before) { "Capture scroll made no progress: $target ${next.label}" }
                        check(++frame <= 30) { "Unbounded capture scroll: $target ${next.label}" }
                        capture("matrix-${target.name}-${next.label}-scroll$frame")
                    }
                }
                capture("matrix-${target.name}-${next.label}-bottom")

            }
            // Report bounded progress while the full matrix runs.
            android.util.Log.i("SettingsUiMatrix", "Completed ${next.label}")
        }
    }

    private fun capture(name: String) {
        val directory = File(context.getExternalFilesDir(null), "settings-fixtures").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            compose.waitForIdle()
            assertTrue(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

}
