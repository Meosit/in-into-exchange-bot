package org.mksn.inintobot.exchange.output

import org.mksn.inintobot.exchange.expression.EvaluatedExpression
import org.mksn.inintobot.exchange.output.strings.QueryStrings
import org.mksn.inintobot.misc.toStr

data class BotJustCalculateOutput(
    val expression: EvaluatedExpression,
    val strings: QueryStrings
) : BotOutput {

    override fun inlineTitle() = strings.inlineTitles.calculate
    override fun inlineThumbUrl() = strings.inlineThumbs.calculate

    override fun inlineDescription() = "${expression.stringRepr} = ${expression.result.toStr()}"

    override fun markdown() = "`${expression.stringRepr}` = `${expression.result.toStr()}`"

}