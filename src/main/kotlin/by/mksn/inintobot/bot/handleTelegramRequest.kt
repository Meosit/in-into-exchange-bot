package by.mksn.inintobot.bot

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.BotTextOutput
import by.mksn.inintobot.settings.UserSettings
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
suspend fun handleTelegramRequest(update: Update, botToken: String) {
    try {
        with(update) {
            val chat = message?.chat ?: editedMessage?.chat
            val user = inlineQuery?.from ?: message?.from ?: editedMessage?.from
            val settings = loadSettings(chat, user)
            logger.info("User {}", user?.userReadableName() ?: chat?.userReadableName())
            when {
                inlineQuery != null -> inlineQuery.handle(settings, botToken)
                message != null -> message.handle(settings, botToken)
                editedMessage != null -> editedMessage.handle(settings, botToken)
            }
        }
    } catch (e: Exception) {
        val cause = (e as? ResponseException)?.response?.readText() ?: e.message
        ?: "No exception message supplied (${e::class.simpleName})"
        val queryString = (update.message ?: update.editedMessage)?.text ?: update.inlineQuery?.query
        val user = update.inlineQuery?.from?.userReadableName()
            ?: (update.message ?: update.editedMessage)?.chat?.userReadableName()
        logger.info("Error for query '$queryString': $cause")
        if ("query is too old" !in cause) {
            val sender = BotOutputSender(AppContext.httpClient, botToken)
            val message = BotTextOutput("Error received.\n```\nQuery: $queryString\nUser: $user\n\nCause: $cause```")
            sender.sendChatMessage(AppContext.creatorId, message)
        }
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        logger.error(sw.toString())
    }
    return
}

fun loadSettings(chat: Chat?, user: User?): UserSettings {
    val userId = chat?.id ?: user?.id
    val inferredLanguage = user?.languageCode?.take(2)?.toLowerCase()
        .takeIf { AppContext.supportedLanguages.contains(it) }
    return if (userId != null) {
        // TODO load stored user settings
        inferredLanguage?.let { UserSettings(language = it) } ?: UserSettings()
    } else {
        UserSettings()
    }
}

private fun Chat.userReadableName() =
    with(this) { username ?: "$firstName ${lastName ?: ""} ($id)" }

private fun User.userReadableName() =
    with(this) { username ?: "$firstName ${lastName ?: ""} ($id)" }