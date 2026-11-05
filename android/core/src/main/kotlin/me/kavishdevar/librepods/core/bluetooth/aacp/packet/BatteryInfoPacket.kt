package me.kavishdevar.librepods.core.bluetooth.aacp.packet

import me.kavishdevar.librepods.core.bluetooth.aacp.types.MessageOpcode
import me.kavishdevar.librepods.core.bluetooth.aacp.types.Opcode
import me.kavishdevar.librepods.core.devices.Battery
import me.kavishdevar.librepods.core.devices.BatteryComponent
import me.kavishdevar.librepods.core.devices.BatterySource
import me.kavishdevar.librepods.core.devices.BatteryStatus
import me.kavishdevar.librepods.core.devices.PacketDestination

private const val TAG = "BatteryInfoPacket"

data class BatteryInfoPacket(
    val batteries: Set<Battery>,
    override val payload: ByteArray,
): AACPPacket {
    override val destination: PacketDestination = PacketDestination.HOST

    override val type: AACPPacketType = AACPPacketType.MESSAGE
    override val service: Byte = 0x04

    override val opcode: Opcode = MessageOpcode.BATTERY_INFO

    companion object {
        fun parse(
            packet: ByteArray,
        ): BatteryInfoPacket {
            val payload = packet.sliceArray(6 until packet.size)

            require(payload.isNotEmpty()) { "Missing battery count" }
            var offset = 0
            val batteryCount = payload[offset].toInt() and 0xff
            require(batteryCount in 1..4 && payload.size == 1 + batteryCount * 5) { "Invalid battery packet length/count" }
            val batteries = mutableSetOf<Battery>()
            offset += 1
            for (i in 0 until batteryCount) {
                val componentByte = payload[offset]
                val component = BatteryComponent.fromAirPodsByte(componentByte)
                val levelByte = payload[offset + 2]
                val level = (levelByte.toInt() and 0xff).takeIf { it in 0..100 } ?: -1
                val statusByte = payload[offset + 3]
                val status = BatteryStatus.fromAirPodsByte(statusByte)


                require(batteries.none { it.component == component }) { "Duplicate battery component" }
                batteries.add(Battery(component, level, status, BatterySource.AACP, System.currentTimeMillis()))
                offset += 5
            }

            return BatteryInfoPacket(batteries, payload)
        }
    }
}
