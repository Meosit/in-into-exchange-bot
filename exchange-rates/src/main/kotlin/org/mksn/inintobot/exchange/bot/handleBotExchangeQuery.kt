package org.mksn.inintobot.exchange.bot

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.expression.ExpressionEvaluator
import org.mksn.inintobot.common.expression.ExpressionType
import org.mksn.inintobot.common.misc.DEFAULT_DECIMAL_DIGITS
import org.mksn.inintobot.common.rate.*
import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.BotContext
import org.mksn.inintobot.exchange.grammar.BotInputGrammar
import org.mksn.inintobot.exchange.output.*
import org.mksn.inintobot.exchange.output.strings.BotMessages
import java.util.logging.Logger

private val logger = Logger.getLogger("handleBotQuery")

data class ExchangeAll(val exchanged: List<Exchange>, val errorMessage: String?)

fun handleBotExchangeQuery(isInline: Boolean, query: String, settings: UserSettings, context: BotContext): Array<BotOutput> {
    val defaultApi = RateApis[settings.apiName]

    when (val result = BotInputGrammar.tryParseToEnd(query)) {
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

            logger.info("Currencies (default: ${defaultCurrency.code}, api base: ${apiBaseCurrency.code}): ${apiCurrencies.joinToString { it.code }}")
            val rates = onDate
                ?.let { context.rateStore.getForDate(api.name, it, backtrackDays = 2) }
                ?: context.rateStore.getLatest(api.name)
                ?: return arrayOf(BotSimpleErrorOutput(with(BotMessages.errors.of(settings.language)) {
                    if (onDate != null) {
                        context.statsStore.logExchangeErrorRequest("ratesOnDateUnavailable", isInline)
                        ratesOnDateUnavailable.format(onDate, context.rateStore.historyStart(api.name))
                    } else {
                        context.statsStore.logExchangeErrorRequest("ratesUnavailable", isInline)
                        ratesUnavailable
                    }
                }))
            logger.info("Loaded rates for API ${rates.api.name} for date $onDate")

            val evaluator = ExpressionEvaluator(defaultCurrency, apiBaseCurrency, rates::exchange)
            val evaluated = try {
                evaluator.evaluate(expression)
            } catch (e: ArithmeticException) {
                logger.info("Division by zero occurred")
                val messages = BotMessages.errors.of(settings.language)
                context.statsStore.logExchangeErrorRequest("divisionByZero", isInline)
                return arrayOf(BotSimpleErrorOutput(messages.divisionByZero))
            } catch (e: UnknownCurrencyException) {
                logger.info("Unknown currency ${e.currency.code} when evaluating expression for api ${api.name}")
                val message = makeMissingCurrenciesMessage(listOf(e.currency), api, settings)
                context.statsStore.logExchangeErrorRequest("unsupportedCurrency", isInline)
                return arrayOf(BotSimpleErrorOutput(message))
            }

            val outputCurrencies = apiCurrencies.filter {
                it in evaluated.involvedCurrencies || it.code in settings.outputCurrencies || it in additionalCurrencies
            }
            logger.info("Output currencies: ${outputCurrencies.joinToString { it.code }}")
            val (exchanged, errorMessage) = try {
                ExchangeAll(rates.exchangeAll(evaluated.result, evaluated.baseCurrency, outputCurrencies), null)
            } catch (e: UnknownCurrencyException) {
                logger.info("Unknown currency ${e.currency.code} when exchanging for api ${api.name}")
                context.statsStore.logExchangeErrorRequest("unsupportedCurrency", isInline)
                return arrayOf(BotSimpleErrorOutput(makeMissingCurrenciesMessage(listOf(e.currency), api, settings)))
            } catch (e: MissingCurrenciesException) {
                logger.info("Missing currencies ${e.missing.joinToString { it.code } } when exchanging for api ${api.name}")
                context.statsStore.logExchangeErrorRequest("missingCurrencies", isInline)
                ExchangeAll(e.exchanges, makeMissingCurrenciesMessage(e.missing, api, settings))
            }
            val queryStrings = BotMessages.query.of(settings.language)

            val nonDefaultApiName = when {
                api.name == settings.apiName && evaluated.type != ExpressionType.ONE_UNIT -> null
                else -> BotMessages.apiDisplayNames.of(settings.language).getValue(api.name)
            }
            val nonDefaultApiTime = when {
                onDate != null -> rates.timeString()
                api.name == settings.apiName && evaluated.type != ExpressionType.ONE_UNIT -> null
                else -> rates.timeString()
            }

            val output = BotSuccessOutput(
                evaluated,
                exchanged,
                queryStrings,
                decimalDigits,
                nonDefaultApiName,
                nonDefaultApiTime
            )
            val outputWithNotice = (errorMessage?.let { BotOutputWithMessage(output, it) } ?: output)
            val outputWithStaleMessage = if (onDate == null && rates.staleData())
                BotStaleRatesOutput(outputWithNotice, api.name, rates.timeString(), settings.language)
            else outputWithNotice

            context.statsStore.logExchangeRequestUsage(evaluated, api, outputCurrencies, isInline, historyRequest = onDate != null)
            return if (evaluated.type == ExpressionType.SINGLE_CURRENCY_EXPR) {
                arrayOf(outputWithStaleMessage, BotJustCalculateOutput(evaluated, queryStrings))
            } else {
                arrayOf(outputWithStaleMessage)
            }
        }

        is ErrorResult -> {
            val messages = BotMessages.errors.of(settings.language)
            val output = result.toBotOutput(query, messages)
            logger.info("Invalid query provided: ${output.errorMessage} (at ${output.errorPosition})")
            context.statsStore.logExchangeErrorRequest("parse${result::class.simpleName}", isInline)
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
        messages.unsupportedCurrencyWithAlternative.format(
            currencyCodes.joinToString(),
            apiDisplayName,
            alternativeApiName
        )
    } else {
        messages.unsupportedCurrency.format(currencyCodes.joinToString(), apiDisplayName)
    }
    return message
}