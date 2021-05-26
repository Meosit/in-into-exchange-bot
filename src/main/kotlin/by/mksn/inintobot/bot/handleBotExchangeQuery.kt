package by.mksn.inintobot.bot

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.currency.CurrencyRateExchanger
import by.mksn.inintobot.currency.UnknownCurrencyException
import by.mksn.inintobot.expression.ExpressionEvaluator
import by.mksn.inintobot.expression.ExpressionType
import by.mksn.inintobot.grammar.BotInputGrammar
import by.mksn.inintobot.misc.AliasMatcher
import by.mksn.inintobot.misc.DEFAULT_DECIMAL_DIGITS
import by.mksn.inintobot.output.*
import by.mksn.inintobot.settings.UserSettings
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("handleBotQuery")

val botOutputRegex = "([\uD83C-\uDBFF\uDC00-\uDFFF]{2}[A-Z]{3} {2}\\d+(\\.\\d+)? ?)+".toRegex()
val botJustCalculateReminder = "\\s?=\\s\\d+(\\.\\d+)?".toRegex()

fun handleBotExchangeQuery(query: String, settings: UserSettings): Array<BotOutput> {
    val currencies = AppContext.supportedCurrencies
    val apis = AppContext.supportedApis
    val defaultApi = apis.first { it.name == settings.apiName }

    val currencyMatcher = AliasMatcher(currencies)
    val apiMatcher = AliasMatcher(apis)
    val grammar = BotInputGrammar(currencyMatcher, apiMatcher)
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
            logger.info("Chosen api is ${api.name} (default: ${defaultApi.name})", api.name)
            val apiCurrencies = currencies.filterNot { it.code in api.unsupported }
            val apiBaseCurrency = apiCurrencies.first { it.code == api.base }
            val defaultCurrency = apiCurrencies
                .firstOrNull { it.code == settings.defaultCurrency } ?: apiBaseCurrency

            logger.info("Currencies (default: ${defaultCurrency.code}, api base: ${apiBaseCurrency.code}): ${apiCurrencies.joinToString { it.code }}")
            val rates = AppContext.exchangeRates.of(api)
            if (rates == null) {
                logger.error("Rates unavailable for API ${api.name}")
                return arrayOf(BotSimpleErrorOutput(AppContext.errorMessages.of(settings.language).ratesUnavailable))
            }
            val isStaleRates = AppContext.exchangeRates.isStale(api)

            val rateExchanger = CurrencyRateExchanger(apiBaseCurrency, rates)

            val evaluator = ExpressionEvaluator(defaultCurrency, apiBaseCurrency, rateExchanger::exchange)
            val evaluated = try {
                evaluator.evaluate(expression)
            } catch (e: ArithmeticException) {
                logger.info("Division by zero occurred")
                val messages = AppContext.errorMessages.of(settings.language)
                return arrayOf(BotSimpleErrorOutput(messages.divisionByZero))
            } catch (e: UnknownCurrencyException) {
                val message = makeUnsupportedCurrencyMessage(e, api, settings)
                return arrayOf(BotSimpleErrorOutput(message))
            }

            val outputCurrencies = apiCurrencies.filter {
                it in evaluated.involvedCurrencies || it.code in settings.outputCurrencies || it in additionalCurrencies
            }
            logger.info("Output currencies: ${outputCurrencies.joinToString { it.code }}")

            val exchanged = try {
                rateExchanger.exchangeAll(evaluated.result, evaluated.baseCurrency, outputCurrencies)
            } catch (e: UnknownCurrencyException) {
                val message = makeUnsupportedCurrencyMessage(e, api, settings)
                return arrayOf(BotSimpleErrorOutput(message))
            }
            val queryStrings = AppContext.queryStrings.of(settings.language)
            val nonDefaultApiName = if (api.name == settings.apiName && evaluated.type != ExpressionType.ONE_UNIT)
                null else AppContext.apiDisplayNames.of(settings.language).getValue(api.name)

            val output = BotSuccessOutput(evaluated, exchanged, queryStrings, decimalDigits, nonDefaultApiName).let {
                if (isStaleRates) BotStaleRatesOutput(it, api.name, settings.language) else it
            }
            return if (evaluated.type == ExpressionType.SINGLE_CURRENCY_EXPR) {
                arrayOf(output, BotJustCalculateOutput(evaluated, queryStrings))
            } else {
                arrayOf(output)
            }
        }
        is ErrorResult -> {
            val messages = AppContext.errorMessages.of(settings.language)
            val output = result.toBotOutput(query, messages)
            val botOutputAsInput = botOutputRegex.containsMatchIn(output.rawInput)
            val botJustCalculateAsInput = output.errorMessage == messages.unparsedReminder
                    && botJustCalculateReminder.matches(output.rawInput.substring(output.errorPosition - 1))
            return if (botOutputAsInput || botJustCalculateAsInput) {
                logger.info("Bot inline query output used as chat input")
                arrayOf(BotSimpleErrorOutput(messages.inlineOutputAsChatInput))
            } else{
                logger.info("Invalid query provided: ${output.errorMessage} (at ${output.errorPosition})")
                arrayOf(output)
            }
        }
    }
}

private fun makeUnsupportedCurrencyMessage(
    e: UnknownCurrencyException,
    api: RateApi,
    settings: UserSettings
): String {
    logger.info("Unsupported currency ${e.currency.code} for ${api.name} api used")
    val displayNames = AppContext.apiDisplayNames.of(settings.language)
    val alternativeApiName = AppContext.supportedApis
        .findLast { !it.unsupported.contains(e.currency.code) }
        ?.let { displayNames.getValue(it.name) }
    val messages = AppContext.errorMessages.of(settings.language)
    val apiDisplayName = displayNames.getValue(api.name)
    val message = if (alternativeApiName != null) {
        messages.unsupportedCurrencyWithAlternative.format(e.currency.code, apiDisplayName, alternativeApiName)
    } else {
        messages.unsupportedCurrency.format(e.currency.code, apiDisplayName)
    }
    return message
}