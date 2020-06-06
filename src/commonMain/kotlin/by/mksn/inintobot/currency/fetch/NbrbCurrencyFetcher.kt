package by.mksn.inintobot.currency.fetch

import by.mksn.inintobot.util.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.builtins.list

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

@UnstableDefault
@ExperimentalUnsignedTypes
class NbrbCurrencyFetcher : ApiCurrencyFetcher<List<NbrbReponseEntry>>() {
    override val serializer: KSerializer<List<NbrbReponseEntry>> = NbrbReponseEntry.serializer().list

    override suspend fun parseResponse(response: List<NbrbReponseEntry>) =
        response.asSequence().associateBy({ it.code }, { it.rate / it.scale })

}