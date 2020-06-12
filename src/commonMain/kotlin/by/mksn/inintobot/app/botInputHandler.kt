package by.mksn.inintobot.app

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.api.fetch.ApiRateFetcher
import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.currency.CurrencyRateExchanger
import by.mksn.inintobot.expression.ExpressionEvaluator
import by.mksn.inintobot.expression.ExpressionType
import by.mksn.inintobot.grammar.BotInputGrammar
import by.mksn.inintobot.misc.AliasMatcher
import by.mksn.inintobot.misc.ResourceLoader
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
    val apis = ResourceLoader.apiConfigs(json)
    val defaultApi = apis.first { it.name == settings.apiName }

    val currencies = ResourceLoader.currencies(json)
        .filterNot { defaultApi.unsupported.contains(it.code) }

    val currencyMatcher = AliasMatcher(currencies)
    val apiMatcher = AliasMatcher(apis, default = defaultApi)
    val grammar = BotInputGrammar(currencyMatcher, apiMatcher)
    when (val result = grammar.tryParseToEnd(query)) {
        is Parsed -> with(result.value) {
            val apiBaseCurrency = currencies.first { it.code == rateApi.base }
            val defaultCurrency = currencies.first { it.code == settings.defaultCurrency }

            val rates = loadRates(rateApi, currencies, httpClient, json)
            val rateExchanger = CurrencyRateExchanger(apiBaseCurrency, rates)

            val evaluator = ExpressionEvaluator(defaultCurrency, apiBaseCurrency, rateExchanger::exchange)
            val evaluated = try {
                evaluator.evaluate(expression)
            } catch (e: ArithmeticException) {
                val messages = ResourceLoader.errorMessages(json, settings.language)
                return arrayOf(BotErrorOutput(query, 1, messages.divisionByZero))
            }

            val outputCurrencies = currencies.filter {
                evaluated.involvedCurrencies.contains(it)
                        || settings.outputCurrencies.contains(it.code)
                        || additionalCurrencies.contains(it)
            }
            val exchanged = rateExchanger.exchangeAll(evaluated.result, evaluated.baseCurrency, outputCurrencies)
            val telegramStrings = ResourceLoader.telegramStrings(json, settings.language)
            val nonDefaultApiName = if (rateApi.name == settings.apiName) null else rateApi.name

            val output = BotSuccessOutput(evaluated, exchanged, telegramStrings, nonDefaultApiName)
            return if (evaluated.type == ExpressionType.SINGLE_CURRENCY_EXPR) {
                arrayOf(output, BotJustCalculateOutput(evaluated, telegramStrings))
            } else {
                arrayOf(output)
            }
        }
        is ErrorResult -> {
            val messages = ResourceLoader.errorMessages(json, settings.language)
            val output = result.toBotOutput(query, messages)
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