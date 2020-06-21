package by.mksn.inintobot.bot

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.currency.CurrencyRateExchanger
import by.mksn.inintobot.expression.EvaluatedExpression
import by.mksn.inintobot.expression.ExpressionType
import by.mksn.inintobot.misc.toFixedScaleBigDecimal
import by.mksn.inintobot.output.BotDeprecatedOutput
import by.mksn.inintobot.output.BotOutputSender
import by.mksn.inintobot.output.BotSimpleErrorOutput
import by.mksn.inintobot.output.BotSuccessOutput
import by.mksn.inintobot.settings.UserSettings
import by.mksn.inintobot.telegram.InlineQuery
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("handleInlineQuery")

suspend fun InlineQuery.handle(settings: UserSettings, sender: BotOutputSender, deprecatedBot: Boolean) {
    val outputs = if (query.isBlank()) {
        logger.info("Handling dashboard inline query")
        val api = AppContext.supportedApis
            .first { it.name == settings.apiName }
        val currencies = AppContext.supportedCurrencies
            .filterNot { api.unsupported.contains(it.code) }
        val apiBaseCurrency = currencies.first { it.code == api.base }
        logger.info("Api is ${api.name} (base: ${apiBaseCurrency.code}), currencies: ${currencies.joinToString { it.code }}")

        val rates = AppContext.exchangeRates.of(api)
        val isStaleRates = AppContext.exchangeRates.isStale(api)
        if (rates != null) {
            val rateExchanger = CurrencyRateExchanger(apiBaseCurrency, rates)
            val queryStrings = AppContext.queryStrings.of(settings.language)
            val apiDisplayNames = AppContext.apiDisplayNames.of(settings.language)
            currencies.asSequence()
                .filter { settings.dashboardCurrencies.contains(it.code) }
                .map { EvaluatedExpression(1.toFixedScaleBigDecimal(), ExpressionType.ONE_UNIT, "1", it, listOf(it)) }
                .map { it to rateExchanger.exchangeAll(it.result, it.baseCurrency, currencies) }
                .map { (expression, exchanged) ->
                    BotSuccessOutput(expression, exchanged, queryStrings, settings.decimalDigits, apiDisplayNames[settings.apiName])
                }
                .map { if (isStaleRates) it else it }
                .toList().toTypedArray()
        } else {
            logger.error("Rates unavailable for API ${api.name}")
            arrayOf(BotSimpleErrorOutput(AppContext.errorMessages.of(settings.language).ratesUnavailable))
        }
    } else {
        logger.info("Handling inline query '$query'")
        handleBotExchangeQuery(query, settings)
    }

    val formattedOutputs = if (deprecatedBot)
        outputs.map { BotDeprecatedOutput(it, settings.language) }.toTypedArray() else outputs
    sender.sendInlineQuery(id, *formattedOutputs)
}
