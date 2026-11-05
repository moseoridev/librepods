package me.kavishdevar.librepods.billing

object BillingManager {
    private var factory: (() -> BillingProvider)? = null
    private var instance: BillingProvider? = null
    val provider: BillingProvider
        get() = synchronized(this) {
            instance ?: checkNotNull(factory) { "Billing not configured" }.invoke().also { instance = it }
        }
    @Synchronized fun configure(create: () -> BillingProvider) {
        check(instance == null) { "Billing already initialized" }
        factory = create
    }
    @Synchronized fun queryIfInitialized() { instance?.queryPurchases() }
}
