package org.mksn.inintobot.common.rate

import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApiExchangeRatesTest {

    private val api = RateApi(
        name = "TEST",
        base = Currencies["USD"],
        aliases = arrayOf(),
        url = "",
        displayLink = "",
        unsupported = setOf(),
        refreshHours = 1,
        staleTimeoutHours = 25,
        backFillInfo = null
    )
    private val rates = ApiExchangeRates(
        LocalTime.MAX,
        LocalDate.MAX,
        api,
        mapOf(
            Currencies["USD"] to "1".toFixedScaleBigDecimal(),
            Currencies["GBP"] to "0.5".toFixedScaleBigDecimal(),
            Currencies["BYN"] to "2".toFixedScaleBigDecimal(),
            Currencies["PLN"] to "5".toFixedScaleBigDecimal()
        )
    )

    @Test
    fun exchange_same() {
        val source = "10".toFixedScaleBigDecimal()
        val target = rates.exchange(source, Currencies["USD"], Currencies["USD"])
        assertEquals(source, target)
    }

    @Test
    fun exchange_to_base() {
        val source = "10".toFixedScaleBigDecimal()
        val expected = "20".toFixedScaleBigDecimal()
        val target = rates.exchange(source, Currencies["GBP"], Currencies["USD"])
        assertEquals(expected, target)
    }

    @Test
    fun exchange_not_base() {
        val source = "10".toFixedScaleBigDecimal()
        val expected = "100".toFixedScaleBigDecimal()
        val target = rates.exchange(source, Currencies["GBP"], Currencies["PLN"])
        assertEquals(expected, target)
    }

    @Test
    fun exchange_unknown_source() {
        val source = "10".toFixedScaleBigDecimal()
        val currency = Currencies["KZT"]
        val exception = assertFailsWith<UnknownCurrencyException> { rates.exchange(source, currency, Currencies["PLN"]) }
        assertEquals(api, exception.api)
        assertEquals(currency, exception.currency)
    }

    @Test
    fun exchange_unknown_target() {
        val source = "10".toFixedScaleBigDecimal()
        val missing = Currencies["KZT"]
        val exception = assertFailsWith<UnknownCurrencyException> { rates.exchange(source, Currencies["PLN"], missing) }
        assertEquals(api, exception.api)
        assertEquals(missing, exception.currency)
    }

    @Test
    fun exchangeAll() {

        val source = "10".toFixedScaleBigDecimal()

        val expected1 = source
        val expected2 = "100".toFixedScaleBigDecimal()
        val target = rates.exchangeAll(source, Currencies["GBP"], listOf(Currencies["GBP"], Currencies["PLN"]))
        assertEquals(expected1, target[0].value)
        assertEquals(expected2, target[1].value)
    }

    @Test
    fun exchangeAll_one_missing() {

        val source = "10".toFixedScaleBigDecimal()

        val expected1 = source
        val expected2 = "100".toFixedScaleBigDecimal()
        val missing = Currencies["KZT"]
        val exc = assertFailsWith<MissingCurrenciesException> { rates.exchangeAll(source, Currencies["GBP"],
            listOf(Currencies["GBP"], Currencies["PLN"], missing)) }

        assertEquals(missing, exc.missing[0])
        assertEquals(expected1, exc.exchanges[0].value)
        assertEquals(expected2, exc.exchanges[1].value)
    }
}