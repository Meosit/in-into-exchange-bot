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

class NbgRateFetcherTest {

    private lateinit var httpClient: HttpClient
    private val apiConfig = RateApis["NBG"]

    private val testResponseString = """[
  {
    "date": "2023-01-06T00:00:00.000Z",
    "currencies": [
      {
        "code": "EUR",
        "quantity": 1,
        "rateFormated": "2.8749",
        "diffFormated": "0.0028",
        "rate": 2.8749,
        "name": "Euro",
        "diff": -0.0028,
        "date": "2023-01-05T17:45:03.776Z",
        "validFromDate": "2023-01-06T00:00:00.000Z"
      },
      {
        "code": "USD",
        "quantity": 1,
        "rateFormated": "2.7086",
        "diffFormated": "0.0008",
        "rate": 2.7086,
        "name": "US Dollar",
        "diff": -0.0008,
        "date": "2023-01-05T17:45:03.776Z",
        "validFromDate": "2023-01-06T00:00:00.000Z"
      },
      {
        "code": "BYN",
        "quantity": 1,
        "rateFormated": "1.0757",
        "diffFormated": "0.0003",
        "rate": 1.0757,
        "name": "Belarusian Rouble",
        "diff": -0.0003,
        "date": "2023-01-05T17:45:03.776Z",
        "validFromDate": "2023-01-06T00:00:00.000Z"
      },
      {
        "code": "UAH",
        "quantity": 10,
        "rateFormated": "0.7352",
        "diffFormated": "0.0016",
        "rate": 0.7352,
        "name": "Ukraine Hryvna",
        "diff": 0.0016,
        "date": "2023-01-05T17:45:03.776Z",
        "validFromDate": "2023-01-06T00:00:00.000Z"
      },
      {
        "code": "RUB",
        "quantity": 100,
        "rateFormated": "3.7682",
        "diffFormated": "0.0039",
        "rate": 3.7682,
        "name": "Russian Ruble",
        "diff": 0.0039,
        "date": "2023-01-05T17:45:03.776Z",
        "validFromDate": "2023-01-06T00:00:00.000Z"
      },
      {
        "code": "DKK",
        "quantity": 10,
        "rateFormated": "3.8652",
        "diffFormated": "0.0039",
        "rate": 3.8652,
        "name": "Danish Krone",
        "diff": -0.0039,
        "date": "2023-01-05T17:45:03.776Z",
        "validFromDate": "2023-01-06T00:00:00.000Z"
      }
    ]
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

        val fetcher = NbgRateFetcher(apiConfig, httpClient, json)
        val testCurrencies = Currencies.filter { it.code in setOf("EUR", "USD", "BYN", "UAH", "RUB", "DKK") }
        val expectedRates = mapOf(
            testCurrencies.first { it.code == "EUR" } to 1.toFixedScaleBigDecimal() / "2.8749".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "USD" } to 1.toFixedScaleBigDecimal() / "2.7086".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "BYN" } to 1.toFixedScaleBigDecimal() / "1.0757".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "UAH" } to 10.toFixedScaleBigDecimal() / "0.7352".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "RUB" } to 100.toFixedScaleBigDecimal() / "3.7682".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "DKK" } to 10.toFixedScaleBigDecimal() / "3.8652".toFixedScaleBigDecimal(),
        )
        runBlocking {
            val actualRates = fetcher.fetch(testCurrencies)
            assertEqualsUnordered(expectedRates.entries, actualRates.entries)
        }
    }
}