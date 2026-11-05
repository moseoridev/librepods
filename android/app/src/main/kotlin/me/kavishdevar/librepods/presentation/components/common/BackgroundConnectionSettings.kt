package me.kavishdevar.librepods.presentation.components.common

import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.IntentSender
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.CompanionConnection
import me.kavishdevar.librepods.presentation.components.primitives.StyledListItem

@Composable
fun BackgroundConnectionSettings() {
    val context = LocalContext.current
    var approved by remember { mutableStateOf(CompanionConnection.associations(context).isNotEmpty()) }
    val unavailable = stringResource(R.string.background_setup_unavailable)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        approved = CompanionConnection.associations(context).isNotEmpty()
        CompanionConnection.reconcile(context)
    }
    LifecycleResumeEffect(Unit) {
        approved = CompanionConnection.associations(context).isNotEmpty()
        CompanionConnection.reconcile(context)
        onPauseOrDispose { }
    }
    StyledListItem(
        contentText = stringResource(if (approved) R.string.background_connection_ready else R.string.background_connection_setup),
        supportingText = stringResource(if (approved) R.string.background_connection_ready_description else R.string.background_connection_setup_description),
        onClick = {
            try {
                val manager = checkNotNull(context.getSystemService(CompanionDeviceManager::class.java))
                val filter = BluetoothDeviceFilter.Builder().addServiceUuid(CompanionConnection.AIRPODS_UUID, null).build()
                manager.associate(AssociationRequest.Builder().addDeviceFilter(filter).build(),
                    context.mainExecutor, object : CompanionDeviceManager.Callback() {
                        override fun onAssociationPending(intentSender: IntentSender) {
                            launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                        }
                        override fun onAssociationCreated(associationInfo: AssociationInfo) {
                            approved = true
                            CompanionConnection.reconcile(context)
                        }
                        override fun onFailure(error: CharSequence?) {
                            Toast.makeText(context, error ?: unavailable, Toast.LENGTH_LONG).show()
                        }
                    })
            } catch (error: Exception) {
                Toast.makeText(context, error.localizedMessage ?: unavailable, Toast.LENGTH_LONG).show()
            }
        }
    )
}
