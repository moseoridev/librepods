package me.kavishdevar.librepods.receivers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.kavishdevar.librepods.bluetooth.CompanionConnection

class BluetoothConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
            if (intent.getIntExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_BREDR) != BluetoothDevice.TRANSPORT_BREDR) return
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)?.let {
                CompanionConnection.disconnected(context, it.address)
            }
            return
        }
        if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                    CompanionConnection.disconnected(context)
                    return
                }
                BluetoothAdapter.STATE_ON -> Unit
                else -> return
            }
        }
        val pending = goAsync()
        CompanionConnection.reconcile(context) { pending.finish() }
    }
}
