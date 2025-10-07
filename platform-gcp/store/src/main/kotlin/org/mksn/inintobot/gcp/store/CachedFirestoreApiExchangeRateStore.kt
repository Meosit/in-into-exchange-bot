package org.mksn.inintobot.gcp.store

import org.mksn.inintobot.common.rate.ApiExchangeRates
import org.mksn.inintobot.common.store.ApiExchangeRateStore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Duration

class CachedFirestoreApiExchangeRateStore(
    private val delegate: FirestoreApiExchangeRateStore,
    private val cacheSize: Int = 100,
    private val cacheTtl: Duration = Duration.ofHours(12)
) : ApiExchangeRateStore {

    private val ratesCache = LRUCache<String, CachedRates>(cacheSize)
    private val historyStartCache = LRUCache<String, CachedHistoryStart>(50)

    private data class CachedRates(
        val rates: ApiExchangeRates,
        val timestamp: LocalDateTime
    )

    private data class CachedHistoryStart(
        val date: LocalDate,
        val timestamp: LocalDateTime
    )

    override fun historyStart(name: String): LocalDate {
        val cacheKey = "start-$name"
        historyStartCache.get(cacheKey)?.let { cached ->
            if (Duration.between(cached.timestamp, LocalDateTime.now()) < Duration.ofHours(1)) {
                return cached.date
            }
        }

        val date = delegate.historyStart(name)
        historyStartCache.put(cacheKey, CachedHistoryStart(date, LocalDateTime.now()))
        return date
    }

    override fun save(rates: ApiExchangeRates) {
        delegate.save(rates)

        // Cache the saved rates
        val rateKey = "${rates.api.name}-${rates.date}"
        ratesCache.put(rateKey, CachedRates(rates, LocalDateTime.now()))

        // Update latest cache only if this is newer than existing latest
        val latestKey = "latest-${rates.api.name}"
        val existingLatest = ratesCache.get(latestKey)?.rates
        if (existingLatest == null ||
            rates.date.isAfter(existingLatest.date) ||
            (rates.date.isEqual(existingLatest.date) && rates.time.isAfter(existingLatest.time))) {
            ratesCache.put(latestKey, CachedRates(rates, LocalDateTime.now()))
        }
    }

    override fun getForDate(name: String, date: LocalDate, backtrackDays: Int): ApiExchangeRates? {
        val cacheKey = "$name-$date-$backtrackDays"
        ratesCache.get(cacheKey)?.let { cached ->
            if (Duration.between(cached.timestamp, LocalDateTime.now()) < cacheTtl) {
                return cached.rates
            }
        }

        val rates = delegate.getForDate(name, date, backtrackDays)
        if (rates != null) {
            ratesCache.put(cacheKey, CachedRates(rates, LocalDateTime.now()))
        }
        return rates
    }

    override fun getHistoryForDate(name: String, date: LocalDate, backtrackDays: Int): List<ApiExchangeRates> {
        val history = delegate.getHistoryForDate(name, date, backtrackDays)

        // Cache each individual rate from the history
        history.forEach { rates ->
            val rateKey = "${rates.api.name}-${rates.date}"
            ratesCache.put(rateKey, CachedRates(rates, LocalDateTime.now()))
        }

        return history
    }

    override fun getLatest(name: String): ApiExchangeRates? {
        val cacheKey = "latest-$name"
        ratesCache.get(cacheKey)?.let { cached ->
            if (Duration.between(cached.timestamp, LocalDateTime.now()) < cacheTtl) {
                return cached.rates
            }
        }

        val rates = delegate.getLatest(name)
        if (rates != null) {
            ratesCache.put(cacheKey, CachedRates(rates, LocalDateTime.now()))
        }
        return rates
    }
}
