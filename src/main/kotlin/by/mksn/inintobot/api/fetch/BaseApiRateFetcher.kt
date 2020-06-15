package by.mksn.inintobot.api.fetch

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.misc.toFixedScaleBigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.math.BigDecimal

abstract class BaseApiRateFetcher<T>(
    private val rateApi: RateApi,
    private val client: HttpClient,
    private val json: Json
) : ApiRateFetcher {

    protected abstract val serializer: KSerializer<T>

    protected abstract suspend fun parseResponse(response: T): Map<String, BigDecimal>

    protected open fun prepareResponseString(response: String): String = response

    override suspend fun fetch(supported: List<Currency>): Map<Currency, BigDecimal> {
        val response = client.get<String>(rateApi.url)
        val preparedResponse = prepareResponseString(response)
        val parsed = json.parse(serializer, preparedResponse)
        val codeToRate = parseResponse(parsed)
        return supported.asSequence()
            .map { it to (if (it.code == rateApi.base) 1.toFixedScaleBigDecimal() else codeToRate[it.code]) }
            .filter { it.second != null }
            .map { it.first to it.second!! }
            .toMap()
    }

}
