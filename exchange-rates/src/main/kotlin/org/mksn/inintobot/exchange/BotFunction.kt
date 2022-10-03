package org.mksn.inintobot.exchange

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.mksn.inintobot.common.HttpBotFunction
import org.mksn.inintobot.common.store.StoreProvider
import org.mksn.inintobot.exchange.bot.handleTelegramRequest
import org.mksn.inintobot.exchange.telegram.Update
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util.logging.Logger

private val logger: Logger = Logger.getLogger(BotFunction::class.simpleName)

@Suppress("unused")
class BotFunction(
    storeProvider: StoreProvider = StoreProvider.load(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
    httpClient: HttpClient = HttpClient(Java) {
        install(ContentNegotiation) {
            json(json)
        }
    }
) : HttpBotFunction {

    private val context = BotContext(
        botToken = System.getenv("BOT_TOKEN"),
        botUsername = System.getenv("BOT_USERNAME"),
        creatorId = System.getenv("CREATOR_ID"),
        storeProvider = storeProvider,
        json = json,
        httpClient = httpClient
    )

    val botToken = context.botToken
    val botSender = context.sender
    val creatorId = context.creatorId

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun serve(input: InputStream): Int {
        try {
            val update = context.json.decodeFromStream<Update>(input)
            handleTelegramRequest(update, context)
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            (e as? ClientRequestException)?.response?.bodyAsText()?.let { logger.severe(it) }
            logger.severe("Uncaught exception: $sw")
        }
        return HttpStatusCode.OK.value
    }
}