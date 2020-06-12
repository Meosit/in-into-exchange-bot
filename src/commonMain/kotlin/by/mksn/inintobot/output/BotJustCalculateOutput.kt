package by.mksn.inintobot.output

import by.mksn.inintobot.expression.EvaluatedExpression
import by.mksn.inintobot.misc.toStr

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