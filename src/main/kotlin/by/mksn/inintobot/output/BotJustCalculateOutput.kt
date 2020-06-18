package by.mksn.inintobot.output

import by.mksn.inintobot.expression.EvaluatedExpression
import by.mksn.inintobot.misc.toStr
import by.mksn.inintobot.output.strings.QueryStrings

data class BotJustCalculateOutput(
    val expression: EvaluatedExpression,
    val strings: QueryStrings
) : BotOutput {

    private val outputExpression = "${expression.stringRepr} = ${expression.result.toStr()}"

    override fun inlineTitle() = strings.inlineTitles.calculate
    override fun inlineThumbUrl() = strings.inlineThumbs.calculate

    override fun inlineDescription() = outputExpression

    override fun markdown() = "`$outputExpression`"

}