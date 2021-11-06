package by.mksn.inintobot.bot.settings

import by.mksn.inintobot.misc.DEFAULT_DECIMAL_DIGITS
import by.mksn.inintobot.output.strings.MessagesSettingsStrings
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.InlineKeyboardButton

object DigitsSettingHandler : SettingHandler(7) {

    override val buttonsPerRow: Int = 4
    override fun keyboardButtons(settings: UserSettings, checkedButtonLabel: String): List<InlineKeyboardButton> =
        (0..DEFAULT_DECIMAL_DIGITS).map {
            val label = if (settings.decimalDigits == it) checkedButtonLabel.format(it) else it.toString()
            InlineKeyboardButton(label, callbackData(it.toString()))
        }

    override fun messageMarkdown(settings: UserSettings, messages: MessagesSettingsStrings): String =
        messages.decimalDigits.format(settings.decimalDigits, "1.12345678901234567".take(settings.decimalDigits + 2))

    override fun isValidPayload(payload: String): Boolean = payload.toIntOrNull() in 0..DEFAULT_DECIMAL_DIGITS

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String) =
        currentSettings.copy(decimalDigits = validPayload.toInt())
}