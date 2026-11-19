package me.kavishdevar.librepods.presentation.overlays

import android.os.SystemClock
import android.graphics.Bitmap
import android.provider.Settings
import android.view.MotionEvent
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import me.kavishdevar.librepods.core.devices.Battery
import me.kavishdevar.librepods.core.devices.BatteryComponent
import me.kavishdevar.librepods.core.devices.BatterySource
import me.kavishdevar.librepods.core.devices.BatteryStatus
import me.kavishdevar.librepods.data.app.DesignSystem
import me.kavishdevar.librepods.database.app.AppSettingsEntity
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class ProximityCardWindowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun touchThroughTheVisibleSamsungBannerReachesUnderlyingButton() {
        val activity = compose.activity
        val context = activity.applicationContext
        assumeTrue("Application overlay permission is needed", Settings.canDrawOverlays(context))
        val card = ProximityCardWindow(context, AppSettingsEntity(designSystem = DesignSystem.Samsung))
        val screenTop = activity.windowManager.currentWindowMetrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()).top
        val windowLocation = IntArray(2)
        compose.runOnUiThread {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.window.decorView.getLocationOnScreen(windowLocation)
        }
        var clicked = false
        compose.setContent {
            val bannerTop = with(LocalDensity.current) { (screenTop + 16 - windowLocation[1]).toDp() } + 16.dp
            Box(Modifier.fillMaxSize()) {
                Button(onClick = { clicked = true },
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = bannerTop + 5.dp)) {
                    Text("Underlying menu")
                }
            }
        }
        val target = compose.onNodeWithText("Underlying menu").fetchSemanticsNode().boundsInWindow.center
        val x = windowLocation[0] + target.x
        val y = windowLocation[1] + target.y
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val background = instrumentation.uiAutomation.takeScreenshot()
        compose.runOnUiThread {
            card.show("Connected AirPods", setOf(
                Battery(BatteryComponent.LEFT, 83, BatteryStatus.NOT_CHARGING, BatterySource.AACP, 0L),
                Battery(BatteryComponent.RIGHT, 83, BatteryStatus.CHARGING, BatterySource.AACP, 0L),
                Battery(BatteryComponent.CASE, 42, BatteryStatus.NOT_CHARGING, BatterySource.AACP, 0L),
            ), airPodsConnected = true)
        }
        try {
            val directory = File(context.getExternalFilesDir(null), "settings-fixtures").apply { mkdirs() }
            // The overlay uses a separate ComposeView and Android draw clock.
            // Confirm it has actually drawn before injecting the underlying tap.
            compose.waitUntil(3_000) {
                val frame = instrumentation.uiAutomation.takeScreenshot()
                val onScreen = frame.getPixel(x.toInt(), y.toInt()) != background.getPixel(x.toInt(), y.toInt())
                frame.recycle()
                onScreen
            }
            // Advance the Compose clock as well as waiting for Android's window draw.
            compose.mainClock.advanceTimeBy(240)
            compose.waitForIdle()
            val settled = instrumentation.uiAutomation.takeScreenshot()
            File(directory, "samsung-connection-banner.png").outputStream().use {
                assertTrue(settled.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            settled.recycle()
            val downAt = SystemClock.uptimeMillis()
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(downAt, SystemClock.uptimeMillis(), action, x, y, 0)
                try {
                    event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
                    assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true))
                } finally {
                    event.recycle()
                }
            }
            compose.runOnIdle {
                assertTrue("Banner must still be visible", card.isVisible)
                assertTrue("The underlying menu should receive the tap", clicked)
            }
            compose.waitUntil(6_000) { !card.isVisible }
        } finally {
            background.recycle()
            compose.runOnUiThread { card.forceClose() }
        }
    }
}
