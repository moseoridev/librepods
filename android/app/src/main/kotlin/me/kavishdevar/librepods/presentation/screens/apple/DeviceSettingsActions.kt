package me.kavishdevar.librepods.presentation.screens.apple

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.core.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.core.bluetooth.att.ATTHandle
import me.kavishdevar.librepods.core.data.StemAction
import me.kavishdevar.librepods.core.devices.AppleSettings
import me.kavishdevar.librepods.core.devices.ConnectionState
import me.kavishdevar.librepods.presentation.viewmodel.AppleViewModel

@Composable
internal fun rememberDeviceSettingsActions(viewModel: AppleViewModel): DeviceSettingsActions {
    val context = LocalContext.current
    return remember(viewModel, context) {
        ViewModelSettingsActions(viewModel) {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
    }
}

@Composable
internal fun BluetoothDisconnectAction(
    actions: DeviceSettingsActions,
    connectionState: ConnectionState,
    content: @Composable (label: Int, onClick: () -> Unit) -> Unit,
) {
    var canDisconnect by remember(actions, connectionState) {
        mutableStateOf(actions.canRequestBluetoothDisconnect())
    }
    content(if (canDisconnect) R.string.disconnect else R.string.open_bluetooth_settings) {
        if (!canDisconnect || !actions.requestBluetoothDisconnect()) {
            canDisconnect = false
            actions.openBluetoothSettings()
        }
    }
}

/** UI intent boundary. Only the production adapter reaches the real device. */
internal interface DeviceSettingsActions {
    fun setControlCommand(command: ControlCommandIdentifier, value: ByteArray)
    fun setControlCommand(command: ControlCommandIdentifier, value: Int) = setControlCommand(command, byteArrayOf(value.toByte()))
    fun setControlCommand(command: ControlCommandIdentifier, value: Boolean) = setControlCommand(command, if (value) 1 else 2)
    fun updateSettings(transform: (AppleSettings) -> AppleSettings)
    fun canRequestBluetoothDisconnect(): Boolean
    fun requestBluetoothDisconnect(): Boolean
    fun openBluetoothSettings()
    fun renameDevice(name: String)
    fun setLongPressAction(side: String, action: StemAction)
    fun setHeadGesturesEnabled(enabled: Boolean)
    fun writeATTCharacteristic(handle: ATTHandle, value: ByteArray)
    fun setCustomEqEnabled(enabled: Boolean)
    fun setCustomEq(low: Int, mid: Int, high: Int)
}

internal class ViewModelSettingsActions(
    private val vm: AppleViewModel,
    private val openSystemBluetoothSettings: () -> Unit,
) : DeviceSettingsActions {
    override fun setControlCommand(command: ControlCommandIdentifier, value: ByteArray) { vm.setControlCommand(command, value) }
    // Keep upstream's overload semantics, including its Boolean encoding.
    override fun setControlCommand(command: ControlCommandIdentifier, value: Int) { vm.setControlCommand(command, value) }
    override fun setControlCommand(command: ControlCommandIdentifier, value: Boolean) { vm.setControlCommand(command, value) }
    override fun updateSettings(transform: (AppleSettings) -> AppleSettings) { vm.updateSettings(transform) }
    override fun canRequestBluetoothDisconnect() = vm.canRequestBluetoothDisconnect()
    override fun requestBluetoothDisconnect() = vm.requestBluetoothDisconnect()
    override fun openBluetoothSettings() = openSystemBluetoothSettings()
    override fun renameDevice(name: String) { vm.renameDevice(name) }
    override fun setLongPressAction(side: String, action: StemAction) { vm.setLongPressAction(side, action) }
    override fun setHeadGesturesEnabled(enabled: Boolean) { vm.setHeadGesturesEnabled(enabled) }
    override fun writeATTCharacteristic(handle: ATTHandle, value: ByteArray) { vm.writeATTCharacteristic(handle, value) }
    override fun setCustomEqEnabled(enabled: Boolean) { vm.setCustomEqEnabled(enabled) }
    override fun setCustomEq(low: Int, mid: Int, high: Int) { vm.setCustomEq(low, mid, high) }
}
