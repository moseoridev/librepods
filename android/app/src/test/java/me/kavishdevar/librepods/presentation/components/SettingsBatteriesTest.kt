package me.kavishdevar.librepods.presentation.components

import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.BatteryStatus
import org.junit.Assert.*
import org.junit.Test

class SettingsBatteriesTest {
    @Test fun unavailableValuesStayUnknownButRealZeroRemainsVisible() {
        for (battery in listOf(null, Battery(4, 80, BatteryStatus.DISCONNECTED),
            Battery(4, -1, BatteryStatus.CHARGING), Battery(4, 101, BatteryStatus.NOT_CHARGING), Battery(4, 50, 99))) {
            val value = SettingsBattery.from(battery)
            assertNull(value.level)
            assertEquals(BatteryStatus.DISCONNECTED, value.status)
        }
        assertEquals(0, SettingsBattery.from(Battery(4, 0, BatteryStatus.NOT_CHARGING)).level)
    }

    @Test fun mergingRequiresTwoValidReadingsWithMatchingChargeStates() {
        fun readings(right: Battery?) = SettingsBatteries.from(listOfNotNull(
            Battery(BatteryComponent.LEFT, 80, BatteryStatus.CHARGING), right))
        assertNull(readings(null).combined)
        assertNull(readings(Battery(2, 80, BatteryStatus.DISCONNECTED)).combined)
        assertNull(readings(Battery(2, 80, BatteryStatus.NOT_CHARGING)).combined)
        assertNull(readings(Battery(2, 80, BatteryStatus.OPTIMIZED_CHARGING)).combined)
        assertEquals(SettingsBattery(77, BatteryStatus.CHARGING), readings(Battery(2, 77, BatteryStatus.CHARGING)).combined)
        assertNull(readings(Battery(2, 76, BatteryStatus.CHARGING)).combined)
    }

    @Test fun newSnapshotDoesNotRetainRemovedEarbudOrCase() {
        val connected = SettingsBatteries.from(listOf(Battery(4, 50, 2), Battery(2, 50, 2), Battery(8, 70, 2)))
        assertEquals(50, connected.combined?.level)
        val next = SettingsBatteries.from(listOf(Battery(4, 49, 2)))
        assertEquals(49, next.left.level)
        assertNull(next.right.level)
        assertNull(next.case.level)
        assertNull(next.combined)
    }
}
