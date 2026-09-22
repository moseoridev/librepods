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

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.io.encoding.ExperimentalEncodingApi
import me.kavishdevar.librepods.bluetooth.NearbyDetection

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_USER_UNLOCKED)) return
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().remove("popup_connection_address").apply()
        }
        me.kavishdevar.librepods.presentation.widgets.WidgetPublisher.update(context)
        val pending = goAsync()
        var remaining = 2
        val completed = { if (--remaining == 0) pending.finish() }
        me.kavishdevar.librepods.bluetooth.CompanionConnection.reconcile(context, completed)
        NearbyDetection.synchronize(context, completed)
    }
}
