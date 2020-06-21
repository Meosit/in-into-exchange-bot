package by.mksn.inintobot.bot.settings

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.output.strings.MessagesSettingsStrings
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.InlineKeyboardButton

object OutputCurrenciesSettingHandler : SettingHandler(5) {
    override fun keyboardButtons(settings: UserSettings, checkedButtonLablel: String): List<InlineKeyboardButton> {
        val api = AppContext.supportedApis.first { settings.apiName == it.name }
        return AppContext.supportedCurrencies.mapNotNull {
            if (it.code in api.unsupported) {
                null
            } else {
                val label = if (it.code in settings.outputCurrencies) checkedButtonLablel.format(it.code) else it.code
                InlineKeyboardButton(label, callbackData(it.code))
            }
        }
    }

    override fun messageMarkdown(settings: UserSettings, messages: MessagesSettingsStrings): String {
        val api = AppContext.supportedApis.first { settings.apiName == it.name }
        val apiDisplayName = AppContext.apiNames.of(settings.language).getValue(api.name)
        return messages.outputCurrencies.format(apiDisplayName, settings.outputCurrencies.joinToString())
    }

    override fun isValidPayload(payload: String): Boolean = AppContext.supportedCurrencies.any { it.code == payload }

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String): UserSettings {
        val currencies = currentSettings.outputCurrencies
        val newOutputCurrencies = when {
            currencies.size == 1 && validPayload == currencies.first() -> currencies
            validPayload in currencies -> currencies - validPayload
            else -> currencies + validPayload
        }
        val orderedNewCurrencies = AppContext.supportedCurrencies
            .mapNotNull { if (it.code in newOutputCurrencies) it.code else null }
        return currentSettings.copy(outputCurrencies = orderedNewCurrencies)
    }
}