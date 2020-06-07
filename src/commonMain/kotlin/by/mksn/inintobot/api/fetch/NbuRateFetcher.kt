package by.mksn.inintobot.api.fetch

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.misc.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json

@Serializable
@ExperimentalUnsignedTypes
data class NbuReponseEntry(
    @SerialName("cc")
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("rate")
    val rate: BigDecimal
)

@ExperimentalUnsignedTypes
class NbuRateFetcher(rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<List<NbuReponseEntry>>(rateApi, client, json) {
    override val serializer: KSerializer<List<NbuReponseEntry>> = NbuReponseEntry.serializer().list

    override suspend fun parseResponse(response: List<NbuReponseEntry>) =
        response.asSequence().associateBy({ it.code }, { it.rate })

}