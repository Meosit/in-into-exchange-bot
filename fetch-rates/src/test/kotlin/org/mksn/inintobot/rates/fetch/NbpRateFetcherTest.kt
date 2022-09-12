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

class NbpRateFetcherTest {

    private lateinit var httpClient: HttpClient
    private val testUrl = "http://test-url.org/getResponse<table_type>"

    private val testUrlA = "http://test-url.org/getResponseA"
    private val testResponseStringA = """[{"table":"A","no":"092/A/NBP/2022","effectiveDate":"2022-05-13","rates":[{"currency":"bat (Tajlandia)","code":"THB","mid":0.1291},{"currency":"dolar amerykański","code":"USD","mid":4.4849},{"currency":"dolar australijski","code":"AUD","mid":3.0893},{"currency":"dolar Hongkongu","code":"HKD","mid":0.5713},{"currency":"dolar kanadyjski","code":"CAD","mid":3.4484},{"currency":"dolar nowozelandzki","code":"NZD","mid":2.8011},{"currency":"dolar singapurski","code":"SGD","mid":3.2146},{"currency":"euro","code":"EUR","mid":4.6679},{"currency":"forint (Węgry)","code":"HUF","mid":0.012155},{"currency":"frank szwajcarski","code":"CHF","mid":4.4843},{"currency":"funt szterling","code":"GBP","mid":5.4736},{"currency":"hrywna (Ukraina)","code":"UAH","mid":0.1467},{"currency":"jen (Japonia)","code":"JPY","mid":0.034818},{"currency":"korona czeska","code":"CZK","mid":0.1881},{"currency":"korona duńska","code":"DKK","mid":0.6273},{"currency":"korona islandzka","code":"ISK","mid":0.033414},{"currency":"korona norweska","code":"NOK","mid":0.4563},{"currency":"korona szwedzka","code":"SEK","mid":0.4444},{"currency":"kuna (Chorwacja)","code":"HRK","mid":0.6204},{"currency":"lej rumuński","code":"RON","mid":0.9435},{"currency":"lew (Bułgaria)","code":"BGN","mid":2.3866},{"currency":"lira turecka","code":"TRY","mid":0.2898},{"currency":"nowy izraelski szekel","code":"ILS","mid":1.3145},{"currency":"peso chilijskie","code":"CLP","mid":0.005181},{"currency":"peso filipińskie","code":"PHP","mid":0.0855},{"currency":"peso meksykańskie","code":"MXN","mid":0.2224},{"currency":"rand (Republika Południowej Afryki)","code":"ZAR","mid":0.2793},{"currency":"real (Brazylia)","code":"BRL","mid":0.8735},{"currency":"ringgit (Malezja)","code":"MYR","mid":1.0206},{"currency":"rupia indonezyjska","code":"IDR","mid":0.00030692},{"currency":"rupia indyjska","code":"INR","mid":0.057962},{"currency":"won południowokoreański","code":"KRW","mid":0.003499},{"currency":"yuan renminbi (Chiny)","code":"CNY","mid":0.6610},{"currency":"SDR (MFW)","code":"XDR","mid":5.9782}]}]""".trimIndent()

