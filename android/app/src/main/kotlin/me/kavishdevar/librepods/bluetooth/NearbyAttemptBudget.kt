package me.kavishdevar.librepods.bluetooth

/** Persist this checkpoint across process/job recreation; advertisements do not refill it. */
internal data class NearbyAttemptBudget(
    val address: String? = null,
    val reason: String? = null,
    val attempts: Int = 0,
    val lastAttempt: Long = -60_000
) {
    fun observe(currentAddress: String, currentReason: String?): NearbyAttemptBudget =
        if (address == currentAddress && reason == currentReason) this
        else NearbyAttemptBudget(currentAddress, currentReason, lastAttempt = lastAttempt)

    fun reserve(now: Long): NearbyAttemptBudget? {
        if (reason == null || attempts >= 3 || (now >= lastAttempt && now - lastAttempt < 60_000)) return null
        return copy(attempts = attempts + 1, lastAttempt = now)
    }
}
