package by.mksn.inintobot.bot

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.BotTextOutput
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.Message
import org.slf4j.LoggerFactory


private val logger = LoggerFactory.getLogger("handleMessage")

suspend fun Message.handle(settings: UserSettings, botToken: String) {
    val sender = BotOutputSender(AppContext.httpClient, botToken)
    when (text) {
        "", null -> {
            logger.info("'$text' message text received")
            val errorMessages = AppContext.errorMessages.of(settings.language)
            sender.sendChatMessage(chat.id.toString(), BotTextOutput(errorMessages.queryExpected))
        }
        "/start", "/help", "/patterns", "/apis" -> {
            logger.info("Handling bot command $text")
            val message = with(AppContext.commandMessages.of(settings.language)) {
                when (text) {
                    "/patterns" -> patterns
                    "/apis" -> apis
                    else -> help
                }
            }
            sender.sendChatMessage(chat.id.toString(), BotTextOutput(message))
        }
        else -> {
            logger.info("Handling '$text' chat message")
            val outputs = handleBotQuery(text, settings)
            outputs.firstOrNull()?.let { sender.sendChatMessage(chat.id.toString(), it) }
        }
    }
}