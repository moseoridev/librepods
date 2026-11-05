package me.kavishdevar.librepods.bluetooth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionReconcilerTest {
    @Test fun aLatePlatformReplyCannotResurrectADisconnectedDevice() = runTest {
        val reconciler = ConnectionReconciler<String>(this)
        val reply = CompletableDeferred<String>()
        val applied = mutableListOf<String?>()
        var completed = 0
        reconciler.refresh(
            read = { withContext(NonCancellable) { reply.await() } },
            apply = { applied.add(it) }, failed = { throw it }, completed = { completed++ }
        )
        reconciler.disconnect { applied.add(it) }
        reply.complete("stale device")
        advanceUntilIdle()
        assertEquals(listOf<String?>(null), applied)
        assertEquals(1, completed)
    }

    @Test fun theLatestProbeWinsAndEveryBroadcastIsFinished() = runTest {
        val reconciler = ConnectionReconciler<String>(this)
        val applied = mutableListOf<String?>()
        var completed = 0
        reconciler.refresh(
            read = { delay(500); "old" }, apply = { applied.add(it) }, failed = { throw it }, completed = { completed++ }
        )
        reconciler.refresh(
            read = { "new" }, apply = { applied.add(it) }, failed = { throw it }, completed = { completed++ }
        )
        advanceUntilIdle()
        assertEquals(listOf("new"), applied)
        assertEquals(2, completed)
    }

    @Test fun unresponsivePlatformProbeIsBoundedAndClearsPresence() = runTest {
        val reconciler = ConnectionReconciler<String>(this)
        val applied = mutableListOf<String?>()
        var completed = false
        reconciler.refresh(
            read = { delay(10000); "too late" }, apply = { applied.add(it) }, failed = { throw it }, completed = { completed = true }
        )
        advanceTimeBy(4000)
        runCurrent()
        assertTrue(completed)
        assertEquals(listOf<String?>(null), applied)
    }

    @Test fun permissionFailureClearsPresenceAndFinishesTheBroadcast() = runTest {
        val reconciler = ConnectionReconciler<String>(this)
        val applied = mutableListOf<String?>()
        var failure: Exception? = null
        var completed = false
        reconciler.refresh(
            read = { throw SecurityException("Bluetooth permission revoked") },
            apply = { applied.add(it) }, failed = { failure = it }, completed = { completed = true }
        )
        assertTrue(completed)
        assertTrue(failure is SecurityException)
        assertEquals(listOf<String?>(null), applied)
    }
}