    private val testUrlB = "http://test-url.org/getResponseB"
    private val testResponseStringB = """[{"table":"B","no":"019/B/NBP/2022","effectiveDate":"2022-05-11","rates":[{"currency":"afgani (Afganistan)","code":"AFN","mid":0.050552},{"currency":"ariary (Madagaskar)","code":"MGA","mid":0.001094},{"currency":"balboa (Panama)","code":"PAB","mid":4.4190},{"currency":"birr etiopski","code":"ETB","mid":0.0853},{"currency":"boliwar soberano (Wenezuela)","code":"VES","mid":0.9619},{"currency":"boliwiano (Boliwia)","code":"BOB","mid":0.6447},{"currency":"colon kostarykański","code":"CRC","mid":0.006621},{"currency":"colon salwadorski","code":"SVC","mid":0.5056},{"currency":"cordoba oro (Nikaragua)","code":"NIO","mid":0.1236},{"currency":"dalasi (Gambia)","code":"GMD","mid":0.0818},{"currency":"denar (Macedonia Północna)","code":"MKD","mid":0.075781},{"currency":"dinar algierski","code":"DZD","mid":0.030354},{"currency":"dinar bahrajski","code":"BHD","mid":11.7396},{"currency":"dinar iracki","code":"IQD","mid":0.003032},{"currency":"dinar jordański","code":"JOD","mid":6.2425},{"currency":"dinar kuwejcki","code":"KWD","mid":14.4247},{"currency":"dinar libijski","code":"LYD","mid":0.9225},{"currency":"dinar serbski","code":"RSD","mid":0.0397},{"currency":"dinar tunezyjski","code":"TND","mid":1.4320},{"currency":"dirham marokański","code":"MAD","mid":0.4411},{"currency":"dirham ZEA (Zjednoczone Emiraty Arabskie)","code":"AED","mid":1.2050},{"currency":"dobra (Wyspy Świętego Tomasza i Książęca)","code":"STN","mid":0.1886},{"currency":"dolar bahamski","code":"BSD","mid":4.4190},{"currency":"dolar barbadoski","code":"BBD","mid":2.1920},{"currency":"dolar belizeński","code":"BZD","mid":2.1957},{"currency":"dolar brunejski","code":"BND","mid":3.1902},{"currency":"dolar Fidżi","code":"FJD","mid":2.0292},{"currency":"dolar gujański","code":"GYD","mid":0.021155},{"currency":"dolar jamajski","code":"JMD","mid":0.0285},{"currency":"dolar liberyjski","code":"LRD","mid":0.0292},{"currency":"dolar namibijski","code":"NAD","mid":0.2755},{"currency":"dolar surinamski","code":"SRD","mid":0.2121},{"currency":"dolar Trynidadu i Tobago","code":"TTD","mid":0.6513},{"currency":"dolar wschodniokaraibski","code":"XCD","mid":1.6329},{"currency":"dolar Wysp Salomona","code":"SBD","mid":0.5507},{"currency":"dolar Zimbabwe","code":"ZWL","mid":0.0278},{"currency":"dong (Wietnam)","code":"VND","mid":0.00019185},{"currency":"dram (Armenia)","code":"AMD","mid":0.009364},{"currency":"escudo Zielonego Przylądka","code":"CVE","mid":0.0422},{"currency":"florin arubański","code":"AWG","mid":2.4450},{"currency":"frank burundyjski","code":"BIF","mid":0.002155},{"currency":"frank CFA BCEAO ","code":"XOF","mid":0.007127},{"currency":"frank CFA BEAC","code":"XAF","mid":0.00701},{"currency":"frank CFP  ","code":"XPF","mid":0.039137},{"currency":"frank Dżibuti","code":"DJF","mid":0.024861},{"currency":"frank gwinejski","code":"GNF","mid":0.0005},{"currency":"frank Komorów","code":"KMF","mid":0.009473},{"currency":"frank kongijski (Dem. Republika Konga)","code":"CDF","mid":0.002213},{"currency":"frank rwandyjski","code":"RWF","mid":0.004343},{"currency":"funt egipski","code":"EGP","mid":0.2392},{"currency":"funt gibraltarski","code":"GIP","mid":5.4677},{"currency":"funt libański","code":"LBP","mid":0.002927},{"currency":"funt południowosudański","code":"SSP","mid":0.010293},{"currency":"funt sudański","code":"SDG","mid":0.0099},{"currency":"funt syryjski","code":"SYP","mid":0.0018},{"currency":"Ghana cedi ","code":"GHS","mid":0.5882},{"currency":"gourde (Haiti)","code":"HTG","mid":0.0399},{"currency":"guarani (Paragwaj)","code":"PYG","mid":0.000645},{"currency":"gulden Antyli Holenderskich","code":"ANG","mid":2.4558},{"currency":"kina (Papua-Nowa Gwinea)","code":"PGK","mid":1.2561},{"currency":"kip (Laos)","code":"LAK","mid":0.000347},{"currency":"kwacha malawijska","code":"MWK","mid":0.00542},{"currency":"kwacha zambijska","code":"ZMW","mid":0.2596},{"currency":"kwanza (Angola)","code":"AOA","mid":0.0109},{"currency":"kyat (Myanmar, Birma)","code":"MMK","mid":0.00239},{"currency":"lari (Gruzja)","code":"GEL","mid":1.4631},{"currency":"lej Mołdawii","code":"MDL","mid":0.2341},{"currency":"lek (Albania)","code":"ALL","mid":0.038739},{"currency":"lempira (Honduras)","code":"HNL","mid":0.1803},{"currency":"leone (Sierra Leone)","code":"SLL","mid":0.000349},{"currency":"lilangeni (Eswatini)","code":"SZL","mid":0.2755},{"currency":"loti (Lesotho)","code":"LSL","mid":0.2755},{"currency":"manat azerbejdżański","code":"AZN","mid":2.6050},{"currency":"metical (Mozambik)","code":"MZN","mid":0.0690},{"currency":"naira (Nigeria)","code":"NGN","mid":0.010682},{"currency":"nakfa (Erytrea)","code":"ERN","mid":0.2935},{"currency":"nowy dolar tajwański","code":"TWD","mid":0.1490},{"currency":"nowy manat (Turkmenistan)","code":"TMT","mid":1.2673},{"currency":"ouguiya (Mauretania)","code":"MRU","mid":0.1214},{"currency":"pa'anga (Tonga)","code":"TOP","mid":1.8984},{"currency":"pataca (Makau)","code":"MOP","mid":0.5474},{"currency":"peso argentyńskie","code":"ARS","mid":0.0378},{"currency":"peso dominikańskie","code":"DOP","mid":0.0803},{"currency":"peso kolumbijskie","code":"COP","mid":0.001085},{"currency":"peso kubańskie","code":"CUP","mid":4.4190},{"currency":"peso urugwajskie","code":"UYU","mid":0.1059},{"currency":"pula (Botswana)","code":"BWP","mid":0.3625},{"currency":"quetzal (Gwatemala)","code":"GTQ","mid":0.5773},{"currency":"rial irański","code":"IRR","mid":0.000105},{"currency":"rial jemeński","code":"YER","mid":0.017688},{"currency":"rial katarski","code":"QAR","mid":1.2118},{"currency":"rial omański","code":"OMR","mid":11.4959},{"currency":"rial saudyjski","code":"SAR","mid":1.1799},{"currency":"riel (Kambodża)","code":"KHR","mid":0.001091},{"currency":"rubel białoruski","code":"BYN","mid":1.2955},{"currency":"rubel rosyjski","code":"RUB","mid":0.0640},{"currency":"rupia lankijska","code":"LKR","mid":0.012126},{"currency":"rupia (Malediwy)","code":"MVR","mid":0.2863},{"currency":"rupia Mauritiusu","code":"MUR","mid":0.1025},{"currency":"rupia nepalska","code":"NPR","mid":0.0358},{"currency":"rupia pakistańska","code":"PKR","mid":0.0232},{"currency":"rupia seszelska","code":"SCR","mid":0.3250},{"currency":"sol (Peru)","code":"PEN","mid":1.1693},{"currency":"som (Kirgistan)","code":"KGS","mid":0.0539},{"currency":"somoni (Tadżykistan)","code":"TJS","mid":0.3543},{"currency":"sum (Uzbekistan)","code":"UZS","mid":0.000397},{"currency":"szyling kenijski","code":"KES","mid":0.038122},{"currency":"szyling somalijski","code":"SOS","mid":0.007651},{"currency":"szyling tanzański","code":"TZS","mid":0.001904},{"currency":"szyling ugandyjski","code":"UGX","mid":0.001231},{"currency":"taka (Bangladesz)","code":"BDT","mid":0.051034},{"currency":"tala (Samoa)","code":"WST","mid":1.6531},{"currency":"tenge (Kazachstan)","code":"KZT","mid":0.009985},{"currency":"tugrik (Mongolia)","code":"MNT","mid":0.001421},{"currency":"vatu (Vanuatu)","code":"VUV","mid":0.038036},{"currency":"wymienialna marka (Bośnia i Hercegowina)","code":"BAM","mid":2.3810}]}]""".trimIndent()

