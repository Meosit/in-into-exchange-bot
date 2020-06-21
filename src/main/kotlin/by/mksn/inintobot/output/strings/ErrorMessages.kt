package by.mksn.inintobot.output.strings

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
    val queryExpected: String,
    val unsupportedCurrency: String,
    val ratesUnavailable: String,
    val deprecatedBot: String,
    val unableToSave: String,
    val unexpectedError: String
)