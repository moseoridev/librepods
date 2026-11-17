package me.kavishdevar.librepods.core.simulation

import me.kavishdevar.librepods.core.bluetooth.aacp.packet.BatteryInfoPacket
import me.kavishdevar.librepods.core.bluetooth.aacp.packet.ControlCommandPacket
import me.kavishdevar.librepods.core.bluetooth.aacp.packet.InformationPacket
import me.kavishdevar.librepods.core.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.core.devices.BatteryComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The simulated accessory is only useful if the production parsers accept its bytes and its
 * loopback answers with a packet that parser reads back. These tests pin that contract.
 */
class SimulatedAirPodsProtocolTest {
    private val scenario = SimulatedAirPods.scenario("pro3")

    @Test fun scenarioPacketsParseWithTheProductionParsers() {
        val information = InformationPacket.parse(SimulatedAirPodsProtocol.information(scenario))
        assertEquals("A3063", information.metadata.modelNumber)
        assertEquals("AirPods Pro", information.metadata.modelName)
        assertEquals("Apple Inc.", information.metadata.manufacturer)

        val battery = BatteryInfoPacket.parse(SimulatedAirPodsProtocol.battery(scenario))
        assertEquals(3, battery.batteries.size)
        assertEquals(82, battery.batteries.single { it.component == BatteryComponent.LEFT }.level)

        val primary = SimulatedAirPodsProtocol.budRole()
        assertEquals(0x01, primary[6].toInt())
    }

    @Test fun listeningModeCommandIsAnsweredWithTheEchoedMode() {
        val requested = SimulatedAirPodsProtocol.controlCommand(
            ControlCommandIdentifier.LISTENING_MODE, byteArrayOf(0x03)
        )
        val response = SimulatedAirPodsProtocol.responseTo(requested)
        assertNotNull(response)

        val command = ControlCommandPacket.parse(response!!).controlCommand
        assertEquals(ControlCommandIdentifier.LISTENING_MODE, command.identifier)
        assertEquals(0x03, command.value.single().toInt())
    }

    @Test fun twoByteHearingAidSettingKeepsBothValues() {
        val requested = SimulatedAirPodsProtocol.controlCommand(
            ControlCommandIdentifier.HEARING_AID, byteArrayOf(0x01, 0x02)
        )
        val command = ControlCommandPacket.parse(SimulatedAirPodsProtocol.responseTo(requested)!!).controlCommand
        assertEquals(listOf(0x01, 0x02), command.value.map { it.toInt() })
    }

    @Test fun nonControlPacketsAndUnknownIdentifiersAreNotAnswered() {
        assertNull(SimulatedAirPodsProtocol.responseTo(byteArrayOf(0x04, 0x00, 0x04, 0x00, 0x0F, 0x00, 0xFF.toByte())))
        assertNull(SimulatedAirPodsProtocol.responseTo(SimulatedAirPodsProtocol.battery(scenario)))
    }

    @Test fun earStatusBytesFollowTheParsedPrimaryBud() {
        val earOnly = scenario.copy(earStatuses = listOf(0, 1))
        val packet = SimulatedAirPodsProtocol.earDetection(earOnly)
        assertEquals(0, packet[6].toInt())
        assertEquals(1, packet[7].toInt())
    }
}
