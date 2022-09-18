package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.misc.BigDecimalSerializer
import org.mksn.inintobot.common.rate.RateApi
import java.math.BigDecimal

/**
 * Standard currency rate API response which have nested object with `"code": rate` pairs
 */
@Serializable
data class RatesMapResponse(
    val rates: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>
)

class RatesMapRateFetcher(rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<RatesMapResponse>(rateApi, client, json) {
    override val serializer: KSerializer<RatesMapResponse> = RatesMapResponse.serializer()
    override suspend fun parseResponse(response: RatesMapResponse): Map<String, BigDecimal> = response.rates
}