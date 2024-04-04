package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.misc.BigDecimalSerializer
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.rate.RateApi
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class NbrbMyfinResponseEntry(
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    val scale: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val rate: BigDecimal
)

class NbrbMyfinRateFetcher(rateApi: RateApi, client: HttpClient, private val json: Json) :
    BaseApiRateFetcher<List<NbrbMyfinResponseEntry>>(rateApi, client, json) {

    private val options = setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    private val currencyItem = """<td>([\d.]+)\s*?(<sup class=.*?</sup>)?\s*?</td>\s*?<td>\s*?<span class="flag.*?">\s*?</span>\s*?([A-Z]{3})</td>\s*?<td>\s*?(\d+)\s*?</td>""".toRegex(options)

    override fun prepareResponseString(response: String): String {
        val entries = currencyItem.findAll(response)
            .map {
                NbrbMyfinResponseEntry(
                    it.groupValues[3],
                    it.groupValues[4].toFixedScaleBigDecimal(),
                    it.groupValues[1].toFixedScaleBigDecimal()
                )
            }
            .toList()
        if (entries.isEmpty())
            throw IllegalArgumentException("No rate entires found for NBRB")
        return json.encodeToString(ListSerializer(NbrbMyfinResponseEntry.serializer()), entries)
    }

    override val serializer: KSerializer<List<NbrbMyfinResponseEntry>> = ListSerializer(NbrbMyfinResponseEntry.serializer())

    override suspend fun parseResponse(response: List<NbrbMyfinResponseEntry>, date: LocalDate?) =
        response.associateBy({ it.code }, { it.scale / it.rate })

}