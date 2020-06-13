package by.mksn.inintobot.output

import by.mksn.inintobot.currency.Exchange
import by.mksn.inintobot.expression.EvaluatedExpression
import by.mksn.inintobot.expression.ExpressionType
import by.mksn.inintobot.misc.BasicInfo
import by.mksn.inintobot.misc.format
import by.mksn.inintobot.misc.toStr
import by.mksn.inintobot.misc.trimToLength

@ExperimentalUnsignedTypes
data class BotSuccessOutput(
    val expression: EvaluatedExpression,
    val exchanges: List<Exchange>,
    val strings: TelegramStrings,
    val decimalDigits: Long,
    val apiName: String? = null
) : BotOutput {

    private val markdown by lazy {
        val expressionHeader = when (expression.type) {
            ExpressionType.ONE_UNIT -> strings.headers.rate.format(expression.baseCurrency.code)
            ExpressionType.SINGLE_VALUE -> ""
            ExpressionType.SINGLE_CURRENCY_EXPR -> strings.headers.singleCurrencyExpression
                .format(expression.stringRepr, expression.involvedCurrencies.first().code)
            ExpressionType.MULTI_CURRENCY_EXPR -> strings.headers.multiCurrencyExpression.format(expression.stringRepr)
        }
        val apiHeader = apiName?.let { strings.headers.api.format(it) } ?: ""
        val exchangeBody = exchanges
            .joinToString("\n") { "`${it.currency.emoji}${it.currency.code}`  `${it.value.toStr(decimalDigits)}`" }
        (expressionHeader + apiHeader + exchangeBody).trimToLength(BasicInfo.maxOutputLength, "… ${strings.outputTooBigMessage}")
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