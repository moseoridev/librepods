package me.kavishdevar.librepods.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsScrollStateTest {
    @Test fun startsWithoutABottomEdge() {
        assertFalse(SettingsScrollState().canScrollForward)
    }

    @Test fun aReportDrivesTheEdge() {
        val state = SettingsScrollState()
        val screen = Any()
        state.report(screen, true)
        assertTrue(state.canScrollForward)
        state.report(screen, false)
        assertFalse(state.canScrollForward)
    }

    @Test fun aDepartedReporterDoesNotLeaveTheEdgeOn() {
        val state = SettingsScrollState()
        val scrolled = Any()
        state.report(scrolled, true)
        state.clear(scrolled)
        assertFalse(state.canScrollForward)
    }

    @Test fun aLateDisposeDoesNotWipeTheIncomingScreen() {
        val state = SettingsScrollState()
        val outgoing = Any()
        val incoming = Any()
        state.report(outgoing, true)
        // Navigation composes the incoming screen before the outgoing one is disposed.
        state.report(incoming, true)
        state.clear(outgoing)
        assertTrue(state.canScrollForward)
        state.clear(incoming)
        assertFalse(state.canScrollForward)
    }
}
