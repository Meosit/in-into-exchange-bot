package by.mksn.inintobot.output

import by.mksn.inintobot.expression.EvaluatedExpression
import by.mksn.inintobot.misc.toStr
import by.mksn.inintobot.output.strings.QueryStrings

data class BotJustCalculateOutput(
    val expression: EvaluatedExpression,
    val strings: QueryStrings
) : BotOutput {

    override fun inlineTitle() = strings.inlineTitles.calculate
    override fun inlineThumbUrl() = strings.inlineThumbs.calculate

    override fun inlineDescription() = "${expression.stringRepr} = ${expression.result.toStr()}"

    override fun markdown() = "`${expression.stringRepr}` = `${expression.result.toStr()}`"

}