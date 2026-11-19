package me.kavishdevar.librepods.presentation.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class PermissionsPageTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun permissionGroupsAndActionsShowWithTheBluetoothGate() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        compose.setContent {
            SettingsTheme(darkTheme = false) { PermissionsPage({}, {}) }
        }

        for (label in listOf(R.string.required_permissions, R.string.bluetooth,
            R.string.optional_permissions, R.string.grant_all, R.string.onboarding_done)) {
            compose.onNodeWithText(context.getString(label)).assertIsDisplayed()
        }
        val bluetoothGranted = listOf(Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE).all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
        val done = compose.onNodeWithText(context.getString(R.string.onboarding_done))
        if (bluetoothGranted) done.assertIsEnabled() else done.assertIsNotEnabled()

        val directory = File(context.getExternalFilesDir(null), "settings-fixtures").apply { mkdirs() }
        File(directory, "permission-cards.png").outputStream().use {
            assertTrue(instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
