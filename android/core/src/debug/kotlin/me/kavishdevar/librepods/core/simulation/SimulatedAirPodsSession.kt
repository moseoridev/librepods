package me.kavishdevar.librepods.core.simulation

import me.kavishdevar.librepods.core.bluetooth.aacp.AACPManager
import me.kavishdevar.librepods.core.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.core.devices.AppleDevice

/**
 * Debug-only accessory stand-in for one [AppleDevice].
 *
 * Connects by calling the same internal transition a real session calls, then answers every
 * outgoing control command with the notification packet the accessory would send, so the UI
 * round trip runs through [AACPManager.processPacket] in both directions. Nothing here opens a
 * socket, touches bonding, or writes state that a parser would not produce.
 */
class SimulatedAirPodsSession internal constructor(
    private val device: AppleDevice,
    val scenario: SimulatedAirPodsScenario,
) {
    private var sender: ((ByteArray) -> Boolean)? = null

    /** Installs the loopback sender ahead of [AppleDevice.onControlConnected]. */
    fun start() {
        val aacp = device.aacp
        val loopback: (ByteArray) -> Boolean = { packet ->
            SimulatedAirPodsProtocol.responseTo(packet)?.let(aacp::processPacket)
            true
        }
        sender = loopback
        aacp.sender = loopback
        device.onControlConnected()
        SimulatedAirPodsProtocol.notifications(scenario).forEach(aacp::processPacket)
    }

    /** Detaches the loopback and returns the device to the disconnected state. */
    fun stop() {
        if (sender != null) {
            device.aacp.sender = null
            sender = null
        }
        device.onControlDisconnected()
    }
}
