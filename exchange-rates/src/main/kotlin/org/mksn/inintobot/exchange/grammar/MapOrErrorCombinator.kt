package org.mksn.inintobot.exchange.grammar

import com.github.h0tk3y.betterParse.combinators.MapCombinator
import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.ParseResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser

internal class ParsedValue<out T>(override val value: T, override val nextPosition : Int) : Parsed<T>()

/** Parses the sequence with [innerParser], and if that succeeds, maps its [Parsed] result with [mapSuccess] and
 * if a `null` returned injects an [ErrorResult] produced by [mapError].
 *
 * Returns the [ErrorResult] of the `innerParser` otherwise.*/
class MapOrErrorCombinator<T, R>(
    private val innerParser: Parser<T>,
    val mapSuccess: (T) -> R?,
    val mapError: (T) -> ErrorResult
) : Parser<R> {
    override fun tryParse(tokens: TokenMatchesSequence, fromPosition: Int): ParseResult<R> {
        return when (val innerResult = innerParser.tryParse(tokens, fromPosition)) {
            is ErrorResult -> innerResult
            is Parsed -> mapSuccess(innerResult.value)
                ?.let { ParsedValue(it, innerResult.nextPosition) }
                ?: mapError(innerResult.value)
        }
    }
}

/** Applies the [transform] function to the successful results of the receiver parser,
 * in case of null inserts the given [error].
 * This allows to control the error flow as well
 * See [MapCombinator].*/
fun <T, R> Parser<T>.mapOrError(mapError: (T) -> ErrorResult, transform: (T) -> R?): Parser<R> =
    MapOrErrorCombinator(this, transform, mapError)