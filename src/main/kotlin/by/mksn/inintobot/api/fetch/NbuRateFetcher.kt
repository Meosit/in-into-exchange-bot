package by.mksn.inintobot.api.fetch

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.misc.BigDecimalSerializer
import by.mksn.inintobot.misc.toFixedScaleBigDecimal
import io.ktor.client.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.math.BigDecimal

@Serializable
data class NbuResponseEntry(
    @SerialName("cc")
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("rate")
    val rate: BigDecimal
)

class NbuRateFetcher(rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<List<NbuResponseEntry>>(rateApi, client, json) {
    override val serializer: KSerializer<List<NbuResponseEntry>> = ListSerializer(NbuResponseEntry.serializer())

    override suspend fun parseResponse(response: List<NbuResponseEntry>) =
        response.asSequence().associateBy({ it.code }, { 1.toFixedScaleBigDecimal() / it.rate })

}