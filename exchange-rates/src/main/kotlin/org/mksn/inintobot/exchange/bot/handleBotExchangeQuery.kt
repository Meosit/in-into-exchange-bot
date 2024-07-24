package org.mksn.inintobot.exchange.bot

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.expression.*
import org.mksn.inintobot.common.misc.DEFAULT_DECIMAL_DIGITS
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.rate.*
import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.BotContext
import org.mksn.inintobot.exchange.grammar.BotInput
import org.mksn.inintobot.exchange.grammar.BotInputGrammar
import org.mksn.inintobot.exchange.output.*
import org.mksn.inintobot.exchange.output.strings.BotMessages
import java.lang.Exception
import java.math.BigDecimal
import java.time.LocalDate
import java.util.logging.Logger
import kotlin.math.min

private val logger = Logger.getLogger("handleBotExchangeQuery")

data class ExchangeAll(val exchanged: List<Exchange>, val errorMessage: String?)

fun handleBotExchangeQuery(
    isInline: Boolean,
    query: String,
    settings: UserSettings,
    context: BotContext
): Array<BotOutput> {
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

            if (expression is ConversionHistoryExpression) {
                return handleBotQueryHistoryRequest(expression, api, context, settings, isInline, decimalDigits)
            }

            val apiCurrencies = Currencies.filterNot { it.code in api.unsupported }
            val defaultCurrency = apiCurrencies.firstOrNull { it.code == settings.defaultCurrency } ?: api.base
            logger.info("Currencies (default: ${defaultCurrency.code}, api base: ${api.base.code}): ${apiCurrencies.joinToString { it.code }}")
            val rates = (if (onDate == null)
                context.rateStore.getLatest(api.name)
            else
                context.rateStore.getForDate(api.name, onDate, backtrackDays = 2))

            rates ?: return errorRatesUnavailable(settings, context, isInline, api)
            logger.info("Loaded rates for API ${rates.api.name} for date $onDate")

            val evaluator = ExpressionEvaluator(defaultCurrency, api.base, rates::exchange)
            val evaluated = try {
                evaluator.evaluate(expression)
            } catch (e: Exception) {
                val output = when(e) {
                    is ArithmeticException -> {
                        logger.info("Division by zero occurred")
                        context.statsStore.logExchangeErrorRequest("divisionByZero", isInline)
                        BotSimpleErrorOutput(BotMessages.errors.of(settings.language).divisionByZero)
                    }
                    is UnknownCurrencyException -> {
                        logger.info("Unknown currency ${e.currency.code} when evaluating expression for api ${api.name}")
                        context.statsStore.logExchangeErrorRequest("unsupportedCurrency", isInline)
                        BotSimpleErrorOutput(makeMissingCurrenciesMessage(listOf(e.currency), api, settings, rates.date.toString()))
                    }
                    is PercentPlacementException -> {
                        logger.info("Percent sign in invalid place for input '${query}' ")
                        context.statsStore.logExchangeErrorRequest("percentPlacement", isInline)
                        BotQueryErrorOutput(query, e.column, BotMessages.errors.of(settings.language).percentPlacement)
                    }
                    is PercentCurrencyException -> {
                        logger.info("Percent expression contains currency for input '${query}' ")
                        context.statsStore.logExchangeErrorRequest("percentCurrency", isInline)
                        BotQueryErrorOutput(query, e.column, BotMessages.errors.of(settings.language).percentCurrency)
                    }
                    else -> throw e
                }
                return arrayOf(output)
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
                return arrayOf(BotSimpleErrorOutput(makeMissingCurrenciesMessage(listOf(e.currency), api, settings, rates.date.toString())))
            } catch (e: MissingCurrenciesException) {
                logger.info("Missing currencies ${e.missing.joinToString { it.code }} when exchanging for api ${api.name}")
                context.statsStore.logExchangeErrorRequest("missingCurrencies", isInline)
                ExchangeAll(e.exchanges, makeMissingCurrenciesMessage(e.missing, api, settings, rates.date.toString()))
            }
            val queryStrings = BotMessages.query.of(settings.language)

            val nonDefaultApiName = when {
                onDate != null -> BotMessages.apiDisplayNames.of(settings.language).getValue(api.name)
                api.name == settings.apiName && evaluated.type != ExpressionType.ONE_UNIT -> null
                else -> BotMessages.apiDisplayNames.of(settings.language).getValue(api.name)
            }
            val nonDefaultApiTime = when {
                onDate != null -> rates.date.toString()
                api.name == settings.apiName && evaluated.type != ExpressionType.ONE_UNIT -> null
                else -> rates.timeString()
            }

            val output = BotQuerySuccessOutput(
                evaluated,
                exchanged,
                queryStrings,
                decimalDigits,
                nonDefaultApiName,
                nonDefaultApiTime,
            )
            val hourlyRateUSD = settings.hourlyRateUSD ?: BigDecimal.ZERO
            val outputWithTime = if (isInline || hourlyRateUSD <= BigDecimal.ZERO) output else {
                val maybeUsdExchange = exchanged.firstOrNull { it.currency.code == "USD" }
                if (maybeUsdExchange != null) {
                    val secondsSpent = ((maybeUsdExchange.value / hourlyRateUSD) * 3600.toFixedScaleBigDecimal()).toLong()
                    val timeString = BotMessages.timeUnitNames.of(settings.language).encodeToStringDuration(secondsSpent)
                    BotOutputWithMessage(output, "⏳$timeString")
                } else {
                    output
                }
            }
            val outputWithNotice = (errorMessage?.let { BotOutputWithMessage(outputWithTime, it) } ?: outputWithTime)
            val outputWithStaleMessage = when {
                onDate == null && rates.staleData() ->
                    BotStaleRatesOutput(outputWithNotice, api.name, rates.timeString(), settings.language)
                rates.date != (onDate ?: LocalDate.now()) -> {
                    val message = BotMessages.errors.of(settings.language).ratesOnDateNotExact
                        .format(onDate ?: LocalDate.now(), rates.date)
                    context.statsStore.logExchangeErrorRequest("ratesOnDateNotExact", isInline)
                    BotOutputWithMessage(outputWithNotice, message)
                }
                else -> outputWithNotice
            }

            context.statsStore.logExchangeRequestUsage(
                evaluated,
                api,
                outputCurrencies,
                isInline,
                historyRequest = onDate != null,
                customApi = api != defaultApi
            )
            return if (evaluated.type == ExpressionType.SINGLE_CURRENCY_EXPR) {
                arrayOf(outputWithStaleMessage, BotJustCalculateOutput(evaluated, queryStrings, decimalDigits))
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

fun BotInput.handleBotQueryHistoryRequest(
    expression: ConversionHistoryExpression,
    api: RateApi,
    context: BotContext,
    settings: UserSettings,
    isInline: Boolean,
    decimalDigits: Int,
    backtrackDays: Int = 7
): Array<BotOutput> {
    logger.info("Got history conversion query for ${expression.source} -> ${expression.target}")
    val evaluator = ExpressionEvaluator(expression.source, api.base) { value, _, _ -> value }
    val evaluated = evaluator.evaluate(expression)
    val (source, target) = evaluated.involvedCurrencies
    val date = onDate ?: LocalDate.now()
    val ratesHistory = date
        .let { context.rateStore.getHistoryForDate(api.name, it, backtrackDays) }
    if (ratesHistory.isEmpty()) {
        return errorRatesUnavailable(settings, context, isInline, api)
    } else {
        val noSource = ratesHistory.filter { source !in it.rates }.map { it.date }.takeIf { it.isNotEmpty() }
            ?.let { makeMissingCurrenciesMessage(listOf(source), api, settings, it.joinToString()) }
        val noTarget = ratesHistory.filter { target !in it.rates }.map { it.date }.takeIf { it.isNotEmpty() }
            ?.let { makeMissingCurrenciesMessage(listOf(target), api, settings, it.joinToString()) }
        if (noTarget != null || noSource != null) {
            return arrayOf(BotSimpleErrorOutput("${noSource ?: ""}\n${noTarget ?: ""}"))
        }
    }
    val conversions = ratesHistory
        .let { rates ->
            generateSequence(date) { it.minusDays(1) }
                .take(backtrackDays + 1)
                .map {
                    rates.firstOrNull { r -> r.date == it }
                        ?.let { r -> it to r.exchange(1.toFixedScaleBigDecimal(), source, target) }
                        ?: (it to null)
                }
                .map { (date, rate) ->
                    val previousRate = rates
                        .firstOrNull { r -> r.date <= date.minusDays(1) }
                        ?.exchange(evaluated.result, source, target)
                    HistoryConversion(date, rate, previousRate)
                }
                .take(backtrackDays)
                .toList()
        }

    context.statsStore.logExchangeRequestUsage(evaluated, api, evaluated.involvedCurrencies, isInline,
        historyRequest = onDate != null, customApi = api.name != settings.apiName)
    return arrayOf(
        BotQueryHistoryOutput(
            evaluated, settings.language, conversions, min(decimalDigits, 4),
            BotMessages.apiDisplayNames.of(settings.language).getValue(api.name), ratesHistory.first().date,
        ).let { if (date != ratesHistory.first().date) {
            val message = BotMessages.errors.of(settings.language).ratesOnDateNotExact
                .format(date, ratesHistory.first().date)
            context.statsStore.logExchangeErrorRequest("ratesOnDateNotExact", isInline)
            BotOutputWithMessage(it, message)
        } else it }
    )
}

private fun BotInput.errorRatesUnavailable(
    settings: UserSettings,
    context: BotContext,
    isInline: Boolean,
    api: RateApi
): Array<BotOutput> =
    arrayOf(BotSimpleErrorOutput(with(BotMessages.errors.of(settings.language)) {
        if (onDate != null) {
            context.statsStore.logExchangeErrorRequest("ratesOnDateUnavailable", isInline)
            ratesOnDateUnavailable.format(onDate, context.rateStore.historyStart(api.name))
        } else {
            context.statsStore.logExchangeErrorRequest("ratesUnavailable", isInline)
            ratesUnavailable
        }
    }))

private fun makeMissingCurrenciesMessage(
    currencies: List<Currency>,
    api: RateApi,
    settings: UserSettings,
    date: String
): String {
    val currencyCodes = currencies.map { it.code }
    logger.info("Unsupported currencies ${currencyCodes.joinToString()} for ${api.name} api used")
    val displayNames = BotMessages.apiDisplayNames.of(settings.language)
    val alternativeApiName = RateApis
        .filter { a -> a.name != api.name }
        .findLast { a -> currencies.all { c -> c.code !in a.unsupported } }
        ?.let { displayNames.getValue(it.name) }
    val messages = BotMessages.errors.of(settings.language)
    val apiDisplayName = displayNames.getValue(api.name)
    val message = if (alternativeApiName != null) {
        messages.unsupportedCurrencyWithAlternative.format(
            currencyCodes.joinToString(),
            apiDisplayName,
            date,
            alternativeApiName
        )
    } else {
        messages.unsupportedCurrency.format(currencyCodes.joinToString(), apiDisplayName, date)
    }
    return message
}