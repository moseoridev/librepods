package me.kavishdevar.librepods.simulation

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.kavishdevar.librepods.core.bluetooth.MacAddress
import me.kavishdevar.librepods.core.devices.AppleDevice
import me.kavishdevar.librepods.core.devices.ConnectionState
import me.kavishdevar.librepods.core.simulation.SIMULATED_AIRPODS_MAC
import me.kavishdevar.librepods.core.simulation.SimulatedAirPods
import me.kavishdevar.librepods.core.simulation.SimulatedAirPodsSession
import me.kavishdevar.librepods.services.LibrePodsService

/**
 * Debug-only owner of the simulated accessory: binds the engine, installs one
 * [SimulatedAirPodsSession], and removes every trace on teardown. The binding is kept for the
 * lifetime of the installed device so the engine outlives the broadcast receiver.
 */
object SimulatedAirPodsController {
    private const val TIMEOUT_MS = 5_000L

    private val mac = MacAddress(SIMULATED_AIRPODS_MAC)
    private var service: LibrePodsService? = null
    private var binding: ServiceConnection? = null
    private var session: SimulatedAirPodsSession? = null

    suspend fun add(context: Context, scenarioKey: String?): String {
        val app = context.applicationContext
        val engine = service ?: bind(app)
        ?: return "error: LibrePodsService did not bind; open the app once first"
        if (session != null) return "error: ${mac.value} is already installed"

        val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return "error: no BluetoothAdapter"
        val scenario = SimulatedAirPods.scenario(scenarioKey)
        // A fixed, locally administered address: getRemoteDevice only builds a handle, it does
        // not bond, connect or make the address reachable over Bluetooth.
        val remote = adapter.getRemoteDevice(mac.value)
        val device = AppleDevice(app, adapter, remote, ConnectionState.DISCONNECTED)
        session = SimulatedAirPods.attach(device, scenario)
        engine.addSimulatedAppleDevice(device)
        return "added ${mac.value} scenario=${scenario.key} model=${scenario.modelNumber}"
    }

    suspend fun remove(context: Context): String {
        val removedSession = session ?: return "error: no simulated device installed"
        session = null
        // Disconnect first so the live collectors cancel alerts and close overlays, then drop
        // the device and its persisted rows.
        removedSession.stop()
        service?.removeSimulatedAppleDevice(mac)
        unbind(context.applicationContext)
        return "removed ${mac.value}"
    }

    fun status(): String {
        val active = session ?: return "idle"
        return "installed ${mac.value} scenario=${active.scenario.key} model=${active.scenario.modelNumber}"
    }

    fun scenarios(): String =
        SimulatedAirPods.scenarios.joinToString("\n") { "${it.key}: ${it.description} (${it.modelNumber})" }

    @SuppressLint("MissingPermission")
    private suspend fun bind(context: Context): LibrePodsService? = withContext(Dispatchers.Main.immediate) {
        val connected = CompletableDeferred<LibrePodsService>()
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val engine = (binder as? LibrePodsService.LocalBinder)?.getService()
                if (engine != null) connected.complete(engine) else connected.completeExceptionally(
                    IllegalStateException("Unexpected binder")
                )
            }

            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        if (!context.bindService(Intent(context, LibrePodsService::class.java), connection, Context.BIND_AUTO_CREATE)) {
            return@withContext null
        }
        binding = connection
        val engine = withTimeoutOrNull(TIMEOUT_MS) { connected.await() }
        if (engine == null) unbind(context)
        service = engine
        engine
    }

    private fun unbind(context: Context) {
        binding?.let { runCatching { context.unbindService(it) } }
        binding = null
        service = null
    }
}
