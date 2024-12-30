package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.output.strings.SettingsStrings
import org.mksn.inintobot.exchange.telegram.InlineKeyboardButton

object ThousandSeparatorSettingHandler : SettingHandler(8) {

    override val buttonsPerRow: Int = 1

    override fun keyboardButtons(settings: UserSettings, checkedButtonLabel: String) =
        BotMessages.supportedthousandSeparators.map { (separator, title) ->
            val label = if (settings.thousandSeparator == separator) checkedButtonLabel.format(title) else title
            InlineKeyboardButton(label, callbackData(separator.toString()))
        }

    override fun messageMarkdown(settings: UserSettings, messages: SettingsStrings.MessagesSettingsStrings): String =
        messages.thousandSeparator.format(BotMessages.supportedthousandSeparators.getValue(settings.thousandSeparator))

    override fun isValidPayload(payload: String) =
        payload in BotMessages.supportedthousandSeparators.keys.map { it.toString() }

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String) =
        currentSettings.copy(thousandSeparator = if (validPayload == "null") null else validPayload.first())
}