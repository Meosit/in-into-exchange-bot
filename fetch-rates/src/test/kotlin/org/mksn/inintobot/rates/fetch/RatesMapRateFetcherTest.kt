package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.rate.RateApi
import org.mksn.inintobot.rates.assertEqualsUnordered
import org.mksn.inintobot.rates.fullUrl
import kotlin.test.BeforeTest
import kotlin.test.Test

class RatesMapRateFetcherTest {

    private lateinit var httpClient: HttpClient
    private val testUrl = "http://test-url.org/getResponse"

    private val testResponseString = """
        {
          "timestamp": 1591459141,
          "base": "USD",
          "rates": {
            "BGN": 1.732927,
            "BTC": 0.000103541115,
            "BYN": 2.382028,
            "CAD": 1.34217,
            "CHF": 0.96235,
            "CNY": 7.082,
            "CZK": 23.5645,
            "DKK": 6.6044,
            "EUR": 0.885622,
            "GBP": 0.789546,
            "ILS": 3.4689,
            "ISK": 132.1,
            "JPY": 109.59000279,
            "KZT": 399.629195,
            "NOK": 9.29962,
            "PLN": 3.92885,
            "RUB": 68.49,
            "SEK": 9.18276,
            "TRY": 6.77,
            "UAH": 26.591049,
            "USD": 1
          }
        }
    """.trimIndent()

    @BeforeTest
    fun setup() {
        httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullUrl) {
                        testUrl -> {
                            val responseHeaders =
                                headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                            respond(testResponseString, headers = responseHeaders)
                        }
                        else -> error("Unhandled ${request.url.fullUrl}")
                    }
                }
            }
        }
    }

    @Test
    fun successful_fetch_and_parse() {
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val apiConfig = RateApi("Fixer", arrayOf(), Currencies["USD"], testUrl, testUrl, setOf(), 1)
        val fetcher = RatesMapRateFetcher(apiConfig, httpClient, json)
        val testCurrencies = Currencies.filter { it.code in setOf("UAH", "USD", "EUR", "KZT", "BYN", "PLN") }
        val expectedRates = mapOf(
            testCurrencies.first { it.code == "UAH" } to "26.591049".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "USD" } to "1".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "EUR" } to "0.885622".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "KZT" } to "399.629195".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "BYN" } to "2.382028".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "PLN" } to "3.92885".toFixedScaleBigDecimal()
        )
        runBlocking {
            val actualRates = fetcher.fetch(testCurrencies)
            assertEqualsUnordered(expectedRates.entries, actualRates.entries)
        }
    }

}