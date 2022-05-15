package by.mksn.inintobot.api.fetch

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.misc.BigDecimalSerializer
import by.mksn.inintobot.misc.toFixedScaleBigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.math.BigDecimal


@Serializable
data class NbpResponse(
    val rates: List<NbpResponseEntry>
)

@Serializable
data class NbpResponseEntry(
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("mid")
    val rate: BigDecimal
)

class NbpRateFetcher(private val rateApi: RateApi, private val client: HttpClient, private val json: Json) : ApiRateFetcher {

    private val tables = listOf("A", "B")

    private val serializer: KSerializer<List<NbpResponse>> = ListSerializer(NbpResponse.serializer())

    override suspend fun fetch(supported: List<Currency>): Map<Currency, BigDecimal> {
        val codeToRate = tables
            .map { client.get<String>(rateApi.url.replace("<table_type>", it)) }
            .map { json.decodeFromString(serializer, it) }
            .flatten().flatMap { it.rates }
            .associateBy({ it.code }, { 1.toFixedScaleBigDecimal() / it.rate })
        return supported.asSequence()
            .map { it to (if (it.code == rateApi.base) 1.toFixedScaleBigDecimal() else codeToRate[it.code]) }
            .filter { it.second != null }
            .map { it.first to it.second!! }
            .toMap()
    }
}