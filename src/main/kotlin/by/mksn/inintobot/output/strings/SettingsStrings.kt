package by.mksn.inintobot.output.strings

import kotlinx.serialization.Serializable

@Serializable
data class SettingsStrings(
    val messages: MessagesSettingsStrings,
    val rootButtons: Map<String, String>,
    val buttons: ButtonSettingsStrings
)

@Serializable
data class MessagesSettingsStrings(
    val root: String,
    val language: String,
    val defaultCurrency: String,
    val defaultApi: String,
    val outputCurrencies: String,
    val dashboardCurrencies: String,
    val decimalDigits: String
)

@Serializable
data class ButtonSettingsStrings(
    val checked: String,
    val back: String,
    val close: String
)