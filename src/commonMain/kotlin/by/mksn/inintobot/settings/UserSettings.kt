package by.mksn.inintobot.settings

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val language: String = "ru",
    val decimalDigits: Long = 2,
    val defaultCurrency: String = "BYN",
    val apiName: String = "NBRB",
    val outputCurrencies: List<String> = listOf("BYN", "USD", "EUR", "RUB"),
    val dashboardCurrencies: List<String> = listOf("USD", "EUR", "BYN", "UAH", "RUB")
)