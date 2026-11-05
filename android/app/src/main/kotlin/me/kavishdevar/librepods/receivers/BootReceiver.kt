package me.kavishdevar.librepods.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.kavishdevar.librepods.bluetooth.CompanionConnection

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_USER_UNLOCKED)) return
        if (intent.action == Intent.ACTION_BOOT_COMPLETED)
            me.kavishdevar.librepods.presentation.overlays.policy.PopupMarkers.disconnected(context)
        me.kavishdevar.librepods.presentation.widgets.WidgetPublisher.update(context)
        val pending = goAsync()
        var remaining = 2
        val complete = { if (--remaining == 0) pending.finish() }
        CompanionConnection.reconcile(context, complete)
        me.kavishdevar.librepods.bluetooth.NearbyDetection.synchronize(context, complete)
    }
}
