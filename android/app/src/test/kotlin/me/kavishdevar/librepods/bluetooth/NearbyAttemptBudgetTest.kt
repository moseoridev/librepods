package me.kavishdevar.librepods.bluetooth

import org.junit.Assert.*
import org.junit.Test

class NearbyAttemptBudgetTest {
    @Test fun repeatedAdvertisementsAndRecreatedJobsCannotRefillTheBudget() {
        var checkpoint = NearbyAttemptBudget().observe("A", "music")
        repeat(3) { attempt ->
            checkpoint = checkNotNull(checkpoint.reserve(attempt * 60_000L))
            checkpoint = NearbyAttemptBudget(checkpoint.address, checkpoint.reason, checkpoint.attempts, checkpoint.lastAttempt)
                .observe("A", "music")
        }
        assertNull(checkpoint.reserve(1_000_000))
    }

    @Test fun phoneStateChangesRestoreEligibilityButDoNotBypassCooldown() {
        val first = checkNotNull(NearbyAttemptBudget().observe("A", "music").reserve(0))
        val next = first.observe("A", null).observe("A", "call")
        assertNull(next.reserve(59_999))
        assertNotNull(next.reserve(60_000))
        assertNull(first.observe("A", null).reserve(60_000))
    }
    @Test fun changingDeviceRestoresAttemptsWithoutRemovingTheGlobalCooldown() {
        var exhausted = NearbyAttemptBudget().observe("A", "music")
        repeat(3) { exhausted = checkNotNull(exhausted.reserve(it * 60_000L)) }
        assertNull(exhausted.reserve(180_000))
        val replacement = exhausted.observe("B", "music")
        assertNull(replacement.reserve(179_999))
        assertEquals(1, replacement.reserve(180_000)?.attempts)
        assertEquals("B", replacement.reserve(180_000)?.address)
    }
}
