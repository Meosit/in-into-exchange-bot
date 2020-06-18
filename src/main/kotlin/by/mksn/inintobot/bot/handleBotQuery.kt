package by.mksn.inintobot.bot

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.api.fetch.ApiRateFetcher
import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.currency.CurrencyRateExchanger
import by.mksn.inintobot.currency.UnknownCurrencyException
import by.mksn.inintobot.expression.ExpressionEvaluator
import by.mksn.inintobot.expression.ExpressionType
import by.mksn.inintobot.grammar.BotInputGrammar
import by.mksn.inintobot.misc.AliasMatcher
import by.mksn.inintobot.misc.DEFAULT_DECIMAL_DIGITS
import by.mksn.inintobot.misc.toStr
import by.mksn.inintobot.output.*
import by.mksn.inintobot.settings.UserSettings
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import org.slf4j.LoggerFactory
import java.math.BigDecimal

private val logger = LoggerFactory.getLogger("handleQuery")

suspend fun handleBotQuery(query: String, settings: UserSettings): Array<BotOutput> {
    val currencies = AppContext.supportedCurrencies
    val apis = AppContext.supportedApis
    val defaultApi = apis.first { it.name == settings.apiName }

    val currencyMatcher = AliasMatcher(currencies)
    val apiMatcher = AliasMatcher(apis)
    val grammar = BotInputGrammar(currencyMatcher, apiMatcher)
    when (val result = grammar.tryParseToEnd(query)) {
        is Parsed -> with(result.value) {
            val api = rateApi ?: defaultApi
            val decimalDigits = if (decimalDigits != null && decimalDigits <= DEFAULT_DECIMAL_DIGITS)
                decimalDigits else settings.decimalDigits
            if (api.name != defaultApi.name) {
                logger.info("Chosen api is ${api.name} (default: ${defaultApi.name})", api.name)
            }
            val apiCurrencies = currencies.filterNot { api.unsupported.contains(it.code) }
            val apiBaseCurrency = apiCurrencies.first { it.code == api.base }
            val defaultCurrency = apiCurrencies
                .firstOrNull { it.code == settings.defaultCurrency } ?: apiBaseCurrency

            logger.info("Currencies (default: ${defaultCurrency.code}, api base: ${apiBaseCurrency.code}): ${apiCurrencies.joinToString { it.code }}")

            val rates = loadRates(api, apiCurrencies)
            logger.info("Loaded rates: ${rates.entries.joinToString { "${it.key.code}: ${it.value.toStr()}" }}")
            val rateExchanger = CurrencyRateExchanger(apiBaseCurrency, rates)

            val evaluator = ExpressionEvaluator(defaultCurrency, apiBaseCurrency, rateExchanger::exchange)
            val evaluated = try {
                evaluator.evaluate(expression)
            } catch (e: ArithmeticException) {
                logger.info("Division by zero occurred")
                val messages = AppContext.errorMessages.of(settings.language)
                return arrayOf(BotErrorOutput(query, 1, messages.divisionByZero))
            } catch (e: UnknownCurrencyException) {
                logger.info("Unsupported currency ${e.currency.code} for ${api.name} api used")
                val messages = AppContext.errorMessages.of(settings.language)
                val apiDisplayName = AppContext.apiNames.of(settings.language).getValue(api.name)
                return arrayOf(
                    BotErrorOutput(
                        query, 1, messages.unsupportedCurrency.format(e.currency.code, apiDisplayName)
                    )
                )
            }

            val outputCurrencies = apiCurrencies.filter {
                evaluated.involvedCurrencies.contains(it)
                        || settings.outputCurrencies.contains(it.code)
                        || additionalCurrencies.contains(it)
            }
            logger.info("Output currencies: ${outputCurrencies.joinToString { it.code }}")

            val exchanged = try {
                rateExchanger.exchangeAll(evaluated.result, evaluated.baseCurrency, outputCurrencies)
            } catch (e: UnknownCurrencyException) {
                logger.info("Unsupported currency ${e.currency.code} for ${api.name} api used")
                val messages = AppContext.errorMessages.of(settings.language)
                val apiDisplayName = AppContext.apiNames.of(settings.language).getValue(api.name)
                return arrayOf(
                    BotErrorOutput(
                        query, 1, messages.unsupportedCurrency.format(e.currency.code, apiDisplayName)
                    )
                )
            }
            val queryStrings = AppContext.queryStrings.of(settings.language)
            val nonDefaultApiName = if (api.name == settings.apiName) null else
                AppContext.apiNames.of(settings.language).getValue(api.name)

            val output = BotSuccessOutput(evaluated, exchanged, queryStrings, decimalDigits, nonDefaultApiName)
            return if (evaluated.type == ExpressionType.SINGLE_CURRENCY_EXPR) {
                arrayOf(output, BotJustCalculateOutput(evaluated, queryStrings))
            } else {
                arrayOf(output)
            }
        }
        is ErrorResult -> {
            val output = result.toBotOutput(query, AppContext.errorMessages.of(settings.language))
            logger.info("Invalid query provided: ${output.errorMessage} (at ${output.errorPosition})")
            return arrayOf(output)
        }
    }
}

// TODO use storage and update rates separately
suspend fun loadRates(api: RateApi, currencies: List<Currency>): Map<Currency, BigDecimal> =
    ApiRateFetcher.forApi(api, AppContext.httpClient, AppContext.json).fetch(currencies)