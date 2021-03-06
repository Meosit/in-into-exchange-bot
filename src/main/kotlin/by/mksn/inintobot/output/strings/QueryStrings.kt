package by.mksn.inintobot.output.strings

import kotlinx.serialization.Serializable

/**
 * Container class for human-readable telegram output strings to be shown to end user.
 */
@Serializable
data class QueryStrings(
    val headers: MessageHeaders,
    val inlineTitles: Inlines,
    val inlineThumbs: Inlines,
    val outputTooBigMessage: String
)

@Serializable
data class MessageHeaders(
    val rate: String,
    val api: String,
    val singleCurrencyExpression: String,
    val multiCurrencyExpression: String
)

@Serializable
data class Inlines(
    val exchange: String,
    val calculate: String,
    val dashboard: String
)
