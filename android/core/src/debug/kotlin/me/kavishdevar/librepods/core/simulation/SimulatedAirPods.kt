package me.kavishdevar.librepods.core.simulation

import me.kavishdevar.librepods.core.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.core.bluetooth.aacp.types.MessageOpcode
import me.kavishdevar.librepods.core.devices.AppleDevice
import me.kavishdevar.librepods.core.devices.AppleSettings
import me.kavishdevar.librepods.core.devices.AppleState

/**
 * Debug-only catalog of simulated accessories.
 *
 * Every packet below uses the production wire format: the header `04 00 04 00`, a
 * little-endian opcode and the documented payload, so the real parsers in AACPManager are
 * the only thing that turns these bytes into UI state. Field order comes from
 * docs/device-info.md, docs/control_commands.md and the packet tests.
 */

/** Locally administered address; never bonded and never a real accessory. */
const val SIMULATED_AIRPODS_MAC = "02:11:22:33:44:55"

data class SimulatedAirPodsBattery(
    val component: Int,
    val level: Int,
    val status: Int,
)

data class SimulatedAirPodsScenario(
    val key: String,
    val description: String,
    val modelNumber: String,
    val name: String,
    val serialNumber: String,
    val version1: String,
    val version2: String,
    val version3: String,
    val batteries: List<SimulatedAirPodsBattery>,
    val earStatuses: List<Int>,
    val capabilities: List<Pair<Int, ByteArray>>,
    val controlStates: List<Pair<ControlCommandIdentifier, ByteArray>>,
)

object SimulatedAirPods {
    // Battery status bytes from docs/opcodes.md.
    private const val NOT_CHARGING = 2
    private const val CHARGING = 1
    private const val DISCONNECTED = 4

    // Battery component bytes: 1 headset, 4 left, 2 right, 8 case.
    private const val HEADSET = 1
    private const val LEFT = 4
    private const val RIGHT = 2
    private const val CASE = 8

    private const val IN_EAR = 0
    private const val OUT_OF_EAR = 1

    // Unverified placeholder, not an observed release build. The device screen hides the
    // equalizer below a first digit of 9 (BudsDeviceSettings, AppleSettingsScreen), so the
    // scenarios that should exercise that row need a 9-family value. Replace it once a real
    // build number from an accessory is available.
    private const val EQ_CAPABLE_VERSION3 = "9442784"

    // Capability identifiers from the Capability enum; value sizes must match it.
    private val PRO_CAPABILITIES: List<Pair<Int, ByteArray>> = listOf(
        0x04 to byteArrayOf(0x01, 0x00, 0x00, 0x00), // SELECTIVE_SPEECH_LISTENING (4 bytes)
        0x21 to byteArrayOf(0x01), // SLEEP_DETECTION
        0x22 to byteArrayOf(0x01), // HEARING_AID
        0x26 to byteArrayOf(0x01), // HEART_RATE_MONITOR
        0x28 to byteArrayOf(0x01), // HEARING_PROTECTION_PPE
        0x29 to byteArrayOf(0x01), // DYNAMIC_END_OF_CHARGE
        0x30 to byteArrayOf(0x01, 0x00, 0x00, 0x00), // HEARING_PROTECTION (4 bytes)
        0x35 to byteArrayOf(0x01), // SMART_ROUTING_VERSION
        0x37 to byteArrayOf(0x01), // PREFERENCE_EQ
        0x38 to byteArrayOf(0x01), // EXTENDED_CLICK_HOLD
    )

    private val BUDS_CONTROL_STATES: List<Pair<ControlCommandIdentifier, ByteArray>> = listOf(
        ControlCommandIdentifier.LISTENING_MODE to byteArrayOf(0x02),
        ControlCommandIdentifier.LISTENING_MODE_CONFIGS to byteArrayOf(0x0F),
        ControlCommandIdentifier.ALLOW_OFF_OPTION to byteArrayOf(0x01),
        ControlCommandIdentifier.CONVERSATION_DETECT_CONFIG to byteArrayOf(0x02),
        ControlCommandIdentifier.ADAPTIVE_VOLUME_CONFIG to byteArrayOf(0x01),
        ControlCommandIdentifier.VOLUME_SWIPE_MODE to byteArrayOf(0x01),
        ControlCommandIdentifier.SLEEP_DETECTION_CONFIG to byteArrayOf(0x02),
        ControlCommandIdentifier.EAR_DETECTION_CONFIG to byteArrayOf(0x01),
        ControlCommandIdentifier.RAW_GESTURES_CONFIG to byteArrayOf(0x0F),
        ControlCommandIdentifier.DYNAMIC_END_OF_CHARGE to byteArrayOf(0x01),
        ControlCommandIdentifier.ONE_BUD_ANC_MODE to byteArrayOf(0x02),
        ControlCommandIdentifier.AUTO_ANC_STRENGTH to byteArrayOf(0x32),
        ControlCommandIdentifier.OWNS_CONNECTION to byteArrayOf(0x01),
        ControlCommandIdentifier.HEARING_AID to byteArrayOf(0x01, 0x02),
    )

