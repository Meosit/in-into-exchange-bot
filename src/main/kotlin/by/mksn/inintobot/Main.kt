package by.mksn.inintobot

import by.mksn.inintobot.bot.handleBotExchangeQuery
import by.mksn.inintobot.bot.handleTelegramRequest
import by.mksn.inintobot.output.ApiErrorResponse
import by.mksn.inintobot.output.ApiExchangeRequest
import by.mksn.inintobot.output.ApiResponse
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.Update
import io.ktor.application.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.netty.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("MainKt")
private val SELF_PING_DELAY = TimeUnit.MINUTES.toMillis(15)
private val RELOAD_RATES_DELAY = TimeUnit.HOURS.toMillis(1)

fun Application.main() {
    val appUrl: String = System.getenv("APP_URL")
    val dbUrl: String = System.getenv("DATABASE_URL")
    val allowedTokens = System.getenv("ALLOWED_TOKENS_STRING")?.split(",") ?: listOf()
    val deprecatedTokens = System.getenv("DEPRECATED_TOKENS_STRING")?.split(",") ?: listOf()
    val apiAccessKeys: Map<String, String> = mapOf(
        "<fixer_access_key>" to System.getenv("FIXER_ACCESS_KEY"),
        "<openexchangerates_access_key>" to System.getenv("OPENEXCHANGERATES_ACCESS_KEY"),
        "<tradermade_access_key>" to System.getenv("TRADERMADE_ACCESS_KEY"),
        "<forex_access_key>" to System.getenv("FOREX_ACCESS_KEY")
    )

    logger.info("app url: $appUrl")
    logger.info("DB url: $dbUrl")
    logger.info("tokens: $allowedTokens")
    logger.info("deprecated: $deprecatedTokens")
    logger.info("access keys: ${apiAccessKeys.map { (k, v) -> "$k: $v" }}")

    AppContext.initialize(dbUrl, apiAccessKeys)

    install(ContentNegotiation) {
        json(AppContext.json, ContentType.Application.Json)
    }
    install(DefaultHeaders)
    install(CallLogging)
    install(Routing) {
        for (token in allowedTokens) {
            post("/handle/$token") {
                try {
                    val update = call.receive<Update>()
                    handleTelegramRequest(this, update, token, token in deprecatedTokens)
                } catch (e: Exception) {
                    val sw = StringWriter()
                    e.printStackTrace(PrintWriter(sw))
                    (e as? ClientRequestException)?.response?.content?.let {
                        logger.error(it.readUTF8Line())
                    }
                    logger.error("Uncaught exception: $sw")
                }
                call.respond(HttpStatusCode.OK)
            }
            logger.info("Added bot route: $token")
        }
        get("/self-ping") {
            call.respondText("What are you looking here?", ContentType.Text.Html)
        }
        post("/exchange") {
            try {
                val (query, settings) = call.receive<ApiExchangeRequest>()
                when {
                    !AppContext.supportedApis.any { settings.apiName == it.name } ->
                        call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("Invalid api ${settings.apiName}"))
                    !AppContext.supportedLanguages.keys.any { settings.language == it } ->
                        call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("Invalid default currency ${settings.defaultCurrency}"))
                    !AppContext.supportedCurrencies.any { settings.defaultCurrency == it.code } ->
                        call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("Invalid default currency ${settings.defaultCurrency}"))
                    settings.outputCurrencies.any { it !in AppContext.supportedCurrencies.map { c -> c.code } } ->
                        call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("Invalid output currencies currency ${settings.outputCurrencies.joinToString()}"))
                    settings.dashboardCurrencies.any { it !in AppContext.supportedCurrencies.map { c -> c.code } } ->
                        call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("Invalid dashboard currencies currency ${settings.outputCurrencies.joinToString()}"))
                    query.isBlank() ->
                        call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("No query found"))
                    else -> {
                        val output = handleBotExchangeQuery(query, settings).first().toApiResponse(settings)
                        val response = AppContext.json.encodeToString(ApiResponse.serializer(), output)
                        call.respondText(response, ContentType.Application.Json, status = output.code)
                    }
                }
            } catch (e: SerializationException) {
                call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("Invalid request format"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiErrorResponse("Unknown error"))
            }
        }
        get("/settings/currencies") {
            val response = AppContext.json.encodeToString(ListSerializer(String.serializer()), AppContext.supportedCurrencies.map { it.code })
            call.respondText(response, ContentType.Application.Json)
        }
        get("/settings/languages") {
            val response = AppContext.json.encodeToString(MapSerializer(String.serializer(), String.serializer()), AppContext.supportedLanguages)
            call.respondText(response, ContentType.Application.Json)
        }
        get("/settings/apis") {
            val settings = call.receiveOrNull() ?: UserSettings()
            val response = AppContext.json.encodeToString(
                MapSerializer(String.serializer(), String.serializer()), AppContext.apiDisplayNames.of(settings.language))
            call.respondText(response, ContentType.Application.Json)
        }
        get("/messages/help") {
            val settings = call.receiveOrNull() ?: UserSettings()
            call.respondText(AppContext.commandMessages.of(settings.language).help, ContentType.Text.Plain)
        }
        get("/messages/apis") {
            val settings = call.receiveOrNull() ?: UserSettings()
            call.respondText(AppContext.commandMessages.of(settings.language).apis, ContentType.Text.Plain)
        }
        get("/messages/patterns") {
            val settings = call.receiveOrNull() ?: UserSettings()
            call.respondText(AppContext.commandMessages.of(settings.language).patterns, ContentType.Text.Plain)
        }
    }

    launch {
        while (isActive) {
            delay(SELF_PING_DELAY)
            if (System.getenv("USE_PING") == "true") {
                logger.info("Starting self-ping...")
                val response = AppContext.httpClient.get<String>("${appUrl.removeSuffix("/")}/self-ping")
                logger.info("Finished self-ping with response: '$response'")
            } else {
                logger.info("Self-ping skipped")
            }
        }
    }

    launch {
        var hourCounter = 0
        AppContext.exchangeRates.reloadAll(AppContext.httpClient, AppContext.json)
        val millisTillNewHourStart = TimeUnit.MINUTES.toMillis(60L - LocalDateTime.now().minute)
        delay(millisTillNewHourStart)
        while (isActive) {
            delay(RELOAD_RATES_DELAY)
            hourCounter++
            for (api in AppContext.supportedApis) {
                if (hourCounter % api.refreshHours == 0) {
                    AppContext.exchangeRates.reloadOne(api, AppContext.httpClient, AppContext.json)
                }
            }
            logger.info("Exchange rates updated")
        }
    }
}

fun main(args: Array<String>) = EngineMain.main(args)