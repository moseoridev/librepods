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

/** Android owns this binding; the engine keeps a separate UI LocalBinder. */
class AirPodsCompanionService : CompanionDeviceService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var bound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (bound) (binder as LibrePodsService.LocalBinder).getService()
                .reconcileClassicConnections(CompanionConnection.connected.value)
        }
        override fun onServiceDisconnected(name: ComponentName?) = Unit
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            CompanionConnection.associated.collect { devices ->
                if (devices.isNotEmpty()) {
                    if (!bound) bound = bindService(Intent(this@AirPodsCompanionService,
                        LibrePodsService::class.java), connection, BIND_AUTO_CREATE)
                } else releaseEngine()
            }
        }
        CompanionConnection.reconcile(this)
    }

    @Suppress("DEPRECATION")
    override fun onDeviceAppeared(associationInfo: AssociationInfo) = CompanionConnection.reconcile(this)
    @Suppress("DEPRECATION")
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) = CompanionConnection.reconcile(this)

    private fun releaseEngine() {
        if (bound) unbindService(connection)
        bound = false
    }

    override fun onDestroy() {
        releaseEngine()
        scope.cancel()
        super.onDestroy()
    }
}
