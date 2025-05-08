package org.mksn.inintobot.exchange.output.strings

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
) {
    @Serializable
    data class MessageHeaders(
        val rate: String,
        val history: String,
        val alert: String,
        val api: String,
        val apiTime: String,
        val apiDate: String,
        val singleCurrencyExpression: String,
        val multiCurrencyExpression: String
    )

    @Serializable
    data class Inlines(
        val exchange: String,
        val calculate: String,
        val dashboard: String,
        val history: String,
    )
}