package by.mksn.inintobot

import by.mksn.inintobot.bot.handleTelegramRequest
import by.mksn.inintobot.misc.BigDecimalSerializer
import by.mksn.inintobot.telegram.Update
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.serialization.json
import io.ktor.server.netty.EngineMain
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("MainKt")
private val SELF_PING_DELAY = TimeUnit.MINUTES.toMillis(15)
private val RELOAD_RATES_DELAY = TimeUnit.MINUTES.toMillis(60)

fun Application.main() {
    val appUrl: String = System.getenv("APP_URL")
    val dbUrl: String = System.getenv("DATABASE_URL")
    val adminKey: String = System.getenv("ADMIN_KEY")
    val allowedTokens = System.getenv("ALLOWED_TOKENS_STRING")?.split(",") ?: listOf()
    val deprecatedTokens = System.getenv("DEPRECATED_TOKENS_STRING")?.split(",") ?: listOf()
    val apiAccessKeys: Map<String, String> = mapOf(
        "<fixer_access_key>" to System.getenv("FIXER_ACCESS_KEY"),
        "<openexchangerates_access_key>" to System.getenv("OPENEXCHANGERATES_ACCESS_KEY")
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
                    handleTelegramRequest(update, token, token in deprecatedTokens)
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
        get("/") {
            call.request
            call.respondText("What are you looking here?", ContentType.Text.Html)
        }
        get("/$adminKey/manual-reload") {
            AppContext.exchangeRates.reload(AppContext.httpClient, AppContext.json)
            val rates = AppContext.supportedApis.asSequence()
                .map {
                    it.name to (AppContext.exchangeRates.of(it) ?: mapOf()).entries.asSequence()
                        .map { it.key.code to it.value }.toMap()
                }.toMap()
            call.respondText(
                AppContext.json.stringify(
                    MapSerializer(String.serializer(), MapSerializer(String.serializer(), BigDecimalSerializer)), rates
                ), ContentType.Application.Json
            )
        }
    }

    launch {
        while (isActive) {
            delay(SELF_PING_DELAY)
            if (System.getenv("USE_PING") == "true") {
                logger.info("Starting self-ping...")
                val response = AppContext.httpClient.get<String>(appUrl)
                logger.info("Finished self-ping with response: '$response'")
            } else {
                logger.info("Self-ping skipped")
            }
        }
    }

    launch {
        AppContext.exchangeRates.reload(AppContext.httpClient, AppContext.json)
        while (isActive) {
            delay(RELOAD_RATES_DELAY)
            AppContext.exchangeRates.reload(AppContext.httpClient, AppContext.json)
        }
    }
}

fun main(args: Array<String>) = EngineMain.main(args)