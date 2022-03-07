package by.mksn.inintobot.output

import by.mksn.inintobot.AppContext
import by.mksn.inintobot.currency.Exchange
import by.mksn.inintobot.expression.EvaluatedExpression
import by.mksn.inintobot.expression.ExpressionType
import by.mksn.inintobot.misc.toStr
import by.mksn.inintobot.misc.trimToLength
import by.mksn.inintobot.output.strings.QueryStrings
import by.mksn.inintobot.settings.UserSettings

data class BotSuccessOutput(
    val expression: EvaluatedExpression,
    val exchanges: List<Exchange>,
    val strings: QueryStrings,
    val decimalDigits: Int,
    val apiName: String? = null,
    val apiTime: String? = null,
) : BotOutput {

    private val expressionHeader = when (expression.type) {
        ExpressionType.ONE_UNIT -> strings.headers.rate.format(expression.baseCurrency.code)
        ExpressionType.SINGLE_VALUE, ExpressionType.CURRENCY_DIVISION -> ""
        ExpressionType.SINGLE_CURRENCY_EXPR -> strings.headers.singleCurrencyExpression
            .format(expression.stringRepr, expression.involvedCurrencies.first().code)
        ExpressionType.MULTI_CURRENCY_EXPR -> strings.headers.multiCurrencyExpression.format(expression.stringRepr)
    }

    private val markdown by lazy {
        if (expression.type != ExpressionType.CURRENCY_DIVISION) {
            val apiHeader = apiName?.let { strings.headers.api.format(it) } ?: ""
            val apiTime = if (expression.type == ExpressionType.ONE_UNIT) strings.headers.apiTime.format(apiTime) else ""
            val exchangeBody = exchanges
                .joinToString("\n") { "`${it.currency.emoji}${it.currency.code}`  `${it.value.toStr(decimalDigits)}`" }
            (expressionHeader + apiHeader + apiTime + exchangeBody).trimToLength(AppContext.maxOutputLength, "… ${strings.outputTooBigMessage}")
        } else {
            "`${expression.stringRepr}` = `${expression.result.toStr()}`"
        }
    }

    override fun inlineTitle() = when (expression.type) {
        ExpressionType.ONE_UNIT -> strings.inlineTitles.dashboard.format(expression.involvedCurrencies.first().code)
        ExpressionType.CURRENCY_DIVISION -> strings.inlineTitles.calculate
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
        ExpressionType.ONE_UNIT -> strings.inlineThumbs.dashboard
        ExpressionType.CURRENCY_DIVISION -> strings.inlineThumbs.calculate
        else -> strings.inlineThumbs.exchange
    }

    override fun markdown() = markdown

    override fun toApiResponse(settings: UserSettings) = ApiSuccessResponse(
        header = expressionHeader.replace("_", "").replace("`", ""),
        apiName = strings.headers.api.format(settings.apiName).replace("_", ""),
        exchanges = exchanges.map { ExchangeRow(it.currency.emoji, it.currency.code, it.value.toStr(decimalDigits)) }
    )
}