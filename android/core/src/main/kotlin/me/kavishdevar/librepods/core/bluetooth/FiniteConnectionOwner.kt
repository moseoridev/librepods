package me.kavishdevar.librepods.core.bluetooth

import java.io.Closeable

/** Main-confined ownership: a finished job cannot tear down a newer job or manual/CDM owner. */
internal class FiniteConnectionOwner {
    private var generation = 0L
    fun handoff() { generation++ }
    fun acquire(releaseOwned: () -> Unit): Closeable {
        val lease = ++generation
        var closed = false
        return Closeable {
            if (!closed) {
                closed = true
                if (generation == lease) {
                    generation++
                    releaseOwned()
                }
            }
        }
    }
}
