package me.kavishdevar.librepods.presentation.overlays.policy

import org.junit.Assert.*
import org.junit.Test

class ConnectionPopupPolicyTest {
    @Test fun onlyFirstUsablePacketConsumesPhysicalConnectionAcrossServiceRecreation() {
        val persisted = mutableSetOf<String>()
        fun policy() = ConnectionPopupPolicy({ it in persisted }, { persisted += it })
        assertFalse(policy().consumeReady("first", true, false, true))
        assertFalse(policy().consumeReady("first", true, true, false))
        assertTrue(policy().consumeReady("first", true, true, true))
        assertFalse(policy().consumeReady("first", true, true, true))
        assertTrue(policy().consumeReady("second", true, true, true))
        persisted.remove("first") // Only a physical disconnect/boot clears the marker.
        assertTrue(policy().consumeReady("first", true, true, true))
    }
    @Test fun disabledOrHiddenPresentationCannotDeferTheConsumedBottomPopup() {
        val persisted = mutableSetOf<String>()
        val gate = ConnectionPopupPolicy({ it in persisted }, { persisted += it })
        assertTrue(gate.consumeReady("device", true, true, true))
        // The caller may skip rendering because the display/permission/preference forbids it.
        assertFalse(gate.consumeReady("device", true, true, true))
    }
    @Test fun repeatedHiddenIslandRequestsDoNotExtendTheDeadline() {
        val pending = PendingIslandRequest<Int>()
        pending.request(1, 0)
        pending.request(1, 29_000)
        assertNull(pending.consume(30_001, true) { true })
    }
    @Test fun unlockOnlyConsumesTheCurrentSessionAndSupersedingRequestsReplaceOldOnes() {
        val pending = PendingIslandRequest<Int>()
        pending.request(1, 0)
        assertNull(pending.consume(1_000, false) { true })
        assertEquals(1, pending.consume(1_001, true) { it == 1 })
        assertNull(pending.consume(1_002, true) { true })
        pending.request(1, 2_000)
        assertNull(pending.consume(2_001, true) { it == 2 })
        pending.request(1, 3_000)
        pending.request(2, 3_001)
        assertEquals(2, pending.consume(3_002, true) { it == 2 })
    }
}
