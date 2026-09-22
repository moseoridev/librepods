package me.kavishdevar.librepods.presentation.widgets

import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.BatteryStatus

/** Unknown is distinct from an empty battery; never display a disconnected cached percentage. */
data class WidgetBattery(val level: Int? = null, val charging: Boolean = false) {
    companion object {
        fun from(level: Int?, status: Int?): WidgetBattery {
            if (level == null || level !in 0..100 || status !in listOf(
                    BatteryStatus.CHARGING, BatteryStatus.NOT_CHARGING, BatteryStatus.OPTIMIZED_CHARGING
                )) return WidgetBattery()
            return WidgetBattery(level, status != BatteryStatus.NOT_CHARGING)
        }
    }
}

data class WidgetState(
    val name: String,
    val connected: Boolean,
    val mode: Int?,
    val allowOff: Boolean,
    val left: WidgetBattery,
    val right: WidgetBattery,
    val case: WidgetBattery,
) {
    companion object {
        fun create(name: String, connected: Boolean, confirmedMode: Int?, allowOff: Boolean,
                   batteries: List<Battery>): WidgetState {
            fun battery(component: Int) = batteries.find { it.component == component }.let {
                WidgetBattery.from(it?.level, it?.status)
            }
            return WidgetState(name, connected, confirmedMode?.takeIf { connected && it in 1..4 },
                allowOff, battery(BatteryComponent.LEFT), battery(BatteryComponent.RIGHT), battery(BatteryComponent.CASE))
        }
    }
}
