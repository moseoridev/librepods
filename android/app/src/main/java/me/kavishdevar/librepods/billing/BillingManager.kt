package me.kavishdevar.librepods.billing

import android.content.Context

object BillingManager {
    private lateinit var appContext: Context
    private var instance: BillingProvider? = null

    fun initialize(context: Context) { appContext = context.applicationContext }

    val provider: BillingProvider
        get() = instance ?: BillingProviderFactory.create(appContext).also { instance = it }
}
