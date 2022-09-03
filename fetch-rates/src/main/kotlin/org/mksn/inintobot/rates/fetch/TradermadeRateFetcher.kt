package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mksn.inintobot.misc.BigDecimalSerializer
import org.mksn.inintobot.rates.RateApi
import java.math.BigDecimal


@Serializable
data class TradermadeResponse(
    val quotes: List<TradermadeResponseEntry>
)

@Serializable
data class TradermadeResponseEntry(
    @SerialName("quote_currency")
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("mid")
    val rate: BigDecimal
)

class TradermadeRateFetcher(rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<TradermadeResponse>(rateApi, client, json) {
    override val serializer: KSerializer<TradermadeResponse> = TradermadeResponse.serializer()

    override suspend fun parseResponse(response: TradermadeResponse) =
        response.quotes.associateBy({ it.code }, { it.rate })

}