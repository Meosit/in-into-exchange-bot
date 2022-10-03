package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.misc.BigDecimalSerializer
import org.mksn.inintobot.common.rate.RateApi
import org.mksn.inintobot.rates.LocalDateSerializer
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class EcbResponseEntry(
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    val rate: BigDecimal
)

class EcbRateFetcher(rateApi: RateApi, client: HttpClient, json: Json) :
    BaseApiRateFetcher<Map<LocalDate, List<EcbResponseEntry>>>(rateApi, client, json) {

    private val options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    private val xmlHeaderRegex = "<\\?xml.+<Cube>".toRegex(options)
    private val xmlEntryRegex = "<Cube\\s+time=[\"'](\\d{4}-\\d{2}-\\d{2})[\"']>".toRegex(options)
    private val xmlEntryEndRegex = "</Cube>".toRegex(options)
    private val xmlFooterRegex = "\\s*</Cube>\\s*</gesmes:Envelope>".toRegex(options)
    private val rateItemRegex = "<Cube currency=[\"'](\\w{3})[\"'] rate=[\"']([\\d.]+)[\"']/>".toRegex(options)
    private val trailingCommaArrayRegex = "}\\s*,\\s*]".toRegex(options)
    private val trailingCommaObjectRegex = "]\\s*,\\s*}".toRegex(options)

    override fun prepareResponseString(response: String): String {
        var result = response
        result = result.replace(xmlHeaderRegex, "{")
        result = result.replace(xmlFooterRegex, "}")
        result = result.replace(xmlEntryRegex) { match -> "\"${match.groupValues[1]}\": ["}
        result = result.replace(xmlEntryEndRegex, "],")
        result = result.replace(rateItemRegex) { match ->
            "{\"code\": \"${match.groupValues[1]}\", \"rate\": ${match.groupValues[2]} },"
        }
        return result.replace(trailingCommaArrayRegex, "}]").replace(trailingCommaObjectRegex, "]}")
    }

    override val serializer: KSerializer<Map<LocalDate, List<EcbResponseEntry>>> =
        MapSerializer(LocalDateSerializer, ListSerializer(EcbResponseEntry.serializer()))

    override suspend fun parseResponse(response: Map<LocalDate, List<EcbResponseEntry>>, date: LocalDate?) =
        response.mapValues { (_, value) -> value.associateBy({ it.code }, { it.rate }) }
            .toSortedMap(Comparator.reverseOrder())
            .firstNotNullOf { (key, value) -> if (date == null) value else date.takeIf { key <= it }?.let { value } }

}