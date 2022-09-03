package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mksn.inintobot.misc.BigDecimalSerializer
import org.mksn.inintobot.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.rates.RateApi
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
        response.associateBy({ it.code }, { 1.toFixedScaleBigDecimal() / it.rate })

}