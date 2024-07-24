package org.mksn.inintobot.common.user

import java.math.BigDecimal

object UserDefaultSettings {
    const val LANGUAGE: String = "en"
    const val DECIMAL_DIGITS: Int = 2
    const val DEFAULT_CURRENCY: String = "USD"
    const val API_NAME: String = "OpenExchangeRates"
    val OUTPUT_CURRENCIES = listOf("USD", "EUR", "BYN", "RUB", "UAH")
    val DASHBOARD_CURRENCIES: List<String> = listOf("USD", "EUR", "BYN", "RUB", "UAH")
    val HOURLY_RATE_USD: BigDecimal? = null
}