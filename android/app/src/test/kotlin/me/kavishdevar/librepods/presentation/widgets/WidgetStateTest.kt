package me.kavishdevar.librepods.presentation.widgets

import me.kavishdevar.librepods.core.devices.Battery
import me.kavishdevar.librepods.core.devices.BatteryComponent
import me.kavishdevar.librepods.core.devices.BatteryStatus
import org.junit.Assert.*
import org.junit.Test

class WidgetStateTest {
    private fun reading(component: BatteryComponent, level: Int, status: BatteryStatus) =
        Battery(component, level, status, me.kavishdevar.librepods.core.devices.BatterySource.AACP, 0)
    @Test fun missingAndDisconnectedComponentsDoNotBecomeLivePercentages() {
        val initial = WidgetState.create("AirPods", true, null, true, listOf(
            reading(BatteryComponent.LEFT, 0, BatteryStatus.NOT_CHARGING),
            reading(BatteryComponent.RIGHT, 88, BatteryStatus.DISCONNECTED),
            reading(BatteryComponent.CASE, -1, BatteryStatus.CHARGING)
        ))
        assertEquals(0, initial.left.level) // A real empty battery must remain distinguishable.
        assertNull(initial.right.level)
        assertNull(initial.case.level)
        assertFalse(initial.case.charging)
        assertEquals(WidgetBattery(), WidgetBattery.from(101, BatteryStatus.NOT_CHARGING))
        val charged = WidgetState.create("AirPods", true, 2, true, listOf(
            reading(BatteryComponent.RIGHT, 88, BatteryStatus.OPTIMIZED_CHARGING)
        ))
        assertNull(charged.left.level)
        assertEquals(WidgetBattery(88, true), charged.right)
    }

    @Test fun aNewOrDisconnectedControlSessionCannotDisplayAnUnconfirmedOffMode() {
        fun state(connected: Boolean, mode: Int?) = WidgetState.create("AirPods", connected, mode, true, emptyList())
        assertNull(state(true, null).mode)
        assertEquals(1, state(true, 1).mode)
        assertNull(state(false, 1).mode)
        assertNull(state(true, 0).mode)
    }
}
