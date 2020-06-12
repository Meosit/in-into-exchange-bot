package by.mksn.inintobot.output

import kotlinx.serialization.Serializable

/**
 * Container class for human-readable error messages to be shown to end user.
 */
@Serializable
data class ErrorMessages(
    val illegalCurrencyPlacement: String,
    val unparsedReminder: String,
    val mismatchedToken: String,
    val noMatchingToken: String,
    val divisionByZero: String,
    val unexpectedError: String,
    val outputTooBig: String,
    val unsupportedCurrency: String
)