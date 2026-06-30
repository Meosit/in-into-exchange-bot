package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.output.strings.SettingsStrings
import org.mksn.inintobot.exchange.telegram.InlineKeyboardButton

object DecimalSeparatorSettingHandler : SettingHandler(10) {

    override val buttonsPerRow: Int = 1

    override fun keyboardButtons(settings: UserSettings, checkedButtonLabel: String) =
        BotMessages.supportedDecimalSeparators.keys.map { separator ->
            val title = settings.copy(decimalSeparator = separator).exampleNumber()
            val label = if (settings.decimalSeparator == separator) checkedButtonLabel.format(title) else title
            InlineKeyboardButton(label, callbackData(separator.toString()))
        }

    override fun messageMarkdown(settings: UserSettings, messages: SettingsStrings.MessagesSettingsStrings): String =
        messages.decimalSeparator.format(settings.exampleNumber())

    override fun isValidPayload(payload: String) =
        payload.length == 1 && payload.first() in BotMessages.supportedDecimalSeparators

    override fun rejectedPayloadNotification(
        currentSettings: UserSettings,
        validPayload: String,
        messages: SettingsStrings.MessagesSettingsStrings
    ): String? {
        val newDecimalSeparator = validPayload.first()
        return if (separatorsAreAmbiguous(currentSettings.thousandSeparator, newDecimalSeparator)) messages.ambiguousSeparators else null
    }

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String) =
        currentSettings.copy(decimalSeparator = validPayload.first())
}
