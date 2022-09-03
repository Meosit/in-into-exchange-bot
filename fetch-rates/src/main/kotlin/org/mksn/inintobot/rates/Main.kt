package org.mksn.inintobot.rates

import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.mksn.inintobot.currency.Currencies
import org.mksn.inintobot.rates.fetch.ApiRateFetcher
import org.mksn.inintobot.rates.store.ApiExchangeRateStore
import org.mksn.inintobot.rates.store.FirestoreApiExchangeRateStore
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZoneOffset
import java.time.ZonedDateTime


@Suppress("unused")
class Function : HttpFunction {
    private val numRetries = 3
    private val logger = LoggerFactory.getLogger(Function::class.java)

    override fun service(request: HttpRequest, response: HttpResponse) = runBlocking {
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val httpClient = HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val store: ApiExchangeRateStore = FirestoreApiExchangeRateStore()
        val jobs = RateApis.AVAILABLE.values.map { async(Dispatchers.Default) { reloadOne(it, store, httpClient, json) } }
        jobs.forEach { it.await() }
        response.setStatusCode(HttpStatusCode.OK.value)
        store.close()
    }

    private suspend fun reloadOne(api: RateApi, store: ApiExchangeRateStore, httpClient: HttpClient, json: Json) {
        for (i in 1..numRetries) {
            logger.info("Reloading exchange rates for ${api.name} (try $i)...")
            try {
                val rates = ApiRateFetcher.forApi(api, httpClient, json).fetch(Currencies.ALL.values.toList())
                logger.info("Loaded rates for ${api.name}")
                val oldRates = store.runCatching { getLatest(api.name) }.onFailure {
                    logger.warn("Failed to load latest rates for api ${api.name}: $it")
                }.getOrNull()
                val refreshed = ZonedDateTime.now(ZoneOffset.UTC)
                if (oldRates != null && oldRates.rates == rates) {
                    logger.info("Rates were not updated since last check on ${oldRates.date} ${oldRates.time}")
                } else {
                    val date = refreshed.toLocalDate()
                    val time = refreshed.toLocalTime()
                    store.save(ApiExchangeRates(time, date, api, rates))
                    logger.info("Stored rates for ${api.name} for $date $time ")
                }
                break
            } catch (e: Exception) {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                logger.error("Failed to load or store new rates for ${api.name}: \n$sw")
            }
        }
    }
}