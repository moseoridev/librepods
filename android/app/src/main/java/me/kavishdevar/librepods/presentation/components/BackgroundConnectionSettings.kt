package me.kavishdevar.librepods.presentation.components

import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.IntentSender
import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.CompanionConnection
import me.kavishdevar.librepods.services.AirPodsService

@Composable
private fun rememberAssociationRequest(onCreated: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val created by rememberUpdatedState(onCreated)
    val unavailable = stringResource(R.string.background_setup_unavailable)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        CompanionConnection.reconcile(context)
    }
    return {
        try {
            val address = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("mac_address", "").orEmpty()
            val filter = BluetoothDeviceFilter.Builder().apply {
                if (address.isNotEmpty()) setAddress(address)
                else addServiceUuid(CompanionConnection.AIRPODS_UUID, null)
            }.build()
            val request = AssociationRequest.Builder().addDeviceFilter(filter).setSingleDevice(address.isNotEmpty()).build()
            val manager = context.getSystemService(CompanionDeviceManager::class.java)
            checkNotNull(manager) { unavailable }
            manager.associate(request, context.mainExecutor, object : CompanionDeviceManager.Callback() {
                override fun onAssociationPending(intentSender: IntentSender) {
                    launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
                override fun onAssociationCreated(associationInfo: AssociationInfo) {
                    CompanionConnection.select(context, associationInfo)
                    created()
                }
                override fun onFailure(error: CharSequence?) {
                    Toast.makeText(context, error ?: unavailable, Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: Exception) {
            Toast.makeText(context, e.localizedMessage ?: unavailable, Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun ConnectionSetupNotice() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var shown by remember { mutableStateOf(!preferences.getBoolean("connection_lifetime_notice_seen", false)) }
    val request = rememberAssociationRequest()
    fun dismiss() {
        preferences.edit { putBoolean("connection_lifetime_notice_seen", true) }
        shown = false
    }
    if (shown) AlertDialog(
        onDismissRequest = { dismiss() },
        title = { Text(stringResource(R.string.background_connection_title)) },
        text = { Text(stringResource(R.string.background_connection_notice)) },
        confirmButton = {
            TextButton(onClick = {
                dismiss()
                if (CompanionConnection.selectedAssociation(context) == null) request()
            }) { Text(stringResource(R.string.background_connection_setup)) }
        },
        dismissButton = { TextButton(onClick = { dismiss() }) { Text(stringResource(R.string.background_connection_later)) } }
    )
}

@Composable
fun BackgroundConnectionSettings() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var association by remember { mutableStateOf(CompanionConnection.selectedAssociation(context)) }
    var detection by remember { mutableStateOf(preferences.getBoolean(CompanionConnection.DETECTION_PREFERENCE, false)) }
    val request = rememberAssociationRequest { association = CompanionConnection.selectedAssociation(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) association = CompanionConnection.selectedAssociation(context)
        }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == CompanionConnection.DETECTION_PREFERENCE) detection = preferences.getBoolean(key, false)
            if (key == "mac_address") association = CompanionConnection.selectedAssociation(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    StyledList(title = stringResource(R.string.background_connection_title)) {
        StyledListItem(
            name = stringResource(if (association == null) R.string.background_connection_setup else R.string.background_connection_ready),
            description = stringResource(if (association == null) R.string.background_connection_setup_description else R.string.background_connection_ready_description),
            onClick = if (association == null) request else null
        )
    }
    StyledToggle(
        label = stringResource(R.string.background_detection_title),
        description = stringResource(R.string.background_detection_description),
        checked = detection,
        onCheckedChange = { enabled ->
            preferences.edit { putBoolean(CompanionConnection.DETECTION_PREFERENCE, enabled) }
            if (enabled) me.kavishdevar.librepods.bluetooth.NearbyDetection.synchronize(context)
        }
    )
}
