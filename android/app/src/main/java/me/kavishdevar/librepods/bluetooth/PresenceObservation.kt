package me.kavishdevar.librepods.bluetooth

/** Observation is a lease for the current Bluetooth connection, not for nearby idle devices. */
internal class PresenceObservation {
    private val observing = mutableSetOf<Int>()

    fun update(
        associations: Map<Int, String>,
        connectedAddress: String?,
        start: (String) -> Unit,
        stop: (String) -> Unit,
        failed: (Exception) -> Unit
    ): Boolean {
        observing.retainAll(associations.keys)
        var active = false
        associations.forEach { (id, address) ->
            try {
                if (connectedAddress != null && address.equals(connectedAddress, true)) {
                    if (id !in observing) {
                        start(address)
                        observing.add(id)
                    }
                    active = true
                } else {
                    // Also stop after process recreation: the system may have persisted an old lease.
                    stop(address)
                    observing.remove(id)
                }
            } catch (e: Exception) {
                failed(e)
            }
        }
        return active
    }
}
