package org.mksn.inintobot.exchange.expression

import org.mksn.inintobot.common.currency.Currency
import java.math.BigDecimal

/**
 * Represents the logical type of the expression
 */
enum class ExpressionType {
    /**
     * The expression is exactly one unit (1) of the provided currency,
     * basically this means that rate for this specific currency was requested
     */
    ONE_UNIT,

    /**
     * The expression is a single value without any operators, e.g. `"1.1023"`
     */
    SINGLE_VALUE,

    /**
     * The expression contains math operators, e.g. `"1.1023 + 10/(12 - 5)"`
     */
    SINGLE_CURRENCY_EXPR,

    /**
     * The expression contains math operators and each operand has it's own currency, e.g. `"10 EUR + 12 USD - 1 BYN"`
     */
    MULTI_CURRENCY_EXPR,

    /**
     * The expression is a division of two math expression where each has it's own currency. `"10 EUR / 12 USD"`
     */
    CURRENCY_DIVISION
}

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