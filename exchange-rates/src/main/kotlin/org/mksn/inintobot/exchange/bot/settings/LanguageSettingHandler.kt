package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.output.strings.SettingsStrings
import org.mksn.inintobot.exchange.telegram.InlineKeyboardButton

object LanguageSettingHandler : SettingHandler(2) {

    override fun keyboardButtons(settings: UserSettings, checkedButtonLabel: String) =
        BotMessages.supportedLanguages.map { (code, name) ->
            val label = if (settings.language == code) checkedButtonLabel.format(name) else name
            InlineKeyboardButton(label, callbackData(code))
        }

    override fun messageMarkdown(settings: UserSettings, messages: SettingsStrings.MessagesSettingsStrings): String =
        messages.language.format(BotMessages.supportedLanguages.getValue(settings.language), settings.language)

    override fun isValidPayload(payload: String) =
        payload in BotMessages.supportedLanguages

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String) =
        currentSettings.copy(language = validPayload)
}