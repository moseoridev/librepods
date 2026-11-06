package me.kavishdevar.librepods.presentation.screens.apple

import me.kavishdevar.librepods.core.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.core.bluetooth.att.ATTHandle
import me.kavishdevar.librepods.core.data.StemAction
import me.kavishdevar.librepods.core.devices.AppleSettings
import me.kavishdevar.librepods.presentation.viewmodel.AppleViewModel

/** UI intent boundary. Only the production adapter reaches the real device. */
internal interface DeviceSettingsActions {
    fun setControlCommand(command: ControlCommandIdentifier, value: ByteArray)
    fun setControlCommand(command: ControlCommandIdentifier, value: Int) = setControlCommand(command, byteArrayOf(value.toByte()))
    fun setControlCommand(command: ControlCommandIdentifier, value: Boolean) = setControlCommand(command, if (value) 1 else 2)
    fun updateSettings(transform: (AppleSettings) -> AppleSettings)
    fun disconnect()
    fun renameDevice(name: String)
    fun setLongPressAction(side: String, action: StemAction)
    fun setHeadGesturesEnabled(enabled: Boolean)
    fun writeATTCharacteristic(handle: ATTHandle, value: ByteArray)
    fun setCustomEqEnabled(enabled: Boolean)
    fun setCustomEq(low: Int, mid: Int, high: Int)
}

internal class ViewModelSettingsActions(private val vm: AppleViewModel) : DeviceSettingsActions {
    override fun setControlCommand(command: ControlCommandIdentifier, value: ByteArray) { vm.setControlCommand(command, value) }
    // Keep upstream's overload semantics, including its Boolean encoding.
    override fun setControlCommand(command: ControlCommandIdentifier, value: Int) { vm.setControlCommand(command, value) }
    override fun setControlCommand(command: ControlCommandIdentifier, value: Boolean) { vm.setControlCommand(command, value) }
    override fun updateSettings(transform: (AppleSettings) -> AppleSettings) { vm.updateSettings(transform) }
    override fun disconnect() { vm.disconnect() }
    override fun renameDevice(name: String) { vm.renameDevice(name) }
    override fun setLongPressAction(side: String, action: StemAction) { vm.setLongPressAction(side, action) }
    override fun setHeadGesturesEnabled(enabled: Boolean) { vm.setHeadGesturesEnabled(enabled) }
    override fun writeATTCharacteristic(handle: ATTHandle, value: ByteArray) { vm.writeATTCharacteristic(handle, value) }
    override fun setCustomEqEnabled(enabled: Boolean) { vm.setCustomEqEnabled(enabled) }
    override fun setCustomEq(low: Int, mid: Int, high: Int) { vm.setCustomEq(low, mid, high) }
}
