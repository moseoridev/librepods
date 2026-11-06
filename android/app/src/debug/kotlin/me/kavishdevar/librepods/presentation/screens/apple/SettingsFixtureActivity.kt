package me.kavishdevar.librepods.presentation.screens.apple

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt
import me.kavishdevar.librepods.core.devices.AirPodsModel

/** Debug-only entry: renders the real screen and routes every intent to memory. */
class SettingsFixtureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        render(intent)
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); render(intent) }

    private fun render(input: Intent) {
        val scenario = input.getStringExtra("scenario")?.takeIf { it in SettingsFixture.scenarios } ?: "normal"
        val page = DevicePage.entries.firstOrNull { it.name.equals(input.getStringExtra("page"), true) } ?: DevicePage.Home
        val model = AirPodsModel.entries.firstOrNull { it.name.equals(input.getStringExtra("model"), true) }
        val dark = input.getStringExtra("theme") != "light"
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark })
        val width = input.getIntExtra("width_dp", 0).takeIf { it in 320..840 }
        val fontScale = input.getFloatExtra("font_scale", 1f).coerceIn(1f, 2f)
        val fixture = if (model == null) SettingsFixture(scenario) else SettingsFixture(scenario, model)
        setContent {
            FixtureViewport(width, fontScale) {
                key(fixture) {
                    BudsDeviceSettings(fixture.ui, fixture, initialPage = page, darkTheme = dark,
                        navigateBack = ::finish, mediaVolume = { FixtureMediaVolume() })
                }
            }
        }
    }
}

/** Simulate logical viewport/font size inside this composition; no system setting changes. */
@Composable
internal fun FixtureViewport(widthDp: Int? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
    val physicalDensity = LocalDensity.current
    val physicalConfiguration = LocalConfiguration.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = widthDp?.let { constraints.maxWidth.toFloat() / it } ?: physicalDensity.density
        val configuration = Configuration(physicalConfiguration).apply {
            screenWidthDp = widthDp ?: physicalConfiguration.screenWidthDp
            screenHeightDp = (constraints.maxHeight / density).roundToInt()
            densityDpi = (density * 160).roundToInt()
            this.fontScale = fontScale
        }
        CompositionLocalProvider(LocalDensity provides Density(density, fontScale), LocalConfiguration provides configuration) {
            content()
        }
    }
}

/** Same One UI primitive, with no AudioManager or lifecycle receiver. */
@Composable
internal fun FixtureMediaVolume() {
    var volume by remember { mutableIntStateOf(7) }
    MediaVolumeControl(volume, 15, true) { volume = it }
}
