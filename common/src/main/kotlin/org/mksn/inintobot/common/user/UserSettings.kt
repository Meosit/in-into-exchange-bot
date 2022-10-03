package org.mksn.inintobot.common.user

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val language: String = UserDefaultSettings.LANGUAGE,
    val decimalDigits: Int = UserDefaultSettings.DECIMAL_DIGITS,
    val defaultCurrency: String = UserDefaultSettings.DEFAULT_CURRENCY,
    val apiName: String = UserDefaultSettings.API_NAME,
    val outputCurrencies: List<String> = UserDefaultSettings.OUTPUT_CURRENCIES,
    val dashboardCurrencies: List<String> = UserDefaultSettings.DASHBOARD_CURRENCIES,
    val persisted: Boolean = true,
)