    @BeforeTest
    fun setup() {
        httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullUrl) {
                        testUrlA -> {
                            val responseHeaders =
                                headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                            respond(testResponseStringA, headers = responseHeaders)
                        }
                        testUrlB -> {
                            val responseHeaders =
                                headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                            respond(testResponseStringB, headers = responseHeaders)
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
        val apiConfig = RateApi("NBP", arrayOf(), Currencies["PLN"], testUrl, testUrl, setOf(), 1)
        val fetcher = NbpRateFetcher(apiConfig, httpClient, json)
        val testCurrencies = Currencies.filter { it.code in setOf("UAH", "USD", "EUR", "KZT", "PLN", "BYN") }
        val expectedRates = mapOf(
            testCurrencies.first { it.code == "UAH" } to 1.toFixedScaleBigDecimal() / "0.1467".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "USD" } to 1.toFixedScaleBigDecimal() / "4.4849".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "EUR" } to 1.toFixedScaleBigDecimal() / "4.6679".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "KZT" } to 1.toFixedScaleBigDecimal() / "0.009985".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "PLN" } to "1".toFixedScaleBigDecimal(),
            testCurrencies.first { it.code == "BYN" } to 1.toFixedScaleBigDecimal() / "1.2955".toFixedScaleBigDecimal()
        )
        runBlocking {
            val actualRates = fetcher.fetch(testCurrencies)
            assertEqualsUnordered(expectedRates.entries, actualRates.entries) { it.key }
        }
    }

}