package by.mksn.inintobot.api.fetch

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.misc.BigDecimalSerializer
import io.ktor.client.HttpClient
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
        response.quotes.asSequence().associateBy({ it.code }, { it.rate })

}