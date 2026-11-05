package me.kavishdevar.librepods.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import me.kavishdevar.librepods.core.devices.AppleDevice
import kotlin.coroutines.resume

/** Finite profile acquisition; a late callback closes its proxy without changing routing. */
@SuppressLint("MissingPermission")
internal suspend fun setAudioRouting(device: AppleDevice, enabled: Boolean, current: () -> Boolean): Boolean {
    var changed = false
    for (profileId in listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET)) {
        val permission = if (profileId == BluetoothProfile.A2DP) "android.permission.BLUETOOTH_PRIVILEGED" else "android.permission.MODIFY_PHONE_STATE"
        val privileged = device.context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        // Nonprivileged A2DP connect remains best-effort, as in the preserved implementation.
        if (!privileged && (!enabled || profileId != BluetoothProfile.A2DP)) continue
        val success = withTimeoutOrNull(1500) {
            suspendCancellableCoroutine { result ->
                val listener = object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        var ok = false
                        try {
                            if (result.isActive && current()) {
                                if (privileged) {
                                    val policy = proxy.javaClass.getMethod("setConnectionPolicy", android.bluetooth.BluetoothDevice::class.java, Int::class.javaPrimitiveType)
                                    ok = policy.invoke(proxy, device.bluetoothDevice, if (enabled) 100 else 0) != false
                                }
                                if (enabled && current()) {
                                    ok = proxy.javaClass.getMethod("connect", android.bluetooth.BluetoothDevice::class.java)
                                        .invoke(proxy, device.bluetoothDevice) != false
                                }
                            }
                        } catch (_: ReflectiveOperationException) {
                            // Hidden/privileged APIs may be unavailable without the optional hook.
                        } catch (_: SecurityException) {
                        } finally {
                            device.bluetoothAdapter.closeProfileProxy(profile, proxy)
                        }
                        if (result.isActive) result.resume(ok)
                    }
                    override fun onServiceDisconnected(profile: Int) { if (result.isActive) result.resume(false) }
                }
                try {
                    if (!device.bluetoothAdapter.getProfileProxy(device.context, listener, profileId) && result.isActive) result.resume(false)
                } catch (_: SecurityException) { if (result.isActive) result.resume(false) }
            }
        } ?: false
        changed = changed || success
    }
    return changed
}
