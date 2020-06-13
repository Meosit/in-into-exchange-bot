package by.mksn.inintobot.output

import kotlinx.serialization.Serializable

/**
 * Container class for human-readable telegram output strings to be shown to end user.
 */
@Serializable
data class TelegramStrings(
    val headers: TelegramMessageHeaders,
    val inlineTitles: TelegramInlines,
    val inlineThumbs: TelegramInlines,
    val outputTooBigMessage: String
)

@Serializable
data class TelegramMessageHeaders(
    val rate: String,
    val api: String,
    val singleCurrencyExpression: String,
    val multiCurrencyExpression: String
)

@Serializable
data class TelegramInlines(
    val exchange: String,
    val calculate: String,
    val dashboard: String
)
