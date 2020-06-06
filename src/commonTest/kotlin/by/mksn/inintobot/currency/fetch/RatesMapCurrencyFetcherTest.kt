package by.mksn.inintobot.currency.fetch

import by.mksn.inintobot.app.ApiConfig
import by.mksn.inintobot.test.runTestBlocking
import by.mksn.inintobot.test.testCurrencies
import by.mksn.inintobot.util.toFiniteBigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.serialization.UnstableDefault
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@UnstableDefault
@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class RatesMapCurrencyFetcherTest {

    private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
    private val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"

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
        val fetcher = RatesMapCurrencyFetcher()
        val apiConfig = ApiConfig("Fixer", "USD", testUrl)
        val actualRates = runTestBlocking { fetcher.fetch(httpClient, testCurrencies, apiConfig) }
        val expectedRates = mapOf(
            testCurrencies.first { it.code == "UAH" } to "26.591049".toFiniteBigDecimal(),
            testCurrencies.first { it.code == "USD" } to "1".toFiniteBigDecimal(),
            testCurrencies.first { it.code == "EUR" } to "0.885622".toFiniteBigDecimal(),
            testCurrencies.first { it.code == "KZT" } to "399.629195".toFiniteBigDecimal(),
            testCurrencies.first { it.code == "BYN" } to "2.382028".toFiniteBigDecimal()
        )
        assertEquals(expectedRates, actualRates)
    }

}