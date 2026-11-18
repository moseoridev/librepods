package me.kavishdevar.librepods.presentation.utils

import me.kavishdevar.librepods.core.devices.AirPodsModel
import me.kavishdevar.librepods.core.devices.AirPodsSpecs
import me.kavishdevar.librepods.core.devices.AppleMetadata
import me.kavishdevar.librepods.core.devices.BaseCapability
import org.junit.Assert.assertEquals
import org.junit.Test

class VisibleBaseCapabilitiesTest {
    @Test
    fun usesModelSpecOnlyWhenModelNumberIsAvailable() {
        val unidentified = AppleMetadata()
        val known = unidentified.copy(model = AirPodsModel.AIRPODS_5_WCC, modelNumber = "A3439")
        val unrecognized = unidentified.copy(modelNumber = "A9999")

        assertEquals(emptySet<BaseCapability>(), unidentified.visibleBaseCapabilities())
        assertEquals(emptySet<BaseCapability>(), unidentified.copy(modelNumber = " ").visibleBaseCapabilities())
        assertEquals(AirPodsSpecs.getSpec(known.model).baseCapabilities, known.visibleBaseCapabilities())
        assertEquals(BaseCapability.entries.toSet(), unrecognized.visibleBaseCapabilities())
    }
}
