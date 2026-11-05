package me.kavishdevar.librepods.receivers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.kavishdevar.librepods.bluetooth.CompanionConnection

class BluetoothConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED &&
            intent.getIntExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_BREDR) == BluetoothDevice.TRANSPORT_BREDR) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)?.let {
                me.kavishdevar.librepods.bluetooth.NearbyDetection.newClassicLink(context, it.address)
            }
        }
        if (intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
            if (intent.getIntExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_BREDR) != BluetoothDevice.TRANSPORT_BREDR) return
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)?.let {
                me.kavishdevar.librepods.presentation.overlays.policy.PopupMarkers.disconnected(context, it.address)
                CompanionConnection.disconnected(context, it.address)
            }
            return
        }
        if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                    me.kavishdevar.librepods.presentation.overlays.policy.PopupMarkers.disconnected(context)
                    CompanionConnection.disconnected(context)
                    val pending = goAsync()
                    me.kavishdevar.librepods.bluetooth.NearbyDetection.synchronize(context) { pending.finish() }
                    return
                }
                BluetoothAdapter.STATE_ON -> Unit
                else -> return
            }
        }
        val pending = goAsync()
        if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            var remaining = 2
            val complete = { if (--remaining == 0) pending.finish() }
            CompanionConnection.reconcile(context, complete)
            me.kavishdevar.librepods.bluetooth.NearbyDetection.synchronize(context, complete)
        } else CompanionConnection.reconcile(context) { pending.finish() }
    }
}
