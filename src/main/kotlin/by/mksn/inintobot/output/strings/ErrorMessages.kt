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
    val currencyUnexpected: String,
    val rateApiUnexpected: String,
    val divisionByZero: String,
    val queryExpected: String,
    val unsupportedCurrency: String,
    val unsupportedCurrencyWithAlternative: String,
    val ratesUnavailable: String,
    val deprecatedBot: String,
    val staleApiRates: String,
    val inlineOutputAsChatInput: String,
    val unableToSave: String,
    val unexpectedError: String
)