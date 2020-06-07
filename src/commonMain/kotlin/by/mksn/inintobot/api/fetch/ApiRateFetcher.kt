package by.mksn.inintobot.api.fetch

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.misc.toFiniteBigDecimal
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

@ExperimentalUnsignedTypes
abstract class ApiRateFetcher<T>(private val json: Json) {

    protected abstract val serializer: KSerializer<T>

    protected abstract suspend fun parseResponse(response: T): Map<String, BigDecimal>

    suspend fun fetch(client: HttpClient, supported: List<Currency>, rateApi: RateApi): Map<Currency, BigDecimal> {
        val response = client.get<String>(rateApi.url)
        val parsed = json.parse(serializer, response)
        val codeToRate = parseResponse(parsed)
        return supported.asSequence()
            .map { it to (if (it.code == rateApi.base) 1.toFiniteBigDecimal() else codeToRate[it.code]) }
            .filter { it.second != null }
            .map { it.first to it.second!! }
            .toMap()
    }

}
