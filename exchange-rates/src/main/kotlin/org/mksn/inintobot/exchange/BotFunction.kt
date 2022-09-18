package org.mksn.inintobot.exchange

import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.mksn.inintobot.common.HttpBotFunction
import org.mksn.inintobot.exchange.bot.handleTelegramRequest
import org.mksn.inintobot.exchange.telegram.Update
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util.logging.Logger

private val logger: Logger = Logger.getLogger(BotFunction::class.simpleName)

@Suppress("unused")
class BotFunction : HttpBotFunction {

    private val context = BotContext(
        botToken = System.getenv("BOT_TOKEN"),
        botUsername = System.getenv("BOT_USERNAME"),
        creatorId = System.getenv("CREATOR_ID"),
    )

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