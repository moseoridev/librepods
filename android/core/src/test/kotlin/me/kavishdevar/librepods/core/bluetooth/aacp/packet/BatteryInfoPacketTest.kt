package me.kavishdevar.librepods.core.bluetooth.aacp.packet

import me.kavishdevar.librepods.core.devices.BatteryComponent
import org.junit.Assert.*
import org.junit.Test

class BatteryInfoPacketTest {
    private val header = byteArrayOf(4, 0, 4, 0, 4, 0)
    private fun row(component: Int, level: Int = 0) = byteArrayOf(component.toByte(), 0, level.toByte(), 2, 0)
    @Test fun singleHeadsetAndThreeComponentPacketsKeepZeroAsARealReading() {
        val headset = BatteryInfoPacket.parse(header + byteArrayOf(1) + row(1, 0))
        assertEquals(0, headset.batteries.single().level)
        assertEquals(BatteryComponent.HEADSET, headset.batteries.single().component)
        val buds = BatteryInfoPacket.parse(header + byteArrayOf(3) + row(4, 0) + row(2, 100) + row(8, 255))
        assertEquals(3, buds.batteries.size)
        assertEquals(-1, buds.batteries.single { it.component == BatteryComponent.CASE }.level)
    }
    @Test fun truncatedExtraAndDuplicateReadingsAreRejectedBeforePublication() {
        val packet = header + byteArrayOf(1) + row(4)
        assertThrows(IllegalArgumentException::class.java) { BatteryInfoPacket.parse(packet.dropLast(1).toByteArray()) }
        assertThrows(IllegalArgumentException::class.java) { BatteryInfoPacket.parse(packet + byteArrayOf(0)) }
        assertThrows(IllegalArgumentException::class.java) { BatteryInfoPacket.parse(header + byteArrayOf(2) + row(4) + row(4)) }
    }
}
