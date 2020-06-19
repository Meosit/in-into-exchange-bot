package by.mksn.inintobot.currency

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.api.fetch.ApiRateFetcher
import io.ktor.client.HttpClient
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.math.BigDecimal
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ExchangeRates(
    private val apis: List<RateApi>,
    private val currencies: List<Currency>
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ExchangeRates::class.simpleName)
    }

    private val lastUpdated: AtomicRef<Map<RateApi, ZonedDateTime>> = atomic(mapOf())

    val whenUpdated get() = lastUpdated.value

    private val apiToRates: AtomicRef<Map<RateApi, Map<Currency, BigDecimal>>> = atomic(mapOf())

    suspend fun reload(httpClient: HttpClient, json: Json) {
        logger.info("Reloading exchange rates...")
        val lastUpdated = lastUpdated.value.toMutableMap()
        val apiToRates = apiToRates.value.toMutableMap()
        for (api in apis) {
            delay(300)
            val rates = try {
                ApiRateFetcher.forApi(api, httpClient, json).fetch(currencies)
            } catch (e: Exception) {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                logger.error("Failed to load rates for ${api.name}: \n$sw")
                continue
            }
            apiToRates[api] = rates
            lastUpdated[api] = ZonedDateTime.now(ZoneOffset.UTC)
            logger.info("Loaded for ${api.name}")
        }
        this.lastUpdated.value = lastUpdated
        this.apiToRates.value = apiToRates
        logger.info("Exchange rates updated")
    }

    fun of(api: RateApi) = apiToRates.value[api]

}