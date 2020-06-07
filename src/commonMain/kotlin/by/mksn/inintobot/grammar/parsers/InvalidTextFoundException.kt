package by.mksn.inintobot.grammar.parsers

import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.NoMatchingToken
import com.github.h0tk3y.betterParse.parser.UnparsedRemainder

/**
 * Represents the situation when the invalid alias placed instead of a expected text (either Currency or API name).
 * Such logic required since the grammar in the most cases returns [UnparsedRemainder] instead of [NoMatchingToken]
 */
class InvalidTextFoundException(private val aliasToken: TokenMatch) : RuntimeException() {
    fun toErrorResult() = NoMatchingToken(aliasToken)
}