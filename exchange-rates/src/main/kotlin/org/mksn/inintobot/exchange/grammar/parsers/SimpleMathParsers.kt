package org.mksn.inintobot.exchange.grammar.parsers

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.parser.Parser
import org.mksn.inintobot.common.expression.*
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal

/**
 * Container class for the parsers of the basic math expression terms
 */
class SimpleMathParsers(tokenDict: TokenDictionary) {

    private val americanNotationRegex = """^\d+(,\d{3})+(\.\d+)?$""".toRegex()
    private val germanNotationRegex = """^\d+(\.\d{3})+(,\d+)?$""".toRegex()
    private fun String.toParsableNumber(): String {
        val noSpaces = replace(" ", "").replace(" ", "").replace("\n", "").replace("\t", "")
        val americanNotation = noSpaces.matches(americanNotationRegex)
        val germanNotation = noSpaces.matches(germanNotationRegex)
        val separators = noSpaces.count { it == '.' || it == ',' }

        return when {
            separators <= 1 -> noSpaces.replace(',', '.')
            americanNotation -> noSpaces.replace(",", "")
            germanNotation -> noSpaces.replace(".", "").replace(',', '.')
            // if it's not a thousand separators - just convert to integer
            else -> noSpaces.replace(",", "").replace(".", "")
        }
    }

    private fun suffixedNumber(numberToken: Token, suffixToken: Token, suffixType: SuffixType) = numberToken and oneOrMore(suffixToken) map
            { (num, suffixes) -> ConstWithSuffixes(num.text.toParsableNumber().toFixedScaleBigDecimal(), suffixes.size, suffixType) }

    val plainNumber = tokenDict.number map { Const(it.text.toParsableNumber().toFixedScaleBigDecimal()) }

    val number: Parser<Expression> =
        suffixedNumber(tokenDict.number, tokenDict.kilo, SuffixType.KILO) or
                suffixedNumber(tokenDict.number, tokenDict.mega, SuffixType.MEGA) or
                plainNumber

    val term: Parser<Expression> = number or
            (skip(tokenDict.minus) and parser(this::term) map { Negate(it) }) or
            (skip(tokenDict.leftPar) and parser(this::subSumChain) and skip(tokenDict.rightPar))

    val divMulChain: Parser<Expression> = leftAssociative(term, tokenDict.divide or tokenDict.multiply) { a, op, b ->
        if (op.type == tokenDict.multiply) Multiply(a, b) else Divide(a, b)
    } and optional(tokenDict.percent) map { (expr, p) -> if (p != null) Percent(expr, p.column) else expr }

    val subSumChain: Parser<Expression> = leftAssociative(divMulChain, tokenDict.plus or tokenDict.minus use { type }) { a, op, b ->
        if (op == tokenDict.plus) Add(a, b) else Subtract(a, b)
    }

}