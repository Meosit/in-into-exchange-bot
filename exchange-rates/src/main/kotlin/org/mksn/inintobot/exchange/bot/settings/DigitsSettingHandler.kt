package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.common.misc.DEFAULT_DECIMAL_DIGITS
import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.output.strings.SettingsStrings
import org.mksn.inintobot.exchange.telegram.InlineKeyboardButton

object DigitsSettingHandler : SettingHandler(7) {

    override val buttonsPerRow: Int = 4
    override fun keyboardButtons(settings: UserSettings, checkedButtonLabel: String): List<InlineKeyboardButton> =
        (0..DEFAULT_DECIMAL_DIGITS).map {
            val label = if (settings.decimalDigits == it) checkedButtonLabel.format(it) else it.toString()
            InlineKeyboardButton(label, callbackData(it.toString()))
        }

    override fun messageMarkdown(settings: UserSettings, messages: SettingsStrings.MessagesSettingsStrings): String =
        messages.decimalDigits.format(settings.decimalDigits, "1.12345678901234567".take(settings.decimalDigits + 2))

    override fun isValidPayload(payload: String): Boolean = payload.toIntOrNull() in 0..DEFAULT_DECIMAL_DIGITS

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String) =
        currentSettings.copy(decimalDigits = validPayload.toInt())
}