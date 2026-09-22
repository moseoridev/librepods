package me.kavishdevar.librepods.bluetooth

import org.junit.Assert.*
import org.junit.Test

class PresenceObservationTest {
    @Test fun processRecreationWhileDisconnectedDisablesPersistedObservation() {
        val stops = mutableListOf<String>()
        val observation = PresenceObservation()
        assertFalse(observation.update(mapOf(1 to "airpods"), null,
            start = { fail("Idle device must not be observed") }, stop = { stops.add(it) }, failed = { throw it }))
        assertEquals(listOf("airpods"), stops)
    }

    @Test fun duplicatePresenceCallbacksDoNotTriggerAnObservationLoop() {
        var starts = 0
        val observation = PresenceObservation()
        repeat(10) {
            assertTrue(observation.update(mapOf(1 to "aa:bb"), "AA:BB",
                start = { starts++ }, stop = { fail("Connected target was stopped") }, failed = { throw it }))
        }
        assertEquals(1, starts)
    }

    @Test fun switchingDevicesAndDisconnectingStopsEveryUnneededObservation() {
        val observation = PresenceObservation()
        val calls = mutableListOf<String>()
        val associations = mapOf(1 to "first", 2 to "second")
        fun update(address: String?) = observation.update(associations, address,
            start = { calls.add("start $it") }, stop = { calls.add("stop $it") }, failed = { throw it })
        update("first")
        calls.clear()
        update("second")
        assertEquals(listOf("start second", "stop first"), calls.sorted())
        calls.clear()
        assertFalse(update(null))
        assertEquals(listOf("stop first", "stop second"), calls.sorted())
    }

    @Test fun failedObservationIsRetriedOnTheNextEventAndCannotClaimABinding() {
        val observation = PresenceObservation()
        var failures = 0
        assertFalse(observation.update(mapOf(1 to "airpods"), "airpods",
            start = { throw SecurityException() }, stop = {}, failed = { failures++ }))
        var starts = 0
        assertTrue(observation.update(mapOf(1 to "airpods"), "airpods",
            start = { starts++ }, stop = {}, failed = { throw it }))
        assertEquals(1, starts)
        assertEquals(1, failures)
    }
}
