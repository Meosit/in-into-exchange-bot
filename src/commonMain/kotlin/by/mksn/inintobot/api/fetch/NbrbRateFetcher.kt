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
data class NbrbReponseEntry(
    @SerialName("Cur_Abbreviation")
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("Cur_Scale")
    val scale: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("Cur_OfficialRate")
    val rate: BigDecimal
)

@ExperimentalUnsignedTypes
class NbrbRateFetcher(rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<List<NbrbReponseEntry>>(rateApi, client, json) {
    override val serializer: KSerializer<List<NbrbReponseEntry>> = NbrbReponseEntry.serializer().list

    override suspend fun parseResponse(response: List<NbrbReponseEntry>) =
        response.asSequence().associateBy({ it.code }, { it.rate / it.scale })

}