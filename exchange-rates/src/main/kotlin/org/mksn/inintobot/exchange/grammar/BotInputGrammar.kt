package org.mksn.inintobot.exchange.grammar


import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.lexer.Tokenizer
import com.github.h0tk3y.betterParse.parser.*
import org.mksn.inintobot.common.expression.Add
import org.mksn.inintobot.common.expression.Const
import org.mksn.inintobot.common.expression.ConversionHistoryExpression
import org.mksn.inintobot.common.expression.CurrenciedExpression
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.rate.RateApi
import org.mksn.inintobot.exchange.grammar.alias.CurrencyAliasMatcher
import org.mksn.inintobot.exchange.grammar.alias.RateAliasMatcher
import org.mksn.inintobot.exchange.grammar.parsers.CurrenciedMathParsers
import org.mksn.inintobot.exchange.grammar.parsers.SimpleMathParsers
import org.mksn.inintobot.exchange.grammar.parsers.TokenDictionary
import java.time.LocalDate
import java.time.format.DateTimeFormatter


object BotInputGrammar : Grammar<BotInput>() {
    private val universalTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val englishTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val slavicTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private val tokenDict = TokenDictionary(CurrencyAliasMatcher, RateAliasMatcher)

    private val mathParsers = SimpleMathParsers(tokenDict)
    private val currParsers = CurrenciedMathParsers(tokenDict, mathParsers, CurrencyAliasMatcher)

    private val currencyKeyPrefix = skip(tokenDict.exclamation or tokenDict.ampersand or tokenDict.inIntoUnion)
    private val currencyKey = skip(tokenDict.whitespace) and (currencyKeyPrefix and currParsers.currency) map { it }
    private val additionalCurrenciesChain by zeroOrMore(currencyKey)

    private val rateApiParser: Parser<RateApi> = tokenDict.apiAlias.map { RateAliasMatcher.match(it.text) }
    private val onDateParser: Parser<LocalDate> = tokenDict.dateKey.mapOrError(::InvalidDate) { match ->
        with(match.text.removeRange(0, match.text.indexOfFirst { it.isDigit() })) {
            this.toLongOrNull()
                ?.let { LocalDate.now().minusDays(it) }
                ?: runCatching { LocalDate.parse(this, universalTimeFormatter) }
                    .recoverCatching { LocalDate.parse(this, englishTimeFormatter) }
                    .recoverCatching { LocalDate.parse(this, slavicTimeFormatter) }
                    .getOrNull()
        }
    }

    private val dateConfig = optional(onDateParser)
    private val apiConfig = optional(skip(tokenDict.whitespace) and rateApiParser map { it })

    private val decimalDigitsNumber = tokenDict.number map { it.text.toIntOrNull() }
    private val decimalDigitsConfig =
        optional(skip(tokenDict.whitespace) and skip(tokenDict.hashtag) and decimalDigitsNumber map { it })

    private val onlyCurrencyExpressionParser by currParsers.currency map {
        CurrenciedExpression(Const(1.toFixedScaleBigDecimal()), it)
    }

    private val conversionHistoryExpressionParser by currParsers.currency and skip(tokenDict.inIntoUnion or tokenDict.divide) and currParsers.currency map
            { (from, to) -> ConversionHistoryExpression(from, to)  }

    private val singleCurrencyExpressionParser by mathParsers.subSumChain and optional(currParsers.currency) map { (expr, currency) ->
        if (currency == null) expr else CurrenciedExpression(expr, currency)
    }

    private val multiCurrencyExpressionParser by currParsers.currenciedSubSumChain
    private val allValidExpressionParsers by multiCurrencyExpressionParser or singleCurrencyExpressionParser or conversionHistoryExpressionParser or onlyCurrencyExpressionParser

    private val botInputParser by allValidExpressionParsers and additionalCurrenciesChain and apiConfig and dateConfig and decimalDigitsConfig map
            { (expr, keys, api, onDate, decimalDigits) -> BotInput(expr, keys.toSet(), api, onDate, decimalDigits) }

    private val botCurrencyDivisionInputParser by currParsers.currenciedDivisionSubSumChain and apiConfig and dateConfig and decimalDigitsConfig map
            { (expr, api, onDate, decimalDigits) -> BotInput(expr, setOf(), api, onDate, decimalDigits) }

    private val botRateAlertInputParser by mathParsers.plainNumber and conversionHistoryExpressionParser and apiConfig map
            { (valueExpr, historyExpr, api) -> BotInput(Add(valueExpr, historyExpr), setOf(), api, null, null) }

    override val tokens = tokenDict.allTokens

    override val tokenizer: Tokenizer by lazy { SingleLineTokenizer(DefaultTokenizer(tokens)) }

    private fun unwrapAlternativeFailure(result: ParseResult<BotInput>) = when (result) {
        // unwrap and keep the last added error as most adequate
        is AlternativesFailure -> {
            fun find(errors: List<ErrorResult>): ErrorResult {
                val error = errors.last()
                return if (error !is AlternativesFailure) error else find(error.errors)
            }
            find(result.errors)
        }

        is ErrorResult -> result
        else -> result
    }

    override val rootParser = object : Parser<BotInput> {
        override fun tryParse(tokens: TokenMatchesSequence, fromPosition: Int) =
            when (val result = unwrapAlternativeFailure(botInputParser.tryParse(tokens, fromPosition))) {
                is ErrorResult -> result
                is Parsed -> tokens.getNotIgnored(result.nextPosition)?.let {
                    // checking if it's an UnparsedRemainder case
                    // applying the currencied division parser only when the others failed
                    botCurrencyDivisionInputParser.tryParseToEnd(tokens, fromPosition) as? Parsed<BotInput>
                } ?: result

                else -> result
            }
    }

    fun tryParseRateAlertInput(input: String) = unwrapAlternativeFailure(
        botRateAlertInputParser.tryParseToEnd(tokenizer.tokenize(input), 0)
    )

}