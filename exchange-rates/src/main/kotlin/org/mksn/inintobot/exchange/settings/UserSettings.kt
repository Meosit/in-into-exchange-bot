package org.mksn.inintobot.exchange.settings

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val language: String = UserDefaultSettings.LANGUAGE,
    val decimalDigits: Int = UserDefaultSettings.DECIMAL_DIGITS,
    val defaultCurrency: String = UserDefaultSettings.DEFAULT_CURRENCY,
    val apiName: String = UserDefaultSettings.API_NAME,
    val outputCurrencies: List<String> = UserDefaultSettings.OUTPUT_CURRENCIES,
    val dashboardCurrencies: List<String> = UserDefaultSettings.DASHBOARD_CURRENCIES,
    val dashboardOutputAll: Boolean = UserDefaultSettings.DASHBOARD_OUTPUT_ALL
)

object UserDefaultSettings {
    const val LANGUAGE: String = "en"
    const val DECIMAL_DIGITS: Int = 2
    const val DEFAULT_CURRENCY: String = "USD"
    const val API_NAME: String = "OpenExchangeRates"
    val OUTPUT_CURRENCIES = listOf("USD", "EUR", "BYN", "RUB", "UAH")
    val DASHBOARD_CURRENCIES: List<String> = listOf("USD", "EUR", "BYN", "RUB", "UAH")
    const val DASHBOARD_OUTPUT_ALL: Boolean = true
}