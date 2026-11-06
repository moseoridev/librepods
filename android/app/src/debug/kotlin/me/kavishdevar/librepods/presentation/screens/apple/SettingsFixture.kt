package me.kavishdevar.librepods.presentation.screens.apple

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.kavishdevar.librepods.core.bluetooth.aacp.types.ControlCommandIdentifier as Command
import me.kavishdevar.librepods.core.bluetooth.att.ATTHandle
import me.kavishdevar.librepods.core.data.StemAction
import me.kavishdevar.librepods.core.devices.*
import me.kavishdevar.librepods.presentation.viewmodel.AppleUiState

/** Synthetic inputs only: no repositories, device instance, services or audio API. */
internal class SettingsFixture(
    val scenario: String = "normal",
    model: AirPodsModel = if (scenario == "headset") AirPodsModel.AIRPODS_MAX_USBC else AirPodsModel.AIRPODS_5_WCC,
) : DeviceSettingsActions {
    val events = mutableStateListOf<String>()
    private val acknowledge = scenario != "pending"
    var ui by mutableStateOf(initialState(scenario, model))
        private set

    override fun setControlCommand(command: Command, value: ByteArray) {
        events += "${command.name}: ${value.joinToString { (it.toInt() and 255).toString() }}"
        ui = ui.copy(state = ui.state.copy(
            controlStates = ui.state.controlStates + (command to value.copyOf()),
            confirmedControlStates = if (acknowledge) ui.state.confirmedControlStates + (command to value.copyOf()) else ui.state.confirmedControlStates,
        ))
    }
    override fun updateSettings(transform: (AppleSettings) -> AppleSettings) { ui = ui.copy(settings = transform(ui.settings)) }
    override fun disconnect() { events += "disconnect"; ui = ui.copy(connectionState = ConnectionState.DISCONNECTED, state = ui.state.copy(confirmedControlStates = emptyMap())) }
    override fun renameDevice(name: String) { events += "rename: $name"; ui = ui.copy(metadata = ui.metadata.copy(name = name)) }
    override fun setLongPressAction(side: String, action: StemAction) {
        events += "longPress: $side $action"
        updateSettings { if (side == "left") it.copy(leftLongPressAction = action) else it.copy(rightLongPressAction = action) }
    }
    override fun setHeadGesturesEnabled(enabled: Boolean) { updateSettings { it.copy(headGesturesEnabled = enabled) } }
    override fun writeATTCharacteristic(handle: ATTHandle, value: ByteArray) {
        events += "ATT: $handle"
        if (handle == ATTHandle.LOUD_SOUND_REDUCTION) ui = ui.copy(state = ui.state.copy(loudSoundReductionEnabled = value.firstOrNull() == 1.toByte()))
    }
    override fun setCustomEqEnabled(enabled: Boolean) { ui = ui.copy(state = ui.state.copy(customEq = ui.state.customEq.copy(state = if (enabled) 2 else 1))) }
    override fun setCustomEq(low: Int, mid: Int, high: Int) { events += "EQ: $low $mid $high"; ui = ui.copy(state = ui.state.copy(customEq = ui.state.customEq.copy(low = low, mid = mid, high = high))) }

    companion object {
        val scenarios = setOf("normal", "charging", "zero_unknown", "headset", "disconnected", "pending", "long_name")

        private fun initialState(scenario: String, model: AirPodsModel): AppleUiState {
            require(scenario in scenarios) { "Unknown fixture scenario: $scenario" }
            val spec = AirPodsSpecs.getSpec(model)
            val controls = mapOf(
                Command.LISTENING_MODE to byteArrayOf(if (BaseCapability.ADAPTIVE_AUDIO in spec.baseCapabilities) 4 else 2),
                Command.ALLOW_OFF_OPTION to byteArrayOf(1),
                Command.LISTENING_MODE_CONFIGS to byteArrayOf(15), Command.MIC_MODE to byteArrayOf(0),
                Command.CALL_MANAGEMENT_CONFIG to byteArrayOf(0, 3), Command.SMART_ROUTING_MODE to byteArrayOf(1),
                Command.DOUBLE_CLICK_INTERVAL to byteArrayOf(0), Command.CLICK_HOLD_INTERVAL to byteArrayOf(0),
                Command.CHIME_VOLUME to byteArrayOf(50), Command.AUTO_ANC_STRENGTH to byteArrayOf(50),
                Command.VOLUME_SWIPE_INTERVAL to byteArrayOf(1),
            ) + listOf(Command.ADAPTIVE_VOLUME_CONFIG, Command.CONVERSATION_DETECT_CONFIG, Command.EAR_DETECTION_CONFIG,
                Command.SLEEP_DETECTION_CONFIG, Command.ONE_BUD_ANC_MODE, Command.VOLUME_SWIPE_MODE,
                Command.DYNAMIC_END_OF_CHARGE).associateWith { byteArrayOf(1) }
            fun battery(component: BatteryComponent, level: Int, status: BatteryStatus = BatteryStatus.NOT_CHARGING) =
                Battery(component, level, status, BatterySource.AACP, 0L)
            val headset = spec.components.any { it.type == DeviceComponent.HEADSET }
            val batteries = when {
                scenario == "disconnected" -> emptySet()
                headset -> setOf(battery(BatteryComponent.HEADSET, if (scenario == "zero_unknown") 0 else 67,
                    if (scenario == "charging") BatteryStatus.CHARGING else BatteryStatus.NOT_CHARGING))
                scenario == "zero_unknown" -> setOf(battery(BatteryComponent.LEFT, 0), battery(BatteryComponent.CASE, -1, BatteryStatus.UNKNOWN))
                scenario == "charging" -> setOf(battery(BatteryComponent.LEFT, 8, BatteryStatus.CHARGING),
                    battery(BatteryComponent.RIGHT, 91), battery(BatteryComponent.CASE, 80, BatteryStatus.OPTIMIZED_CHARGING))
                else -> setOf(battery(BatteryComponent.LEFT, 83), battery(BatteryComponent.RIGHT, 91), battery(BatteryComponent.CASE, 100))
            }
            return AppleUiState(
                connectionState = if (scenario == "disconnected") ConnectionState.DISCONNECTED else ConnectionState.CONNECTED,
                metadata = AppleMetadata(name = if (scenario == "long_name") "긴 기기 이름을 사용하는 AirPods 화면 검증용 헤드폰" else "AirPods UI Fixture",
                    model = model, modelName = spec.name, modelNumber = spec.modelNumbers.firstOrNull() ?: "—",
                    serialNumber = "DEMO-0000", version1 = "fixture-v1", version2 = "fixture-v2", version3 = "9442784"),
                state = AppleState(battery = batteries, batteryReceived = scenario != "disconnected", controlStates = controls,
                    confirmedControlStates = if (scenario in setOf("pending", "disconnected")) emptyMap() else controls),
                isPremium = true, vendorIdHook = true,
            )
        }
    }
}
