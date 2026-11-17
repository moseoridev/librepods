package me.kavishdevar.librepods.core.simulation

import me.kavishdevar.librepods.core.bluetooth.aacp.packet.BatteryInfoPacket
import me.kavishdevar.librepods.core.bluetooth.aacp.packet.ControlCommandPacket
import me.kavishdevar.librepods.core.bluetooth.aacp.packet.InformationPacket
import me.kavishdevar.librepods.core.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.core.devices.AirPodsModel
import me.kavishdevar.librepods.core.devices.AirPodsSpecs
import me.kavishdevar.librepods.core.devices.BatteryComponent
import me.kavishdevar.librepods.core.devices.BaseCapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertTrue
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

    @Test fun airPods5ScenarioIsTheDefaultAndResolvesToItsSpec() {
        assertEquals("airpods5-wcc", SimulatedAirPods.scenarios.first().key)
        assertEquals("airpods5-wcc", SimulatedAirPods.scenario(null).key)
        assertEquals("airpods5-wcc", SimulatedAirPods.scenario("not-a-scenario").key)

        val scenario = SimulatedAirPods.scenario("airpods5-wcc")
        assertEquals(AirPodsModel.AIRPODS_5_WCC, AirPodsModel.fromModelNumber(scenario.modelNumber))

        val parsed = InformationPacket.parse(SimulatedAirPodsProtocol.information(scenario))
        assertEquals("A3439", parsed.metadata.modelNumber)
        // The spec, not the packet, supplies the display name for a known model number.
        assertEquals("AirPods 5 with Wireless Charging Case", parsed.metadata.modelName)
    }

    @Test fun airPods5BatteriesCoverBothBudsAndAChargingWirelessCase() {
        val batteries = BatteryInfoPacket.parse(
            SimulatedAirPodsProtocol.battery(SimulatedAirPods.scenario("airpods5-wcc"))
        ).batteries
        assertEquals(3, batteries.size)
        assertEquals(setOf(BatteryComponent.LEFT, BatteryComponent.RIGHT, BatteryComponent.CASE),
            batteries.map { it.component }.toSet())
        assertNotEquals(batteries.single { it.component == BatteryComponent.LEFT }.status,
            batteries.single { it.component == BatteryComponent.CASE }.status)
    }

    /**
     * The scenario decides which rows appear: reporting a capability the spec lacks would show a
     * control for hardware that does not have it, and hiding one the spec has would drop a
     * supported row. Only the capabilities the spec claims are asserted here.
     */
    @Test fun airPods5CapabilitiesMatchItsSpecRowSet() {
        val scenario = SimulatedAirPods.scenario("airpods5-wcc")
        val spec = AirPodsSpecs.getSpec(AirPodsModel.AIRPODS_5_WCC).baseCapabilities
        val reported = scenario.capabilities.map { it.first.toInt() }.toSet()

        assertTrue(BaseCapability.LISTENING_MODE in spec)
        assertTrue(BaseCapability.SLEEP_DETECTION in spec)
        assertTrue("sleep detection", 0x21 in reported)
        assertTrue("selective speech listening", 0x04 in reported)
        assertTrue("dynamic end of charge", 0x29 in reported)
        assertFalse("hearing aid is not in the WCC spec", 0x22 in reported)
        assertFalse("heart rate is not in the WCC spec", 0x26 in reported)
        assertFalse(BaseCapability.HEARING_AID in spec)
        assertFalse(BaseCapability.HRM in spec)
    }
}
