package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.exchange.json
import org.mksn.inintobot.exchange.output.BotOutput
import org.mksn.inintobot.exchange.output.BotOutputSender
import org.mksn.inintobot.exchange.output.BotTextOutput
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.output.strings.SettingsStrings
import org.mksn.inintobot.exchange.settings.UserSettings
import org.mksn.inintobot.exchange.telegram.InlineKeyboardButton
import org.mksn.inintobot.exchange.telegram.InlineKeyboardMarkup
import org.mksn.inintobot.exchange.telegram.Message
import org.mksn.inintobot.exchange.userSettingsStore
import org.mksn.inintobot.misc.lettersDiffer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(SettingHandler::class.simpleName)

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

    protected open fun createOutputWithKeyboard(settings: UserSettings): BotOutput {
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

    open suspend fun handle(data: String?, message: Message, current: UserSettings, sender: BotOutputSender) {
        logger.info("Handling settings payload: $data")
        val payload = data?.trimType()
        if (payload == null) {
            val output = createOutputWithKeyboard(current)
            if (output.markdown() lettersDiffer message.text) {
                sender.editChatMessage(message.chat.id.toString(), message.messageId, output)
            }
        } else {
            if (isValidPayload(payload)) {
                val newSettings = createNewSettings(current, payload)
                try {
                    userSettingsStore.save(message.chat.id.toString(), newSettings)
                    logger.info("Settings successfully updated: $newSettings")
                    val output = createOutputWithKeyboard(newSettings)
                    if (output.markdown() lettersDiffer message.text) {
                        sender.editChatMessage(message.chat.id.toString(), message.messageId, output)
                    }
                } catch (e: Exception) {
                    val error = BotMessages.errors.of(current.language).unableToSave
                    sender.editChatMessage(message.chat.id.toString(), message.messageId, BotTextOutput(error))
                    throw e
                }
            } else {
                throwInvalid(data)
            }
        }
    }

}