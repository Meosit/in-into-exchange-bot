package by.mksn.inintobot.grammar.parsers

import by.mksn.inintobot.expression.*
import by.mksn.inintobot.util.toFiniteBigDecimal
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser

/**
 * Container class for the parsers of the basic math expression terms
 */
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
class SimpleMathParsers(tokenDict: TokenDictionary) {

    private fun String.toParsableNumber() = replace(" ", "").replace(',', '.')

    val number: Parser<Expression> = (tokenDict.number and zeroOrMore(tokenDict.kilo)) or
            (tokenDict.number and zeroOrMore(tokenDict.mega)) map { (num, suffixes) ->
        val number = num.text.toParsableNumber().toFiniteBigDecimal()
        val expr = if (suffixes.isEmpty()) {
            Const(number)
        } else {
            ConstWithSuffixes(
                number,
                suffixes.size,
                suffixes.first().let {
                    when (it.type) {
                        tokenDict.kilo -> SuffixType.KILO
                        tokenDict.mega -> SuffixType.MEGA
                        else -> throw IllegalStateException("Unknown number suffix token $it")
                    }
                })
        }
        expr
    }

    val term: Parser<Expression> = number or
            (skip(tokenDict.minus) and parser(this::term) map { Negate(it) }) or
            (skip(tokenDict.leftPar) and parser(this::subSumChain) and skip(tokenDict.rightPar))

    val divMulChain: Parser<Expression> = leftAssociative(term, tokenDict.divide or tokenDict.multiply) { a, op, b ->
        if (op.type == tokenDict.multiply) Multiply(
            a,
            b
        ) else Divide(a, b)
    }

    val subSumChain = leftAssociative(divMulChain, tokenDict.plus or tokenDict.minus use { type }) { a, op, b ->
        if (op == tokenDict.plus) Add(
            a,
            b
        ) else Subtract(a, b)
    }

}