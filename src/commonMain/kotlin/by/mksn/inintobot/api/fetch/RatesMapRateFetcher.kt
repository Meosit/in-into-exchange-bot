package by.mksn.inintobot.api.fetch

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.misc.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Standard currency rate API response which have nested object with `"code": rate` pairs
 */
@Serializable
@ExperimentalUnsignedTypes
data class RatesMapResponse(
    val rates: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>
)

@ExperimentalUnsignedTypes
class RatesMapRateFetcher(rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<RatesMapResponse>(rateApi, client, json) {
    override val serializer: KSerializer<RatesMapResponse> = RatesMapResponse.serializer()
    override suspend fun parseResponse(response: RatesMapResponse): Map<String, BigDecimal> = response.rates
}