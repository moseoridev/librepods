package me.kavishdevar.librepods.presentation.widgets

import android.content.Context
import me.kavishdevar.librepods.LibrePodsApplication
import me.kavishdevar.librepods.bluetooth.CompanionConnection
import me.kavishdevar.librepods.bluetooth.NearbyDetection
import me.kavishdevar.librepods.core.bluetooth.MacAddress
import me.kavishdevar.librepods.core.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.core.devices.*

/** Reuses live device state and persisted routing; publication never starts/binds an engine. */
internal object WidgetDataSource {
    fun address(context: Context, id: Int?): MacAddress? {
        val app = context.applicationContext as LibrePodsApplication
        val configured = app.widgetConfigRepository.widgetConfigs.value.find { it.appWidgetId == id }?.macAddress
        if (configured != null) return configured
        val selected = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("mac_address", null)
        val legacy = selected?.let { runCatching { MacAddress(it.uppercase()) }.getOrNull() }
        return legacy ?: CompanionConnection.activeEngine?.devices?.value?.values
            ?.firstOrNull { it.connectionState.value == ConnectionState.CONNECTED }?.macAddress
            ?: app.appleRepository.devices.value.keys.firstOrNull()
    }

    fun state(context: Context, id: Int?, override: MacAddress? = null): WidgetState {
        val app = context.applicationContext as LibrePodsApplication
        val address = override ?: address(context, id)
        val device = CompanionConnection.activeEngine?.devices?.value?.get(address) as? AppleDevice
        val cached = app.appleRepository.devices.value[address]
        val metadata = device?.metadata?.value ?: cached?.metadata ?: AppleMetadata()
        val state = device?.state?.value
        val connected = device?.connectionState?.value == ConnectionState.CONNECTED
        val source = if (connected) {
            if (state?.batteryReceived == true) state.battery.filter { it.source == BatterySource.AACP } else emptyList()
        } else {
            NearbyDetection.initialize(context)
            NearbyDetection.currentSnapshots()[address]?.batteries?.toList().orEmpty()
        }
        val spec = AirPodsSpecs.getSpec(metadata.model)
        val supportsModes = (metadata.model != AirPodsModel.UNKNOWN && BaseCapability.LISTENING_MODE in spec.baseCapabilities) ||
            state?.confirmedControlStates?.containsKey(ControlCommandIdentifier.LISTENING_MODE) == true
        val allowOff = (state?.controlStates ?: cached?.cache?.controlStates)
            ?.get(ControlCommandIdentifier.ALLOW_OFF_OPTION)?.firstOrNull()?.let { it == 1.toByte() } ?: true
        val confirmed = state?.confirmedControlStates?.get(ControlCommandIdentifier.LISTENING_MODE)?.firstOrNull()?.toInt()
        val modes = listOf(1, 3, 4, 2).filter { mode ->
            (mode != 1 || allowOff) && (mode != 4 || (metadata.model != AirPodsModel.UNKNOWN && BaseCapability.ADAPTIVE_AUDIO in spec.baseCapabilities) || confirmed == 4)
        }
        return WidgetState.create(metadata.name.ifBlank { spec.displayName }, connected && supportsModes,
            confirmed, allowOff, source, modes)
    }

    fun setMode(context: Context, id: Int, mode: Int): Boolean {
        val info = android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetInfo(id)
        if (info?.provider?.className != NoiseControlWidget::class.java.name) return false
        val state = state(context, id)
        if (!state.connected || mode !in state.availableModes) return false
        val device = CompanionConnection.activeEngine?.devices?.value?.get(address(context, id)) as? AppleDevice ?: return false
        return device.setControlCommand(ControlCommandIdentifier.LISTENING_MODE, mode)
    }
}
