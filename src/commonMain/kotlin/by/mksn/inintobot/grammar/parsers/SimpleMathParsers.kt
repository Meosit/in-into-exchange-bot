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

    val number: Parser<Expression> =
        (tokenDict.number and oneOrMore(tokenDict.kilo) map { (num, suffixes) ->
            ConstWithSuffixes(num.text.toParsableNumber().toFiniteBigDecimal(), suffixes.size, SuffixType.KILO)
        }) or
                (tokenDict.number and oneOrMore(tokenDict.mega) map { (num, suffixes) ->
                    ConstWithSuffixes(num.text.toParsableNumber().toFiniteBigDecimal(), suffixes.size, SuffixType.MEGA)
                }) or
                (tokenDict.number map { Const(it.text.toParsableNumber().toFiniteBigDecimal()) })

    val term: Parser<Expression> = number or
            (skip(tokenDict.minus) and parser(this::term) map { Negate(it) }) or
            (skip(tokenDict.leftPar) and parser(this::subSumChain) and skip(tokenDict.rightPar))

    val divMulChain: Parser<Expression> = leftAssociative(term, tokenDict.divide or tokenDict.multiply) { a, op, b ->
        if (op.type == tokenDict.multiply) Multiply(a, b) else Divide(a, b)
    }

    val subSumChain = leftAssociative(divMulChain, tokenDict.plus or tokenDict.minus use { type }) { a, op, b ->
        if (op == tokenDict.plus) Add(a, b) else Subtract(a, b)
    }

}