package me.kavishdevar.librepods.bluetooth

/** A finite retry budget per Bluetooth connection, unaffected by duplicate connection events. */
internal class ConnectionRecovery {
    var address: String? = null
        private set
    private var retries = 0
    private var suppressed = false
    val allowsAutomaticConnection: Boolean get() = !suppressed

    fun connected(address: String) {
        if (this.address == address) return
        this.address = address
        retries = 0
        suppressed = false
    }

    fun disconnected() {
        address = null
        retries = 0
        suppressed = false
    }

    fun suppress() { suppressed = true }

    fun manualRetry() {
        retries = 0
        suppressed = false
    }

    fun nextDelayMillis(): Long? {
        if (address == null || suppressed) return null
        return listOf(1000L, 3000L, 10000L).getOrNull(retries)?.also { retries++ }
    }
}
