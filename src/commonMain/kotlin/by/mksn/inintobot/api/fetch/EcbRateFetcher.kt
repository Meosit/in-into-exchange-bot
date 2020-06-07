package by.mksn.inintobot.api.fetch

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.misc.BigDecimalSerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json

@Serializable
@ExperimentalUnsignedTypes
data class EcbReponseEntry(
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    val rate: BigDecimal
)

@ExperimentalUnsignedTypes
class EcbRateFetcher(rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<List<EcbReponseEntry>>(rateApi, client, json) {

    private val options = setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    private val xmlHeaderRegex = "<\\?xml.+<Cube\\s+time='.*?'>".toRegex(options)
    private val xmlFooterRegex = "</Cube>.+</gesmes:Envelope>".toRegex(options)
    private val rateItemRegex = "<Cube currency='(\\w{3})' rate='([\\d.]+)'/>".toRegex(options)
    private val trailingCommaRegex = "}\\s*,\\s*]".toRegex(options)

    override fun prepareResponseString(response: String): String {
        var result = response.replace("\n", " ")
        result = result.replace(xmlHeaderRegex, "[")
        result = result.replace(xmlFooterRegex, "]")
        result = result.replace(rateItemRegex) { match ->
            "{\"code\": \"${match.groupValues[1]}\", \"rate\": ${match.groupValues[2]} },"
        }
        return result.replace(trailingCommaRegex, "}]")
    }

    override val serializer: KSerializer<List<EcbReponseEntry>> = EcbReponseEntry.serializer().list

    override suspend fun parseResponse(response: List<EcbReponseEntry>) =
        response.asSequence().associateBy({ it.code }, { it.rate })

}