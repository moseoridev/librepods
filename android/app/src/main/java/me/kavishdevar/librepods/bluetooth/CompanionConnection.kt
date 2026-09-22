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
import androidx.core.content.edit
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import me.kavishdevar.librepods.services.ServiceManager

/** Event-driven reconciliation. No scanner, polling loop, or service binding is owned here. */
@SuppressLint("MissingPermission")
object CompanionConnection {
    const val DETECTION_PREFERENCE = "background_detection"
    val AIRPODS_UUID: ParcelUuid = ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _companionDevice = MutableStateFlow<BluetoothDevice?>(null)
    val companionDevice = _companionDevice.asStateFlow()
    internal val engineOwner = MutableStateFlow<String?>(null)
    private val presence = PresenceObservation()
    private val reconciler = ConnectionReconciler<BluetoothDevice>(scope)

    fun selectedAssociation(context: Context): AssociationInfo? {
        val address = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("mac_address", "")
        return associations(context).find { it.deviceMacAddress?.toString().equals(address, ignoreCase = true) }
    }

    private fun associations(context: Context): List<AssociationInfo> = try {
        context.getSystemService(CompanionDeviceManager::class.java)?.myAssociations.orEmpty()
    } catch (e: Exception) {
        Log.w("CompanionConnection", "Cannot read companion associations", e)
        emptyList()
    }

    fun select(context: Context, association: AssociationInfo) {
        val address = association.deviceMacAddress?.toString()?.uppercase() ?: return
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit { putString("mac_address", address) }
        reconcile(context)
    }

    /** Also used on boot, app resume and companion callbacks; late probes cannot resurrect a disconnect. */
    fun reconcile(context: Context, completed: () -> Unit = {}) {
        val app = context.applicationContext
        reconciler.refresh(
            read = {
                val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter
                val permitted = app.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                val savedAddress = app.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("mac_address", "").orEmpty()
                val candidates = if (permitted && adapter?.isEnabled == true) {
                    adapter.bondedDevices.filter {
                        if (savedAddress.isNotEmpty()) it.address.equals(savedAddress, true)
                        else it.uuids?.contains(AIRPODS_UUID) == true
                    }
                } else emptyList()
                candidates.firstOrNull { isConnected(app, adapter!!, it) }
            },
            apply = { device ->
                val preferences = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
                if (device != null && preferences.getString("mac_address", "").isNullOrEmpty()) {
                    preferences.edit { putString("mac_address", device.address) }
                }
                setConnected(app, device)
            },
            failed = { Log.w("CompanionConnection", "Connection reconciliation failed", it) },
            completed = completed
        )
    }

    fun disconnected(context: Context, address: String? = null) {
        val selected = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("mac_address", "")
        if (address != null && !address.equals(selected, true)) return
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit { remove("popup_connection_address") }
        reconciler.disconnect { setConnected(context.applicationContext, null) }
    }

    @Suppress("DEPRECATION") // The address API also supports Android 13–15.
    private fun setConnected(context: Context, device: BluetoothDevice?) {
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
        val associations = associations(context).mapNotNull { association ->
            association.deviceMacAddress?.toString()?.let { association.id to it }
        }.toMap()
        val associated = presence.update(
            associations, device?.address,
            start = { checkNotNull(manager).startObservingDevicePresence(it) },
            stop = { checkNotNull(manager).stopObservingDevicePresence(it) },
            failed = { Log.w("CompanionConnection", "Could not update device presence observation", it) }
        )
        _companionDevice.value = if (associated) device else null
        ServiceManager.getService()?.let { service ->
            if (device == null) service.onBluetoothDisconnected()
            else service.onBluetoothConnected(device)
        }
        if (device == null) me.kavishdevar.librepods.presentation.widgets.BatteryWidget.update(context)
    }

    private suspend fun isConnected(context: Context, adapter: BluetoothAdapter, device: BluetoothDevice): Boolean {
        if (Build.VERSION.SDK_INT >= 36 && Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
            return device.isConnected(BluetoothDevice.TRANSPORT_BREDR)
        }
        // Audio profiles can be deliberately disconnected while AACP still carries ear events.
        // A live control socket is positive evidence; a cached association/device is not.
        return hasControlConnection(device) ||
            profileConnected(context, adapter, device, BluetoothProfile.A2DP) ||
            profileConnected(context, adapter, device, BluetoothProfile.HEADSET) ||
            hasControlConnection(device) // AACP may have connected during the profile probes.
    }

    private fun hasControlConnection(device: BluetoothDevice): Boolean =
        BluetoothConnectionManager.aacpSocket?.let { it.remoteDevice == device && it.isConnected } == true

    private suspend fun profileConnected(context: Context, adapter: BluetoothAdapter, device: BluetoothDevice, profile: Int): Boolean =
        withTimeoutOrNull(1500) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        val connected = try { device in proxy.connectedDevices } catch (_: SecurityException) { false } finally {
                            adapter.closeProfileProxy(profile, proxy)
                        }
                        if (continuation.isActive) continuation.resume(connected)
                    }
                    override fun onServiceDisconnected(profile: Int) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
                if (!adapter.getProfileProxy(context, listener, profile) && continuation.isActive) continuation.resume(false)
            }
        } ?: false
}
