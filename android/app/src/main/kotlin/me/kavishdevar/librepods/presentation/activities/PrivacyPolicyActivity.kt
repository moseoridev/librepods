package me.kavishdevar.librepods.presentation.activities

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import me.kavishdevar.librepods.presentation.screens.onboarding.PrivacyPolicyPage
import me.kavishdevar.librepods.ui.theme.SettingsTheme

/** Standalone policy reader; it shows the same page as onboarding and closes on either action. */
class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val darkTheme = isSystemInDarkTheme()

            val view = LocalView.current
            val window = (view.context as Activity).window

            LaunchedEffect(darkTheme) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }

            SettingsTheme(darkTheme = darkTheme) {
                PrivacyPolicyPage(
                    onBack = { finish() },
                    onForward = { finish() },
                )
            }
        }
    }
}
