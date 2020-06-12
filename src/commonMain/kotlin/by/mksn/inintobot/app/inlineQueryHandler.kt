package by.mksn.inintobot.app

import by.mksn.inintobot.api.fetch.ApiRateFetcher
import by.mksn.inintobot.currency.CurrencyRateExchanger
import by.mksn.inintobot.expression.EvaluatedExpression
import by.mksn.inintobot.expression.ExpressionType
import by.mksn.inintobot.misc.ResourceLoader
import by.mksn.inintobot.misc.toFiniteBigDecimal
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.BotSuccessOutput
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.InlineQuery
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
suspend fun InlineQuery.handle(json: Json, httpClient: HttpClient, settings: UserSettings, botToken: String) {
    val outputs = if (query.isBlank()) {
        val api = ResourceLoader.apiConfigs(json)
            .first { it.name == settings.apiName }
        val currencies = ResourceLoader.currencies(json)
            .filterNot { api.unsupported.contains(it.code) }
        val apiBaseCurrency = currencies.first { it.code == api.base }

        val rateFetcher = ApiRateFetcher.forApi(api, httpClient, json)
        val rates = rateFetcher.fetch(currencies)
        val rateExchanger = CurrencyRateExchanger(apiBaseCurrency, rates)
        val telegramStrings = ResourceLoader.telegramStrings(json, settings.language)

        currencies.asSequence()
            .filter { settings.dashboardCurrencies.contains(it.code) }
            .map { EvaluatedExpression(1.toFiniteBigDecimal(), ExpressionType.ONE_UNIT, "1", it, listOf(it)) }
            .map { it to rateExchanger.exchangeAll(it.result, it.baseCurrency, currencies) }
            .map { (expression, exchanged) -> BotSuccessOutput(expression, exchanged, telegramStrings) }
            .toList().toTypedArray()
    } else {
        handleNonEmptyInput(query, json, httpClient, settings)
    }
    val sender = BotOutputSender(httpClient, botToken)
    sender.sendInlineQuery(id, *outputs)
}
