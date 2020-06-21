package by.mksn.inintobot.bot.settings

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.output.strings.MessagesSettingsStrings
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.InlineKeyboardButton

object LanguageSettingHandler : SettingHandler(2) {

    override fun keyboardButtons(settings: UserSettings, checkedButtonLablel: String) =
        AppContext.supportedLanguages.map { (code, name) ->
            val label = if (settings.language == code) checkedButtonLablel.format(name) else name
            InlineKeyboardButton(label, callbackData(code))
        }

    override fun messageMarkdown(settings: UserSettings, messages: MessagesSettingsStrings): String =
        messages.language.format(AppContext.supportedLanguages.getValue(settings.language), settings.language)

    override fun isValidPayload(payload: String) =
        payload in AppContext.supportedLanguages

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String) =
        currentSettings.copy(language = validPayload)
}