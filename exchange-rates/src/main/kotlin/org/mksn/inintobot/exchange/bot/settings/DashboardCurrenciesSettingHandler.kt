package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.rate.RateApis
import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.output.strings.SettingsStrings
import org.mksn.inintobot.exchange.telegram.InlineKeyboardButton

object DashboardCurrenciesSettingHandler : SettingHandler(6) {
    override val buttonsPerRow: Int = 3
    override fun keyboardButtons(settings: UserSettings, checkedButtonLabel: String): List<InlineKeyboardButton> {
        val api = RateApis[settings.apiName]
        return Currencies.mapNotNull {
            if (it.code in api.unsupported) {
                null
            } else {
                val label = if (it.code in settings.dashboardCurrencies) checkedButtonLabel.format(it.code) else it.code
                InlineKeyboardButton(label, callbackData(it.code))
            }
        }
    }

    override fun messageMarkdown(settings: UserSettings, messages: SettingsStrings.MessagesSettingsStrings): String {
        val api = RateApis[settings.apiName]
        val apiDisplayName = BotMessages.apiDisplayNames.of(settings.language).getValue(api.name)
        return messages.dashboardCurrencies.format(apiDisplayName, settings.dashboardCurrencies.joinToString())
    }

    override fun isValidPayload(payload: String): Boolean = payload in Currencies

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String): UserSettings {
        val currencies = currentSettings.dashboardCurrencies
        val newDashboardCurrencies = when {
            currencies.size == 1 && validPayload == currencies.first() -> currencies
            validPayload in currencies -> currencies - validPayload
            else -> currencies + validPayload
        }
        val orderedNewCurrencies = Currencies
            .mapNotNull { if (it.code in newDashboardCurrencies) it.code else null }
        return currentSettings.copy(dashboardCurrencies = orderedNewCurrencies)
    }
}