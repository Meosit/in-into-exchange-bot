package by.mksn.inintobot.api.fetch

import by.mksn.inintobot.misc.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
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
class NbuRateFetcher(json: Json) : ApiRateFetcher<List<NbuReponseEntry>>(json) {
    override val serializer: KSerializer<List<NbuReponseEntry>> = NbuReponseEntry.serializer().list

    override suspend fun parseResponse(response: List<NbuReponseEntry>) =
        response.asSequence().associateBy({ it.code }, { it.rate })

}