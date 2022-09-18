package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.common.rate.RateApis
import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.output.strings.SettingsStrings
import org.mksn.inintobot.exchange.telegram.InlineKeyboardButton

object DefaultApiSettingHandler : SettingHandler(4) {

    override fun keyboardButtons(settings: UserSettings, checkedButtonLabel: String): List<InlineKeyboardButton> {
        val apiDisplayNames = BotMessages.apiDisplayNames.of(settings.language)
        return RateApis.map { api ->
            val displayName = apiDisplayNames.getValue(api.name)
            val label = if (settings.apiName == api.name) checkedButtonLabel.format(displayName) else displayName
            InlineKeyboardButton(label, callbackData(api.name))
        }
    }

    override fun messageMarkdown(settings: UserSettings, messages: SettingsStrings.MessagesSettingsStrings): String {
        val apiDisplayName = BotMessages.apiDisplayNames.of(settings.language).getValue(settings.apiName)
        val unsupported = RateApis[settings.apiName].unsupported.joinToString()
        return messages.defaultApi.format(apiDisplayName, settings.apiName, unsupported)
    }

    override fun isValidPayload(payload: String): Boolean = payload in RateApis

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String): UserSettings =
        currentSettings.copy(apiName = validPayload)
}