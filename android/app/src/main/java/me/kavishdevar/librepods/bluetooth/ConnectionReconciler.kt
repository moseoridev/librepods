package me.kavishdevar.librepods.bluetooth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Main-dispatcher coordination for finite platform probes and immediate disconnect events. */
internal class ConnectionReconciler<T>(private val scope: CoroutineScope) {
    private var revision = 0L
    private var query: Job? = null

    fun refresh(
        read: suspend () -> T?,
        apply: (T?) -> Unit,
        failed: (Exception) -> Unit,
        completed: () -> Unit = {}
    ) {
        val generation = ++revision
        query?.cancel()
        // Enter finally even if a subsequent broadcast cancels this query before its first resume.
        query = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val result = withTimeoutOrNull(4000) { read() }
                if (generation == revision) apply(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation == revision) {
                    failed(e)
                    apply(null)
                }
            } finally {
                completed()
            }
        }
    }

    fun disconnect(apply: (T?) -> Unit) {
        revision++
        query?.cancel()
        apply(null)
    }
}
