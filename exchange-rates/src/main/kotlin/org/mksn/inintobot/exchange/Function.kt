package org.mksn.inintobot.exchange

import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.mksn.inintobot.exchange.bot.handleTelegramRequest
import org.mksn.inintobot.exchange.telegram.Update
import java.io.PrintWriter
import java.io.StringWriter
import java.util.logging.Logger

private val logger: Logger = Logger.getLogger(Function::class.simpleName)

@Suppress("unused")
class Function : HttpFunction {

    private val context = BotContext(
        botToken = System.getenv("BOT_TOKEN"),
        botUsername = System.getenv("BOT_USERNAME"),
        creatorId = System.getenv("CREATOR_ID"),
    )

    @OptIn(ExperimentalSerializationApi::class)
    override fun service(request: HttpRequest, response: HttpResponse) = runBlocking {
        try {
            val update = context.json.decodeFromStream<Update>(request.inputStream)
            handleTelegramRequest(update, context)
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            (e as? ClientRequestException)?.response?.bodyAsText()?.let { logger.severe(it) }
            logger.severe("Uncaught exception: $sw")
        }
    }
}