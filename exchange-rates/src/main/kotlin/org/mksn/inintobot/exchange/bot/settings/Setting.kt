package org.mksn.inintobot.exchange.bot.settings

import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.BotContext
import org.mksn.inintobot.exchange.telegram.CallbackQuery
import org.mksn.inintobot.exchange.telegram.Message

enum class Setting(private val handler: SettingHandler) {
    @Suppress("unused")
    CONTROL(ControlButtonHandler),
    ROOT(RootSettingHandler),
    LANGUAGE(LanguageSettingHandler),
    DEFAULT_CURRENCY(DefaultCurrencySettingHandler),
    DEFAULT_API(DefaultApiSettingHandler),
    OUTPUT_CURRENCIES(OutputCurrenciesSettingHandler),
    DASHBOARD_CURRENCIES(DashboardCurrenciesSettingHandler),
    DECIMAL_DIGITS(DigitsSettingHandler),
    THOUSAND_SEPARATOR(ThousandSeparatorSettingHandler),
    ALERTS(AlertsSettingHandler),

    START_COMMAND(StartCommandSettingHandler)
    ;

    suspend fun handle(data: String?, message: Message, current: UserSettings, context: BotContext) {
        handler.handle(data, message, current, context)
    }

    companion object {

        private fun ofCallbackQuery(data: String) = values().firstOrNull { it.handler.canHandle(data) }
            ?: throw IllegalStateException("Invalid payload '$data' supplied")

        suspend fun handle(callbackQuery: CallbackQuery, currentSettings: UserSettings, context: BotContext) {
            if (callbackQuery.message == null) {
                throw IllegalStateException("This callback was originated from inline query")
            }
            ofCallbackQuery(callbackQuery.data).handle(callbackQuery.data, callbackQuery.message, currentSettings, context)
        }
    }
}