    private val MAX_CONTROL_STATES: List<Pair<ControlCommandIdentifier, ByteArray>> = listOf(
        ControlCommandIdentifier.LISTENING_MODE to byteArrayOf(0x03),
        ControlCommandIdentifier.LISTENING_MODE_CONFIGS to byteArrayOf(0x0F),
        ControlCommandIdentifier.ALLOW_OFF_OPTION to byteArrayOf(0x01),
        ControlCommandIdentifier.CONVERSATION_DETECT_CONFIG to byteArrayOf(0x02),
        ControlCommandIdentifier.ADAPTIVE_VOLUME_CONFIG to byteArrayOf(0x02),
        ControlCommandIdentifier.CROWN_ROTATION_DIRECTION to byteArrayOf(0x02),
        ControlCommandIdentifier.OWNS_CONNECTION to byteArrayOf(0x01),
    )

    /**
     * AirPods 5 with Wireless Charging Case, from `AirPodsSpecs` for A3439/A3440/A3441.
     * A subset of AACP capabilities for the simulator. The clean A3439 report did not include
     * Dynamic End of Charge, so this scenario omits that capability and its setting.
     */
    private val AIRPODS5_WCC_CAPABILITIES: List<Pair<Int, ByteArray>> = listOf(
        0x04 to byteArrayOf(0x01, 0x00, 0x00, 0x00), // SELECTIVE_SPEECH_LISTENING (4 bytes)
        0x21 to byteArrayOf(0x01), // SLEEP_DETECTION
        0x35 to byteArrayOf(0x01), // SMART_ROUTING_VERSION
        0x38 to byteArrayOf(0x01), // EXTENDED_CLICK_HOLD
    )

    /** A subset of AirPods 5 control states. */
    private val AIRPODS5_WCC_CONTROL_STATES: List<Pair<ControlCommandIdentifier, ByteArray>> = listOf(
        ControlCommandIdentifier.LISTENING_MODE to byteArrayOf(0x02),
        ControlCommandIdentifier.LISTENING_MODE_CONFIGS to byteArrayOf(0x0F),
        ControlCommandIdentifier.ALLOW_OFF_OPTION to byteArrayOf(0x01),
        ControlCommandIdentifier.CONVERSATION_DETECT_CONFIG to byteArrayOf(0x02),
        ControlCommandIdentifier.ADAPTIVE_VOLUME_CONFIG to byteArrayOf(0x01),
        ControlCommandIdentifier.VOLUME_SWIPE_MODE to byteArrayOf(0x01),
        ControlCommandIdentifier.SLEEP_DETECTION_CONFIG to byteArrayOf(0x02),
        ControlCommandIdentifier.EAR_DETECTION_CONFIG to byteArrayOf(0x01),
        ControlCommandIdentifier.RAW_GESTURES_CONFIG to byteArrayOf(0x0F),
        ControlCommandIdentifier.ONE_BUD_ANC_MODE to byteArrayOf(0x02),
        ControlCommandIdentifier.AUTO_ANC_STRENGTH to byteArrayOf(0x32),
        ControlCommandIdentifier.OWNS_CONNECTION to byteArrayOf(0x01),
    )

