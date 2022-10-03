package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import org.mksn.inintobot.common.misc.BigDecimalSerializer
import org.mksn.inintobot.common.rate.RateApi
import java.math.BigDecimal
import java.time.LocalDate


@Serializable
data class TradermadeResponse(
    val quotes: List<TradermadeResponseEntry>
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class TradermadeResponseEntry(
    @SerialName("quote_currency")
    val code: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("mid") @JsonNames("close", "mid")
    val rate: BigDecimal? = null
)

class TradermadeRateFetcher(rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<TradermadeResponse>(rateApi, client, json) {
    override val serializer: KSerializer<TradermadeResponse> = TradermadeResponse.serializer()

    override suspend fun parseResponse(response: TradermadeResponse, date: LocalDate?) =
        response.quotes.mapNotNull { it.code?.let { c -> it.rate?.let { r -> c to r } } }.toMap()

}