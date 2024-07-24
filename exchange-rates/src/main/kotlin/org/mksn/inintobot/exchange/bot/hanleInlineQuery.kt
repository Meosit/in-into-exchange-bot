package org.mksn.inintobot.exchange.bot

import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.expression.EvaluatedExpression
import org.mksn.inintobot.common.expression.ExpressionType
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.rate.ApiExchangeRates
import org.mksn.inintobot.common.rate.MissingCurrenciesException
import org.mksn.inintobot.common.rate.RateApis
import org.mksn.inintobot.common.user.UserSettings
import org.mksn.inintobot.exchange.BotContext
import org.mksn.inintobot.exchange.output.BotQuerySuccessOutput
import org.mksn.inintobot.exchange.output.BotSimpleErrorOutput
import org.mksn.inintobot.exchange.output.BotStaleRatesOutput
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.telegram.InlineQuery
import java.util.logging.Logger

private val logger = Logger.getLogger("handleInlineQuery")

suspend fun InlineQuery.handle(settings: UserSettings, context: BotContext) {
    val outputs = when {
        /*query.isBlank() && this.from.id.toString() == context.creatorId -> {
            mutableListOf<BotInput>()
            logger.info("Handling owner dashboard inline query")
            listOf(
                BotInput(
                    expression = ConversionHistoryExpression(Currencies["USD"], Currencies["PLN"]),
                    additionalCurrencies = setOf(),
                    rateApi = RateApis["NBP"],
                    onDate = null,
                    decimalDigits = settings.decimalDigits,
                ),
                BotInput(
                    expression = ConversionHistoryExpression(Currencies["USD"], Currencies["BYN"]),
                    additionalCurrencies = setOf(),
                    rateApi = RateApis["NBRB"],
                    onDate = null,
                    decimalDigits = settings.decimalDigits,
                ),
                BotInput(
                    expression = ConversionHistoryExpression(Currencies["EUR"], Currencies["USD"]),
                    additionalCurrencies = setOf(),
                    rateApi = null,
                    onDate = null,
                    decimalDigits = settings.decimalDigits,
                )
            ).mapNotNull { input ->
                (input.expression as? ConversionHistoryExpression)?.let {
                    input.handleBotQueryHistoryRequest(
                        expression = it,
                        api = input.rateApi ?: RateApis[settings.apiName],
                        context = context,
                        settings = settings,
                        isInline = true,
                        decimalDigits = settings.decimalDigits
                    )
                }
            }.flatMap { it.asIterable() }.toTypedArray()
        }*/
        query.isBlank() -> {
            logger.info("Handling dashboard inline query")
            val api = RateApis[settings.apiName]
            val currencies = settings.outputCurrencies.map { Currencies[it] }.filterNot { it.code in api.unsupported }
            val apiBaseCurrency = currencies.first { it == api.base }
            logger.info("Api is ${api.name} (base: ${apiBaseCurrency.code}), currencies: ${currencies.joinToString { it.code }}")

            val rates = context.rateStore.getLatest(api.name)
            if (rates != null) {
                val queryStrings = BotMessages.query.of(settings.language)
                val apiDisplayNames = BotMessages.apiDisplayNames.of(settings.language)
                val apiTime = rates.timeString()
                currencies.asSequence()
                    .filter { settings.dashboardCurrencies.contains(it.code) }
                    .map { EvaluatedExpression(1.toFixedScaleBigDecimal(), ExpressionType.ONE_UNIT, "1", it, listOf(it)) }
                    .map { it to exchangeAllGracefully(rates, it, currencies) }
                    .map { (expression, exchanged) ->
                        BotQuerySuccessOutput(expression, exchanged, queryStrings, settings.decimalDigits, apiDisplayNames[settings.apiName], apiTime)
                    }
                    .map { if (rates.staleData()) BotStaleRatesOutput(it, api.name, apiTime, settings.language) else it }
                    .toList().toTypedArray()
            } else {
                logger.severe("Rates unavailable for API ${api.name}")
                context.statsStore.logExchangeErrorRequest("ratesUnavailable", inlineRequest = true)
                arrayOf(BotSimpleErrorOutput(BotMessages.errors.of(settings.language).ratesUnavailable))
            }
        }
        else -> {
            logger.info("Handling inline query '$query'")
            handleBotExchangeQuery(isInline = true, query, settings, context)
        }
    }

    val label = if (query.isNotBlank()) null else BotMessages.settings.of(settings.language).customiseSettingsLabel
    context.sender.sendInlineQuery(id, label, outputs=outputs)
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
