package me.kavishdevar.librepods.bluetooth

import java.io.Closeable
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/** One connection attempt and its resources, including sockets still inside connect(). */
internal class ConnectionSession(parentScope: CoroutineScope) : Closeable {
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val lock = Any()
    private val resources = mutableListOf<Closeable>()
    private var closed = false

    fun <T : Closeable> own(resource: T): T {
        synchronized(lock) {
            if (!closed) {
                resources.add(resource)
                return resource
            }
        }
        // Socket creation may finish after cancellation. Never leak that late socket.
        try {
            resource.close()
        } finally {
            throw CancellationException("Connection session closed")
        }
    }

    /** Cancellation closes sockets immediately, rather than waiting for blocking I/O to return. */
    suspend fun <T> connect(timeoutMillis: Long, block: () -> T): T = withTimeout(timeoutMillis) {
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { close() }
            scope.launch(Dispatchers.IO) {
                try {
                    continuation.resume(block())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    /** Queue protocol writes in order, off main, and bind them to this connection's socket. */
    fun packetSender(write: (ByteArray) -> Unit, onFailure: (Exception) -> Unit): (ByteArray) -> Boolean {
        val packets = Channel<ByteArray>(64)
        own(Closeable { packets.cancel() })
        scope.launch(Dispatchers.IO) {
            try {
                for (packet in packets) write(packet)
            } catch (e: Exception) {
                if (e !is CancellationException) scope.launch { onFailure(e) }
            }
        }
        // The result means accepted for sending. A write failure ends the owning session.
        return { packet ->
            val result = packets.trySend(packet.copyOf())
            if (result.isFailure && !result.isClosed) {
                scope.launch { onFailure(IOException("AACP outgoing queue is full")) }
            }
            result.isSuccess
        }
    }

    override fun close() {
        val toClose = synchronized(lock) {
            if (closed) return
            closed = true
            resources.toList().also { resources.clear() }
        }
        job.cancel()
        // A failure closing one resource must not keep another socket or reader alive.
        toClose.asReversed().forEach { runCatching { it.close() } }
    }
}

/** Transitions are confined to the service's main dispatcher. Worker threads only own resources. */
internal class ConnectionSessions(private val scope: CoroutineScope) {
    var current: ConnectionSession? = null
        private set
    private var closed = false

    fun begin(): ConnectionSession? {
        if (closed || current != null) return null
        return ConnectionSession(scope).also { current = it }
    }

    fun finish(session: ConnectionSession): Boolean {
        if (current !== session) return false
        current = null
        session.close()
        return true
    }

    fun close() {
        closed = true
        current?.let { finish(it) }
    }
}
