package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.misc.toStr
import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.output.strings.SettingsStrings
import org.mksn.inintobot.exchange.telegram.InlineKeyboardButton

object AlertsSettingHandler : SettingHandler(9) {
    override val buttonsPerRow: Int = 1
    override fun keyboardButtons(settings: UserSettings, checkedButtonLabel: String): List<InlineKeyboardButton> {
        return settings.alerts?.map {
            val fromCurrency = Currencies[it.fromCurrency]
            val toCurrency = Currencies[it.toCurrency]
            val value = (if (it.isRelative) "±" else "⇵") + it.value.toStr(settings.decimalDigits)
            val label = "\uD83D\uDDD1 $value (${fromCurrency.code} → ${toCurrency.code} | ${BotMessages.apiDisplayNames.of(settings.language).getValue(it.apiName)})"
            InlineKeyboardButton(label, callbackData(it.id))
        } ?: emptyList()
    }

    override fun messageMarkdown(settings: UserSettings, messages: SettingsStrings.MessagesSettingsStrings): String {
        return messages.alerts.format(settings.alerts.orEmpty().size)
    }

    override fun isValidPayload(payload: String): Boolean =
        payload matches "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String): UserSettings {
        val newAlerts = currentSettings.alerts?.filterNot { it.id == validPayload }?.ifEmpty { null }
        return currentSettings.copy(alerts = newAlerts)
    }
}