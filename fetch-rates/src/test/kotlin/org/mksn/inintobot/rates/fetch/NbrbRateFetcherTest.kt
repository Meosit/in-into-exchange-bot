package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.mksn.inintobot.currency.Currencies
import org.mksn.inintobot.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.rates.RateApi
import org.mksn.inintobot.rates.assertEqualsUnordered
import org.mksn.inintobot.rates.fullUrl
import kotlin.test.BeforeTest
import kotlin.test.Test

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
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val apiConfig = RateApi("NBRB", arrayOf(), Currencies["BYN"], testUrl, testUrl, setOf(), 1)
        val fetcher = NbrbRateFetcher(apiConfig, httpClient, json)
        val testCurrencies = Currencies.filter { it.code in setOf("UAH", "USD", "EUR", "KZT", "BYN") }
        val expectedRates = mapOf(
            testCurrencies.first { it.code == "UAH" } to 100.toFixedScaleBigDecimal() / "8.952".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "USD" } to 1.toFixedScaleBigDecimal() / "2.3810".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "EUR" } to 1.toFixedScaleBigDecimal() / "2.7028".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "KZT" } to 1000.toFixedScaleBigDecimal() / "5.9591".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "BYN" } to "1".toFixedScaleBigDecimal()
        )
        runBlocking {
            val actualRates = fetcher.fetch(testCurrencies)
            assertEqualsUnordered(expectedRates.entries, actualRates.entries) { it.key }
        }
    }

}