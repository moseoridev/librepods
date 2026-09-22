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
import android.os.Bundle
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.services.ServiceManager

class BatteryWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ServiceManager.getService()?.synchronizeWidgetBatteryReceiver()
        WidgetPublisher.update(context, forceIds = ids.toSet())
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        WidgetPublisher.update(context, forceIds = setOf(id))
    }

    override fun onEnabled(context: Context) { ServiceManager.getService()?.synchronizeWidgetBatteryReceiver() }
    override fun onDisabled(context: Context) { ServiceManager.getService()?.synchronizeWidgetBatteryReceiver() }
    override fun onDeleted(context: Context, ids: IntArray) {
        WidgetPublisher.forget(ids)
        WidgetPreferences.delete(context, ids, controls = false)
        ServiceManager.getService()?.synchronizeWidgetBatteryReceiver()
    }

    override fun onRestored(context: Context, oldIds: IntArray, newIds: IntArray) {
        WidgetPreferences.restore(context, oldIds, newIds, controls = false)
        WidgetPublisher.forget(oldIds)
        WidgetPublisher.update(context, forceIds = newIds.toSet())
    }

    companion object {
        fun update(context: Context, batteries: List<Battery>? = null) = WidgetPublisher.update(context, batteries)
    }
}
