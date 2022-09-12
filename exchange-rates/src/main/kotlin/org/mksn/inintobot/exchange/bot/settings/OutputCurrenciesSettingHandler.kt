package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.currency.Currencies
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.output.strings.SettingsStrings
import org.mksn.inintobot.exchange.settings.UserSettings
import org.mksn.inintobot.exchange.telegram.InlineKeyboardButton
import org.mksn.inintobot.rates.RateApis

object OutputCurrenciesSettingHandler : SettingHandler(5) {
    override val buttonsPerRow: Int = 3
    override fun keyboardButtons(settings: UserSettings, checkedButtonLabel: String): List<InlineKeyboardButton> {
        val api = RateApis[settings.apiName]
        return Currencies.mapNotNull {
            if (it.code in api.unsupported) {
                null
            } else {
                val label = if (it.code in settings.outputCurrencies) checkedButtonLabel.format(it.code) else it.code
                InlineKeyboardButton(label, callbackData(it.code))
            }
        }
    }

    override fun messageMarkdown(settings: UserSettings, messages: SettingsStrings.MessagesSettingsStrings): String {
        val api = RateApis[settings.apiName]
        val apiDisplayName = BotMessages.apiDisplayNames.of(settings.language).getValue(api.name)
        return messages.outputCurrencies.format(apiDisplayName, settings.outputCurrencies.joinToString())
    }

    override fun isValidPayload(payload: String): Boolean = payload in Currencies

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String): UserSettings {
        val currencies = currentSettings.outputCurrencies
        val newOutputCurrencies = when {
            currencies.size == 1 && validPayload == currencies.first() -> currencies
            validPayload in currencies -> currencies - validPayload
            else -> currencies + validPayload
        }
        val orderedNewCurrencies = Currencies
            .mapNotNull { if (it.code in newOutputCurrencies) it.code else null }
        return currentSettings.copy(outputCurrencies = orderedNewCurrencies)
    }
}