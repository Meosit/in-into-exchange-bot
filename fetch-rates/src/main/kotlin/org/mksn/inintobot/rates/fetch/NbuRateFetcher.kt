package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.misc.BigDecimalSerializer
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.rate.RateApi
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class NbuResponseEntry(
    @SerialName("CurrencyCodeL")
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("Units")
    val units: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("Amount")
    val rate: BigDecimal
)

class NbuRateFetcher(rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<List<NbuResponseEntry>>(rateApi, client, json) {
    override val serializer: KSerializer<List<NbuResponseEntry>> = ListSerializer(NbuResponseEntry.serializer())

    override suspend fun parseResponse(response: List<NbuResponseEntry>, date: LocalDate?) =
        response.associateBy({ it.code }, { 1.toFixedScaleBigDecimal() / (it.rate / it.units) })

}