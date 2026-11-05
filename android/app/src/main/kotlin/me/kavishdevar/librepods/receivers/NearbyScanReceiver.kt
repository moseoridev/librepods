package me.kavishdevar.librepods.receivers

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.kavishdevar.librepods.bluetooth.NearbyDetection

class NearbyScanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NearbyDetection.ACTION_SCAN) return
        NearbyDetection.initialize(context)
        if (!NearbyDetection.isEnabled()) return
        val error = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, 0)
        if (error != 0) {
            NearbyDetection.scanFailed(error)
            return
        }
        val results = intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT, ScanResult::class.java)
            ?: return
        val pending = goAsync()
        NearbyDetection.receive(results) { pending.finish() }
    }
}
