package me.kavishdevar.librepods.core.devices

import org.junit.Assert.assertEquals
import org.junit.Test

class AppleMetadataDisplayNameTest {
    @Test fun blankPersistedNameFallsBackToTheModelName() {
        // A fresh database row has no name until the information packet arrives, and
        // Android throws when a notification channel is created with an empty name.
        assertEquals("AirPods Pro", AppleMetadata(model = AirPodsModel.AIRPODS_PRO_3).displayName)
        assertEquals("Unknown AirPods", AppleMetadata().displayName)
    }

    @Test fun userVisibleNameWinsWhenPresent() {
        assertEquals("MoseoriPods", AppleMetadata(name = "MoseoriPods", model = AirPodsModel.AIRPODS_PRO_3).displayName)
    }
}
