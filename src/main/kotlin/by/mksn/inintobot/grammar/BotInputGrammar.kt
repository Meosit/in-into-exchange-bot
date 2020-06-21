package by.mksn.inintobot.grammar


import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.expression.Const
import by.mksn.inintobot.expression.CurrenciedExpression
import by.mksn.inintobot.grammar.parsers.*
import by.mksn.inintobot.misc.AliasMatcher
import by.mksn.inintobot.misc.toFixedScaleBigDecimal
import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.Tokenizer
import com.github.h0tk3y.betterParse.parser.AlternativesFailure
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parser


class BotInputGrammar(
    currencyAliasMatcher: AliasMatcher<Currency>,
    rateApiAliasMatcher: AliasMatcher<RateApi>
) : Grammar<BotInput>() {

    private val tokenDict = TokenDictionary(
        AliasMatcher
            .createAliasRegex(currencyAliasMatcher.aliasRegexParts + rateApiAliasMatcher.aliasRegexParts)
    )

    private val currencyOrApiParser = tokenDict.currencyOrApi map {
        it to (currencyAliasMatcher.matchOrNull(it.text)
            ?: rateApiAliasMatcher.matchOrNull(it.text)
            ?: throw InvalidTextFoundException(it))
    }

    private val mathParsers = SimpleMathParsers(tokenDict)
    private val currParsers = CurrenciedMathParsers(tokenDict, mathParsers, currencyOrApiParser)

    private val currencyKeyPrefix = skip(tokenDict.exclamation or tokenDict.ampersand or tokenDict.inIntoUnion)
    private val currencyKey = skip(tokenDict.whitespace) and (currencyKeyPrefix and currParsers.currency) map { it }
    private val additionalCurrenciesChain by zeroOrMore(currencyKey)

    private val rateApiParser: Parser<RateApi> = currencyOrApiParser map {
        if (it.second is RateApi) it.second as RateApi else throw InvalidCurrencyPlacementException(it.first)
    }
    private val apiConfig = optional(skip(tokenDict.whitespace) and rateApiParser map { it })

    private val decimalDigitsNumber = tokenDict.number map { it.text.toIntOrNull() }
    private val decimalDigitsConfig =
        optional(skip(tokenDict.whitespace) and skip(tokenDict.decimalDigitsOption) and decimalDigitsNumber map { it })

    private val onlyCurrencyExpressionParser by currParsers.currency map {
        CurrenciedExpression(Const(1.toFixedScaleBigDecimal()), it)
    }

    private val singleCurrencyExpressionParser by mathParsers.subSumChain and optional(currParsers.currency) map { (expr, currency) ->
        if (currency == null) expr else CurrenciedExpression(expr, currency)
    }

    private val multiCurrencyExpressionParser by currParsers.currenciedSubSumChain
    private val allValidExpressionParsers by multiCurrencyExpressionParser or singleCurrencyExpressionParser or onlyCurrencyExpressionParser

    private val botInputParser by allValidExpressionParsers and apiConfig and additionalCurrenciesChain and decimalDigitsConfig map
            { (expr, apiConfig, keys, decimalDigits) ->
                BotInput(expr, keys.toSet(), apiConfig, decimalDigits)
            }

    override val tokens = tokenDict.allTokens

    override val tokenizer: Tokenizer by lazy { SingleLineTokenizer(DefaultTokenizer(tokens)) }

    override val rootParser = object : Parser<BotInput> {
        override fun tryParse(tokens: Sequence<TokenMatch>) =
            try {
                when (val result = botInputParser.tryParse(tokens)) {
                    // unwrap and keep the last added error as most adequate
                    is AlternativesFailure -> {
                        fun find(errors: List<ErrorResult>): ErrorResult {
                            val error = errors.last()
                            return if (error !is AlternativesFailure) error else find(error.errors)
                        }
                        find(result.errors)
                    }
                    else -> result
                }
            } catch (e: InvalidTextFoundException) {
                e.toErrorResult()
            }
    }

}