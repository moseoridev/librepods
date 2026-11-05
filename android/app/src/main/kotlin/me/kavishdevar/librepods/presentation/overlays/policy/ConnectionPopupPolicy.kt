package me.kavishdevar.librepods.presentation.overlays.policy

/** The persisted marker belongs to a physical link, not an AACP retry or service instance. */
internal class ConnectionPopupPolicy(
    private val consumed: (String) -> Boolean,
    private val mark: (String) -> Unit,
) {
    fun consumeReady(address: String, connected: Boolean, fresh: Boolean, usable: Boolean): Boolean {
        if (!connected || !fresh || !usable || consumed(address)) return false
        mark(address)
        return true
    }
}

/** One memory-only request. Repeated events cannot extend its deadline. */
internal class PendingIslandRequest<T> {
    private data class Entry<T>(val owner: T, val deadline: Long)
    private var pending: Entry<T>? = null
    val owner: T? get() = pending?.owner
    fun request(owner: T, now: Long) {
        if (pending?.owner != owner) pending = Entry(owner, now + 30_000)
    }
    fun clear() { pending = null }
    fun clearOwner(predicate: (T) -> Boolean) { if (pending?.owner?.let(predicate) == true) clear() }
    fun consume(now: Long, displayReady: Boolean, isCurrent: (T) -> Boolean): T? {
        val request = pending ?: return null
        if (now > request.deadline || !isCurrent(request.owner)) { clear(); return null }
        if (!displayReady) return null
        clear()
        return request.owner
    }
}
