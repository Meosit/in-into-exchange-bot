package by.mksn.inintobot.api.fetch

import by.mksn.inintobot.misc.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

/**
 * Standard currency rate API response which have nested object with `"code": rate` pairs
 */
@Serializable
@ExperimentalUnsignedTypes
data class RatesMapResponse(
    val rates: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>
)

@UnstableDefault
@ExperimentalUnsignedTypes
class RatesMapRateFetcher(json: Json) : ApiRateFetcher<RatesMapResponse>(json) {
    override val serializer: KSerializer<RatesMapResponse> = RatesMapResponse.serializer()
    override suspend fun parseResponse(response: RatesMapResponse): Map<String, BigDecimal> = response.rates
}