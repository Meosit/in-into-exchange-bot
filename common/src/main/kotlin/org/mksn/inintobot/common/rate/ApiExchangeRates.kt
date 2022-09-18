package org.mksn.inintobot.common.rate

import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.misc.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Currency API Configuration
 */
data class ApiExchangeRates(
    val time: LocalTime,
    val date: LocalDate,
    val api: RateApi,
    val rates: Map<Currency, BigDecimal>
) {
    /**
     * Exchanges the provided [value] in the given [currency] to the [api] base according to the [exchangeRates]
     */
    private fun exchangeToBase(value: BigDecimal, currency: Currency): BigDecimal =
        value.takeIf { currency == api.base }
            ?: rates[currency]?.let { rate -> (value / rate).toFixedScaleBigDecimal() }
            ?: throw UnknownCurrencyException(api, currency)

    /**
     * Exchanges the provided [value] in the given [sourceCurrency] to the [targetCurrency]
     * using [rates] and [api] base as comparison criteria
     */
    fun exchange(value: BigDecimal, sourceCurrency: Currency, targetCurrency: Currency): BigDecimal =
        when (targetCurrency) {
            sourceCurrency -> value
            api.base -> exchangeToBase(value, sourceCurrency)
            else -> rates[targetCurrency]?.let { rate -> (exchangeToBase(value, sourceCurrency) * rate).toFixedScaleBigDecimal() }
                ?: throw UnknownCurrencyException(api, targetCurrency)
        }

    /**
     * Exchanges the provided [value] in the given [sourceCurrency] to multiple [targets] currencies
     * using [rates] and [api] base as comparison criteria
     */
    fun exchangeAll(value: BigDecimal, sourceCurrency: Currency, targets: List<Currency>): List<Exchange> {
        val missingRates = targets.minus(rates.keys)
        val exchanges = targets.asSequence().filterNot { it in missingRates }.map { Exchange(it, exchange(value, sourceCurrency, it)) }.toList()
        if (missingRates.isNotEmpty()) {
            throw MissingCurrenciesException(api, exchanges, missingRates)
        }
        return exchanges
    }

    fun staleData() = ChronoUnit.HOURS.between(ZonedDateTime.of(date, time, ZoneOffset.UTC), ZonedDateTime.now(ZoneOffset.UTC)) >= api.refreshHours * 2

}

