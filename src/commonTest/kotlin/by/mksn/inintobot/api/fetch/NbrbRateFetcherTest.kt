package by.mksn.inintobot.api.fetch

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.misc.toFiniteBigDecimal
import by.mksn.inintobot.test.assertEqualsUnordered
import by.mksn.inintobot.test.fullUrl
import by.mksn.inintobot.test.runTestBlocking
import by.mksn.inintobot.test.testCurrencies
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.headersOf
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.test.BeforeTest
import kotlin.test.Test

@UnstableDefault
@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class NbrbRateFetcherTest {

    private lateinit var httpClient: HttpClient
    private val testUrl = "http://test-url.org/getResponse"

    private val testResponseString = """[
    {"Cur_ID":290,"Date":"2020-06-06T00:00:00","Cur_Abbreviation":"UAH","Cur_Scale":100,"Cur_Name":"Гривен","Cur_OfficialRate":8.9520},
    {"Cur_ID":145,"Date":"2020-06-06T00:00:00","Cur_Abbreviation":"USD","Cur_Scale":1,"Cur_Name":"Доллар США","Cur_OfficialRate":2.3810},
    {"Cur_ID":292,"Date":"2020-06-06T00:00:00","Cur_Abbreviation":"EUR","Cur_Scale":1,"Cur_Name":"Евро","Cur_OfficialRate":2.7028},
    {"Cur_ID":301,"Date":"2020-06-06T00:00:00","Cur_Abbreviation":"KZT","Cur_Scale":1000,"Cur_Name":"Тенге","Cur_OfficialRate":5.9591},
    {"Cur_ID":302,"Date":"2020-06-06T00:00:00","Cur_Abbreviation":"TRY","Cur_Scale":10,"Cur_Name":"Турецких лир","Cur_OfficialRate":3.5166}
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
        val json = Json(JsonConfiguration(ignoreUnknownKeys = true))
        val apiConfig = RateApi("NBRB", setOf(), "BYN", testUrl)
        val fetcher = NbrbRateFetcher(apiConfig, httpClient, json)
        val actualRates = runTestBlocking { fetcher.fetch(testCurrencies) }
        val expectedRates = mapOf(
            testCurrencies.first { it.code == "UAH" } to "0.08952".toFiniteBigDecimal(),
            testCurrencies.first { it.code == "USD" } to "2.3810".toFiniteBigDecimal(),
            testCurrencies.first { it.code == "EUR" } to "2.7028".toFiniteBigDecimal(),
            testCurrencies.first { it.code == "KZT" } to "0.0059591".toFiniteBigDecimal(),
            testCurrencies.first { it.code == "BYN" } to "1".toFiniteBigDecimal()
        )
        assertEqualsUnordered(expectedRates.entries, actualRates.entries)
    }

}