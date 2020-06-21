package by.mksn.inintobot.bot.settings

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.strings.ButtonSettingsStrings
import by.mksn.inintobot.output.strings.MessagesSettingsStrings
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.InlineKeyboardButton
import by.mksn.inintobot.telegram.Message

object RootSettingHandler : SettingHandler(1) {

    override fun controlButtons(buttonLabels: ButtonSettingsStrings) =
        listOf(ControlButtonHandler.closeButton(buttonLabels.close))

    override val buttonsPerRow: Int = 1
    override fun keyboardButtons(settings: UserSettings, checkedButtonLablel: String) =
        AppContext.settingsStrings.of(settings.language).rootButtons
            .map { (key, label) -> InlineKeyboardButton(label, callbackData(key)) }

    override fun messageMarkdown(settings: UserSettings, messages: MessagesSettingsStrings) = messages.root

    override fun isValidPayload(payload: String): Boolean {
        throw IllegalStateException("Not applicable for root button press")
    }

    override fun createNewSettings(currentSettings: UserSettings, validPayload: String): UserSettings {
        throw IllegalStateException("Not applicable for root button press")
    }

    override suspend fun handle(data: String?, message: Message, current: UserSettings, sender: BotOutputSender) {
        when (data?.trimType()) {
            "language" -> Setting.LANGUAGE.handle(null, message, current, sender)
            "defaultCurrency" -> Setting.DEFAULT_CURRENCY.handle(null, message, current, sender)
            "defaultApi" -> Setting.DEFAULT_API.handle(null, message, current, sender)
            "outputCurrencies" -> Setting.OUTPUT_CURRENCIES.handle(null, message, current, sender)
            "dashboardCurrencies" -> Setting.DASHBOARD_CURRENCIES.handle(null, message, current, sender)
            "decimalDigits" -> Setting.DECIMAL_DIGITS.handle(null, message, current, sender)
            else -> {
                val output = createOutputWithKeyboard(current)
                sender.sendChatMessage(message.chat.id.toString(), output)
            }
        }
    }
}