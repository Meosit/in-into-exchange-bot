package by.mksn.inintobot

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.currency.ExchangeRates
import by.mksn.inintobot.misc.Localized
import by.mksn.inintobot.misc.TimeUnitNames
import by.mksn.inintobot.output.strings.CommandMessages
import by.mksn.inintobot.output.strings.ErrorMessages
import by.mksn.inintobot.output.strings.QueryStrings
import by.mksn.inintobot.output.strings.SettingsStrings
import by.mksn.inintobot.settings.UserStore
import com.vladsch.kotlin.jdbc.HikariCP
import com.vladsch.kotlin.jdbc.SessionImpl
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.net.URI


@Serializable
private data class BasicInfoEntity(
    val creatorId: String,
    val maxOutputLength: Int,
    val maxErrorLineLength: Int,
    val supportedLanguages: Map<String, String>
)

private data class AppContextEntity(
    val basicInfo: BasicInfoEntity,
    val json: Json,
    val httpClient: HttpClient,
    val supportedApis: List<RateApi>,
    val supportedCurrencies: List<Currency>,
    val exchangeRates: ExchangeRates,
    val queryStrings: Localized<QueryStrings>,
    val apiNames: Localized<Map<String, String>>,
    val timeUnitNames: Localized<TimeUnitNames>,
    val errorMessages: Localized<ErrorMessages>,
    val commandMessages: Localized<CommandMessages>,
    val settingsStrings: Localized<SettingsStrings>
)

/**
 * Various application system info
 */
object AppContext {

    private val logger = LoggerFactory.getLogger(AppContext.javaClass.simpleName)

    private lateinit var context: AppContextEntity

    private fun loadResourceAsString(resourceBaseName: String): String = AppContext::class.java.classLoader
        .getResourceAsStream(resourceBaseName)
        .let { it ?: throw IllegalStateException("Null resource stream for $resourceBaseName") }
        .use { InputStreamReader(it).use(InputStreamReader::readText) }

    private fun initializeDataSource(dbUrl: String) {
        val dbUri = URI(dbUrl)
        val (username: String, password: String) = dbUri.userInfo.split(":")
        val jdbcUrl = "jdbc:postgresql://${dbUri.host}:${dbUri.port}${dbUri.path}?sslmode=require"
        HikariCP.default(jdbcUrl, username, password)
        SessionImpl.defaultDataSource = { HikariCP.dataSource() }
        logger.info("JDBC url: $jdbcUrl")
        UserStore.initializeStore()
        logger.info("Initialized UserStore")
    }

    private fun <T> Json.load(resourceBaseName: String, deserializer: DeserializationStrategy<T>): T =
        this.decodeFromString(deserializer, loadResourceAsString(resourceBaseName))

    fun initialize(dbUrl: String, apiAccessKeys: Map<String, String>) {
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val httpClient = HttpClient {
            install(JsonFeature) {
                serializer = KotlinxSerializer(json)
            }
        }

        initializeDataSource(dbUrl)

        val basicInfo = json.load("core/basic-info.json", BasicInfoEntity.serializer())
        val supportedCurrencies = json.load("core/currencies.json", ListSerializer(Currency.serializer()))
        val supportedApis = json.decodeFromString(ListSerializer(RateApi.serializer()), loadResourceAsString("core/apis.json")
            .let {
                var replaced = it
                apiAccessKeys.forEach { (key, token) -> replaced = replaced.replace(key, token) }
                replaced
            })

        val outputStrings =
            Localized(basicInfo.supportedLanguages.keys) { language ->
                json.load("message/$language/query.json", QueryStrings.serializer())
            }
        val errorMessages =
            Localized(basicInfo.supportedLanguages.keys) { language ->
                json.load("message/$language/errors.json", ErrorMessages.serializer())
            }
        val commandMessages =
            Localized(basicInfo.supportedLanguages.keys) { language ->
                CommandMessages(
                    start = loadResourceAsString("message/$language/start.md"),
                    help = loadResourceAsString("message/$language/help.md"),
                    api = loadResourceAsString("message/$language/api.md"),
                    apis = loadResourceAsString("message/$language/apis.md"),
                    apiStatus = loadResourceAsString("message/$language/api-status.md"),
                    patterns = loadResourceAsString("message/$language/patterns.md")
                )
            }
        val apiNames =
            Localized(basicInfo.supportedLanguages.keys) { language ->
                json.load("message/$language/api-names.json", MapSerializer(String.serializer(), String.serializer()))
            }

        val timeUnitNames =
            Localized(basicInfo.supportedLanguages.keys) { language ->
                json.load("message/$language/time-unit-names.json", TimeUnitNames.serializer())
            }

        val settingsStrings =
            Localized(basicInfo.supportedLanguages.keys) { language ->
                json.load("message/$language/settings.json", SettingsStrings.serializer())
            }

        val exchangeRates = ExchangeRates(supportedApis, supportedCurrencies)

        context = AppContextEntity(
            basicInfo, json, httpClient, supportedApis, supportedCurrencies, exchangeRates,
            outputStrings, apiNames, timeUnitNames, errorMessages, commandMessages, settingsStrings
        )
        logger.info("AppContext initialized")
    }

    val creatorId get() = context.basicInfo.creatorId
    val maxOutputLength get() = context.basicInfo.maxOutputLength
    val maxErrorLineLength get() = context.basicInfo.maxErrorLineLength

    val json get() = context.json
    val httpClient get() = context.httpClient

    val supportedLanguages get() = context.basicInfo.supportedLanguages
    val supportedApis get() = context.supportedApis
    val supportedCurrencies get() = context.supportedCurrencies
    val exchangeRates get() = context.exchangeRates

    val queryStrings get() = context.queryStrings
    val apiDisplayNames get() = context.apiNames
    val timeUnitNames get() = context.timeUnitNames
    val errorMessages get() = context.errorMessages
    val commandMessages get() = context.commandMessages
    val settingsStrings get() = context.settingsStrings

}