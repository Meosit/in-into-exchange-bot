package org.mksn.inintobot.common.user

import kotlinx.serialization.Serializable
import org.mksn.inintobot.common.misc.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class UserSettings(
    val language: String = UserDefaultSettings.LANGUAGE,
    val decimalDigits: Int = UserDefaultSettings.DECIMAL_DIGITS,
    val defaultCurrency: String = UserDefaultSettings.DEFAULT_CURRENCY,
    val apiName: String = UserDefaultSettings.API_NAME,
    val outputCurrencies: List<String> = UserDefaultSettings.OUTPUT_CURRENCIES,
    val dashboardCurrencies: List<String> = UserDefaultSettings.DASHBOARD_CURRENCIES,
    val hourlyRateUSD: @Serializable(with = BigDecimalSerializer::class) BigDecimal? = null,
    val thousandSeparator: Char? = UserDefaultSettings.THOUSAND_SEPARATOR,
    val alerts: List<RateAlert>? = null,
    val persisted: Boolean = true,
)