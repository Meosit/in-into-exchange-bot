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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.test.BeforeTest
import kotlin.test.Test

class NbuRateFetcherTest {

    private lateinit var httpClient: HttpClient
    private val testUrl = "http://test-url.org/getResponse"

    private val testResponseString = """[
    { 
    "r030":398,"txt":"Теньге","rate":0.066609,"cc":"KZT","exchangedate":"09.06.2020"
     }
    ,{ 
    "r030":410,"txt":"Вона","rate":0.022022,"cc":"KRW","exchangedate":"09.06.2020"
     }
    ,{ 
    "r030":643,"txt":"Російський рубль","rate":0.38778,"cc":"RUB","exchangedate":"09.06.2020"
     }
    ,{ 
    "r030":840,"txt":"Долар США","rate":26.6005,"cc":"USD","exchangedate":"09.06.2020"
     }
    ,{ 
    "r030":933,"txt":"Бiлоруський рубль","rate":11.1743,"cc":"BYN","exchangedate":"09.06.2020"
     }
    ,{ 
    "r030":978,"txt":"Євро","rate":30.1477,"cc":"EUR","exchangedate":"09.06.2020"
     }
    ]""".trimIndent()

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
        val json = Json(JsonConfiguration.Stable.copy(ignoreUnknownKeys = true))
        val apiConfig = RateApi("NBY", setOf(), "UAH", testUrl, setOf())
        val fetcher = NbuRateFetcher(apiConfig, httpClient, json)
        val expectedRates = mapOf(
            testCurrencies.first { it.code == "UAH" } to "1".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "USD" } to "26.6005".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "EUR" } to "30.1477".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "KZT" } to "0.066609".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "BYN" } to "11.1743".toFixedScaleBigDecimal()
        )
        runBlocking {
            val actualRates = fetcher.fetch(testCurrencies)
            assertEqualsUnordered(expectedRates.entries, actualRates.entries)
        }
    }
}