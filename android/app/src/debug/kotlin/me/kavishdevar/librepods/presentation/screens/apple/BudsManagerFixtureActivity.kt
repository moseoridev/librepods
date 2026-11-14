package me.kavishdevar.librepods.presentation.screens.apple

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle

/**
 * Debug-only entry for the Buds Manager comparison fixture.
 *
 * Inputs are the same three the native capture harness fixes: theme, density and font scale.
 * Nothing here reads or writes the device's own display settings; the configuration is built
 * from the intent and supplied through CompositionLocals.
 */
class BudsManagerFixtureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dark = intent.getStringExtra("theme") != "light"
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark })
        val configuration = Configuration(resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            // Preserve the system font step, including Samsung's 0.8 minimum.
            fontScale = intent.getFloatExtra("font_scale", resources.configuration.fontScale)
        }
        // Manager BaseActivity.z: its full-window phone landscape surface is fullscreen.
        // The comparison activity does not provide DeX or Samsung pop-over windows.
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            configuration.smallestScreenWidthDp < 420) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        }
        showBudsManagerPage(dark, configuration,
            page = intent.getStringExtra("page") ?: "Home",
            checked = intent.getBooleanExtra("checked", true))
    }
}
