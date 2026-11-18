package me.kavishdevar.librepods.presentation.screens.apple

import me.kavishdevar.librepods.core.bluetooth.aacp.types.Capability
import me.kavishdevar.librepods.core.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.core.devices.AppleState

internal fun AppleState.supportsOptimizedChargeLimit(): Boolean =
    capabilities.any { it.capability == Capability.DYNAMIC_END_OF_CHARGE } ||
        controlStates[ControlCommandIdentifier.DYNAMIC_END_OF_CHARGE]?.firstOrNull() != null
