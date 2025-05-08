package org.mksn.inintobot.exchange.bot

import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import org.mksn.inintobot.common.store.UserSettingsStore
import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.BotContext
import org.mksn.inintobot.exchange.bot.settings.Setting
import org.mksn.inintobot.exchange.output.BotTextOutput
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.telegram.Update
import java.time.LocalDateTime
import java.util.logging.Logger


private val logger = Logger.getLogger("handleTelegramRequest")

suspend fun handleTelegramRequest(
    update: Update,
    context: BotContext
) {
    logger.info("Handling Update...")
    try {
        val settings = context.settingsStore.loadSettings(update)
        with(update) {
            logger.info("User ${userReadableName()}, settings: $settings")
            when {
                inlineQuery != null -> inlineQuery.handle(settings, context)
                message != null -> message.handle(settings, context)
                editedMessage != null -> editedMessage.handle(settings, context)
                callbackQuery != null -> {
                    Setting.handle(callbackQuery, settings, context)
                    context.sender.pingCallbackQuery(callbackQuery.id)
                }
            }
        }
    } catch (e: Exception) {
        val cause = (e as? ResponseException)?.response?.bodyAsText() ?: "${e::class.simpleName} ${e.message}"
        val queryString = (update.message ?: update.editedMessage)?.text ?: update.inlineQuery?.query
        val user = update.userReadableName()
        logger.info("Error for query '$queryString': $cause")
        if ("query is too old" !in cause) {
            val message = BotTextOutput("Error received.\n```\nQuery: $queryString\nTime: ${LocalDateTime.now()}\nUser: $user\n\nCause: $cause```")
            context.sender.sendChatMessage(context.creatorId, message, disableNotification = true)
        }
        logger.severe(e.stackTraceToString())
    }
}

private fun UserSettingsStore.loadSettings(update: Update) = with(update) {
    val chat = message?.chat ?: editedMessage?.chat ?: callbackQuery?.message?.chat
    val telegramUser = inlineQuery?.from ?: message?.from ?: editedMessage?.from ?: callbackQuery?.from
    val userId = chat?.id ?: telegramUser?.id
    val settings = userId?.let { get(it.toString()) }
    if (settings != null) {
        logger.info("Loaded settings for user $userId (${userReadableName()}): $settings")
        settings
    } else {
        val inferredLanguage = telegramUser?.languageCode
            ?.take(2)
            ?.lowercase()
            ?.takeIf { it in BotMessages.supportedLanguages }
        inferredLanguage?.let { UserSettings(language = it, persisted = false) } ?: UserSettings(persisted = false)
    }
}