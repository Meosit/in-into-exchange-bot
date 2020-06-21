package by.mksn.inintobot.bot.settings

import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.strings.MessagesSettingsStrings
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.InlineKeyboardButton
import by.mksn.inintobot.telegram.Message

object ControlButtonHandler : SettingHandler(0) {

    private val CLOSE_BUTTON_PAYLOAD = callbackData("close")
    private val BACK_BUTTON_PAYLOAD = callbackData("back")

    fun closeButton(label: String) = InlineKeyboardButton(label, CLOSE_BUTTON_PAYLOAD)
    fun backButton(label: String) = InlineKeyboardButton(label, BACK_BUTTON_PAYLOAD)

    override fun keyboardButtons(settings: UserSettings, checkedButtonLablel: String) =
        throw IllegalStateException("Control button do not have a keyboard")

    override fun isValidPayload(payload: String): Boolean =
        throw IllegalStateException("Not applicable for control button press")

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String): UserSettings =
        throw IllegalStateException("Not applicable for control button press")

    override fun messageMarkdown(settings: UserSettings, messages: MessagesSettingsStrings): String =
        throw IllegalStateException("Not applicable for control button press")

    override suspend fun handle(data: String?, message: Message, current: UserSettings, sender: BotOutputSender) {
        when (data) {
            BACK_BUTTON_PAYLOAD -> RootSettingHandler.handle(data, message, current, sender)
            CLOSE_BUTTON_PAYLOAD -> sender.deleteChatMessage(message.chat.id.toString(), message.messageId)
            else -> throwInvalid(data)
        }
    }
}