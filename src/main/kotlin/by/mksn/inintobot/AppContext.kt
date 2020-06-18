package by.mksn.inintobot

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.misc.Localized
import by.mksn.inintobot.output.strings.CommandMessages
import by.mksn.inintobot.output.strings.ErrorMessages
import by.mksn.inintobot.output.strings.QueryStrings
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.InputStreamReader


@Serializable
private data class BasicInfoEntity(
    val creatorId: String,
    val maxOutputLength: Int,
    val maxErrorLineLength: Int,
    val supportedLocales: Set<String>
)

private data class AppContextEntity(
    val basicInfo: BasicInfoEntity,
    val json: Json,
    val httpClient: HttpClient,
    val supportedApis: List<RateApi>,
    val supportedCurrencies: List<Currency>,
    val queryStrings: Localized<QueryStrings>,
    val apiNames: Localized<Map<String, String>>,
    val errorMessages: Localized<ErrorMessages>,
    val commandMessages: Localized<CommandMessages>
)

/**
 * Various application system info
 */
object AppContext {

    private lateinit var context: AppContextEntity

    private fun loadResourceAsString(resourceBaseName: String): String = AppContext::class.java.classLoader
        .getResourceAsStream(resourceBaseName)
        .let { it ?: throw IllegalStateException("Null resource stream for $resourceBaseName") }
        .use { InputStreamReader(it).use(InputStreamReader::readText) }

    private fun <T> Json.load(resourceBaseName: String, deserializer: DeserializationStrategy<T>): T =
        this.parse(deserializer, loadResourceAsString(resourceBaseName))

    fun initialize(apiAccessKeys: Map<String, String>) {
        val json = Json(JsonConfiguration.Stable.copy(ignoreUnknownKeys = true))
        val httpClient = HttpClient {
            install(JsonFeature) {
                serializer = KotlinxSerializer(json)
            }
        }

        val basicInfo = json.load("core/basic-info.json", BasicInfoEntity.serializer())
        val supportedCurrencies = json.load("core/currencies.json", Currency.serializer().list)
        val supportedApis = json.parse(RateApi.serializer().list, loadResourceAsString("core/apis.json")
            .let {
                var replaced = it
                apiAccessKeys.forEach { (key, token) -> replaced = replaced.replace(key, token) }
                replaced
            })

        val outputStrings =
            Localized(basicInfo.supportedLocales) { language ->
                json.load("message/$language/query.json", QueryStrings.serializer())
            }
        val errorMessages =
            Localized(basicInfo.supportedLocales) { language ->
                json.load("message/$language/errors.json", ErrorMessages.serializer())
            }
        val commandMessages =
            Localized(basicInfo.supportedLocales) { language ->
                CommandMessages(
                    loadResourceAsString("message/$language/help.md"),
                    loadResourceAsString("message/$language/patterns.md"),
                    loadResourceAsString("message/$language/apis.md")
                )
            }
        val apiNames =
            Localized(basicInfo.supportedLocales) { language ->
                json.load("message/$language/api-names.json", MapSerializer(String.serializer(), String.serializer()))
            }

        context = AppContextEntity(
            basicInfo, json, httpClient, supportedApis, supportedCurrencies,
            outputStrings, apiNames, errorMessages, commandMessages
        )
    }

    val creatorId get() = context.basicInfo.creatorId
    val maxOutputLength get() = context.basicInfo.maxOutputLength
    val maxErrorLineLength get() = context.basicInfo.maxErrorLineLength

    val json get() = context.json
    val httpClient get() = context.httpClient

    val supportedLanguages get() = context.basicInfo.supportedLocales
    val supportedApis get() = context.supportedApis
    val supportedCurrencies get() = context.supportedCurrencies

    val queryStrings get() = context.queryStrings
    val apiNames get() = context.apiNames
    val errorMessages get() = context.errorMessages
    val commandMessages get() = context.commandMessages

}