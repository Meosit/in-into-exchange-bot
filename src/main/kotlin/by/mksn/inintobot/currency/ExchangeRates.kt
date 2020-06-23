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

    private val apiStatuses: AtomicRef<Map<RateApi, ApiStatus>> = atomic(mapOf())

    val ratesStatus = apiStatuses.value

    private val apiToRates: AtomicRef<Map<RateApi, Map<Currency, BigDecimal>>> = atomic(mapOf())

    suspend fun reloadOne(api: RateApi, httpClient: HttpClient, json: Json) {
        for (i in 1..NUM_RETRIES) {
            logger.info("Reloading exchange rates for ${api.name} (try $i)...")
            val apiStatuses = apiStatuses.value.toMutableMap()
            val apiToRates = apiToRates.value.toMutableMap()
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
                this.apiStatuses.value = apiStatuses
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
        val status = apiStatuses.value[api]
        return status == null || ChronoUnit.HOURS
            .between(status.lastChecked, ZonedDateTime.now(ZoneOffset.UTC)) >= api.refreshHours * 2
    }
}