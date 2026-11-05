package me.kavishdevar.librepods.presentation.overlays.policy

import android.content.Context

internal object PopupMarkers {
    private const val PREFIX = "popup_connection:"
    private fun store(context: Context) = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    fun policy(context: Context): ConnectionPopupPolicy {
        val preferences = store(context)
        return ConnectionPopupPolicy({ address ->
            preferences.getBoolean(PREFIX + address, false) ||
                preferences.getString("popup_connection_address", null).equals(address, true)
        }, { address -> preferences.edit().putBoolean(PREFIX + address, true).apply() })
    }
    fun disconnected(context: Context, address: String? = null) {
        val preferences = store(context)
        val edit = preferences.edit()
        if (address == null) preferences.all.keys.filter { it.startsWith(PREFIX) }.forEach { edit.remove(it) }
        else edit.remove(PREFIX + address)
        if (address == null || preferences.getString("popup_connection_address", null).equals(address, true))
            edit.remove("popup_connection_address")
        edit.apply()
    }
}
