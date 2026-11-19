package me.kavishdevar.librepods.presentation.overlays

import android.os.SystemClock
import android.provider.Settings
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

class ProximityCardWindowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun touchAboveConnectedCardReachesUnderlyingButton() {
        val activity = compose.activity
        val context = activity.applicationContext
        assumeTrue("Application overlay permission is needed", Settings.canDrawOverlays(context))
        val card = ProximityCardWindow(context, AppSettingsEntity(designSystem = DesignSystem.Samsung))
        var clicked = false
        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                Button(onClick = { clicked = true },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 222.dp)) {
                    Text("Underlying menu")
                }
            }
        }
        val target = compose.onNodeWithText("Underlying menu").fetchSemanticsNode().boundsInWindow.center
        val windowLocation = IntArray(2)
        compose.runOnUiThread {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.window.decorView.getLocationOnScreen(windowLocation)
            card.show("Connected AirPods", setOf(
                Battery(BatteryComponent.LEFT, 70, BatteryStatus.NOT_CHARGING, BatterySource.AACP, 0L),
            ), airPodsConnected = true)
        }
        try {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.waitForIdleSync()
            val x = windowLocation[0] + target.x
            val y = windowLocation[1] + target.y
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
                assertTrue("Card must still be visible", card.isVisible)
                assertTrue("The underlying menu should receive the tap", clicked)
            }
        } finally {
            compose.runOnUiThread { card.forceClose() }
        }
    }
}
