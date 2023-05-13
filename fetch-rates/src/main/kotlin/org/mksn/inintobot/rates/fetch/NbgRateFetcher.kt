package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.misc.BigDecimalSerializer
import org.mksn.inintobot.common.rate.RateApi
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class NbgResponseEntry(
    val currencies: List<NbgRateEntry>
)

@Serializable
data class NbgRateEntry(
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val rate: BigDecimal
)

class NbgRateFetcher(rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<List<NbgResponseEntry>>(rateApi, client, json) {
    override val serializer: KSerializer<List<NbgResponseEntry>> = ListSerializer(NbgResponseEntry.serializer())

    override suspend fun parseResponse(response: List<NbgResponseEntry>, date: LocalDate?) =
        response.first().currencies.associateBy({ it.code }, { it.quantity / it.rate })

}