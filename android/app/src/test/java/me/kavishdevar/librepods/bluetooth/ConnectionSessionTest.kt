package me.kavishdevar.librepods.bluetooth

import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionSessionTest {
    @Test
    fun duplicateConnectAndLateDisconnectCannotReplaceTheCurrentSession() = runTest {
        val sessions = ConnectionSessions(this)
        val first = sessions.begin()!!
        assertNull(sessions.begin())
        assertTrue(sessions.finish(first))
        val second = sessions.begin()!!
        val socket = second.own(BlockingSocket())

        assertFalse(sessions.finish(first))
        assertSame(second, sessions.current)
        assertEquals(0, socket.closes.get())
        sessions.close()
        assertEquals(1, socket.closes.get())
        assertNull(sessions.begin())
    }

    @Test
    fun teardownClosesEveryResourceOnceAndCancelsDelayedWork() = runTest {
        val session = ConnectionSession(this)
        val aacp = session.own(BlockingSocket())
        val att = session.own(BlockingSocket())
        session.own(Closeable { throw IOException("close failed") })
        var delayedCalls = 0
        session.scope.launch { delay(5000); delayedCalls++ }
        runCurrent()

        session.close()
        session.close()
        advanceUntilIdle()
        assertEquals(1, aacp.closes.get())
        assertEquals(1, att.closes.get())
        assertEquals(0, delayedCalls)
    }

    @Test
    fun timeoutClosesSocketWhileConnectIsStillBlocked() = runTest {
        val session = ConnectionSession(this)
        val socket = session.own(BlockingSocket())
        var failure: Exception? = null
        val attempt = session.scope.launch {
            try {
                session.connect(5000) { socket.blockUntilClosed() }
            } catch (e: Exception) {
                failure = e
            }
        }
        runCurrent()
        socket.entered.awaitOrFail()
        advanceTimeBy(5000)
        runCurrent()
        socket.exited.awaitOrFail()
        attempt.join()

        assertTrue("Expected a timeout, got $failure", failure is TimeoutCancellationException)
        assertEquals(1, socket.closes.get())
    }

    @Test
    fun explicitDisconnectAbortsConnectWithoutWaitingForItsDeadline() = runTest {
        val session = ConnectionSession(this)
        val socket = session.own(BlockingSocket())
        var published = false
        val attempt = session.scope.launch {
            session.connect(5000) { socket.blockUntilClosed() }
            published = true
        }
        runCurrent()
        socket.entered.awaitOrFail()

        session.close()
        socket.exited.awaitOrFail()
        attempt.join()
        assertFalse(published)
        assertEquals(1, socket.closes.get())
    }

    @Test
    fun socketCreatedAfterCancellationIsImmediatelyClosed() = runTest {
        val session = ConnectionSession(this)
        val creating = CountDownLatch(1)
        val allowCreation = CountDownLatch(1)
        val creationFinished = CountDownLatch(1)
        val lateSocket = BlockingSocket()
        val rejected = AtomicBoolean()
        val attempt = session.scope.launch {
            session.connect(5000) {
                creating.countDown()
                allowCreation.awaitOrFail()
                try {
                    session.own(lateSocket)
                } catch (e: CancellationException) {
                    rejected.set(true)
                    throw e
                } finally {
                    creationFinished.countDown()
                }
            }
        }
        runCurrent()
        creating.awaitOrFail()
        session.close()
        allowCreation.countDown()
        creationFinished.awaitOrFail()
        attempt.join()
        assertTrue(rejected.get())
        assertEquals(1, lateSocket.closes.get())
    }

    @Test
    fun disconnectUnblocksReadAndRejectsItsLateResult() = runTest {
        val session = ConnectionSession(this)
        val socket = session.own(BlockingSocket())
        var delivered = false
        val reader = session.scope.launch {
            withContext(Dispatchers.IO) { socket.blockUntilClosed() }
            delivered = true
        }
        runCurrent()
        socket.entered.awaitOrFail()
        session.close()
        socket.exited.awaitOrFail()
        reader.join()
        assertFalse(delivered)
    }

    @Test
    fun queuedPacketsKeepTheirOrderAndTheirContents() = runBlocking {
        val session = ConnectionSession(this)
        val firstEntered = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val allWritten = CountDownLatch(3)
        val written = mutableListOf<Int>()
        val send = session.packetSender(write = { packet ->
            if (written.isEmpty()) {
                firstEntered.countDown()
                releaseFirst.awaitOrFail()
            }
            written.add(packet[0].toInt())
            allWritten.countDown()
        }, onFailure = { throw it })
        try {
            assertTrue(send(byteArrayOf(1)))
            firstEntered.awaitOrFail()
            val mutablePacket = byteArrayOf(2)
            assertTrue(send(mutablePacket))
            mutablePacket[0] = 99
            assertTrue(send(byteArrayOf(3)))
            releaseFirst.countDown()
            allWritten.awaitOrFail()
            assertEquals(listOf(1, 2, 3), written)
        } finally {
            releaseFirst.countDown()
            session.close()
        }
    }

    @Test
    fun disconnectAbortsBlockedWriteAndDiscardsQueuedPackets() = runBlocking {
        val session = ConnectionSession(this)
        val socket = session.own(BlockingSocket())
        val writes = AtomicInteger()
        val send = session.packetSender(write = {
            writes.incrementAndGet()
            socket.blockUntilClosed()
        }, onFailure = { throw it })
        assertTrue(send(byteArrayOf(1)))
        socket.entered.awaitOrFail()
        assertTrue(send(byteArrayOf(2)))
        session.close()
        socket.exited.awaitOrFail()
        withTimeout(5000) { session.scope.coroutineContext[Job]!!.join() }

        assertEquals(1, writes.get())
        assertFalse(send(byteArrayOf(3)))
    }

    @Test
    fun writeFailureNotifiesTheOwnerToCloseItsResources() = runBlocking {
        val sessions = ConnectionSessions(this)
        val session = sessions.begin()!!
        val socket = session.own(BlockingSocket())
        val failed = CompletableDeferred<Exception>()
        val send = session.packetSender(write = {
            throw IOException("write failed")
        }, onFailure = {
            sessions.finish(session)
            failed.complete(it)
        })
        assertTrue(send(byteArrayOf(1)))
        assertTrue(withTimeout(5000) { failed.await() } is IOException)
        assertNull(sessions.current)
        assertEquals(1, socket.closes.get())
        sessions.close()
    }

    /** Models Bluetooth I/O that ignores coroutine cancellation until close() is called. */
    private class BlockingSocket : Closeable {
        val entered = CountDownLatch(1)
        val exited = CountDownLatch(1)
        val closes = AtomicInteger()
        private val closed = CountDownLatch(1)

        fun blockUntilClosed() {
            entered.countDown()
            try {
                closed.awaitOrFail()
            } finally {
                exited.countDown()
            }
        }

        override fun close() {
            closes.incrementAndGet()
            closed.countDown()
        }
    }

    companion object {
        private fun CountDownLatch.awaitOrFail() {
            check(await(5, TimeUnit.SECONDS)) { "Blocking worker did not reach the expected state" }
        }
    }
}