    val scenarios: List<SimulatedAirPodsScenario> = listOf(
        SimulatedAirPodsScenario(
            key = "airpods5-wcc",
            description = "AirPods 5 with Wireless Charging Case, both buds in ear, case charging",
            modelNumber = "A3439",
            name = "AirPods",
            serialNumber = "SIMULATEDAIRPODS5",
            version1 = "61.1868040002000000.2713",
            version2 = "61.1868040002000000.2713",
            version3 = EQ_CAPABLE_VERSION3,
            batteries = listOf(
                SimulatedAirPodsBattery(LEFT, 96, NOT_CHARGING),
                SimulatedAirPodsBattery(RIGHT, 95, NOT_CHARGING),
                // A wireless charging case reports the case component charging.
                SimulatedAirPodsBattery(CASE, 88, CHARGING),
            ),
            earStatuses = listOf(IN_EAR, IN_EAR),
            capabilities = AIRPODS5_WCC_CAPABILITIES,
            controlStates = AIRPODS5_WCC_CONTROL_STATES,
        ),
        SimulatedAirPodsScenario(
            key = "pro3",
            description = "AirPods Pro 3, both buds in ear, healthy batteries",
            modelNumber = "A3063",
            name = "AirPods Pro",
            serialNumber = "SIMULATEDPRO3",
            version1 = "61.1868040002000000.2713",
            version2 = "61.1868040002000000.2713",
            version3 = EQ_CAPABLE_VERSION3,
            batteries = listOf(
                SimulatedAirPodsBattery(LEFT, 82, NOT_CHARGING),
                SimulatedAirPodsBattery(RIGHT, 79, NOT_CHARGING),
                SimulatedAirPodsBattery(CASE, 64, CHARGING),
            ),
            earStatuses = listOf(IN_EAR, IN_EAR),
            capabilities = PRO_CAPABILITIES,
            controlStates = BUDS_CONTROL_STATES,
        ),
        SimulatedAirPodsScenario(
            key = "pro3-low-battery",
            description = "AirPods Pro 3, low batteries, right bud out of ear",
            modelNumber = "A3063",
            name = "AirPods Pro",
            serialNumber = "SIMULATEDPRO3",
            version1 = "61.1868040002000000.2713",
            version2 = "61.1868040002000000.2713",
            version3 = "8454371",
            batteries = listOf(
                SimulatedAirPodsBattery(LEFT, 9, NOT_CHARGING),
                SimulatedAirPodsBattery(RIGHT, 4, CHARGING),
                SimulatedAirPodsBattery(CASE, 0, NOT_CHARGING),
            ),
            earStatuses = listOf(IN_EAR, OUT_OF_EAR),
            capabilities = PRO_CAPABILITIES,
            controlStates = BUDS_CONTROL_STATES,
        ),
        SimulatedAirPodsScenario(
            key = "airpods4-anc",
            description = "AirPods 4 (ANC)",
            modelNumber = "A3056",
            name = "AirPods",
            serialNumber = "SIMULATEDAIRPODS4",
            version1 = "61.1868040002000000.2713",
            version2 = "61.1868040002000000.2713",
            version3 = "8454371",
            batteries = listOf(
                SimulatedAirPodsBattery(LEFT, 55, NOT_CHARGING),
                SimulatedAirPodsBattery(RIGHT, 57, NOT_CHARGING),
                SimulatedAirPodsBattery(CASE, 88, NOT_CHARGING),
            ),
            earStatuses = listOf(IN_EAR, IN_EAR),
            capabilities = listOf(
                0x21 to byteArrayOf(0x01),
                0x29 to byteArrayOf(0x01),
                0x38 to byteArrayOf(0x01),
            ),
            controlStates = BUDS_CONTROL_STATES,
        ),
        SimulatedAirPodsScenario(
            key = "max2",
            description = "AirPods Max 2, headset battery only",
            modelNumber = "A3454",
            name = "AirPods Max",
            serialNumber = "SIMULATEDMAX2",
            version1 = "61.1868040002000000.2713",
            version2 = "61.1868040002000000.2713",
            version3 = "8454371",
            batteries = listOf(SimulatedAirPodsBattery(HEADSET, 71, DISCONNECTED)),
            earStatuses = emptyList(),
            capabilities = listOf(
                0x29 to byteArrayOf(0x01),
                0x30 to byteArrayOf(0x01, 0x00, 0x00, 0x00),
                0x38 to byteArrayOf(0x01),
            ),
            controlStates = MAX_CONTROL_STATES,
        ),
    )

    fun scenario(key: String?): SimulatedAirPodsScenario =
        scenarios.firstOrNull { it.key.equals(key, true) } ?: scenarios.first()

    /** Loads a clean, scenario-driven initial state and starts the loopback session. */
    fun attach(device: AppleDevice, scenario: SimulatedAirPodsScenario): SimulatedAirPodsSession {
        device.loadInitialState(
            state = AppleState(),
            settings = AppleSettings(showIslandPopup = false, showBottomSheetPopup = false),
            metadata = device.metadata.value,
        )
        return SimulatedAirPodsSession(device, scenario).also { it.start() }
    }
}

