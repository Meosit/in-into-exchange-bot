package by.mksn.inintobot.settings

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val language: String,
    val decimalDigits: Long,
    val defaultCurrency: String,
    val apiName: String,
    val outputCurrencies: List<String>,
    val dashboardCurrencies: List<String>
)