package me.kavishdevar.librepods.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.companion.AssociationInfo
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import me.kavishdevar.librepods.core.bluetooth.MacAddress
import me.kavishdevar.librepods.core.devices.ConnectionState
import me.kavishdevar.librepods.services.LibrePodsService
import kotlin.coroutines.resume

/** Event driven, multi-device classic connectivity. BLE presence alone never owns the engine. */
@SuppressLint("MissingPermission")
object CompanionConnection {
    val AIRPODS_UUID: ParcelUuid = ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val reconciler = ConnectionReconciler<Map<String, BluetoothDevice>>(scope)
    private val presence = PresenceObservation()
    private val _connected = MutableStateFlow<Map<String, BluetoothDevice>>(emptyMap())
    val connected = _connected.asStateFlow()
    private val _associated = MutableStateFlow<Map<String, BluetoothDevice>>(emptyMap())
    val associated = _associated.asStateFlow()
    private var engine: LibrePodsService? = null

    fun attach(service: LibrePodsService) {
        engine = service
        service.reconcileClassicConnections(connected.value)
    }

    fun detach(service: LibrePodsService) {
        if (engine === service) engine = null
    }

    fun associations(context: Context): List<AssociationInfo> = try {
        context.getSystemService(CompanionDeviceManager::class.java)?.myAssociations.orEmpty()
    } catch (error: Exception) {
        Log.w("CompanionConnection", "Cannot read associations", error)
        emptyList()
    }

    fun reconcile(context: Context, completed: () -> Unit = {}) {
        val app = context.applicationContext
        reconciler.refresh(read = {
            val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter
            val permitted = app.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            if (!permitted || adapter?.isEnabled != true) emptyMap()
            else coroutineScope {
                val known = associations(app).mapNotNull { it.deviceMacAddress?.toString() }.toSet()
                adapter.bondedDevices.filter { device ->
                    known.any { it.equals(device.address, true) } || device.uuids?.contains(AIRPODS_UUID) == true
                }.map { device -> async {
                    if (isConnected(app, adapter, device)) device.address to device else null
                } }.awaitAll().filterNotNull().toMap()
            }
        }, apply = { update(app, it.orEmpty()) },
            failed = { Log.w("CompanionConnection", "Classic query failed", it) }, completed = completed)
    }

    fun disconnected(context: Context, address: String? = null) {
        reconciler.disconnect {
            update(context.applicationContext, if (address == null) emptyMap()
                else connected.value.filterKeys { !it.equals(address, true) })
        }
    }

    @Suppress("DEPRECATION") // Required for Android13–15 address API compatibility.
    private fun update(context: Context, devices: Map<String, BluetoothDevice>) {
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        val addresses = associations(context).mapNotNull { a ->
            a.deviceMacAddress?.toString()?.let { a.id to it }
        }.toMap()
        presence.update(addresses, devices.keys,
            start = { checkNotNull(manager).startObservingDevicePresence(it) },
            stop = { checkNotNull(manager).stopObservingDevicePresence(it) },
            failed = { Log.w("CompanionConnection", "Presence update failed", it) })
        _connected.value = devices
        val active = addresses.filterKeys { presence.isObserving(it) }.values
        _associated.value = devices.filterKeys { address -> active.any { it.equals(address, true) } }
        engine?.reconcileClassicConnections(devices)
    }

    private suspend fun isConnected(context: Context, adapter: BluetoothAdapter, device: BluetoothDevice): Boolean {
        if (Build.VERSION.SDK_INT >= 36 && Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
            return device.isConnected(BluetoothDevice.TRANSPORT_BREDR)
        }
        return hasControl(device) || profileConnected(context, adapter, device, BluetoothProfile.A2DP) ||
            profileConnected(context, adapter, device, BluetoothProfile.HEADSET) || hasControl(device)
    }

    private fun hasControl(device: BluetoothDevice) =
        engine?.devices?.value?.get(MacAddress(device.address))?.connectionState?.value == ConnectionState.CONNECTED

    private suspend fun profileConnected(context: Context, adapter: BluetoothAdapter, device: BluetoothDevice, profile: Int): Boolean =
        withTimeoutOrNull(1500) {
            suspendCancellableCoroutine { result ->
                val listener = object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        val yes = try { device in proxy.connectedDevices } catch (_: SecurityException) { false }
                        finally { adapter.closeProfileProxy(profile, proxy) }
                        if (result.isActive) result.resume(yes)
                    }
                    override fun onServiceDisconnected(profile: Int) {
                        if (result.isActive) result.resume(false)
                    }
                }
                if (!adapter.getProfileProxy(context, listener, profile) && result.isActive) result.resume(false)
            }
        } ?: false
}
