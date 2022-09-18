package org.mksn.inintobot.exchange.bot

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.misc.DEFAULT_DECIMAL_DIGITS
import org.mksn.inintobot.common.rate.*
import org.mksn.inintobot.common.store.ApiExchangeRateStore
import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.expression.ExpressionEvaluator
import org.mksn.inintobot.exchange.expression.ExpressionType
import org.mksn.inintobot.exchange.grammar.BotInputGrammar
import org.mksn.inintobot.exchange.grammar.alias.CurrencyAliasMatcher
import org.mksn.inintobot.exchange.grammar.alias.RateAliasMatcher
import org.mksn.inintobot.exchange.output.*
import org.mksn.inintobot.exchange.output.strings.BotMessages
import java.util.logging.Logger

private val logger = Logger.getLogger("handleBotQuery")
private val grammar = BotInputGrammar(CurrencyAliasMatcher, RateAliasMatcher)

data class ExchangeAll(val exchanged: List<Exchange>, val errorMessage: String?)

fun handleBotExchangeQuery(query: String, settings: UserSettings, rateStore: ApiExchangeRateStore): Array<BotOutput> {
    val defaultApi = RateApis[settings.apiName]

    when (val result = grammar.tryParseToEnd(query)) {
        is Parsed -> with(result.value) {
            val api = rateApi ?: defaultApi
            val decimalDigits = when {
                decimalDigits == null -> settings.decimalDigits
                decimalDigits >= DEFAULT_DECIMAL_DIGITS -> DEFAULT_DECIMAL_DIGITS
                decimalDigits <= 0 -> 0
                else -> decimalDigits
            }
            logger.info("New precision is $decimalDigits")
            logger.info("Chosen api is ${api.name} (default: ${defaultApi.name})")
            val apiCurrencies = Currencies.filterNot { it.code in api.unsupported }
            val apiBaseCurrency = api.base
            val defaultCurrency = apiCurrencies.firstOrNull { it.code == settings.defaultCurrency } ?: apiBaseCurrency

            logger.info("Currencies (default: ${defaultCurrency.code}, api base: ${apiBaseCurrency.code}): ${apiCurrencies.joinToString { it.code } }")
            val rates = rateStore.getLatest(api.name)
            if (rates == null) {
                logger.severe("Rates unavailable for API ${api.name}")
                return arrayOf(BotSimpleErrorOutput(BotMessages.errors.of(settings.language).ratesUnavailable))
            }

            val evaluator = ExpressionEvaluator(defaultCurrency, apiBaseCurrency, rates::exchange)
            val evaluated = try {
                evaluator.evaluate(expression)
            } catch (e: ArithmeticException) {
                logger.info("Division by zero occurred")
                val messages = BotMessages.errors.of(settings.language)
                return arrayOf(BotSimpleErrorOutput(messages.divisionByZero))
            } catch (e: UnknownCurrencyException) {
                val message = makeMissingCurrenciesMessage(listOf(e.currency), api, settings)
                return arrayOf(BotSimpleErrorOutput(message))
            }

            val outputCurrencies = apiCurrencies.filter {
                it in evaluated.involvedCurrencies || it.code in settings.outputCurrencies || it in additionalCurrencies
            }
            logger.info("Output currencies: ${outputCurrencies.joinToString { it.code }}")
            val (exchanged, errorMessage) = try {
                ExchangeAll(rates.exchangeAll(evaluated.result, evaluated.baseCurrency, outputCurrencies), null)
            } catch (e: UnknownCurrencyException) {
                val message = makeMissingCurrenciesMessage(listOf(e.currency), api, settings)
                return arrayOf(BotSimpleErrorOutput(message))
            } catch (e: MissingCurrenciesException) {
                ExchangeAll(e.exchanges, makeMissingCurrenciesMessage(e.missing, api, settings))
            }
            val queryStrings = BotMessages.query.of(settings.language)
            val nonDefaultApiName = if (api.name == settings.apiName && evaluated.type != ExpressionType.ONE_UNIT)
                null else BotMessages.apiDisplayNames.of(settings.language).getValue(api.name)
            val nonDefaultApiTime = if (api.name == settings.apiName && evaluated.type != ExpressionType.ONE_UNIT)
                null else ("${rates.date}  ${rates.time}")

            val output = BotSuccessOutput(evaluated, exchanged, queryStrings, decimalDigits, nonDefaultApiName, nonDefaultApiTime)
                .let { if (rates.staleData()) BotStaleRatesOutput(it, api.name, settings.language) else it }
                .let { if (errorMessage != null) BotOutputWithMessage(it, errorMessage) else it }
            return if (evaluated.type == ExpressionType.SINGLE_CURRENCY_EXPR) {
                arrayOf(output, BotJustCalculateOutput(evaluated, queryStrings))
            } else {
                arrayOf(output)
            }
        }
        is ErrorResult -> {
            val messages = BotMessages.errors.of(settings.language)
            val output = result.toBotOutput(query, messages)
            logger.info("Invalid query provided: ${output.errorMessage} (at ${output.errorPosition})")
            return arrayOf(output)
        }
    }
}

private fun makeMissingCurrenciesMessage(
    currencies: List<Currency>,
    api: RateApi,
    settings: UserSettings
): String {
    val currencyCodes = currencies.map { it.code }
    logger.info("Unsupported currencies ${currencyCodes.joinToString()} for ${api.name} api used")
    val displayNames = BotMessages.apiDisplayNames.of(settings.language)
    val alternativeApiName = RateApis
        .findLast { a -> currencies.all { c -> c.code !in a.unsupported } }
        ?.let { displayNames.getValue(it.name) }
    val messages = BotMessages.errors.of(settings.language)
    val apiDisplayName = displayNames.getValue(api.name)
    val message = if (alternativeApiName != null) {
        messages.unsupportedCurrencyWithAlternative.format(currencyCodes.joinToString(), apiDisplayName, alternativeApiName)
    } else {
        messages.unsupportedCurrency.format(currencyCodes.joinToString(), apiDisplayName)
    }
    return message
}