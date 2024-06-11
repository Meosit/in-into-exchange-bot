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

class NbrbRateFetcherTest {

    private lateinit var httpClient: HttpClient
    private val apiConfig = RateApis["NBRB"]

    private val testResponseString = """[
        {
            "Cur_ID": 459,
            "Date": "2024-04-10T00:00:00",
            "Cur_Abbreviation": "KZT",
            "Cur_Scale": 1000,
            "Cur_Name": "Тенге",
            "Cur_OfficialRate": 7.2886
        },
        {
            "Cur_ID": 449,
            "Date": "2024-04-10T00:00:00",
            "Cur_Abbreviation": "UAH",
            "Cur_Scale": 100,
            "Cur_Name": "Гривен",
            "Cur_OfficialRate": 8.3688
        },
        {
            "Cur_ID": 431,
            "Date": "2024-04-10T00:00:00",
            "Cur_Abbreviation": "USD",
            "Cur_Scale": 1,
            "Cur_Name": "Доллар США",
            "Cur_OfficialRate": 3.2551
        },
        {
            "Cur_ID": 451,
            "Date": "2024-04-10T00:00:00",
            "Cur_Abbreviation": "EUR",
            "Cur_Scale": 1,
            "Cur_Name": "Евро",
            "Cur_OfficialRate": 3.5368
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

        val fetcher = NbrbRateFetcher(apiConfig, httpClient, json)
        val testCurrencies = Currencies.filter { it.code in setOf("UAH", "USD", "EUR", "KZT", "BYN") }
        val expectedRates = mapOf(
            testCurrencies.first { it.code == "UAH" } to 100.toFixedScaleBigDecimal() / "8.3688".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "USD" } to 1.toFixedScaleBigDecimal() / "3.2551".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "EUR" } to 1.toFixedScaleBigDecimal() / "3.5368".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "KZT" } to 1000.toFixedScaleBigDecimal() / "7.2886".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "BYN" } to 1.toFixedScaleBigDecimal()
        )
        runBlocking {
            val actualRates = fetcher.fetch(testCurrencies)
            assertEqualsUnordered(expectedRates.entries, actualRates.entries)
        }
    }
}