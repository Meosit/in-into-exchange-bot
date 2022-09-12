package org.mksn.inintobot.exchange.bot

import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import org.mksn.inintobot.exchange.bot.settings.Setting
import org.mksn.inintobot.exchange.httpClient
import org.mksn.inintobot.exchange.output.BotOutputSender
import org.mksn.inintobot.exchange.output.BotTextOutput
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.settings.UserSettings
import org.mksn.inintobot.exchange.telegram.Update
import org.mksn.inintobot.exchange.userSettingsStore
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter


private val logger = LoggerFactory.getLogger("handleTelegramRequest")

/**
 * Handles the telegram bot [Update] for the specific [botToken]
 */
suspend fun handleTelegramRequest(
    creatorId: String,
    update: Update,
    botToken: String,
) {
    val sender = BotOutputSender(httpClient, botToken)
    try {
        val settings = loadSettings(update)
        with(update) {
            logger.info("User {}", userReadableName())
            logger.info("Settings: $settings")
            when {
                inlineQuery != null -> inlineQuery.handle(settings, sender)
                message != null -> message.handle(settings, sender)
                editedMessage != null -> editedMessage.handle(settings, sender)
                callbackQuery != null -> {
                    Setting.handle(callbackQuery, settings, sender)
                    sender.pingCallbackQuery(callbackQuery.id)
                }
            }
        }
    } catch (e: Exception) {
        val cause = (e as? ResponseException)?.response?.bodyAsText() ?: "${e::class.simpleName} ${e.message}"
        val queryString = (update.message ?: update.editedMessage)?.text ?: update.inlineQuery?.query
        val user = update.userReadableName()
        logger.info("Error for query '$queryString': $cause")
        if ("query is too old" !in cause) {
            val message = BotTextOutput("Error received.\n```\nQuery: $queryString\nUser: $user\n\nCause: $cause```")
            sender.sendChatMessage(creatorId, message)
        }
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        logger.error(sw.toString())
    }
    return
}

fun loadSettings(update: Update) = with(update) {
    val chat = message?.chat ?: editedMessage?.chat ?: callbackQuery?.message?.chat
    val telegramUser = inlineQuery?.from ?: message?.from ?: editedMessage?.from ?: callbackQuery?.from
    val userId = chat?.id ?: telegramUser?.id
    val settings = userId?.let { userSettingsStore.get(it.toString()) }
    if (settings != null) {
        logger.info("Loaded settings for user $userId (${userReadableName()}): $settings")
        settings
    } else {
        val inferredLanguage = telegramUser?.languageCode
            ?.take(2)
            ?.lowercase()
            ?.takeIf { it in BotMessages.supportedLanguages }
        inferredLanguage?.let { UserSettings(language = it) } ?: UserSettings()
    }
}