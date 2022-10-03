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

class NbuRateFetcherTest {

    private lateinit var httpClient: HttpClient
    private val apiConfig = RateApis["NBU"]

    private val testResponseString = """[
{ 
"StartDate":"29.09.2022","TimeSign":"0000","CurrencyCode":"933","CurrencyCodeL":"BYN","Units":1,"Amount":13.2919
 }
,{ 
"StartDate":"29.09.2022","TimeSign":"0000","CurrencyCode":"840","CurrencyCodeL":"USD","Units":1,"Amount":36.5686
 }
,{ 
"StartDate":"29.09.2022","TimeSign":"0000","CurrencyCode":"978","CurrencyCodeL":"EUR","Units":1,"Amount":34.9742
 }
,{ 
"StartDate":"29.09.2022","TimeSign":"0000","CurrencyCode":"398","CurrencyCodeL":"KZT","Units":100,"Amount":7.6539
 }
]""".trimIndent()

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

        val fetcher = NbuRateFetcher(apiConfig, httpClient, json)
        val testCurrencies = Currencies.filter { it.code in setOf("UAH", "USD", "EUR", "KZT", "BYN") }
        val expectedRates = mapOf(
            testCurrencies.first { it.code == "UAH" } to 1.toFixedScaleBigDecimal() / "1".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "USD" } to 1.toFixedScaleBigDecimal() / "36.5686".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "EUR" } to 1.toFixedScaleBigDecimal() / "34.9742".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "KZT" } to 1.toFixedScaleBigDecimal() / "0.076539".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "BYN" } to 1.toFixedScaleBigDecimal() / "13.2919".toFixedScaleBigDecimal()
        )
        runBlocking {
            val actualRates = fetcher.fetch(testCurrencies)
            assertEqualsUnordered(expectedRates.entries, actualRates.entries)
        }
    }
}