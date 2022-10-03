package org.mksn.inintobot.exchange.output

import org.mksn.inintobot.common.expression.EvaluatedExpression
import org.mksn.inintobot.common.expression.ExpressionType
import org.mksn.inintobot.common.misc.toStr
import org.mksn.inintobot.common.misc.trimToLength
import org.mksn.inintobot.common.rate.Exchange
import org.mksn.inintobot.exchange.output.strings.BotMessages
import org.mksn.inintobot.exchange.output.strings.QueryStrings

data class BotQuerySuccessOutput(
    val expression: EvaluatedExpression,
    val exchanges: List<Exchange>,
    val strings: QueryStrings,
    val decimalDigits: Int,
    val apiName: String? = null,
    val apiTime: String? = null,
) : BotOutput {

    private val expressionHeader = when (expression.type) {
        ExpressionType.ONE_UNIT -> strings.headers.rate.format(expression.baseCurrency.code)
        ExpressionType.CONVERSION_HISTORY -> {
            val (from, to) = expression.involvedCurrencies
            strings.headers.history.format("${from.emoji}${from.code}", "${to.emoji}${to.code}")
        }
        ExpressionType.SINGLE_VALUE, ExpressionType.CURRENCY_DIVISION -> ""
        ExpressionType.SINGLE_CURRENCY_EXPR -> strings.headers.singleCurrencyExpression
            .format(expression.stringRepr, expression.involvedCurrencies.first().code)

        ExpressionType.MULTI_CURRENCY_EXPR -> strings.headers.multiCurrencyExpression.format(expression.stringRepr)
    }

    private val markdown = if (expression.type != ExpressionType.CURRENCY_DIVISION) {
        val apiHeader = apiName?.let { strings.headers.api.format(it) } ?: ""
        val apiTime = apiTime?.let { (if (":" in it) strings.headers.apiTime else strings.headers.apiDate).format(it) } ?: ""
        val exchangeBody = exchanges
            .joinToString("\n") { "`${it.currency.emoji}${it.currency.code}`  `${it.value.toStr(decimalDigits)}`" }
        (expressionHeader + apiHeader + apiTime + exchangeBody).trimToLength(
            BotMessages.maxOutputLength,
            "… ${strings.outputTooBigMessage}"
        )
    } else {
        "`${expression.stringRepr}` = `${expression.result.toStr()}`"
    }

    override fun inlineTitle() = when (expression.type) {
        ExpressionType.ONE_UNIT -> strings.inlineTitles.dashboard.format(expression.involvedCurrencies.first().code)
        ExpressionType.CURRENCY_DIVISION -> strings.inlineTitles.calculate
        ExpressionType.CONVERSION_HISTORY -> {
            val (from, to) = expression.involvedCurrencies
            strings.inlineTitles.history.format("${from.emoji}${from.code}", "${to.emoji}${to.code}")
        }
        ExpressionType.MULTI_CURRENCY_EXPR ->
            strings.inlineTitles.exchange.format("\uD83D\uDCB1",
                expression.involvedCurrencies.joinToString(",") { it.code },
                exchanges.asSequence()
                    .filterNot { it.currency in expression.involvedCurrencies }
                    .map { it.currency.code }
                    .joinToString(",")
            )

        else ->
            strings.inlineTitles.exchange.format(expression.result.toStr(),
                expression.involvedCurrencies.joinToString(",") { it.code },
                exchanges.asSequence()
                    .filterNot { it.currency in expression.involvedCurrencies }
                    .map { it.currency.code }
                    .joinToString(",")
            )
    }

    override fun inlineDescription() = markdown
        .replace("`", "")
        .replace("_", "")
        .replace("\n", " ")

    override fun inlineThumbUrl() = when (expression.type) {
        ExpressionType.ONE_UNIT, ExpressionType.CONVERSION_HISTORY -> strings.inlineThumbs.dashboard
        ExpressionType.CURRENCY_DIVISION -> strings.inlineThumbs.calculate
        else -> strings.inlineThumbs.exchange
    }

    override fun markdown() = markdown
}