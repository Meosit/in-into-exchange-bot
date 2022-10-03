package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.misc.BigDecimalSerializer
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.rate.RateApi
import java.math.BigDecimal
import java.time.LocalDate
import java.util.logging.Logger


private val logger: Logger = Logger.getLogger(NbpRateFetcher::class.simpleName)

@Serializable
data class NbpResponse(
    val rates: List<NbpResponseEntry>
)

@Serializable
data class NbpResponseEntry(
    val code: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("mid")
    val rate: BigDecimal
)

class NbpRateFetcher(
    private val rateApi: RateApi,
    private val client: HttpClient,
    private val json: Json
) : ApiRateFetcher {

    private val tables = listOf("A", "B")

    private val serializer: KSerializer<List<NbpResponse>> = ListSerializer(NbpResponse.serializer())

    override suspend fun fetch(supported: Iterable<Currency>, date: LocalDate?): Map<Currency, BigDecimal> {
        val codeToRate = tables
            .map { table ->
                if (date != null) {
                    generateSequence(date) { it.minusDays(1) }.take(10).firstNotNullOfOrNull { d ->
                        val url = rateApi.backFillInfo!!.url.replace("<date>", rateApi.backFillInfo!!.dateFormat.format(d))
                        logger.info("${rateApi.name}: Trying to load NBP table $table for $d")
                        runCatching {
                            val response = client.get(url.replace("<table_type>", table)).bodyAsText()
                            json.decodeFromString(serializer, response)
                        }.onFailure {
                            logger.info("${rateApi.name}: Failed to load date $date for table $table:\n${it.stackTraceToString()}")
                        }.getOrNull()
                    } ?: throw Exception("Failed to load date $date with look back of 10 days for table $table")
                } else {
                    val response = client.get(rateApi.url.replace("<table_type>", table)).bodyAsText()
                    json.decodeFromString(serializer, response)
                }
            }
            .flatten().flatMap { it.rates }
            .associateBy({ it.code }, { 1.toFixedScaleBigDecimal() / it.rate })
        return supported.asSequence()
            .map { it to (if (it == rateApi.base) 1.toFixedScaleBigDecimal() else codeToRate[it.code]) }
            .filter { it.second != null }
            .map { it.first to it.second!! }
            .toMap()
    }
}