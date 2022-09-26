package org.mksn.inintobot.exchange.grammar.parsers

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.expression.*
import org.mksn.inintobot.exchange.grammar.alias.AliasMatcher

/**
 * Container class for the parsers of the basic currencied math expression terms
 */
class CurrenciedMathParsers(
    tokenDict: TokenDictionary,
    mathParsers: SimpleMathParsers,
    currencyAliasMatcher: AliasMatcher<Currency>,
) {

    val currency: Parser<Currency> = tokenDict.currencyAlias
        .map { currencyAliasMatcher.match(it.text) }

    private val currenciedTerm: Parser<Expression> =
        (currency and mathParsers.divMulChain map { (c, e) -> CurrenciedExpression(e, c) }) or
                (mathParsers.divMulChain and currency map { (e, c) -> CurrenciedExpression(e, c) }) or
                (skip(tokenDict.minus) and parser(this::currenciedTerm) map { Negate(it) }) or
                (skip(tokenDict.leftPar) and parser(this::currenciedSubSumChain) and skip(tokenDict.rightPar))

    // can't multiply or divide two currencied expressions, in this case the value type would be unknown and will lead to confusion
    private val currenciedDivMulChain: Parser<Expression> =
        (currenciedTerm and oneOrMore((tokenDict.divide or tokenDict.multiply) and mathParsers.term) map { (initial, operands) ->
            operands.fold(initial) { a, (op, b) ->
                if (op.type == tokenDict.multiply) Multiply(a, b) else Divide(a, b)
            }
        }) or currenciedTerm

    val currenciedSubSumChain: Parser<Expression> =
        leftAssociative(currenciedDivMulChain, tokenDict.plus or tokenDict.minus use { type }) { a, op, b ->
            if (op == tokenDict.plus) Add(a, b) else Subtract(a, b)
        }


    // Division of two currencied expressions can be useful in some limited cases, especially when you need to have a ratio,
    // in this case we will assume that the output is a single value with no currency (e.g. math mode).
    // Example use-case: how many 2-dollar banknotes I can buy with 10 euro?
    private val currenciedDivision: Parser<Expression> = (currenciedTerm and tokenDict.divide and currenciedTerm map { (a, _, b) -> Divide(a, b) })
    private val currenciedDivisionTerm: Parser<Expression> = parser(this::currenciedDivision) or
            (skip(tokenDict.minus) and parser(this::currenciedDivisionTerm) map { Negate(it) }) or
            (skip(tokenDict.leftPar) and parser(this::currenciedDivisionSubSumChain) and skip(tokenDict.rightPar)) or mathParsers.term

    private val currenciedDivisionDivMulChain: Parser<Expression> = leftAssociative(currenciedDivisionTerm, tokenDict.divide or tokenDict.multiply) { a, op, b ->
        if (op.type == tokenDict.multiply) Multiply(a, b) else Divide(a, b)
    }

    val currenciedDivisionSubSumChain = leftAssociative(currenciedDivisionDivMulChain, tokenDict.plus or tokenDict.minus use { type }) { a, op, b ->
        if (op == tokenDict.plus) Add(a, b) else Subtract(a, b)
    }
}