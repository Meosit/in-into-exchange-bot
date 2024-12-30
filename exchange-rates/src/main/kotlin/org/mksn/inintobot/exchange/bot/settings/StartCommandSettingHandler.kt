package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.BotContext
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.output.strings.SettingsStrings
import org.mksn.inintobot.exchange.telegram.InlineKeyboardButton
import org.mksn.inintobot.exchange.telegram.Message

/**
 * Pretty similar to [LanguageSettingHandler] but isolated only to choose the language
 */
object StartCommandSettingHandler : SettingHandler(99) {

    override fun controlButtons(buttonLabels: SettingsStrings.ButtonSettingsStrings): List<InlineKeyboardButton> = emptyList()

    override fun keyboardButtons(settings: UserSettings, checkedButtonLabel: String) =
        BotMessages.supportedLanguages.map { (code, name) ->
            val label = if (settings.language == code) checkedButtonLabel.format(name) else name
            InlineKeyboardButton(label, callbackData(code))
        }

    override fun messageMarkdown(settings: UserSettings, messages: SettingsStrings.MessagesSettingsStrings): String =
        BotMessages.startCommand.of(settings.language)

    override fun isValidPayload(payload: String) =
        payload in BotMessages.supportedLanguages

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String) =
        currentSettings.copy(language = validPayload)

    override suspend fun handle(data: String?, message: Message, current: UserSettings, context: BotContext) {
        val output = createOutputWithKeyboard(current, context.json)
        if (data == null) {
            context.sender.sendChatMessage(message.chat.id.toString(), output)
        } else {
            super.handle(data, message, current, context)
        }
    }
}