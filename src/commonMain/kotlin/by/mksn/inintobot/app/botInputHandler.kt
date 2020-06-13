package by.mksn.inintobot.app

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.api.fetch.ApiRateFetcher
import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.currency.CurrencyRateExchanger
import by.mksn.inintobot.currency.UnknownCurrencyException
import by.mksn.inintobot.expression.ExpressionEvaluator
import by.mksn.inintobot.expression.ExpressionType
import by.mksn.inintobot.grammar.BotInputGrammar
import by.mksn.inintobot.misc.*
import by.mksn.inintobot.output.*
import by.mksn.inintobot.settings.UserSettings
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json


@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
suspend fun handleNonEmptyInput(
    query: String,
    json: Json,
    httpClient: HttpClient,
    settings: UserSettings
): Array<BotOutput> {
    val currencies = ResourceLoader.currencies(json)
    val apis = ResourceLoader.apiConfigs(json)
    val defaultApi = apis.first { it.name == settings.apiName }

    val currencyMatcher = AliasMatcher(currencies)
    val apiMatcher = AliasMatcher(apis)
    val grammar = BotInputGrammar(currencyMatcher, apiMatcher)
    when (val result = grammar.tryParseToEnd(query)) {
        is Parsed -> with(result.value) {
            val api = rateApi ?: defaultApi
            val decimalDigits = if (decimalDigits != null && decimalDigits <= DEFAULT_DECIMAL_PRECISION)
                decimalDigits else settings.decimalDigits
            if (api.name != defaultApi.name) {
                println("Chosen api is ${api.name} (default: ${defaultApi.name})")
            }
            val apiCurrencies = currencies.filterNot { api.unsupported.contains(it.code) }
            val apiBaseCurrency = apiCurrencies.first { it.code == api.base }
            val defaultCurrency = apiCurrencies
                .firstOrNull { it.code == settings.defaultCurrency } ?: apiBaseCurrency

            println("Currencies (default: ${defaultCurrency.code}, api base: ${apiBaseCurrency.code}): ${apiCurrencies.joinToString { it.code }}")

            val rates = loadRates(api, apiCurrencies, httpClient, json)
            println("Loaded rates: ${rates.entries.joinToString { "${it.key.code}: ${it.value.toStr()}" }}")
            val rateExchanger = CurrencyRateExchanger(apiBaseCurrency, rates)

            val evaluator = ExpressionEvaluator(defaultCurrency, apiBaseCurrency, rateExchanger::exchange)
            val evaluated = try {
                evaluator.evaluate(expression)
            } catch (e: ArithmeticException) {
                println("Division by zero occurred")
                val messages = ResourceLoader.errorMessages(json, settings.language)
                return arrayOf(BotErrorOutput(query, 1, messages.divisionByZero))
            } catch (e: UnknownCurrencyException) {
                println("Unsupported currency ${e.currency.code} for ${api.name} api used")
                val messages = ResourceLoader.errorMessages(json, settings.language)
                val apiDisplayName = ResourceLoader.apiNames(json, settings.language).getValue(api.name)
                return arrayOf(
                    BotErrorOutput(
                        query, 1, messages.unsupportedCurrency
                            .format(e.currency.code, apiDisplayName)
                    )
                )
            }

            val outputCurrencies = apiCurrencies.filter {
                evaluated.involvedCurrencies.contains(it)
                        || settings.outputCurrencies.contains(it.code)
                        || additionalCurrencies.contains(it)
            }
            println("Output currencies: ${outputCurrencies.joinToString { it.code }}")

            val exchanged = try {
                rateExchanger.exchangeAll(evaluated.result, evaluated.baseCurrency, outputCurrencies)
            } catch (e: UnknownCurrencyException) {
                println("Unsupported currency ${e.currency.code} for ${api.name} api used")
                val messages = ResourceLoader.errorMessages(json, settings.language)
                val apiDisplayName = ResourceLoader.apiNames(json, settings.language).getValue(api.name)
                return arrayOf(
                    BotErrorOutput(
                        query, 1, messages.unsupportedCurrency
                            .format(e.currency.code, apiDisplayName)
                    )
                )
            }
            val telegramStrings = ResourceLoader.telegramStrings(json, settings.language)
            val nonDefaultApiName = if (api.name == settings.apiName) null else
                ResourceLoader.apiNames(json, settings.language).getValue(api.name)

            val output = BotSuccessOutput(evaluated, exchanged, telegramStrings, decimalDigits, nonDefaultApiName)
            return if (evaluated.type == ExpressionType.SINGLE_CURRENCY_EXPR) {
                arrayOf(output, BotJustCalculateOutput(evaluated, telegramStrings))
            } else {
                arrayOf(output)
            }
        }
        is ErrorResult -> {
            val messages = ResourceLoader.errorMessages(json, settings.language)
            val output = result.toBotOutput(query, messages)
            println("Invalid query provided: ${output.errorMessage} (at ${output.errorPosition})")
            return arrayOf(output)
        }
    }
}

// TODO use storage and update rates separately
@ExperimentalUnsignedTypes
suspend fun loadRates(
    api: RateApi,
    currencies: List<Currency>,
    httpClient: HttpClient,
    json: Json
): Map<Currency, BigDecimal> =
    ApiRateFetcher.forApi(api, httpClient, json).fetch(currencies)