package org.mksn.inintobot.exchange.grammar

import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.ParseResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import kotlin.reflect.KClass

/** Tries to parse the sequence with [parser], and if that fails, returns [Parsed] of null instead. */
class OptionalNotIgnoringCombinator<T, E: ErrorResult>(private val errorClass: KClass<E>, private val parser: Parser<T>) :
    Parser<T?> {
    override fun tryParse(tokens: TokenMatchesSequence, fromPosition: Int): ParseResult<T?> {
        val result = parser.tryParse(tokens, fromPosition)
        if (errorClass.isInstance(result)) {
            return result
        }
        return when (result) {
            is ErrorResult -> ParsedValue(null, fromPosition)
            is Parsed -> result
        }
    }
}

/** Uses [parser] and if that fails returns [Parsed] of null. */
fun <T, E: ErrorResult> optionalNotIgnoring(errorClass: KClass<E>, parser: Parser<T>): Parser<T?> = OptionalNotIgnoringCombinator(errorClass, parser)