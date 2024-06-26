package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.rate.RateApi
import java.math.BigDecimal
import java.time.LocalDate

interface ApiRateFetcher {

    suspend fun fetch(supported: Iterable<Currency>, date: LocalDate? = null): Map<Currency, BigDecimal>

    companion object {
        fun forApi(api: RateApi, client: HttpClient, json: Json): ApiRateFetcher {
            return when (api.name) {
                "NBRB" -> NbrbRateFetcher(api, client, json)
                "NBU" -> NbuRateFetcher(api, client, json)
                "NBP" -> NbpRateFetcher(api, client, json)
                "NBG" -> NbgRateFetcher(api, client, json)
                "CBR" -> CbrRateFetcher(api, client, json)
                "ECB" -> EcbRateFetcher(api, client, json)
                "Fixer", "OpenExchangeRates" -> RatesMapRateFetcher(api, client, json)
                "TraderMade" -> TradermadeRateFetcher(api, client, json)
                "Forex" -> ForexRateFetcher(api, client, json)
                else -> throw IllegalArgumentException("No mapping for rate api '${api.name}'")
            }
        }
    }

}