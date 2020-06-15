package by.mksn.inintobot.app

import by.mksn.inintobot.currency.CurrencyRateExchanger
import by.mksn.inintobot.expression.EvaluatedExpression
import by.mksn.inintobot.expression.ExpressionType
import by.mksn.inintobot.misc.ResourceLoader
import by.mksn.inintobot.misc.toFixedScaleBigDecimal
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.BotSuccessOutput
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.InlineQuery
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

suspend fun InlineQuery.handle(json: Json, httpClient: HttpClient, settings: UserSettings, botToken: String) {
    val outputs = if (query.isBlank()) {
        println("Handling dashboard inline query")
        val api = ResourceLoader.apiConfigs(json)
            .first { it.name == settings.apiName }
        val currencies = ResourceLoader.currencies(json)
            .filterNot { api.unsupported.contains(it.code) }
        val apiBaseCurrency = currencies.first { it.code == api.base }
        println("Api is ${api.name} (base: ${apiBaseCurrency.code}), currencies: ${currencies.joinToString { it.code }}")

        val rates = loadRates(api, currencies, httpClient, json)
        val rateExchanger = CurrencyRateExchanger(apiBaseCurrency, rates)
        val telegramStrings = ResourceLoader.telegramStrings(json, settings.language)

        currencies.asSequence()
            .filter { settings.dashboardCurrencies.contains(it.code) }
            .map { EvaluatedExpression(1.toFixedScaleBigDecimal(), ExpressionType.ONE_UNIT, "1", it, listOf(it)) }
            .map { it to rateExchanger.exchangeAll(it.result, it.baseCurrency, currencies) }
            .map { (expression, exchanged) -> BotSuccessOutput(expression, exchanged, telegramStrings, settings.decimalDigits) }
            .toList().toTypedArray()
    } else {
        println("Handling inline query '$query'")
        handleNonEmptyInput(query, json, httpClient, settings)
    }
    val sender = BotOutputSender(httpClient, botToken)
    sender.sendInlineQuery(id, *outputs)
}
