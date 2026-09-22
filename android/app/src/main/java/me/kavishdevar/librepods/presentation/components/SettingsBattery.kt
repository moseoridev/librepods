package me.kavishdevar.librepods.presentation.components

import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.data.BatteryComponent
import me.kavishdevar.librepods.data.BatteryStatus
import kotlin.math.abs

internal data class SettingsBattery(val level: Int?, val status: Int) {
    companion object {
        fun from(battery: Battery?): SettingsBattery {
            val supported = when (battery?.status) {
                BatteryStatus.CHARGING, BatteryStatus.NOT_CHARGING, BatteryStatus.OPTIMIZED_CHARGING -> true
                else -> false
            }
            return if (supported && battery != null && battery.level in 0..100) {
                SettingsBattery(battery.level, battery.status)
            } else SettingsBattery(null, BatteryStatus.DISCONNECTED)
        }
    }
}

internal data class SettingsBatteries(
    val left: SettingsBattery,
    val right: SettingsBattery,
    val case: SettingsBattery,
) {
    // Preserve upstream's conservative within-three-percent merge, but never merge a stale side.
    val combined: SettingsBattery?
        get() {
            val l = left.level ?: return null
            val r = right.level ?: return null
            return if (left.status == right.status && abs(l - r) <= 3) {
                SettingsBattery(minOf(l, r), left.status)
            } else null
        }

    companion object {
        fun from(batteries: List<Battery>) = SettingsBatteries(
            SettingsBattery.from(batteries.find { it.component == BatteryComponent.LEFT }),
            SettingsBattery.from(batteries.find { it.component == BatteryComponent.RIGHT }),
            SettingsBattery.from(batteries.find { it.component == BatteryComponent.CASE }),
        )
    }
}
