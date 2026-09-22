package me.kavishdevar.librepods.services

import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.bluetooth.CompanionConnection

/** The system owns this binding. The UI continues to use AirPodsService's separate LocalBinder. */
class AirPodsCompanionService : CompanionDeviceService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var bound = false
    private var engine: AirPodsService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (!bound) return
            engine = (binder as AirPodsService.LocalBinder).getService()
            CompanionConnection.companionDevice.value?.let {
                engine?.onBluetoothConnected(it)
                CompanionConnection.engineOwner.value = it.address
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            engine = null
            CompanionConnection.engineOwner.value = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            CompanionConnection.companionDevice.collect { device ->
                if (device != null && CompanionConnection.selectedAssociation(this@AirPodsCompanionService) != null) {
                    if (!bound) bound = bindService(Intent(this@AirPodsCompanionService, AirPodsService::class.java), connection, BIND_AUTO_CREATE)
                    engine?.let {
                        it.onBluetoothConnected(device)
                        CompanionConnection.engineOwner.value = device.address
                    }
                } else releaseEngine()
            }
        }
        // CDM can rebind after process death without replaying an appearance callback.
        CompanionConnection.reconcile(this)
    }

    @Suppress("DEPRECATION")
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        // A BLE proximity callback alone is not permission to start connection work.
        CompanionConnection.reconcile(this)
    }

    @Suppress("DEPRECATION")
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        CompanionConnection.reconcile(this)
    }

    private fun releaseEngine() {
        CompanionConnection.engineOwner.value = null
        if (bound) unbindService(connection)
        bound = false
        engine = null
    }

    override fun onDestroy() {
        scope.cancel()
        releaseEngine()
        super.onDestroy()
    }
}