object SimulatedAirPodsProtocol {
    private val header = byteArrayOf(0x04, 0x00, 0x04, 0x00)

    private fun message(opcode: MessageOpcode): ByteArray =
        header + byteArrayOf(opcode.value, 0x00)

    private fun nullTerminated(fields: List<String>): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        fields.forEach { field ->
            out.write(field.encodeToByteArray())
            out.write(0x00)
        }
        return out.toByteArray()
    }

    /**
     * INFORMATION: `02 d5 00 04 00` then null-terminated fields. The leading 0x04 is the
     * one-byte length prefix the production parser drops with `strings.removeAt(0)`; the
     * capture in docs/AAP Definitions.md shows the same layout.
     */
    fun information(scenario: SimulatedAirPodsScenario): ByteArray =
        message(MessageOpcode.INFORMATION) +
            byteArrayOf(0x02, 0xD5.toByte(), 0x00, 0x04, 0x00) +
            nullTerminated(
                listOf(
                    scenario.name,
                    scenario.modelNumber,
                    "Apple Inc.",
                    scenario.serialNumber,
                    scenario.version1,
                    scenario.version2,
                    "1.0.0",
                    "com.apple.accessory.updater.app.71",
                    "${scenario.serialNumber}L",
                    "${scenario.serialNumber}R",
                    scenario.version3,
                )
            )

    fun capabilities(scenario: SimulatedAirPodsScenario): ByteArray {
        val payload = java.io.ByteArrayOutputStream()
        payload.write(scenario.capabilities.size)
        scenario.capabilities.forEach { (id, value) ->
            payload.write(id)
            payload.write(value)
        }
        return message(MessageOpcode.CAPABILITIES) + payload.toByteArray()
    }

    fun battery(scenario: SimulatedAirPodsScenario): ByteArray {
        val payload = java.io.ByteArrayOutputStream()
        payload.write(scenario.batteries.size)
        scenario.batteries.forEach { battery ->
            payload.write(battery.component)
            payload.write(0x00)
            payload.write(battery.level)
            payload.write(battery.status)
            payload.write(0x00)
        }
        return message(MessageOpcode.BATTERY_INFO) + payload.toByteArray()
    }

    /** Left is primary so EAR_DETECTION byte order matches AppleDevice's parsed role. */
    fun budRole(): ByteArray =
        message(MessageOpcode.BUD_ROLE) + byteArrayOf(0x01, 0x00, 0x00)

    fun earDetection(scenario: SimulatedAirPodsScenario): ByteArray =
        message(MessageOpcode.EAR_DETECTION) + scenario.earStatuses.map { it.toByte() }.toByteArray()

    fun controlCommand(identifier: ControlCommandIdentifier, value: ByteArray): ByteArray {
        val payload = ByteArray(5)
        payload[0] = identifier.value
        value.copyInto(payload, 1, 0, value.size.coerceAtMost(4))
        return message(MessageOpcode.CONTROL_COMMAND) + payload
    }

    /**
     * The notification an accessory would send back for an outgoing packet, or null when the
     * accessory has nothing to report. Only control commands are answered; the accessory keeps
     * the last accepted value and reports it, and multi-byte values are kept only for the one
     * documented two-byte setting.
     */
    fun responseTo(packet: ByteArray): ByteArray? {
        if (packet.size < 11 || packet[0] != 0x04.toByte() || packet[4] != MessageOpcode.CONTROL_COMMAND.value) return null
        val identifier = ControlCommandIdentifier.fromByte(packet[6]) ?: return null
        val sent = packet.copyOfRange(7, 11).dropLastWhile { it == 0x00.toByte() }.toByteArray()
        val value = if (sent.isEmpty()) byteArrayOf(0x00) else sent
        val stored = if (value.size > 1 && identifier != ControlCommandIdentifier.HEARING_AID) value.copyOf(1) else value
        return controlCommand(identifier, stored)
    }

    /** Packets an accessory sends once the control channel is up. */
    fun notifications(scenario: SimulatedAirPodsScenario): List<ByteArray> = buildList {
        add(information(scenario))
        add(capabilities(scenario))
        add(budRole())
        add(battery(scenario))
        if (scenario.earStatuses.isNotEmpty()) add(earDetection(scenario))
        scenario.controlStates.forEach { (identifier, value) -> add(controlCommand(identifier, value)) }
    }
}
