package by.mksn.inintobot.currency

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.api.fetch.ApiRateFetcher
import io.ktor.client.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.math.BigDecimal
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicReference

data class ApiStatus(
    val api: RateApi,
    val ratesUpdated: ZonedDateTime,
    val lastChecked: ZonedDateTime
)

class ExchangeRates(
    private val apis: List<RateApi>,
    private val currencies: List<Currency>
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ExchangeRates::class.simpleName)
        private const val NUM_RETRIES = 3
    }

    private val apiStatuses: AtomicReference<Map<RateApi, ApiStatus>> = AtomicReference(mapOf())

    val ratesStatus get() = apiStatuses.get()

    private val apiToRates: AtomicReference<Map<RateApi, Map<Currency, BigDecimal>>> = AtomicReference(mapOf())

    suspend fun reloadOne(api: RateApi, httpClient: HttpClient, json: Json) {
        for (i in 1..NUM_RETRIES) {
            logger.info("Reloading exchange rates for ${api.name} (try $i)...")
            val apiStatuses = apiStatuses.get().toMutableMap()
            val apiToRates = apiToRates.get().toMutableMap()
            try {
                val oldRates = apiToRates[api]
                val rates = ApiRateFetcher.forApi(api, httpClient, json).fetch(currencies)
                apiToRates[api] = rates
                val refreshed = ZonedDateTime.now(ZoneOffset.UTC)
                apiStatuses.merge(api, ApiStatus(api, refreshed, refreshed)) { old, new ->
                    if (rates == oldRates) {
                        logger.info("Rates were not updated since last check")
                        new.copy(ratesUpdated = old.ratesUpdated)
                    } else {
                        logger.info("New rates were loaded!")
                        new
                    }
                }
                this.apiStatuses.getAndSet(apiStatuses)
                this.apiToRates.getAndSet(apiToRates)
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

    fun of(api: RateApi) = apiToRates.get()[api]

    fun isStale(api: RateApi): Boolean {
        val status = apiStatuses.get()[api]
        return status == null || ChronoUnit.HOURS
            .between(status.lastChecked, ZonedDateTime.now(ZoneOffset.UTC)) >= api.refreshHours * 2
    }
}