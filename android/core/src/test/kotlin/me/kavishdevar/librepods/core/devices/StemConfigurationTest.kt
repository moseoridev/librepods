package me.kavishdevar.librepods.core.devices

import me.kavishdevar.librepods.core.data.StemAction
import me.kavishdevar.librepods.core.data.StemPressBud
import me.kavishdevar.librepods.core.data.StemPressType
import org.junit.Assert.*
import org.junit.Test

class StemConfigurationTest {
    @Test fun defaultGesturesStayOnFirmware() {
        val settings = AppleSettings()
        assertEquals(0, settings.stemConfigMask())
        StemPressType.entries.forEach { assertFalse(settings.handlesStemType(it)) }
    }
    @Test fun oneCustomizedBudMovesBothSidesOfThatPressTypeToHost() {
        val settings = AppleSettings(leftLongPressAction = StemAction.DIGITAL_ASSISTANT)
        assertEquals(8, settings.stemConfigMask())
        assertTrue(settings.handlesStemType(StemPressType.LONG_PRESS))
        assertEquals(StemAction.DIGITAL_ASSISTANT, settings.stemAction(StemPressBud.LEFT, StemPressType.LONG_PRESS))
        assertEquals(StemAction.CYCLE_listening_mode_MODES, settings.stemAction(StemPressBud.RIGHT, StemPressType.LONG_PRESS))
        assertFalse(settings.handlesStemType(StemPressType.SINGLE_PRESS))
    }
    @Test fun independentPressTypesKeepTheirFirmwareBits() {
        assertEquals(7, AppleSettings(leftSinglePressAction = StemAction.NEXT_TRACK,
            rightDoublePressAction = StemAction.PREVIOUS_TRACK, leftTriplePressAction = StemAction.PLAY_PAUSE).stemConfigMask())
    }
    @Test fun configuredBitsAreNotListeningModeValuesAndUnsupportedModesAreOmitted() {
        assertEquals(listOf(1, 3, 4, 2), configuredListeningModes(15, adaptive = true, off = true))
        assertEquals(listOf(3, 2), configuredListeningModes(15, adaptive = false, off = false))
        assertEquals(listOf(3), configuredListeningModes(4, adaptive = true, off = true))
        assertTrue(configuredListeningModes(0, adaptive = true, off = true).isEmpty())
    }
}
