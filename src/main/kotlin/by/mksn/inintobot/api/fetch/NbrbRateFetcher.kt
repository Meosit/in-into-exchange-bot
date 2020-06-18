package by.mksn.inintobot.api.fetch

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.misc.BigDecimalSerializer
import io.ktor.client.HttpClient
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import java.math.BigDecimal

@Serializable
data class NbrbResponseEntry(
    @SerialName("Cur_Abbreviation")
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("Cur_Scale")
    val scale: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("Cur_OfficialRate")
    val rate: BigDecimal
)

class NbrbRateFetcher(rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<List<NbrbResponseEntry>>(rateApi, client, json) {
    override val serializer: KSerializer<List<NbrbResponseEntry>> = NbrbResponseEntry.serializer().list

    override suspend fun parseResponse(response: List<NbrbResponseEntry>) =
        response.asSequence().associateBy({ it.code }, { it.scale / it.rate })

}