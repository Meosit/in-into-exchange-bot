package org.mksn.inintobot.common.expression

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
    CURRENCY_DIVISION,

    /**
     * The expression is conversion history from source to target for some period, e.g. `PLN to USD`
     */
    CONVERSION_HISTORY,
}