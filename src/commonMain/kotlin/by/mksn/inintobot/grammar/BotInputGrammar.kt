package by.mksn.inintobot.grammar


import by.mksn.inintobot.currency.CurrencyAliasMatcher
import by.mksn.inintobot.expression.Const
import by.mksn.inintobot.expression.CurrenciedExpression
import by.mksn.inintobot.grammar.parsers.CurrenciedMathParsers
import by.mksn.inintobot.grammar.parsers.InvalidCurrencyFoundException
import by.mksn.inintobot.grammar.parsers.SimpleMathParsers
import by.mksn.inintobot.grammar.parsers.TokenDictionary
import by.mksn.inintobot.util.toFiniteBigDecimal
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.Tokenizer
import com.github.h0tk3y.betterParse.parser.AlternativesFailure
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parser

@ExperimentalStdlibApi
@Suppress("PrivatePropertyName")
@ExperimentalUnsignedTypes
class BotInputGrammar(
    aliasMatcher: CurrencyAliasMatcher
) : Grammar<BotInput>() {

    private val tokenDict = TokenDictionary(aliasMatcher.allAliasesRegex)
    private val mathParsers = SimpleMathParsers(tokenDict)
    private val currParsers = CurrenciedMathParsers(tokenDict, mathParsers, aliasMatcher)

    private val keyPrefix = skip(tokenDict.exclamation or tokenDict.ampersand or tokenDict.inIntoUnion)
    private val currencyKey = skip(tokenDict.whitespace) and (keyPrefix and currParsers.currency) map { it }
    private val additionalCurrenciesChain by zeroOrMore(currencyKey)

    private val onlyCurrencyExpressionParser by currParsers.currency map {
        CurrenciedExpression(Const(1.toFiniteBigDecimal()), it)
    }

    private val singleCurrencyExpressionParser by mathParsers.subSumChain and optional(currParsers.currency) map { (expr, currency) ->
        if (currency == null) expr else CurrenciedExpression(expr, currency)
    }

    private val multiCurrencyExpressionParser by currParsers.currenciedSubSumChain
    private val allValidExpressionParsers by multiCurrencyExpressionParser or singleCurrencyExpressionParser or onlyCurrencyExpressionParser

    private val botInputParser by allValidExpressionParsers and additionalCurrenciesChain map { (expr, keys) ->
        BotInput(expr, keys.toSet())
    }

    override val tokens = tokenDict.allTokens

    override val tokenizer: Tokenizer by lazy { SingleLineTokenizer(DefaultTokenizer(tokens)) }

    override val rootParser = object : Parser<BotInput> {
        override fun tryParse(tokens: Sequence<TokenMatch>) =
            try {
                when (val result = botInputParser.tryParse(tokens)) {
                    is AlternativesFailure -> {
                        fun find(errors: List<ErrorResult>): ErrorResult {
                            val error = errors.last()
                            return if (error !is AlternativesFailure) error else find(error.errors)
                        }
                        find(result.errors)
                    }
                    else -> result
                }
            } catch (e: InvalidCurrencyFoundException) {
                e.toErrorResult()
            }
    }

}