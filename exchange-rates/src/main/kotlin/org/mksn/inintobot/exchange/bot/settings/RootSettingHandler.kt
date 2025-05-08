package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.common.misc.lettersDiffer
import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.BotContext
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.output.strings.SettingsStrings
import org.mksn.inintobot.exchange.telegram.InlineKeyboardButton
import org.mksn.inintobot.exchange.telegram.Message

object RootSettingHandler : SettingHandler(1) {

    override fun controlButtons(buttonLabels: SettingsStrings.ButtonSettingsStrings) =
        listOf(ControlButtonHandler.closeButton(buttonLabels.close))

    override val buttonsPerRow: Int = 1
    override fun keyboardButtons(settings: UserSettings, checkedButtonLabel: String) =
        BotMessages.settings.of(settings.language).rootButtons
            .mapNotNull { (key, label) -> InlineKeyboardButton(label, callbackData(key))
                .takeIf { key != "alerts" || settings.alerts != null } }

    override fun messageMarkdown(settings: UserSettings, messages: SettingsStrings.MessagesSettingsStrings) = messages.root

    override fun isValidPayload(payload: String): Boolean {
        throw IllegalStateException("Not applicable for root button press")
    }

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String): UserSettings {
        throw IllegalStateException("Not applicable for root button press")
    }

    override suspend fun handle(data: String?, message: Message, current: UserSettings, context: BotContext) {
        when (data?.trimType()) {
            "language" -> Setting.LANGUAGE.handle(null, message, current, context)
            "defaultCurrency" -> Setting.DEFAULT_CURRENCY.handle(null, message, current, context)
            "defaultApi" -> Setting.DEFAULT_API.handle(null, message, current, context)
            "outputCurrencies" -> Setting.OUTPUT_CURRENCIES.handle(null, message, current, context)
            "dashboardCurrencies" -> Setting.DASHBOARD_CURRENCIES.handle(null, message, current, context)
            "decimalDigits" -> Setting.DECIMAL_DIGITS.handle(null, message, current, context)
            "thousandSeparator" -> Setting.THOUSAND_SEPARATOR.handle(null, message, current, context)
            "alerts" -> Setting.ALERTS.handle(null, message, current, context)
            else -> {
                val output = createOutputWithKeyboard(current, context.json)
                if (data == null) {
                    context.sender.sendChatMessage(message.chat.id.toString(), output)
                } else {
                    if (output.markdown() lettersDiffer message.text) {
                        context.sender.editChatMessage(message.chat.id.toString(), message.messageId, output)
                    }
                }
            }
        }
    }
}