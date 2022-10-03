package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.rate.RateApis
import org.mksn.inintobot.rates.assertEqualsUnordered
import org.mksn.inintobot.rates.fullUrl
import kotlin.test.BeforeTest
import kotlin.test.Test

class TradermadeRateFetcherTest {

    private lateinit var httpClient: HttpClient
    private val apiConfig = RateApis["TraderMade"]

    private val testResponseString = """
        {
          "date": "2022-07-20", 
          "endpoint": "historical", 
          "quotes": [
            {
              "base_currency": "USD", 
              "close": 0.98237, 
              "high": 0.98454, 
              "low": 0.97402, 
              "open": 0.97766, 
              "quote_currency": "EUR"
            }, 
            {
              "base_currency": "USD", 
              "close": 54.24403, 
              "high": 56.50151, 
              "low": 53.92607, 
              "open": 56.49762, 
              "quote_currency": "RUB"
            }, 
            {
              "base_currency": "USD", 
              "close": 4.6839, 
              "high": 4.7075, 
              "low": 4.6239, 
              "open": 4.6478, 
              "quote_currency": "PLN"
            }, 
            {
              "error": 204, 
              "instrument": "USDKRW", 
              "message": "data requested not available for the date"
            }
          ], 
          "request_time": "Thu, 29 Sep 2022 08:46:25 GMT"
        }
    """.trimIndent()

    @BeforeTest
    fun setup() {
        httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullUrl) {
                        apiConfig.url -> {
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

        val fetcher = TradermadeRateFetcher(apiConfig, httpClient, json)
        val testCurrencies = Currencies.filter { it.code in setOf("EUR", "USD", "RUB", "PLN", "KRW") }
        val expectedRates = mapOf(
            testCurrencies.first { it.code == "EUR" } to "0.98237".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "USD" } to "1".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "RUB" } to "54.24403".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "PLN" } to "4.6839".toFixedScaleBigDecimal()
        )
        runBlocking {
            val actualRates = fetcher.fetch(testCurrencies)
            assertEqualsUnordered(expectedRates.entries, actualRates.entries)
        }
    }
}