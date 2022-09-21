package org.mksn.inintobot.exchange.output

import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.MismatchedToken
import com.github.h0tk3y.betterParse.parser.NoMatchingToken
import com.github.h0tk3y.betterParse.parser.UnparsedRemainder
import org.mksn.inintobot.common.misc.escapeMarkdown
import org.mksn.inintobot.common.misc.trimToLength
import org.mksn.inintobot.exchange.grammar.InvalidDate
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.output.strings.ErrorMessages

data class BotQueryErrorOutput(
    val rawInput: String,
    val errorPosition: Int,
    val errorMessage: String
) : BotOutput {
    private val trimmedRawInput = rawInput.trimToLength(BotMessages.maxErrorLineLength, tail = "…")
    override fun inlineTitle() = errorMessage
    override fun inlineThumbUrl() = "https://i.imgur.com/yTMgvf9.png"

    override fun inlineDescription() = "(at $errorPosition) $rawInput"

    override fun markdown() = """
        ${errorMessage.escapeMarkdown()} (at $errorPosition)
        `${"▼".padStart(if (errorPosition > trimmedRawInput.length) trimmedRawInput.length else errorPosition)}`
        `$trimmedRawInput`
        `${"▲".padStart(if (errorPosition > trimmedRawInput.length) trimmedRawInput.length else errorPosition)}`
    """.trimIndent()
}

fun ErrorResult.toBotOutput(rawInput: String, messages: ErrorMessages) = when (this) {
    is UnparsedRemainder -> {
        val message = if (startsWith.type.name == "currency")
            messages.illegalCurrencyPlacement else messages.unparsedReminder
        BotQueryErrorOutput(rawInput, startsWith.column, message)
    }
    is MismatchedToken -> BotQueryErrorOutput(rawInput, found.column, messages.mismatchedToken.format(found.text))
    is NoMatchingToken -> BotQueryErrorOutput(rawInput, tokenMismatch.column, messages.noMatchingToken.format(tokenMismatch.text))
    is InvalidDate -> BotQueryErrorOutput(rawInput, match.column, messages.noMatchingToken.format(match.text))
    else -> BotQueryErrorOutput(rawInput, 1, messages.unexpectedError)
}