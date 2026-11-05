package me.kavishdevar.librepods.presentation.screens.settings

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.NearbyDetection
import me.kavishdevar.librepods.presentation.components.primitives.StyledScaffold
import me.kavishdevar.librepods.presentation.components.primitives.StyledToggle
import me.kavishdevar.librepods.presentation.viewmodel.AppSettingsViewModel

@Composable
fun BLESettingsScreenRoute(viewModel: AppSettingsViewModel, navigateBack: (() -> Unit)?) {
    BLESettingsScreen(navigateBack)
}

/** User opt-in; arbitrary scan rates cannot bypass the bounded background policy. */
@Composable
fun BLESettingsScreen(navigateBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(preferences.getBoolean(NearbyDetection.PREFERENCE, false)) }
    fun setEnabled(value: Boolean) {
        preferences.edit().putBoolean(NearbyDetection.PREFERENCE, value).apply()
        enabled = value
        NearbyDetection.synchronize(context)
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        setEnabled(result.values.all { it })
    }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == NearbyDetection.PREFERENCE) enabled = preferences.getBoolean(key, false)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    StyledScaffold(title = stringResource(R.string.background_detection_title), navigateBack = navigateBack) { top, bottom ->
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(top))
            StyledToggle(label = stringResource(R.string.background_detection_title),
                description = stringResource(R.string.background_detection_description), checked = enabled,
                onCheckedChange = { requested ->
                    val required = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                    if (requested && required.any { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED })
                        permissions.launch(required)
                    else setEnabled(requested)
                })
            Spacer(Modifier.height(bottom))
        }
    }
}
