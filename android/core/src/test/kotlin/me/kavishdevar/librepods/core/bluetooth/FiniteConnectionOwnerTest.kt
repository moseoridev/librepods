package me.kavishdevar.librepods.core.bluetooth

import org.junit.Assert.*
import org.junit.Test

class FiniteConnectionOwnerTest {
    @Test fun timedOutAttemptReleasesItsOwnConnectionOnce() {
        val owner = FiniteConnectionOwner()
        var closes = 0
        val lease = owner.acquire { closes++ }
        lease.close(); lease.close()
        assertEquals(1, closes)
    }
    @Test fun manualOrCompanionHandoffSurvivesOldJobCancellation() {
        val owner = FiniteConnectionOwner()
        val lease = owner.acquire { fail("job closed a handed-off connection") }
        owner.handoff()
        lease.close()
    }
    @Test fun staleJobCannotReleaseReplacementAttempt() {
        val owner = FiniteConnectionOwner()
        val first = owner.acquire { fail("stale job released resources") }
        var releases = 0
        val second = owner.acquire { releases++ }
        first.close()
        assertEquals(0, releases)
        second.close()
        assertEquals(1, releases)
    }
}
