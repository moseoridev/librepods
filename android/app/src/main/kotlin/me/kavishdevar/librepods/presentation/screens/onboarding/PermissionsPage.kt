package me.kavishdevar.librepods.presentation.screens.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

/**
 * The permission page in the Wearable `권한` shape: flat 16sp/13sp rows with 24dp icons under
 * 14sp group labels and no cards. The request chains, their order, the granted checks and the
 * forward gate are the upstream ones.
 */
@Composable
fun PermissionsPage(
    onBackward: () -> Unit,
    onForward: () -> Unit
) {
    var grantingAll = false

    val context = LocalContext.current
    val canDrawOverlays = remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val healthPermissions = rememberPermissionState(
        HealthPermission.getWritePermission(HeartRateRecord::class)
    ) {
        if (grantingAll) {
            if (!canDrawOverlays.value) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()
                )
                context.startActivity(intent)
            }
        }
    }

    val phonePermissionState = rememberMultiplePermissionsState(
        listOf(
            "android.permission.READ_PHONE_STATE",
            "android.permission.ANSWER_PHONE_CALLS"
        )
    ) {
        if (grantingAll) {
            if (!healthPermissions.status.isGranted) healthPermissions.launchPermissionRequest()
            else if (!canDrawOverlays.value) canDrawOverlays.value = Settings.canDrawOverlays(context)
        }
    }

    val notificationPermissionState = rememberPermissionState("android.permission.POST_NOTIFICATIONS") {
        if (grantingAll) {
            if (!phonePermissionState.allPermissionsGranted) phonePermissionState.launchMultiplePermissionRequest()
            else if (!healthPermissions.status.isGranted) healthPermissions.launchPermissionRequest()
            else if (!canDrawOverlays.value) canDrawOverlays.value = Settings.canDrawOverlays(context)
        }
    }

    val bluetoothPermissionsState = rememberMultiplePermissionsState(
        listOf(
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH_ADVERTISE"
        )
    ) {
        if (grantingAll) {
            if (!notificationPermissionState.status.isGranted) notificationPermissionState.launchPermissionRequest()
            else if (!phonePermissionState.allPermissionsGranted) phonePermissionState.launchMultiplePermissionRequest()
            else if (!healthPermissions.status.isGranted) healthPermissions.launchPermissionRequest()
            else if (!canDrawOverlays.value) canDrawOverlays.value = Settings.canDrawOverlays(context)
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
        bottom = {
            OnboardingBottomButtonRow {
                OnboardingPrimaryButton(
                    text = stringResource(R.string.grant_all),
                    onClick = {
                        grantingAll = true
                        if (!bluetoothPermissionsState.allPermissionsGranted) bluetoothPermissionsState.launchMultiplePermissionRequest()
                        else if (!notificationPermissionState.status.isGranted) notificationPermissionState.launchPermissionRequest()
                        else if (!phonePermissionState.allPermissionsGranted) phonePermissionState.launchMultiplePermissionRequest()
                        else if (!canDrawOverlays.value) canDrawOverlays.value =
                            Settings.canDrawOverlays(context)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !bluetoothPermissionsState.allPermissionsGranted || !notificationPermissionState.status.isGranted || !phonePermissionState.allPermissionsGranted || !canDrawOverlays.value,
                    style = SettingsButtonStyle.Tonal,
                )
                OnboardingPrimaryButton(
                    text = stringResource(R.string.onboarding_done),
                    onClick = onForward,
                    modifier = Modifier.weight(1f),
                    enabled = bluetoothPermissionsState.allPermissionsGranted,
                    fill = OnboardingStyle.WearablePrimaryFill,
                    label = OnboardingStyle.WearablePrimaryLabel,
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
        ) {
            OnboardingHero(title = stringResource(R.string.onboarding_permission_heading))
            Spacer(Modifier.height(OnboardingStyle.HeroToGroup))
            OnboardingGroupLabel(stringResource(R.string.required_permissions))
            OnboardingPermissionRow(
                title = stringResource(R.string.bluetooth),
                summary = stringResource(R.string.permission_description_bluetooth),
                onClick = if (!bluetoothPermissionsState.allPermissionsGranted) {
                    {
                        grantingAll = false
                        bluetoothPermissionsState.launchMultiplePermissionRequest()
                    }
                } else null,
                icon = MaterialIcons.Bluetooth,
            )

            OnboardingGroupLabel(stringResource(R.string.optional_permissions))
            OnboardingPermissionRow(
                title = stringResource(R.string.notifications),
                summary = stringResource(R.string.permission_description_notification),
                onClick = if (!notificationPermissionState.status.isGranted) {
                    {
                        grantingAll = false
                        notificationPermissionState.launchPermissionRequest()
                    }
                } else null,
                icon = MaterialIcons.Notifications,
            )
            OnboardingPermissionRow(
                title = stringResource(R.string.phone),
                summary = stringResource(R.string.permission_description_phone),
                onClick = if (!phonePermissionState.allPermissionsGranted) {
                    {
                        grantingAll = false
                        phonePermissionState.launchMultiplePermissionRequest()
                    }
                } else null,
                icon = MaterialIcons.Call,
            )

            OnboardingPermissionRow(
                title = stringResource(R.string.permission_healthconnect),
                summary = stringResource(R.string.permission_description_healthconnect),
                onClick = if (!healthPermissions.status.isGranted) {
                    {
                        grantingAll = false
                        healthPermissions.launchPermissionRequest()
                    }
                } else null,
                icon = MaterialIcons.VitalSigns,
            )
            OnboardingPermissionRow(
                title = stringResource(R.string.permission_overlay),
                summary = stringResource(R.string.permission_description_overlay),
                onClick = if (!canDrawOverlays.value) {
                    {
                        grantingAll = false
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:${context.packageName}".toUri()
                        )
                        context.startActivity(intent)
                    }
                } else null,
                icon = MaterialIcons.Overlay,
            )
        }
    }
}
