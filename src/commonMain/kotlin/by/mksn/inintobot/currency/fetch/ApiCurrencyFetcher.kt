package by.mksn.inintobot.currency.fetch

import by.mksn.inintobot.config.ApiConfig
import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.util.toFiniteBigDecimal
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.serialization.KSerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@UnstableDefault
@ExperimentalUnsignedTypes
abstract class ApiCurrencyFetcher<T> {

    private val json = Json(JsonConfiguration(ignoreUnknownKeys = true))

    protected abstract val serializer: KSerializer<T>

    protected abstract suspend fun parseResponse(response: T): Map<String, BigDecimal>

    suspend fun fetch(client: HttpClient, supported: List<Currency>, apiConfig: ApiConfig): Map<Currency, BigDecimal> {
        val response = client.get<String>(apiConfig.url)
        val parsed = json.parse(serializer, response)
        val codeToRate = parseResponse(parsed)
        return supported.asSequence()
            .map { it to (if (it.code == apiConfig.baseCode) 1.toFiniteBigDecimal() else codeToRate[it.code]) }
            .filter { it.second != null }
            .map { it.first to it.second!! }
            .toMap()
    }

}
