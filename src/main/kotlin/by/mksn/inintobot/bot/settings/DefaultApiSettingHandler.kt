package by.mksn.inintobot.bot.settings

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.output.strings.MessagesSettingsStrings
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.InlineKeyboardButton

object DefaultApiSettingHandler : SettingHandler(4) {

    override fun keyboardButtons(settings: UserSettings, checkedButtonLablel: String): List<InlineKeyboardButton> {
        val apiDisplayNames = AppContext.apiNames.of(settings.language)
        return AppContext.supportedApis.map { api ->
            val displayName = apiDisplayNames.getValue(api.name)
            val label = if (settings.apiName == api.name) checkedButtonLablel.format(displayName) else displayName
            InlineKeyboardButton(label, callbackData(api.name))
        }
    }

    override fun messageMarkdown(settings: UserSettings, messages: MessagesSettingsStrings): String {
        val apiDisplayName = AppContext.apiNames.of(settings.language).getValue(settings.apiName)
        return messages.defaultApi.format(apiDisplayName, settings.apiName)
    }

    override fun isValidPayload(payload: String): Boolean = AppContext.supportedApis.any { it.name == payload }

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String): UserSettings =
        currentSettings.copy(apiName = validPayload)
}