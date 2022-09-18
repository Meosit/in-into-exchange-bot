package org.mksn.inintobot.exchange.grammar


import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.lexer.Tokenizer
import com.github.h0tk3y.betterParse.parser.*
import org.mksn.inintobot.currency.Currency
import org.mksn.inintobot.exchange.expression.Const
import org.mksn.inintobot.exchange.expression.CurrenciedExpression
import org.mksn.inintobot.exchange.grammar.alias.AliasMatcher
import org.mksn.inintobot.exchange.grammar.parsers.CurrenciedMathParsers
import org.mksn.inintobot.exchange.grammar.parsers.SimpleMathParsers
import org.mksn.inintobot.exchange.grammar.parsers.TokenDictionary
import org.mksn.inintobot.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.rates.RateApi


class BotInputGrammar(
    currencyAliasMatcher: AliasMatcher<Currency>,
    apiAliasMatcher: AliasMatcher<RateApi>,
) : Grammar<BotInput>() {

    private val tokenDict = TokenDictionary(currencyAliasMatcher, apiAliasMatcher)

    private val mathParsers = SimpleMathParsers(tokenDict)
    private val currParsers = CurrenciedMathParsers(tokenDict, mathParsers, currencyAliasMatcher)

    private val currencyKeyPrefix = skip(tokenDict.exclamation or tokenDict.ampersand or tokenDict.inIntoUnion)
    private val currencyKey = skip(tokenDict.whitespace) and (currencyKeyPrefix and currParsers.currency) map { it }
    private val additionalCurrenciesChain by zeroOrMore(currencyKey)

    private val rateApiParser: Parser<RateApi> = tokenDict.apiAlias.map { apiAliasMatcher.match(it.text) }

    private val apiConfig = skip(tokenDict.whitespace) and rateApiParser map { it }

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

    private val botInputParser by allValidExpressionParsers and optional(apiConfig) and additionalCurrenciesChain and decimalDigitsConfig map
            { (expr, api, keys, decimalDigits) -> BotInput(expr, keys.toSet(), api, decimalDigits) }

    private val botCurrencyDivisionInputParser by currParsers.currenciedDivisionSubSumChain and optional(apiConfig) and decimalDigitsConfig map
            { (expr, api, decimalDigits) -> BotInput(expr, setOf(), api, decimalDigits) }

    override val tokens = tokenDict.allTokens

    override val tokenizer: Tokenizer by lazy { SingleLineTokenizer(DefaultTokenizer(tokens)) }

    override val rootParser = object : Parser<BotInput> {
        override fun tryParse(tokens: TokenMatchesSequence, fromPosition: Int) =
            when (val result = botInputParser.tryParse(tokens, fromPosition)) {
                // unwrap and keep the last added error as most adequate
                is AlternativesFailure -> {
                    fun find(errors: List<ErrorResult>): ErrorResult {
                        val error = errors.last()
                        return if (error !is AlternativesFailure) error else find(error.errors)
                    }
                    find(result.errors)
                }

                is ErrorResult -> result
                is Parsed -> tokens.getNotIgnored(result.nextPosition)?.let {
                    // applying the currencied division parser only when the others failed
                    botCurrencyDivisionInputParser.tryParseToEnd(tokens, fromPosition) as? Parsed<BotInput>
                } ?: result

                else -> result
            }
    }

}