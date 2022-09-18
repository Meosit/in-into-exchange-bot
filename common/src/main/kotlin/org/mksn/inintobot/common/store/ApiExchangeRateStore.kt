package org.mksn.inintobot.common.store

import org.mksn.inintobot.common.rate.ApiExchangeRates
import java.time.LocalDate

interface ApiExchangeRateStore {

    /**
     * Saves specified [rates] to the platform-specific store
     */
    fun save(rates: ApiExchangeRates)


    /**
     * Finds [ApiExchangeRates] for specified [name] and [date] or [backtrackDays] before it in case nothing found.
     * Returns `null` if no rates found.
     */
    fun getForDate(name: String, date: LocalDate, backtrackDays: Int = 0): ApiExchangeRates?

    /**
     * Finds latest [ApiExchangeRates] for the specified [name]
     */
    fun getLatest(name: String): ApiExchangeRates?

}