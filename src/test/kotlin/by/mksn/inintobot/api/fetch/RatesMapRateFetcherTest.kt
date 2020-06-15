package by.mksn.inintobot.api.fetch

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.misc.toFixedScaleBigDecimal
import by.mksn.inintobot.test.assertEqualsUnordered
import by.mksn.inintobot.test.fullUrl
import by.mksn.inintobot.test.testCurrencies
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.test.BeforeTest
import kotlin.test.Test

@UnstableDefault
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
        val json = Json(JsonConfiguration(ignoreUnknownKeys = true))
        val apiConfig = RateApi("Fixer", setOf(), "USD", testUrl, setOf())
        val fetcher = RatesMapRateFetcher(apiConfig, httpClient, json)
        val expectedRates = mapOf(
            testCurrencies.first { it.code == "UAH" } to "26.591049".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "USD" } to "1".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "EUR" } to "0.885622".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "KZT" } to "399.629195".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "BYN" } to "2.382028".toFixedScaleBigDecimal()
        )
        runBlocking {
            val actualRates = fetcher.fetch(testCurrencies)
            assertEqualsUnordered(expectedRates.entries, actualRates.entries)
        }
    }

}