package me.kavishdevar.librepods.presentation.utils

import me.kavishdevar.librepods.core.devices.AirPodsSpecs
import me.kavishdevar.librepods.core.devices.AppleMetadata
import me.kavishdevar.librepods.core.devices.BaseCapability

internal fun AppleMetadata.visibleBaseCapabilities(): Set<BaseCapability> =
    if (modelNumber.isBlank()) emptySet() else AirPodsSpecs.getSpec(model).baseCapabilities
