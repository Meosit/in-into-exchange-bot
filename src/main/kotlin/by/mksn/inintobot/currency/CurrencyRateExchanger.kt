package by.mksn.inintobot.currency

import by.mksn.inintobot.misc.toFixedScaleBigDecimal
import java.math.BigDecimal

/**
 * Provides currency rate conversion logic according to provided [exchangeRates] relative to [baseCurrency]
 *
 * @property baseCurrency the main [Currency] this class operates with.
 *                        Two different currencies are compared through this currency
 * @property exchangeRates the map of rates compared to [baseCurrency].
 */
class CurrencyRateExchanger(
    private val baseCurrency: Currency,
    private val exchangeRates: Map<Currency, BigDecimal>
) {

    /**
     * Exchanges the provided [value] in the given [currency] to the [baseCurrency] according to the [exchangeRates]
     */
    fun exchangeToBase(value: BigDecimal, currency: Currency): BigDecimal =
        value.takeIf { currency == baseCurrency }
            ?: exchangeRates[currency]?.let { rate -> (value / rate).toFixedScaleBigDecimal() }
            ?: throw UnknownCurrencyException(currency)

    /**
     * Exchanges the provided [value] in the given [sourceCurrency] to the [targetCurrency]
     * using [exchangeRates] and [baseCurrency] as comparison criteria
     */
    fun exchange(value: BigDecimal, sourceCurrency: Currency, targetCurrency: Currency): BigDecimal =
        when (targetCurrency) {
            sourceCurrency -> value
            baseCurrency -> exchangeToBase(value, sourceCurrency)
            else -> exchangeRates[targetCurrency]?.let { rate -> (exchangeToBase(value, sourceCurrency) * rate).toFixedScaleBigDecimal() }
                ?: throw UnknownCurrencyException(targetCurrency)
        }

    /**
     * Exchanges the provided [value] in the given [sourceCurrency] to multiple [targets] currencies
     * using [exchangeRates] and [baseCurrency] as comparison criteria
     */
    fun exchangeAll(value: BigDecimal, sourceCurrency: Currency, targets: List<Currency>): List<Exchange> =
        targets.asSequence().map { Exchange(it, exchange(value, sourceCurrency, it)) }.toList()

}

class UnknownCurrencyException(val currency: Currency) : RuntimeException()