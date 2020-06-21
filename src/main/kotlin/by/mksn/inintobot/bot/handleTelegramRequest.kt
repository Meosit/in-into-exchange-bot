package by.mksn.inintobot.bot

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.bot.settings.Setting
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.BotTextOutput
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.settings.UserStore
import by.mksn.inintobot.telegram.Chat
import by.mksn.inintobot.telegram.Update
import by.mksn.inintobot.telegram.User
import io.ktor.client.features.ResponseException
import io.ktor.client.statement.readText
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter


private val logger = LoggerFactory.getLogger("handleTelegramRequest")

/**
 * Handles the telegram bot [Update] for the specific [botToken]
 */
suspend fun handleTelegramRequest(update: Update, botToken: String, deprecatedBot: Boolean) {
    val sender = BotOutputSender(AppContext.httpClient, botToken)
    try {
        val settings = loadSettings(update)
        with(update) {
            logger.info("User {}", userReadableName())
            logger.info("Settings: $settings")
            when {
                inlineQuery != null -> inlineQuery.handle(settings, sender, deprecatedBot)
                message != null -> message.handle(settings, sender, deprecatedBot)
                editedMessage != null -> editedMessage.handle(settings, sender, deprecatedBot)
                callbackQuery != null -> Setting.handle(callbackQuery, settings, sender)
            }
        }
    } catch (e: Exception) {
        val cause = (e as? ResponseException)?.response?.readText() ?: "${e::class.simpleName} ${e.message}"
        val queryString = (update.message ?: update.editedMessage)?.text ?: update.inlineQuery?.query
        val user = update.userReadableName()
        logger.info("Error for query '$queryString': $cause")
        if ("query is too old" !in cause) {
            val message = BotTextOutput("Error received.\n```\nQuery: $queryString\nUser: $user\n\nCause: $cause```")
            sender.sendChatMessage(AppContext.creatorId, message)
        }
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        logger.error(sw.toString())
    }
    return
}

fun loadSettings(update: Update) = with(update) {
    val chat = message?.chat ?: editedMessage?.chat ?: callbackQuery?.message?.chat
    val user = inlineQuery?.from ?: message?.from ?: editedMessage?.from ?: callbackQuery?.from
    val query = inlineQuery?.query
        ?: message?.text
        ?: editedMessage?.text
        ?: callbackQuery?.let { "callback payload '${it.data}'" }
        ?: "null chat message"
    val userId = chat?.id ?: user?.id
    val botUser = userId?.let { UserStore.refreshAndGet(it, userReadableName(), query, inlineQuery != null) }
    logger.info("Load user $botUser")
    if (botUser?.settings != null) {
        botUser.settings
    } else {
        val inferredLanguage = user?.languageCode?.take(2)?.toLowerCase()
            ?.takeIf { it in AppContext.supportedLanguages }
        inferredLanguage?.let { UserSettings(language = it) } ?: UserSettings()
    }
}

private fun Update.userReadableName() = inlineQuery?.from?.userReadableName()
    ?: (message ?: editedMessage ?: callbackQuery?.message)?.chat?.userReadableName()
    ?: "Unknown username"

private fun Chat.userReadableName() =
    username ?: "$firstName ${lastName ?: ""} ($id)"

private fun User.userReadableName() =
    username ?: "$firstName ${lastName ?: ""} ($id)"