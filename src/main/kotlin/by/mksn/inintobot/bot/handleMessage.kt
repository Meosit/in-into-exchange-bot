package by.mksn.inintobot.bot

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.misc.trimToLength
import by.mksn.inintobot.output.BotDeprecatedOutput
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.BotTextOutput
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.settings.UserStore
import by.mksn.inintobot.telegram.Message
import org.slf4j.LoggerFactory
import java.time.ZoneId
import java.time.ZoneOffset


private val logger = LoggerFactory.getLogger("handleMessage")

suspend fun Message.handle(settings: UserSettings, botToken: String, deprecatedBot: Boolean) {
    val sender = BotOutputSender(AppContext.httpClient, botToken)
    if (chat.id.toString() == AppContext.creatorId) {
        handleAdminCommand(sender)
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
                    else -> help
                }
            }
            val formattedMessage = if (deprecatedBot) BotDeprecatedOutput(BotTextOutput(message), settings.language)
            else BotTextOutput(message)
            sender.sendChatMessage(chat.id.toString(), formattedMessage)
        }
        else -> {
            logger.info("Handling '$text' chat message")
            val outputs = handleBotQuery(text, settings)
            outputs.firstOrNull()?.let {
                val formattedOutput = if (deprecatedBot) BotDeprecatedOutput(it, settings.language) else it
                sender.sendChatMessage(chat.id.toString(), formattedOutput)
            }
        }
    }
}

suspend fun Message.handleAdminCommand(sender: BotOutputSender) {
    when (text) {
        "/reload" -> {
            AppContext.exchangeRates.reload(AppContext.httpClient, AppContext.json)
            val markdown = AppContext.exchangeRates.whenUpdated.asSequence()
                .map { (api, updated) -> "${api.name}: ${updated.withZoneSameInstant(ZoneId.of("UTC+3"))}" }
                .joinToString(separator = "\n", prefix = "Last updated:\n```\n", postfix = "\n```")
            sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
        }
        "/last" -> {
            val users = UserStore.lastUsed(5)
            val markdown = users
                .joinToString(separator = "\n---\n", prefix = "Last 5 users:\n```\n", postfix = "\n```") { user ->
                    """
                        User: ${user.name}
                        When: ${user.lastUsed.toLocalDateTime().atZone(ZoneOffset.UTC)
                        .withZoneSameInstant(ZoneId.of("UTC+3"))}
                        Query: ${user.lastQuery.trimToLength(25, "…")}
                    """.trimIndent()
                }
            sender.sendChatMessage(AppContext.creatorId, BotTextOutput(markdown))
        }
    }
}