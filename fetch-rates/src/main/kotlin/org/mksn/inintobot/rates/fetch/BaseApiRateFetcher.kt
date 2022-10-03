package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.rate.RateApi
import java.math.BigDecimal
import java.time.LocalDate
import java.util.logging.Logger

private val logger: Logger = Logger.getLogger(BaseApiRateFetcher::class.simpleName)

abstract class BaseApiRateFetcher<T>(
    private val rateApi: RateApi,
    private val client: HttpClient,
    private val json: Json
) : ApiRateFetcher {

    protected abstract val serializer: KSerializer<T>

    protected abstract suspend fun parseResponse(response: T, date: LocalDate?): Map<String, BigDecimal>

    protected open fun prepareResponseString(response: String): String = response

    override suspend fun fetch(supported: Iterable<Currency>, date: LocalDate?): Map<Currency, BigDecimal> {
        val url = date?.let { rateApi.backFillInfo!!.url.replace("<date>", rateApi.backFillInfo!!.dateFormat.format(date)) } ?: rateApi.url
        val response = client.get(url).body<String>()
        val preparedResponse = prepareResponseString(response)
        val parsed = runCatching { json.decodeFromString(serializer, preparedResponse) }
            .onFailure { logger.severe("${rateApi.name} Failed to parse response from url $url:\n$preparedResponse") }
            .getOrThrow()
        val codeToRate = parseResponse(parsed, date)
        return supported.asSequence()
            .map { it to (if (it == rateApi.base) 1.toFixedScaleBigDecimal() else codeToRate[it.code]) }
            .filter { it.second != null }
            .map { it.first to it.second!! }
            .toMap()
    }

}
