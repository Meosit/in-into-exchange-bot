package org.mksn.inintobot.exchange.bot.settings

import kotlinx.serialization.json.Json
import org.mksn.inintobot.common.misc.lettersDiffer
import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.BotContext
import org.mksn.inintobot.exchange.output.BotOutput
import org.mksn.inintobot.exchange.output.BotTextOutput
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.output.strings.SettingsStrings
import org.mksn.inintobot.exchange.telegram.InlineKeyboardButton
import org.mksn.inintobot.exchange.telegram.InlineKeyboardMarkup
import org.mksn.inintobot.exchange.telegram.Message
import java.util.logging.Logger

private val logger = Logger.getLogger(SettingHandler::class.simpleName)

abstract class SettingHandler(id: Int) {

    private val typePrefix = "$id|"

    fun canHandle(data: String) = data.startsWith(typePrefix)

    protected fun callbackData(data: String) = "$typePrefix$data"

    protected fun String.trimType() = removePrefix(typePrefix)

    protected open fun controlButtons(buttonLabels: SettingsStrings.ButtonSettingsStrings) =
        listOf(ControlButtonHandler.backButton(buttonLabels.back), ControlButtonHandler.closeButton(buttonLabels.close))

    protected open val buttonsPerRow = 2
    protected abstract fun keyboardButtons(
        settings: UserSettings,
        checkedButtonLabel: String
    ): List<InlineKeyboardButton>

    protected abstract fun messageMarkdown(settings: UserSettings, messages: SettingsStrings.MessagesSettingsStrings): String

    protected open fun createOutputWithKeyboard(settings: UserSettings, json: Json): BotOutput {
        val settingsStrings = BotMessages.settings.of(settings.language)
        val buttons = keyboardButtons(settings, settingsStrings.buttons.checked)
        logger.info("${buttons.size} buttons generated")
        val keyboard = sequence {
            for (i in buttons.indices step buttonsPerRow) {
                yield((0 until buttonsPerRow).mapNotNull { buttons.getOrNull(i + it) })
            }
        }.toMutableList()
        val controlButtons = controlButtons(settingsStrings.buttons)
        if (controlButtons.isNotEmpty()) {
            keyboard.add(controlButtons)
        }
        val markdown = messageMarkdown(settings, settingsStrings.messages)
        val keyboardJson = json.encodeToString(InlineKeyboardMarkup.serializer(), InlineKeyboardMarkup(keyboard))
        return BotTextOutput(markdown, keyboardJson)
    }

    protected abstract fun isValidPayload(payload: String): Boolean

    protected abstract fun createNewSettings(currentSettings: UserSettings, validPayload: String): UserSettings

    protected fun throwInvalid(data: String?): Nothing =
        throw IllegalStateException("Invalid payload '$data' supplied")

    open suspend fun handle(data: String?, message: Message, current: UserSettings, context: BotContext) {
        logger.info("Handling settings payload: $data")
        val payload = data?.trimType()
        if (payload == null) {
            val output = createOutputWithKeyboard(current, context.json)
            if (output.markdown() lettersDiffer message.text) {
                context.sender.editChatMessage(message.chat.id.toString(), message.messageId, output)
            }
        } else {
            if (isValidPayload(payload)) {
                val newSettings = createNewSettings(current, payload).copy(persisted = true)
                try {
                    context.settingsStore.save(message.chat.id.toString(), newSettings)
                    logger.info("Settings successfully updated: $newSettings")
                    val output = createOutputWithKeyboard(newSettings, context.json)
                    if (output.markdown() lettersDiffer message.text) {
                        context.sender.editChatMessage(message.chat.id.toString(), message.messageId, output)
                    }
                    context.statsStore.logSettingsChange(current, newSettings)
                } catch (e: Exception) {
                    val error = BotMessages.errors.of(current.language).unableToSave
                    context.sender.editChatMessage(message.chat.id.toString(), message.messageId, BotTextOutput(error))
                    throw e
                }
            } else {
                throwInvalid(data)
            }
        }
    }

}