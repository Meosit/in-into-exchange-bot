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

class CbrRateFetcherTest {

    private lateinit var httpClient: HttpClient
    val apiConfig = RateApis["CBR"]

    private val testResponseString = """
    <?xml version="1.0" encoding="windows-1251"?>
    <ValCurs Date="06.06.2020" name="Foreign Currency Market">
        <Valute ID="R01010"><NumCode>036</NumCode><CharCode>AUD</CharCode><Nominal>1</Nominal><Name>Австралийский доллар</Name><Value>47,9737</Value><VunitRate>47,9737</VunitRate></Valute>
        <Valute ID="R01020A"><NumCode>944</NumCode><CharCode>AZN</CharCode><Nominal>1</Nominal><Name>Азербайджанский манат</Name><Value>40,4550</Value><VunitRate>40,4550</VunitRate></Valute>
        <Valute ID="R01035"><NumCode>826</NumCode><CharCode>GBP</CharCode><Nominal>1</Nominal><Name>Фунт стерлингов Соединенного королевства</Name><Value>86,8468</Value><VunitRate>86,8468</VunitRate></Valute>
        <Valute ID="R01060"><NumCode>051</NumCode><CharCode>AMD</CharCode><Nominal>100</Nominal><Name>Армянских драмов</Name><Value>14,1801</Value><VunitRate>14,1801</VunitRate></Valute>
        <Valute ID="R01090B"><NumCode>933</NumCode><CharCode>BYN</CharCode><Nominal>1</Nominal><Name>Белорусский рубль</Name><Value>28,8248</Value><VunitRate>28,8248</VunitRate></Valute>
        <Valute ID="R01100"><NumCode>975</NumCode><CharCode>BGN</CharCode><Nominal>1</Nominal><Name>Болгарский лев</Name><Value>39,8120</Value><VunitRate>39,8120</VunitRate></Valute>
        <Valute ID="R01115"><NumCode>986</NumCode><CharCode>BRL</CharCode><Nominal>1</Nominal><Name>Бразильский реал</Name><Value>13,4081</Value><VunitRate>13,4081</VunitRate></Valute>
        <Valute ID="R01135"><NumCode>348</NumCode><CharCode>HUF</CharCode><Nominal>100</Nominal><Name>Венгерских форинтов</Name><Value>22,5908</Value><VunitRate>22,5908</VunitRate></Valute>
        <Valute ID="R01200"><NumCode>344</NumCode><CharCode>HKD</CharCode><Nominal>10</Nominal><Name>Гонконгских долларов</Name><Value>88,5561</Value><VunitRate>88,5561</VunitRate></Valute>
        <Valute ID="R01215"><NumCode>208</NumCode><CharCode>DKK</CharCode><Nominal>1</Nominal><Name>Датская крона</Name><Value>10,4461</Value><VunitRate>10,4461</VunitRate></Valute>
        <Valute ID="R01235"><NumCode>840</NumCode><CharCode>USD</CharCode><Nominal>1</Nominal><Name>Доллар США</Name><Value>68,6319</Value><VunitRate>68,6319</VunitRate></Valute>
        <Valute ID="R01239"><NumCode>978</NumCode><CharCode>EUR</CharCode><Nominal>1</Nominal><Name>Евро</Name><Value>77,9658</Value><VunitRate>77,9658</VunitRate></Valute>
        <Valute ID="R01270"><NumCode>356</NumCode><CharCode>INR</CharCode><Nominal>100</Nominal><Name>Индийских рупий</Name><Value>90,8334</Value><VunitRate>90,8334</VunitRate></Valute>
        <Valute ID="R01335"><NumCode>398</NumCode><CharCode>KZT</CharCode><Nominal>100</Nominal><Name>Казахстанских тенге</Name><Value>17,1363</Value><VunitRate>17,1363</VunitRate></Valute>
        <Valute ID="R01350"><NumCode>124</NumCode><CharCode>CAD</CharCode><Nominal>1</Nominal><Name>Канадский доллар</Name><Value>50,9403</Value><VunitRate>50,9403</VunitRate></Valute>
        <Valute ID="R01370"><NumCode>417</NumCode><CharCode>KGS</CharCode><Nominal>100</Nominal><Name>Киргизских сомов</Name><Value>92,8371</Value><VunitRate>92,8371</VunitRate></Valute>
        <Valute ID="R01375"><NumCode>156</NumCode><CharCode>CNY</CharCode><Nominal>10</Nominal><Name>Китайских юаней</Name><Value>96,8447</Value><VunitRate>96,8447</VunitRate></Valute>
        <Valute ID="R01500"><NumCode>498</NumCode><CharCode>MDL</CharCode><Nominal>10</Nominal><Name>Молдавских леев</Name><Value>39,9023</Value><VunitRate>39,9023</VunitRate></Valute>
        <Valute ID="R01535"><NumCode>578</NumCode><CharCode>NOK</CharCode><Nominal>10</Nominal><Name>Норвежских крон</Name><Value>73,8605</Value><VunitRate>73,8605</VunitRate></Valute>
        <Valute ID="R01565"><NumCode>985</NumCode><CharCode>PLN</CharCode><Nominal>1</Nominal><Name>Польский злотый</Name><Value>17,5283</Value><VunitRate>17,5283</VunitRate></Valute>
        <Valute ID="R01585F"><NumCode>946</NumCode><CharCode>RON</CharCode><Nominal>1</Nominal><Name>Румынский лей</Name><Value>16,0908</Value><VunitRate>16,0908</VunitRate></Valute>
        <Valute ID="R01589"><NumCode>960</NumCode><CharCode>XDR</CharCode><Nominal>1</Nominal><Name>СДР (специальные права заимствования)</Name><Value>94,4052</Value><VunitRate>94,4052</VunitRate></Valute>
        <Valute ID="R01625"><NumCode>702</NumCode><CharCode>SGD</CharCode><Nominal>1</Nominal><Name>Сингапурский доллар</Name><Value>49,2939</Value><VunitRate>49,2939</VunitRate></Valute>
        <Valute ID="R01670"><NumCode>972</NumCode><CharCode>TJS</CharCode><Nominal>10</Nominal><Name>Таджикских сомони</Name><Value>66,7139</Value><VunitRate>66,7139</VunitRate></Valute>
        <Valute ID="R01700J"><NumCode>949</NumCode><CharCode>TRY</CharCode><Nominal>1</Nominal><Name>Турецкая лира</Name><Value>10,1424</Value><VunitRate>10,1424</VunitRate></Valute>
        <Valute ID="R01710A"><NumCode>934</NumCode><CharCode>TMT</CharCode><Nominal>1</Nominal><Name>Новый туркменский манат</Name><Value>19,6372</Value><VunitRate>19,6372</VunitRate></Valute>
        <Valute ID="R01717"><NumCode>860</NumCode><CharCode>UZS</CharCode><Nominal>10000</Nominal><Name>Узбекских сумов</Name><Value>67,6243</Value><VunitRate>67,6243</VunitRate></Valute>
        <Valute ID="R01720"><NumCode>980</NumCode><CharCode>UAH</CharCode><Nominal>10</Nominal><Name>Украинских гривен</Name><Value>25,7973</Value><VunitRate>25,7973</VunitRate></Valute>
        <Valute ID="R01760"><NumCode>203</NumCode><CharCode>CZK</CharCode><Nominal>10</Nominal><Name>Чешских крон</Name><Value>29,2936</Value><VunitRate>29,2936</VunitRate></Valute>
        <Valute ID="R01770"><NumCode>752</NumCode><CharCode>SEK</CharCode><Nominal>10</Nominal><Name>Шведских крон</Name><Value>74,9993</Value><VunitRate>74,9993</VunitRate></Valute>
        <Valute ID="R01775"><NumCode>756</NumCode><CharCode>CHF</CharCode><Nominal>1</Nominal><Name>Швейцарский франк</Name><Value>71,7457</Value><VunitRate>71,7457</VunitRate></Valute>
        <Valute ID="R01810"><NumCode>710</NumCode><CharCode>ZAR</CharCode><Nominal>10</Nominal><Name>Южноафриканских рэндов</Name><Value>40,8791</Value><VunitRate>40,8791</VunitRate></Valute>
        <Valute ID="R01815"><NumCode>410</NumCode><CharCode>KRW</CharCode><Nominal>1000</Nominal><Name>Вон Республики Корея</Name><Value>56,7905</Value><VunitRate>56,7905</VunitRate></Valute>
        <Valute ID="R01820"><NumCode>392</NumCode><CharCode>JPY</CharCode><Nominal>100</Nominal><Name>Японских иен</Name><Value>62,7951</Value><VunitRate>62,7951</VunitRate></Valute>
    </ValCurs>
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
        val json = Json
        val fetcher = CbrRateFetcher(apiConfig, httpClient, json)
        val testCurrencies = Currencies.filter { it.code in setOf("UAH", "USD", "EUR", "KZT", "BYN", "PLN") }
        val expectedRates = mapOf(
            testCurrencies.first { it.code == "UAH" } to 10.toFixedScaleBigDecimal() / "25.7973".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "USD" } to 1.toFixedScaleBigDecimal() / "68.6319".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "EUR" } to 1.toFixedScaleBigDecimal() / "77.9658".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "KZT" } to 100.toFixedScaleBigDecimal() / "17.1363".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "BYN" } to 1.toFixedScaleBigDecimal() / "28.8248".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "PLN" } to 1.toFixedScaleBigDecimal() / "17.5283".toFixedScaleBigDecimal()
        )
        runBlocking {
            val actualRates = fetcher.fetch(testCurrencies)
            assertEqualsUnordered(expectedRates.entries, actualRates.entries) { it.key }
        }
    }
}