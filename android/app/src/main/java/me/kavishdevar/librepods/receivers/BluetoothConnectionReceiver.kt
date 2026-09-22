package me.kavishdevar.librepods.receivers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.kavishdevar.librepods.bluetooth.CompanionConnection

class BluetoothConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED && device != null &&
            intent.getIntExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_BREDR) == BluetoothDevice.TRANSPORT_BREDR) {
            val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            if (device.address.equals(preferences.getString("mac_address", ""), true)) {
                preferences.edit().remove("nearby_suppressed_address").apply()
            }
        }
        if (intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
            if (device != null && intent.getIntExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_BREDR) == BluetoothDevice.TRANSPORT_BREDR) {
                CompanionConnection.disconnected(context, device.address)
            }
            return
        }
        if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                CompanionConnection.disconnected(context)
                val pending = goAsync()
                me.kavishdevar.librepods.bluetooth.NearbyDetection.synchronize(context) { pending.finish() }
                return
            }
            if (state != BluetoothAdapter.STATE_ON) return
        }
        val pending = goAsync()
        if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            var remaining = 2
            val completed = { if (--remaining == 0) pending.finish() }
            CompanionConnection.reconcile(context, completed)
            me.kavishdevar.librepods.bluetooth.NearbyDetection.synchronize(context, completed)
        } else CompanionConnection.reconcile(context) { pending.finish() }
    }
}
