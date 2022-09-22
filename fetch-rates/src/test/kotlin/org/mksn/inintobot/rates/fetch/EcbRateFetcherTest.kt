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

class EcbRateFetcherTest {

    private lateinit var httpClient: HttpClient
    private val testUrl = "http://test-url.org/getResponse"

    private val testResponseString = """
    <?xml version="1.0" encoding="UTF-8"?>
    <gesmes:Envelope xmlns:gesmes="http://www.gesmes.org/xml/2002-08-01" xmlns="http://www.ecb.int/vocabulary/2002-08-01/eurofxref">
        <gesmes:subject>Reference rates</gesmes:subject>
        <gesmes:Sender>
            <gesmes:name>European Central Bank</gesmes:name>
        </gesmes:Sender>
        <Cube>
            <Cube time='2020-06-05'>
                <Cube currency='USD' rate='1.1330'/>
                <Cube currency='JPY' rate='123.77'/>
                <Cube currency='BGN' rate='1.9558'/>
                <Cube currency='CZK' rate='26.589'/>
                <Cube currency='DKK' rate='7.4564'/>
                <Cube currency='GBP' rate='0.89448'/>
                <Cube currency='HUF' rate='344.62'/>
                <Cube currency='PLN' rate='4.4425'/>
                <Cube currency='RON' rate='4.8382'/>
                <Cube currency='SEK' rate='10.4250'/>
                <Cube currency='CHF' rate='1.0866'/>
                <Cube currency='ISK' rate='148.90'/>
                <Cube currency='NOK' rate='10.5403'/>
                <Cube currency='HRK' rate='7.5715'/>
                <Cube currency='RUB' rate='77.8155'/>
                <Cube currency='TRY' rate='7.6747'/>
                <Cube currency='AUD' rate='1.6227'/>
                <Cube currency='BRL' rate='5.7329'/>
                <Cube currency='CAD' rate='1.5237'/>
                <Cube currency='CNY' rate='8.0349'/>
                <Cube currency='HKD' rate='8.7809'/>
                <Cube currency='IDR' rate='15882.40'/>
                <Cube currency='ILS' rate='3.9172'/>
                <Cube currency='INR' rate='85.6300'/>
                <Cube currency='KRW' rate='1365.57'/>
                <Cube currency='MXN' rate='24.6466'/>
                <Cube currency='MYR' rate='4.8345'/>
                <Cube currency='NZD' rate='1.7392'/>
                <Cube currency='PHP' rate='56.457'/>
                <Cube currency='SGD' rate='1.5775'/>
                <Cube currency='THB' rate='35.650'/>
                <Cube currency='ZAR' rate='19.0823'/>
            </Cube>
        </Cube>
    </gesmes:Envelope>
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
        val json = Json
        val apiConfig = RateApi("ECB", arrayOf(), Currencies["EUR"], testUrl, testUrl, setOf(), 1, 24)
        val fetcher = EcbRateFetcher(apiConfig, httpClient, json)
        val testCurrencies = Currencies.filter { it.code in setOf("USD", "EUR", "PLN") }
        val expectedRates = mapOf(
            testCurrencies.first { it.code == "USD" } to "1.1330".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "EUR" } to "1".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "PLN" } to "4.4425".toFixedScaleBigDecimal()
        )
        runBlocking {
            val actualRates = fetcher.fetch(testCurrencies)
            assertEqualsUnordered(expectedRates.entries, actualRates.entries)
        }
    }
}