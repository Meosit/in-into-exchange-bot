package by.mksn.inintobot.output

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.currency.Exchange
import by.mksn.inintobot.expression.EvaluatedExpression
import by.mksn.inintobot.expression.ExpressionType
import by.mksn.inintobot.misc.toStr
import by.mksn.inintobot.misc.trimToLength
import by.mksn.inintobot.output.strings.QueryStrings

data class BotSuccessOutput(
    val expression: EvaluatedExpression,
    val exchanges: List<Exchange>,
    val strings: QueryStrings,
    val decimalDigits: Int,
    val apiName: String? = null
) : BotOutput {

    private val markdown by lazy {
        val expressionHeader = when (expression.type) {
            ExpressionType.ONE_UNIT -> strings.headers.rate.format(expression.baseCurrency.code)
            ExpressionType.SINGLE_VALUE -> ""
            ExpressionType.SINGLE_CURRENCY_EXPR -> strings.headers.singleCurrencyExpression
                .format(expression.stringRepr, exchanges.joinToString { it.currency.code })
            ExpressionType.MULTI_CURRENCY_EXPR -> strings.headers.multiCurrencyExpression.format(expression.stringRepr)
        }
        val apiHeader = apiName?.let { strings.headers.api.format(it) } ?: ""
        val exchangeBody = exchanges
            .joinToString("\n") { "`${it.currency.emoji}${it.currency.code}`  `${it.value.toStr(decimalDigits)}`" }
        (expressionHeader + apiHeader + exchangeBody).trimToLength(AppContext.maxOutputLength, "… ${strings.outputTooBigMessage}")
    }

    override fun inlineTitle() = when (expression.type) {
        ExpressionType.ONE_UNIT -> strings.inlineTitles.dashboard.format(expression.involvedCurrencies.first().code)
        ExpressionType.MULTI_CURRENCY_EXPR ->
            strings.inlineTitles.exchange.format("\uD83D\uDCB1",
                expression.involvedCurrencies.joinToString(",") { it.code },
                exchanges.asSequence()
                    .filter { expression.involvedCurrencies.contains(it.currency) }
                    .map { it.currency.code }
                    .joinToString(",")
            )
        else ->
            strings.inlineTitles.exchange.format(expression.result.toStr(),
                expression.involvedCurrencies.joinToString(",") { it.code },
                exchanges.asSequence()
                    .filter { expression.involvedCurrencies.contains(it.currency) }
                    .map { it.currency.code }
                    .joinToString(",")
            )
    }

    override fun inlineDescription() = markdown
        .replace("`", "")
        .replace("_", "")
        .replace("\n", " ")

    override fun inlineThumbUrl() = when (expression.type) {
        ExpressionType.ONE_UNIT -> strings.inlineThumbs.dashboard
        else -> strings.inlineThumbs.exchange
    }

    override fun markdown() = markdown
}