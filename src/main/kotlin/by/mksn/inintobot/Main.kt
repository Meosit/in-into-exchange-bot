package by.mksn.inintobot

import by.mksn.inintobot.bot.handleTelegramRequest
import by.mksn.inintobot.telegram.Update
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.features.ClientRequestException
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
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.utils.io.readUTF8Line
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter

private val logger = LoggerFactory.getLogger("MainKt")

fun Application.main() {
    val allowedTokens = System.getenv("ALLOWED_TOKENS_STRING")?.split(",") ?: listOf()
    val apiAccessKeys: Map<String, String> = mapOf(
        "<fixer_access_key>" to System.getenv("FIXER_ACCESS_KEY"),
        "<openexchangerates_access_key>" to System.getenv("OPENEXCHANGERATES_ACCESS_KEY")
    )

    AppContext.initialize(apiAccessKeys)

    install(ContentNegotiation) {
        json(AppContext.json, ContentType.Application.Json)
    }
    install(DefaultHeaders)
    install(CallLogging)
    install(Routing) {
        for (token in allowedTokens) {
            get("/handle/$token") {
                try {
                    val update = call.receive<Update>()
                    handleTelegramRequest(update, token)
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
        }
        get("/") {
            call.request
            call.respondText("What are you looking here?", ContentType.Text.Html)
        }
    }
}

fun main() {
    embeddedServer(Netty, module = Application::main).start()
}