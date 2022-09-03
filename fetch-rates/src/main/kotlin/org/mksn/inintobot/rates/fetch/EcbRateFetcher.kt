package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mksn.inintobot.misc.BigDecimalSerializer
import org.mksn.inintobot.rates.RateApi
import java.math.BigDecimal

@Serializable
data class EcbResponseEntry(
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    val rate: BigDecimal
)

class EcbRateFetcher(rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<List<EcbResponseEntry>>(rateApi, client, json) {

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

    override val serializer: KSerializer<List<EcbResponseEntry>> = ListSerializer(EcbResponseEntry.serializer())

    override suspend fun parseResponse(response: List<EcbResponseEntry>) =
        response.associateBy({ it.code }, { it.rate })

}