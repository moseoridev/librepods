package me.kavishdevar.librepods.presentation.screens.apple

import org.junit.Assert.assertEquals
import org.junit.Test

class FirmwareDisplayVersionTest {
    @Test fun formatsObservedReleaseFamily() {
        assertEquals("9A348", firmwareDisplayVersion("9442752"))
        assertEquals("9A350", firmwareDisplayVersion("9442784"))
    }

    @Test fun preservesUnestablishedFamiliesAndPrereleases() {
        for (raw in listOf("8454371", "9508288", "9442785", "9437184", "9A350", "", "—",
            "2147483648", "-9442784", "+9442784", " 9442784")) {
            assertEquals(raw, raw, firmwareDisplayVersion(raw))
        }
    }
}
