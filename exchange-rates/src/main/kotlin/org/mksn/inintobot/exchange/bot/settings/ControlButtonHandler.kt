package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.exchange.output.BotOutputSender
import org.mksn.inintobot.exchange.output.strings.SettingsStrings
import org.mksn.inintobot.exchange.settings.UserSettings
import org.mksn.inintobot.exchange.telegram.InlineKeyboardButton
import org.mksn.inintobot.exchange.telegram.Message

object ControlButtonHandler : SettingHandler(0) {

    private val CLOSE_BUTTON_PAYLOAD = callbackData("close")
    private val BACK_BUTTON_PAYLOAD = callbackData("back")

    fun closeButton(label: String) = InlineKeyboardButton(label, CLOSE_BUTTON_PAYLOAD)
    fun backButton(label: String) = InlineKeyboardButton(label, BACK_BUTTON_PAYLOAD)

    override fun keyboardButtons(settings: UserSettings, checkedButtonLabel: String) =
        throw IllegalStateException("Control button do not have a keyboard")

    override fun isValidPayload(payload: String): Boolean =
        throw IllegalStateException("Not applicable for control button press")

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String): UserSettings =
        throw IllegalStateException("Not applicable for control button press")

    override fun messageMarkdown(settings: UserSettings, messages: SettingsStrings.MessagesSettingsStrings): String =
        throw IllegalStateException("Not applicable for control button press")

    override suspend fun handle(data: String?, message: Message, current: UserSettings, sender: BotOutputSender) {
        when (data) {
            BACK_BUTTON_PAYLOAD -> RootSettingHandler.handle(data, message, current, sender)
            CLOSE_BUTTON_PAYLOAD -> sender.deleteChatMessage(message.chat.id.toString(), message.messageId)
            else -> throwInvalid(data)
        }
    }
}