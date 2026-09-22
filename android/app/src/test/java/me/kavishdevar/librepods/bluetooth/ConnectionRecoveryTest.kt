package me.kavishdevar.librepods.bluetooth

import org.junit.Assert.*
import org.junit.Test

class ConnectionRecoveryTest {
    @Test fun duplicateBluetoothEventsDoNotRefillTheRetryBudget() {
        val recovery = ConnectionRecovery()
        recovery.connected("airpods")
        assertEquals(1000L, recovery.nextDelayMillis())
        recovery.connected("airpods")
        assertEquals(3000L, recovery.nextDelayMillis())
        recovery.connected("airpods")
        assertEquals(10000L, recovery.nextDelayMillis())
        repeat(10) {
            recovery.connected("airpods")
            assertNull(recovery.nextDelayMillis())
        }
    }

    @Test fun disconnectStopsRetriesAndANewConnectionGetsItsOwnBudget() {
        val recovery = ConnectionRecovery()
        recovery.connected("airpods")
        recovery.nextDelayMillis()
        recovery.disconnected()
        assertNull(recovery.nextDelayMillis())
        recovery.connected("airpods")
        assertEquals(1000L, recovery.nextDelayMillis())
        recovery.connected("different airpods")
        assertEquals(1000L, recovery.nextDelayMillis())
    }

    @Test fun explicitDisconnectStaysSuppressedUntilManualRetryOrANewLink() {
        val recovery = ConnectionRecovery()
        recovery.connected("airpods")
        recovery.suppress()
        recovery.connected("airpods")
        assertFalse(recovery.allowsAutomaticConnection)
        assertNull(recovery.nextDelayMillis())
        recovery.manualRetry()
        assertTrue(recovery.allowsAutomaticConnection)
        assertEquals(1000L, recovery.nextDelayMillis())
        recovery.suppress()
        recovery.disconnected()
        recovery.connected("airpods")
        assertTrue(recovery.allowsAutomaticConnection)
        assertEquals(1000L, recovery.nextDelayMillis())
    }
}
