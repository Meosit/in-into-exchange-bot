package org.mksn.inintobot.common.store

import java.util.*

/**
 * Connection point for platform-dependent store implementation
 */
interface StoreProvider {

    fun exchangeRateStore(): ApiExchangeRateStore

    fun userSettingsStore(): UserSettingsStore

    fun userAggregateStatsStore(): UserAggregateStatsStore

    companion object {
        fun load(): StoreProvider {
            val providers = StoreProvider::class.java.let { ServiceLoader.load(it, it.classLoader) }.toList()
            if (providers.size != 1) {
                throw Exception("Expected exactly one StoreProvider available, got ${providers.size}")
            }
            return providers[0]
        }
    }
}