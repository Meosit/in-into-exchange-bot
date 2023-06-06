package org.mksn.inintobot.common.expression

import org.mksn.inintobot.common.currency.Currency
import java.math.BigDecimal

/**
 * Represents the result of the [Expression] evaluation.
 *
 * @property result the calculated result of the expression in [baseCurrency]
 * @property type the [ExpressionType] of the expression with respect of different currencies
 * @property stringRepr pretty-print string representation of this expression, e.g. *`"5/(23 - 4)"`*
 * @property baseCurrency the main/default currency of this expression, the [result] value provided in this currency.
 * @property involvedCurrencies the list of currencies used in this expression, if the expression has all operands
 *           in a single currency, it contains a single [baseCurrency] element.
 */
data class EvaluatedExpression(
    val result: BigDecimal,
    val type: ExpressionType,
    val stringRepr: String,
    val baseCurrency: Currency,
    val involvedCurrencies: List<Currency>
)

class PercentPlacementException(val column: Int) : RuntimeException()
class PercentCurrencyException(val column: Int) : RuntimeException()