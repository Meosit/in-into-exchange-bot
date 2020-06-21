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
data class ForexResponse(
    val response: List<ForexResponseEntry>
)

@Serializable
data class ForexResponseEntry(
    @SerialName("symbol")
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("price")
    val rate: BigDecimal
)

class ForexRateFetcher(private val rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<ForexResponse>(rateApi, client, json) {
    override val serializer: KSerializer<ForexResponse> = ForexResponse.serializer()

    override suspend fun parseResponse(response: ForexResponse) =
        response.response.asSequence().associateBy({ it.code.replace("${rateApi.base}/", "") }, { it.rate })

}