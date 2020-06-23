package by.mksn.inintobot.bot

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.bot.settings.Setting
import by.mksn.inintobot.output.BotDeprecatedOutput
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.BotTextOutput
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.Message
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter


private val logger = LoggerFactory.getLogger("handleMessage")
private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

suspend fun Message.handle(settings: UserSettings, sender: BotOutputSender, deprecatedBot: Boolean) {
    if (chat.id.toString() == AppContext.creatorId) {
        val handled = handleAdminCommand(sender)
        if (handled) return
    }
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
                    "/start" -> start
                    else -> help
                }
            }
            val formattedMessage = if (deprecatedBot) BotDeprecatedOutput(BotTextOutput(message), settings.language)
            else BotTextOutput(message)
            sender.sendChatMessage(chat.id.toString(), formattedMessage)
        }
        "/apistatus" -> {
            val statusFormat = AppContext.commandMessages.of(settings.language).apiStatus
            val apiDisplayNames = AppContext.apiDisplayNames.of(settings.language)
            logger.info("Getting api status")
            val message = AppContext.exchangeRates.ratesStatus.values.joinToString(separator = "\n\n") {
                statusFormat.format(
                    apiDisplayNames.getValue(it.api.name),
                    dateFormat.format(it.ratesUpdated),
                    dateFormat.format(it.lastChecked)
                )
            }
            val formattedMessage = if (deprecatedBot) BotDeprecatedOutput(BotTextOutput(message), settings.language)
            else BotTextOutput(message)
            sender.sendChatMessage(chat.id.toString(), formattedMessage)
        }
        "/settings" -> {
            logger.info("Handling settings command")
            Setting.ROOT.handle(null, this, settings, sender)
        }
        else -> {
            logger.info("Handling '$text' chat message")
            val outputs = handleBotExchangeQuery(text, settings)
            outputs.firstOrNull()?.let {
                val formattedOutput = if (deprecatedBot) BotDeprecatedOutput(it, settings.language) else it
                sender.sendChatMessage(chat.id.toString(), formattedOutput)
            }
        }
    }
}