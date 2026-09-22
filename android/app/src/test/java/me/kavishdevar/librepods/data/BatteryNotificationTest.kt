package me.kavishdevar.librepods.data

import org.junit.Assert.*
import org.junit.Test

class BatteryNotificationTest {
    @Test fun rejectedPacketsCannotMarkCachedBatteriesAsFresh() {
        val parser = AirPodsNotifications.BatteryNotification()
        fun packet(level: Int) = ByteArray(22).apply {
            this[7] = BatteryComponent.LEFT.toByte()
            this[12] = BatteryComponent.RIGHT.toByte()
            this[17] = BatteryComponent.CASE.toByte()
            listOf(9, 14, 19).forEach { this[it] = level.toByte() }
            listOf(10, 15, 20).forEach { this[it] = BatteryStatus.NOT_CHARGING.toByte() }
        }

        assertTrue(parser.setBattery(packet(80)))
        val previousSession = parser.getBattery()
        // A warm reconnect can receive truncated or extended data before a valid packet.
        assertFalse(parser.setBattery(packet(20).copyOf(21)))
        assertFalse(parser.setBattery(packet(20).copyOf(23)))
        assertEquals(previousSession, parser.getBattery())
        assertTrue(parser.setBattery(packet(20)))
        assertEquals(listOf(20, 20, 20), parser.getBattery().map { it.level })
    }
}
