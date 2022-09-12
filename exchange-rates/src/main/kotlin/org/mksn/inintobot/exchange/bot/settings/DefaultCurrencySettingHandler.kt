package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.currency.Currencies
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.output.strings.SettingsStrings
import org.mksn.inintobot.exchange.settings.UserSettings
import org.mksn.inintobot.exchange.telegram.InlineKeyboardButton
import org.mksn.inintobot.rates.RateApis

object DefaultCurrencySettingHandler : SettingHandler(3) {

    override val buttonsPerRow: Int = 3
    override fun keyboardButtons(settings: UserSettings, checkedButtonLabel: String): List<InlineKeyboardButton> {
        val api = RateApis[settings.apiName]
        return Currencies.mapNotNull {
            if (it.code in api.unsupported) null else {
                val label =
                    if (settings.defaultCurrency == it.code) checkedButtonLabel.format(it.code) else it.code
                InlineKeyboardButton(label, callbackData(it.code))
            }
        }
    }

    override fun messageMarkdown(settings: UserSettings, messages: SettingsStrings.MessagesSettingsStrings): String {
        val api = RateApis[settings.apiName]
        val apiDisplayName = BotMessages.apiDisplayNames.of(settings.language).getValue(api.name)
        return messages.defaultCurrency.format(apiDisplayName, settings.defaultCurrency)
    }

    override fun isValidPayload(payload: String): Boolean = payload in Currencies

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String) =
        currentSettings.copy(defaultCurrency = validPayload)


}