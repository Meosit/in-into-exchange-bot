package org.mksn.inintobot.exchange.output

import org.mksn.inintobot.common.expression.EvaluatedExpression
import org.mksn.inintobot.common.misc.toStr
import org.mksn.inintobot.exchange.output.strings.QueryStrings

data class BotJustCalculateOutput(
    val expression: EvaluatedExpression,
    val strings: QueryStrings,
    val decimalDigits: Int
) : BotOutput {

    override fun inlineTitle() = strings.inlineTitles.calculate
    override fun inlineThumbUrl() = strings.inlineThumbs.calculate

    override fun inlineDescription() = "${expression.stringRepr} = ${expression.result.toStr(decimalDigits)}"

    override fun markdown() = "`${expression.stringRepr}` = `${expression.result.toStr(decimalDigits)}`"

}