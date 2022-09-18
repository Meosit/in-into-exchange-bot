package org.mksn.inintobot.rates

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.HttpBotFunction
import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.rate.ApiExchangeRates
import org.mksn.inintobot.common.rate.RateApi
import org.mksn.inintobot.common.rate.RateApis
import org.mksn.inintobot.common.store.ApiExchangeRateStore
import org.mksn.inintobot.common.store.StoreProvider
import org.mksn.inintobot.rates.fetch.ApiRateFetcher
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.logging.Logger

private const val numRetries = 3
private val logger: Logger = Logger.getLogger(Function::class.simpleName)
private val store: ApiExchangeRateStore = StoreProvider.load().exchangeRateStore()

@Suppress("unused")
class Function : HttpBotFunction {

    val json = Json { ignoreUnknownKeys = true; isLenient = true }
    val httpClient = HttpClient(Java) {
        install(ContentNegotiation) {
            json(json)
        }
    }
    override suspend fun serve(input: InputStream): Int {
        val jobs = RateApis.map { CoroutineScope(Dispatchers.Default).async { reloadOne(it, store, httpClient, json) } }
        jobs.forEach { it.await() }
        return HttpStatusCode.OK.value
    }

    private suspend fun reloadOne(api: RateApi, store: ApiExchangeRateStore, httpClient: HttpClient, json: Json) {
        val hour = ZonedDateTime.now(ZoneOffset.UTC).hour
        if ((hour + 1) % api.refreshHours == 0) {
            for (i in 1..numRetries) {
                logger.info("${api.name}: Reloading exchange rates (try $i)...")
                try {
                    val rates = ApiRateFetcher.forApi(api, httpClient, json).fetch(Currencies)
                    logger.info("${api.name}: Loaded rates")
                    val oldRates = store.runCatching { getLatest(api.name) }.onFailure {
                        logger.warning("${api.name}: Failed fetch latest rates for api: $it")
                    }.getOrNull()
                    val refreshed = ZonedDateTime.now(ZoneOffset.UTC).withNano(0)
                    if (oldRates != null && oldRates.rates == rates) {
                        logger.info("${api.name}: Rates were not updated since last check on ${oldRates.date} ${oldRates.time}")
                    } else {
                        val date = refreshed.toLocalDate()
                        val time = refreshed.toLocalTime()
                        store.save(ApiExchangeRates(time, date, api, rates))
                        logger.info("${api.name}: Stored rates for $date $time ")
                    }
                    break
                } catch (e: Exception) {
                    val sw = StringWriter()
                    e.printStackTrace(PrintWriter(sw))
                    logger.severe("${api.name}: Failed to load or store new rates: \n$sw")
                }
            }
        } else {
            logger.info("${api.name}: Skipping load as current hour $hour not aligned with refresh rate ${api.refreshHours}")
        }
    }
}