package by.mksn.inintobot.bot.settings

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.strings.ButtonSettingsStrings
import by.mksn.inintobot.output.strings.MessagesSettingsStrings
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.InlineKeyboardButton
import by.mksn.inintobot.telegram.Message

/**
 * Pretty similar to [LanguageSettingHandler] but isolated only to choose the language
 */
object StartCommandSettingHandler : SettingHandler(8) {

    override fun controlButtons(buttonLabels: ButtonSettingsStrings): List<InlineKeyboardButton> = emptyList()

    override fun keyboardButtons(settings: UserSettings, checkedButtonLabel: String) =
        AppContext.supportedLanguages.map { (code, name) ->
            val label = if (settings.language == code) checkedButtonLabel.format(name) else name
            InlineKeyboardButton(label, callbackData(code))
        }

    override fun messageMarkdown(settings: UserSettings, messages: MessagesSettingsStrings): String =
        AppContext.commandMessages.of(settings.language).start

    override fun isValidPayload(payload: String) =
        payload in AppContext.supportedLanguages

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String) =
        currentSettings.copy(language = validPayload)

    override suspend fun handle(data: String?, message: Message, current: UserSettings, sender: BotOutputSender) {
        val output = createOutputWithKeyboard(current)
        if (data == null) {
            sender.sendChatMessage(message.chat.id.toString(), output)
        } else {
            super.handle(data, message, current, sender)
        }
    }
}