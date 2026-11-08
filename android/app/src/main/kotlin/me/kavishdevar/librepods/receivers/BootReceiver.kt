/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

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
