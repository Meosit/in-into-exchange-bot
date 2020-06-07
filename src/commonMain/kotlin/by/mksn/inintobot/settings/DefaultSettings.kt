package by.mksn.inintobot.settings

import kotlinx.serialization.Serializable

@Serializable
data class DefaultSettings(
    val language: String,
    val apiName: String,
    val outputCurrencies: List<String>,
    val dashboardCurrencies: List<String>
)