package me.kavishdevar.librepods.presentation.screens.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.icons.MaterialIcons
import me.kavishdevar.librepods.ui.components.SettingsButtonStyle
import me.kavishdevar.librepods.ui.components.SettingsList
import me.kavishdevar.librepods.ui.theme.SettingsStyle

private enum class PermissionStep { Bluetooth, Notifications, Phone, Health, Overlay }

@Composable
fun PermissionsPage(
    onBackward: () -> Unit,
    onForward: () -> Unit
) {
    val context = LocalContext.current
    val canDrawOverlays = remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val requestOverlay = {
        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()))
    }
    var grantStep by remember { mutableStateOf<PermissionStep?>(null) }
    val healthPermissions = rememberPermissionState(
        HealthPermission.getWritePermission(HeartRateRecord::class)
    ) { if (grantStep == PermissionStep.Health) grantStep = PermissionStep.Overlay }
    val phonePermissionState = rememberMultiplePermissionsState(
        listOf("android.permission.READ_PHONE_STATE", "android.permission.ANSWER_PHONE_CALLS")
    ) { if (grantStep == PermissionStep.Phone) grantStep = PermissionStep.Health }
    val notificationPermissionState = rememberPermissionState("android.permission.POST_NOTIFICATIONS") {
        if (grantStep == PermissionStep.Notifications) grantStep = PermissionStep.Phone
    }
    val bluetoothPermissionsState = rememberMultiplePermissionsState(
        listOf(
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH_ADVERTISE"
        )
    ) { results ->
        if (grantStep == PermissionStep.Bluetooth) grantStep = if (results.isNotEmpty() && results.values.all { it }) PermissionStep.Notifications else null
    }
    val bluetoothGranted = bluetoothPermissionsState.allPermissionsGranted
    LaunchedEffect(grantStep) {
        when (grantStep) {
            PermissionStep.Bluetooth -> if (bluetoothGranted) grantStep = PermissionStep.Notifications else bluetoothPermissionsState.launchMultiplePermissionRequest()
            PermissionStep.Notifications -> if (notificationPermissionState.status.isGranted) grantStep = PermissionStep.Phone else notificationPermissionState.launchPermissionRequest()
            PermissionStep.Phone -> if (phonePermissionState.allPermissionsGranted) grantStep = PermissionStep.Health else phonePermissionState.launchMultiplePermissionRequest()
            PermissionStep.Health -> if (healthPermissions.status.isGranted) grantStep = PermissionStep.Overlay else healthPermissions.launchPermissionRequest()
            PermissionStep.Overlay -> {
                grantStep = null
                if (!canDrawOverlays.value) requestOverlay()
            }
            null -> Unit
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlays.value = Settings.canDrawOverlays(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val scrollState = rememberScrollState()

    OnboardingPageScaffold(
        title = stringResource(R.string.permissions),
        onBack = onBackward,
        background = MaterialTheme.colorScheme.background,
        bottom = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val allGranted = bluetoothGranted && notificationPermissionState.status.isGranted &&
                    phonePermissionState.allPermissionsGranted && healthPermissions.status.isGranted && canDrawOverlays.value
                OnboardingPrimaryButton(
                    text = stringResource(R.string.grant_all),
                    onClick = { grantStep = PermissionStep.Bluetooth },
                    modifier = Modifier.weight(1f),
                    enabled = !allGranted && grantStep == null,
                    style = SettingsButtonStyle.Tonal,
                )
                OnboardingPrimaryButton(
                    text = stringResource(R.string.onboarding_done),
                    onClick = onForward,
                    modifier = Modifier.weight(1f),
                    enabled = bluetoothGranted && grantStep == null,
                    fill = OnboardingStyle.WearablePrimaryFill,
                    label = OnboardingStyle.WearablePrimaryLabel,
                )
            }
        }
    ) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = SettingsStyle.pageInset())
                .padding(bottom = 16.dp)
        ) {
            SettingsList(title = stringResource(R.string.required_permissions)) {
                OnboardingPermissionRow(
                    title = stringResource(R.string.bluetooth),
                    summary = stringResource(R.string.permission_description_bluetooth),
                    onClick = if (bluetoothGranted) null else ({ bluetoothPermissionsState.launchMultiplePermissionRequest() }),
                    icon = MaterialIcons.Bluetooth,
                )
            }
            SettingsList(title = stringResource(R.string.optional_permissions)) {
                OnboardingPermissionRow(
                    title = stringResource(R.string.notifications),
                    summary = stringResource(R.string.permission_description_notification),
                    onClick = if (notificationPermissionState.status.isGranted) null else ({ notificationPermissionState.launchPermissionRequest() }),
                    icon = MaterialIcons.Notifications,
                )
                OnboardingPermissionRow(
                    title = stringResource(R.string.phone),
                    summary = stringResource(R.string.permission_description_phone),
                    onClick = if (phonePermissionState.allPermissionsGranted) null else ({ phonePermissionState.launchMultiplePermissionRequest() }),
                    icon = MaterialIcons.Call,
                )
                OnboardingPermissionRow(
                    title = stringResource(R.string.permission_healthconnect),
                    summary = stringResource(R.string.permission_description_healthconnect),
                    onClick = if (healthPermissions.status.isGranted) null else ({ healthPermissions.launchPermissionRequest() }),
                    icon = MaterialIcons.VitalSigns,
                )
                OnboardingPermissionRow(
                    title = stringResource(R.string.permission_overlay),
                    summary = stringResource(R.string.permission_description_overlay),
                    onClick = requestOverlay.takeUnless { canDrawOverlays.value },
                    icon = MaterialIcons.Overlay,
                )
            }
        }
    }
}
