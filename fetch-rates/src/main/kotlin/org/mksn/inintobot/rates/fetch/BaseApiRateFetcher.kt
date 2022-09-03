package org.mksn.inintobot.rates.fetch

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.mksn.inintobot.currency.Currency
import org.mksn.inintobot.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.rates.RateApi
import java.math.BigDecimal

abstract class BaseApiRateFetcher<T>(
    private val rateApi: RateApi,
    private val client: HttpClient,
    private val json: Json
) : ApiRateFetcher {

    protected abstract val serializer: KSerializer<T>

    protected abstract suspend fun parseResponse(response: T): Map<String, BigDecimal>

    protected open fun prepareResponseString(response: String): String = response

    override suspend fun fetch(supported: Iterable<Currency>): Map<Currency, BigDecimal> {
        val response = client.get(rateApi.url).body<String>()
        val preparedResponse = prepareResponseString(response)
        val parsed = json.decodeFromString(serializer, preparedResponse)
        val codeToRate = parseResponse(parsed)
        return supported.asSequence()
            .map { it to (if (it == rateApi.base) 1.toFixedScaleBigDecimal() else codeToRate[it.code]) }
            .filter { it.second != null }
            .map { it.first to it.second!! }
            .toMap()
    }

}
