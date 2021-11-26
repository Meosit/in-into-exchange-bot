package by.mksn.inintobot.output

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.grammar.CurrencyUnexpected
import by.mksn.inintobot.grammar.RateApiUnexpected
import by.mksn.inintobot.misc.escapeMarkdown
import by.mksn.inintobot.misc.trimToLength
import by.mksn.inintobot.output.strings.ErrorMessages
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.MismatchedToken
import com.github.h0tk3y.betterParse.parser.NoMatchingToken
import com.github.h0tk3y.betterParse.parser.UnparsedRemainder

data class BotQueryErrorOutput(
    val rawInput: String,
    val errorPosition: Int,
    val errorMessage: String
) : BotOutput {
    private val trimmedRawInput = rawInput.trimToLength(AppContext.maxErrorLineLength, tail = "…")
    override fun inlineTitle() = errorMessage
    override fun inlineThumbUrl() = "https://i.imgur.com/yTMgvf9.png"

    override fun inlineDescription() = "(at $errorPosition) $rawInput"

    override fun markdown() = """
        ${errorMessage.escapeMarkdown()} (at $errorPosition)
        `${"▼".padStart(if (errorPosition > trimmedRawInput.length) trimmedRawInput.length else errorPosition)}`
        `$trimmedRawInput`
        `${"▲".padStart(if (errorPosition > trimmedRawInput.length) trimmedRawInput.length else errorPosition)}`
    """.trimIndent()

    override fun toApiResponse() = ApiErrorResponse(
        message = errorMessage,
        rawInput = trimmedRawInput,
        position = if (errorPosition > trimmedRawInput.length) trimmedRawInput.length else errorPosition
    )
}

fun ErrorResult.toBotOutput(rawInput: String, messages: ErrorMessages) = when (this) {
    is UnparsedRemainder -> {
        val message = if (startsWith.type.name == "currency")
            messages.illegalCurrencyPlacement else messages.unparsedReminder
        BotQueryErrorOutput(rawInput, startsWith.column, message)
    }
    is MismatchedToken -> BotQueryErrorOutput(rawInput, found.column, messages.mismatchedToken.format(found.text))
    is NoMatchingToken -> BotQueryErrorOutput(rawInput, tokenMismatch.column, messages.noMatchingToken.format(tokenMismatch.text))
    is CurrencyUnexpected -> BotQueryErrorOutput(rawInput, tokenMismatch.column, messages.currencyUnexpected.format(tokenMismatch.text))
    is RateApiUnexpected -> BotQueryErrorOutput(rawInput, tokenMismatch.column, messages.rateApiUnexpected.format(tokenMismatch.text))
    else -> BotQueryErrorOutput(rawInput, 1, messages.unexpectedError)
}