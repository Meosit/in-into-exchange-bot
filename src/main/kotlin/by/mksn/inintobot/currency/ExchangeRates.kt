package by.mksn.inintobot.currency

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.api.fetch.ApiRateFetcher
import io.ktor.client.HttpClient
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.math.BigDecimal

class ExchangeRates(
    private val apis: List<RateApi>,
    private val currencies: List<Currency>
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ExchangeRates::class.simpleName)
    }

    private val apiToRates: AtomicRef<Map<RateApi, Map<Currency, BigDecimal>>> = atomic(mapOf())

    suspend fun reload(httpClient: HttpClient, json: Json) {
        logger.info("Reloading exchange rates...")
        val apiToRates: MutableMap<RateApi, Map<Currency, BigDecimal>> = mutableMapOf()
        for (api in apis) {
            val rates = ApiRateFetcher.forApi(api, httpClient, json)
                .fetch(currencies)
            apiToRates[api] = rates
            logger.info("Loaded for ${api.name}")
        }
        this.apiToRates.value = apiToRates
        logger.info("Exchange rates updated")
    }

    fun of(api: RateApi) = apiToRates.value.getValue(api)

}