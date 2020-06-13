package by.mksn.inintobot.currency

import by.mksn.inintobot.test.bigDecimal
import by.mksn.inintobot.test.toCurrency
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class CurrencyRateExchangerTest {

    private val baseCurrency = "BYN".toCurrency()
    private val exchangeRates = mapOf(
        "USD".toCurrency() to 0.5.bigDecimal,
        "EUR".toCurrency() to 0.4.bigDecimal,
        "UAH".toCurrency() to 2.bigDecimal
    )

    private val currencyRateExchanger = CurrencyRateExchanger(baseCurrency, exchangeRates)

    @Test
    fun exchange_to_base_from_USD() {
        val sourceCurrency = "USD".toCurrency()
        val value = 10.bigDecimal

        val exchangedValue = currencyRateExchanger.exchangeToBase(value, sourceCurrency)

        assertEquals(20.bigDecimal, exchangedValue)
    }

    @Test
    fun exchange_to_base_from_UAH() {
        val sourceCurrency = "UAH".toCurrency()
        val value = 10.bigDecimal

        val exchangedValue = currencyRateExchanger.exchangeToBase(value, sourceCurrency)

        assertEquals(5.bigDecimal, exchangedValue)
    }

    @Test
    fun exchange_from_USD_to_UAH() {
        val sourceCurrency = "USD".toCurrency()
        val targetCurrency = "UAH".toCurrency()
        val value = 10.bigDecimal

        val exchangedValue = currencyRateExchanger.exchange(value, sourceCurrency, targetCurrency)

        assertEquals(40.bigDecimal, exchangedValue)
    }

    @Test
    fun exchange_from_USD_to_EUR() {
        val sourceCurrency = "USD".toCurrency()
        val targetCurrency = "EUR".toCurrency()
        val value = 10.bigDecimal

        val exchangedValue = currencyRateExchanger.exchange(value, sourceCurrency, targetCurrency)

        assertEquals(10.bigDecimal / 2.5.bigDecimal * 2.bigDecimal, exchangedValue)
    }


}