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

package me.kavishdevar.librepods.presentation.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.services.ServiceManager

class NoiseControlWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        WidgetPublisher.update(context, forceIds = ids.toSet())
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        WidgetPublisher.update(context, forceIds = setOf(id))
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        WidgetPublisher.forget(ids)
        WidgetPreferences.delete(context, ids, controls = true)
    }

    override fun onRestored(context: Context, oldIds: IntArray, newIds: IntArray) {
        WidgetPreferences.restore(context, oldIds, newIds, controls = true)
        WidgetPublisher.forget(oldIds)
        WidgetPublisher.update(context, forceIds = newIds.toSet())
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_MODE && intent.action != ACTION_CYCLE) return
        val service = ServiceManager.getService()
        val mode = if (intent.action == ACTION_CYCLE) {
            val state = service?.getWidgetState()
            val modes = if (state?.allowOff == true) listOf(1, 3, 4, 2) else listOf(3, 4, 2)
            modes[(modes.indexOf(state?.mode) + 1) % modes.size]
        } else intent.getIntExtra(EXTRA_MODE, -1)
        if (mode !in 1..4) return
        if (service?.setWidgetNoiseMode(mode) != true) {
            Toast.makeText(context, R.string.widget_open_to_connect, Toast.LENGTH_SHORT).show()
            WidgetPublisher.update(context)
        }
        // Selection follows the AirPods acknowledgement, not an optimistic local value.
    }

    companion object {
        const val ACTION_MODE = "me.kavishdevar.librepods.widget.SET_MODE"
        const val ACTION_CYCLE = "me.kavishdevar.librepods.widget.CYCLE_MODE"
        const val EXTRA_MODE = "mode"
    }
}
