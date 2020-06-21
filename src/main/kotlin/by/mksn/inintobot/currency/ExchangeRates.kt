package by.mksn.inintobot.currency

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.api.fetch.ApiRateFetcher
import io.ktor.client.HttpClient
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.math.BigDecimal
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ExchangeRates(
    private val apis: List<RateApi>,
    private val currencies: List<Currency>
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ExchangeRates::class.simpleName)
        private const val NUM_RETRIES = 3
    }

    private val lastUpdated: AtomicRef<Map<RateApi, ZonedDateTime>> = atomic(mapOf())

    val whenUpdated get() = lastUpdated.value

    private val apiToRates: AtomicRef<Map<RateApi, Map<Currency, BigDecimal>>> = atomic(mapOf())

    suspend fun reloadOne(api: RateApi, httpClient: HttpClient, json: Json) {
        for (i in 1..NUM_RETRIES) {
            logger.info("Reloading exchange rates for ${api.name} (try $i)...")
            val lastUpdated = lastUpdated.value.toMutableMap()
            val apiToRates = apiToRates.value.toMutableMap()
            try {
                val rates = ApiRateFetcher.forApi(api, httpClient, json).fetch(currencies)
                apiToRates[api] = rates
                lastUpdated[api] = ZonedDateTime.now(ZoneOffset.UTC)
                this.lastUpdated.value = lastUpdated
                this.apiToRates.value = apiToRates
                logger.info("Successfully loaded rates for ${api.name}")
                break
            } catch (e: Exception) {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                logger.error("Failed to load rates for ${api.name}: \n$sw")
            }
        }
    }

    suspend fun reloadAll(httpClient: HttpClient, json: Json) {
        logger.info("Reloading exchange rates...")
        for (api in apis) {
            reloadOne(api, httpClient, json)
        }
        logger.info("Exchange rates updated")
    }

    fun of(api: RateApi) = apiToRates.value[api]

    fun isStale(api: RateApi): Boolean {
        val lastUpdated = lastUpdated.value[api]
        return lastUpdated == null ||
                ChronoUnit.HOURS.between(lastUpdated, ZonedDateTime.now(ZoneOffset.UTC)) >= api.refreshHours * 2
    }
}