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
data class CbrResponseEntry(
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    val scale: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val rate: BigDecimal
)

class CbrRateFetcher(rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<List<CbrResponseEntry>>(rateApi, client, json) {

    private val options = setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    private val arrayHeader = "<ValCurs.*?>".toRegex(options)
    private val arrayFooter = "</ValCurs>".toRegex(options)
    private val arrayItemHeader = "<Valute.*?>".toRegex(options)
    private val arrayItemFooter = "</Valute>".toRegex(options)
    private val unusedElementRegex =
        "<\\?.*?\\?>|<Name>.*?</Name>|<NumCode>.*?</NumCode>|<ParentCode>.*?</ParentCode>".toRegex(options)
    private val scaleRegex = "<Nominal>(\\d+)</Nominal>".toRegex(options)
    private val codeRegex = "<CharCode>(\\w{3})</CharCode>".toRegex(options)
    private val rateRegex = "<Value>([\\d,]+)</Value>".toRegex(options)
    private val trailingCommaRegex = "}\\s*,\\s*]".toRegex(options)

    override fun prepareResponseString(response: String): String {
        var result = response.replace("\n", " ")
        result = result.replace(unusedElementRegex, "")
        result = result.replace(arrayHeader, "[")
        result = result.replace(arrayFooter, "]")
        result = result.replace(arrayItemHeader, "{")
        result = result.replace(arrayItemFooter, "},")
        result = result.replace(scaleRegex) { match -> "\"scale\": ${match.groupValues[1]}" }
        result = result.replace(codeRegex) { match -> "\"code\": \"${match.groupValues[1]}\"" }
        result = result.replace(rateRegex) { match -> "\"rate\": ${match.groupValues[1].replace(",", ".")}" }
        return result.replace(trailingCommaRegex, "}]")
    }

    override val serializer: KSerializer<List<CbrResponseEntry>> = ListSerializer(CbrResponseEntry.serializer())

    override suspend fun parseResponse(response: List<CbrResponseEntry>) =
        response.associateBy({ it.code }, { it.scale / it.rate })

}