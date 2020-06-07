package by.mksn.inintobot.api.fetch

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.currency.Currency
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

@ExperimentalUnsignedTypes
interface ApiRateFetcher {

    suspend fun fetch(supported: List<Currency>): Map<Currency, BigDecimal>

    companion object {
        fun <T> forApi(api: RateApi, client: HttpClient, json: Json): ApiRateFetcher {
            return when (api.name) {
                "NBRB" -> NbrbRateFetcher(api, client, json)
                "NBU" -> NbuRateFetcher(api, client, json)
                "CBR" -> CbrRateFetcher(api, client, json)
                "Fixer", "OpenExchangeRates" -> RatesMapRateFetcher(api, client, json)
                else -> throw IllegalArgumentException("No mapping for rate api '${api.name}'")
            }
        }
    }

}