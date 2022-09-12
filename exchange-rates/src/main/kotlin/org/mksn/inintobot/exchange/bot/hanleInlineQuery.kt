package org.mksn.inintobot.exchange.bot

import org.mksn.inintobot.currency.Currencies
import org.mksn.inintobot.currency.Currency
import org.mksn.inintobot.exchange.expression.EvaluatedExpression
import org.mksn.inintobot.exchange.expression.ExpressionType
import org.mksn.inintobot.exchange.output.BotOutputSender
import org.mksn.inintobot.exchange.output.BotSimpleErrorOutput
import org.mksn.inintobot.exchange.output.BotStaleRatesOutput
import org.mksn.inintobot.exchange.output.BotSuccessOutput
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.rateStore
import org.mksn.inintobot.exchange.settings.UserSettings
import org.mksn.inintobot.exchange.telegram.InlineQuery
import org.mksn.inintobot.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.rates.ApiExchangeRates
import org.mksn.inintobot.rates.MissingCurrenciesException
import org.mksn.inintobot.rates.RateApis
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("handleInlineQuery")

suspend fun InlineQuery.handle(settings: UserSettings, sender: BotOutputSender) {
    val outputs = if (query.isBlank()) {
        logger.info("Handling dashboard inline query")
        val api = RateApis[settings.apiName]
        val currencies = Currencies.filterNot { it.code in api.unsupported }
        val apiBaseCurrency = currencies.first { it == api.base }
        logger.info("Api is ${api.name} (base: ${apiBaseCurrency.code}), currencies: ${currencies.joinToString { it.code }}")

        val rates = rateStore.getLatest(api.name)
        if (rates != null) {
            val queryStrings = BotMessages.query.of(settings.language)
            val apiDisplayNames = BotMessages.apiDisplayNames.of(settings.language)
            val apiTime = "${rates.date} ${rates.time}"
            currencies.asSequence()
                .filter { settings.dashboardCurrencies.contains(it.code) }
                .map { EvaluatedExpression(1.toFixedScaleBigDecimal(), ExpressionType.ONE_UNIT, "1", it, listOf(it)) }
                .map { it to exchangeAllGracefully(rates, it, currencies) }
                .map { (expression, exchanged) ->
                    BotSuccessOutput(expression, exchanged, queryStrings, settings.decimalDigits, apiDisplayNames[settings.apiName], apiTime)
                }
                .map { if (rates.staleData()) BotStaleRatesOutput(it, api.name, settings.language) else it }
                .toList().toTypedArray()
        } else {
            logger.error("Rates unavailable for API ${api.name}")
            arrayOf(BotSimpleErrorOutput(BotMessages.errors.of(settings.language).ratesUnavailable))
        }
    } else {
        logger.info("Handling inline query '$query'")
        handleBotExchangeQuery(query, settings)
    }

    sender.sendInlineQuery(id, *outputs)
}

private fun exchangeAllGracefully(
    rates: ApiExchangeRates,
    it: EvaluatedExpression,
    currencies: List<Currency>
) = try {
    rates.exchangeAll(it.result, it.baseCurrency, currencies)
} catch (e: MissingCurrenciesException) {
    e.exchanges
}
