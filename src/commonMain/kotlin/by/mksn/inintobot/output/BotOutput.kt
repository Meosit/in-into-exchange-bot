package by.mksn.inintobot.output

import by.mksn.inintobot.currency.Exchange
import by.mksn.inintobot.expression.EvaluatedExpression
import by.mksn.inintobot.expression.ExpressionType
import by.mksn.inintobot.misc.escapeMarkdown
import by.mksn.inintobot.misc.format
import by.mksn.inintobot.misc.toStr
import by.mksn.inintobot.misc.trimToLength

interface BotOutput {
    fun inlineTitle(): String
    fun inlineDescription(): String
    fun markdown(): String
}


@ExperimentalUnsignedTypes
data class BotSuccessOutput(
    val expression: EvaluatedExpression,
    val exchanges: List<Exchange>,
    val strings: TelegramStrings,
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
            .joinToString("\n") { "`${it.currency.emoji}${it.currency.code}`  `${it.value.toStr()}`" }
        expressionHeader + apiHeader + exchangeBody
    }

    override fun inlineTitle() = when (expression.type) {
        ExpressionType.ONE_UNIT -> strings.inlineTitles.dashboard.format(expression.involvedCurrencies.first())
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

    override fun markdown() = markdown
}


@ExperimentalUnsignedTypes
data class BotJustCalculateOutput(
    val expression: EvaluatedExpression,
    val strings: TelegramStrings
) : BotOutput {

    private val outputExpression = "${expression.stringRepr} = ${expression.result.toStr()}"

    override fun inlineTitle() = strings.inlineTitles.calculate

    override fun inlineDescription() = outputExpression

    override fun markdown() = "`$outputExpression`"

}


data class BotErrorOutput(
    val rawInput: String,
    val errorPosition: Int,
    val errorMessage: String
) : BotOutput {

    private val trimmedRawInput = rawInput.trimToLength(32, tail = "…")
    override fun inlineTitle() = errorMessage

    override fun inlineDescription() = "(at $errorPosition) $rawInput"

    override fun markdown() = """
        ${errorMessage.escapeMarkdown()} (at $errorPosition)
        ```  ${"▼".padStart(if (errorPosition > trimmedRawInput.length) trimmedRawInput.length else errorPosition)}
        > $trimmedRawInput
          ${"▲".padStart(if (errorPosition > trimmedRawInput.length) trimmedRawInput.length else errorPosition)}```
    """.trimIndent()
}


data class BotTextOutput(
    val markdown: String
) : BotOutput {
    override fun inlineTitle(): String = markdown.escapeMarkdown().trimToLength(30, "…")

    override fun inlineDescription(): String = markdown.escapeMarkdown()

    override fun markdown(): String = markdown
}