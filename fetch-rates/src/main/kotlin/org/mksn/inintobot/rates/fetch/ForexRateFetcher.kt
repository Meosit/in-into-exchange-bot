package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.misc.BigDecimalSerializer
import org.mksn.inintobot.common.rate.RateApi
import java.math.BigDecimal
import java.time.LocalDate


@Serializable
data class ForexResponse(
    val response: List<ForexResponseEntry>
)

@Serializable
data class ForexResponseEntry(
    @SerialName("s")
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("c")
    val rate: BigDecimal
)

class ForexRateFetcher(private val rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<ForexResponse>(rateApi, client, json) {
    override val serializer: KSerializer<ForexResponse> = ForexResponse.serializer()

    override suspend fun parseResponse(response: ForexResponse, date: LocalDate?) =
        response.response.associateBy({ it.code.replace("${rateApi.base.code}/", "") }, { it.rate })

}