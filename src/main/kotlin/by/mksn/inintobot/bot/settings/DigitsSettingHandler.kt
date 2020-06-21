package by.mksn.inintobot.bot.settings

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.misc.DEFAULT_DECIMAL_DIGITS
import by.mksn.inintobot.output.strings.MessagesSettingsStrings
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.InlineKeyboardButton

object DigitsSettingHandler : SettingHandler(7) {

    override val buttonsPerRow: Int = 4
    override fun keyboardButtons(settings: UserSettings, checkedButtonLablel: String): List<InlineKeyboardButton> =
        (0..DEFAULT_DECIMAL_DIGITS).map {
            val label = if (settings.decimalDigits == it) checkedButtonLablel.format(it) else it.toString()
            InlineKeyboardButton(label, callbackData(it.toString()))
        }

    override fun messageMarkdown(settings: UserSettings, messages: MessagesSettingsStrings): String {
        val api = AppContext.supportedApis.first { settings.apiName == it.name }
        val apiDisplayName = AppContext.apiNames.of(settings.language).getValue(api.name)
        return messages.decimalDigits.format(apiDisplayName, settings.defaultCurrency)
    }

    override fun isValidPayload(payload: String): Boolean = payload.toIntOrNull() in 0..DEFAULT_DECIMAL_DIGITS

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String) =
        currentSettings.copy(decimalDigits = validPayload.toInt())
}