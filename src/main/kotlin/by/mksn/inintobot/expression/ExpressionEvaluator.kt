package by.mksn.inintobot.expression

import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.expression.ExpressionType.*
import by.mksn.inintobot.misc.toFixedScaleBigDecimal
import by.mksn.inintobot.misc.toStr
import java.math.BigDecimal

/**
 * Evaluates the provided [Expression] with multiple currencies support.
 * @param defaultCurrency used as default in case no other currencies supplied
 * @param apiBaseCurrency base [Currency] of the Exchange Rate API used for the evaluation
 * @param exchange function which exchanges the sum from source to target [Currency]
 */
class ExpressionEvaluator(
    private val defaultCurrency: Currency,
    private val apiBaseCurrency: Currency,
    private val exchange: (value: BigDecimal, source: Currency, target: Currency) -> BigDecimal
) {

    private data class CurrencyMetadata(
        val type: ExpressionType,
        val baseCurrency: Currency,
        val involvedCurrencies: Set<Currency>
    )

    private fun captureCurrencyMetadata(rootExpr: Expression): CurrencyMetadata {
        val involvedCurrencies = linkedSetOf<Currency>()
        var isCurrencyInDenominator = false

        fun findCurrencies(expr: Expression, inDenominator: Boolean): Any = when (expr) {
            is Const -> Unit
            is ConstWithSuffixes -> Unit
            is Negate -> findCurrencies(expr.e, inDenominator)
            is Add -> {
                findCurrencies(expr.e1, inDenominator)
                findCurrencies(expr.e2, inDenominator)
            }
            is Subtract -> {
                findCurrencies(expr.e1, inDenominator)
                findCurrencies(expr.e2, inDenominator)
            }
            is Multiply -> {
                findCurrencies(expr.e1, inDenominator)
                findCurrencies(expr.e2, inDenominator)
            }
            is Divide -> {
                findCurrencies(expr.e1, inDenominator)
                findCurrencies(expr.e2, inDenominator = true)
            }
            is CurrenciedExpression -> {
                if (inDenominator) {
                    isCurrencyInDenominator = true
                }
                involvedCurrencies.add(expr.currency)
            }
        }

        findCurrencies(rootExpr, false)

        val expressionType: ExpressionType?
        val baseCurrency: Currency?
        when (involvedCurrencies.size) {
            0 -> {
                involvedCurrencies.add(defaultCurrency)
                baseCurrency = defaultCurrency
                expressionType = when {
                    rootExpr.isOneUnitConst() -> ONE_UNIT
                    rootExpr.isConst() -> SINGLE_VALUE
                    else -> SINGLE_CURRENCY_EXPR
                }
            }
            1 -> {
                baseCurrency = involvedCurrencies.first()
                expressionType = when {
                    rootExpr is CurrenciedExpression && rootExpr.e.isOneUnitConst() -> ONE_UNIT
                    rootExpr is CurrenciedExpression && rootExpr.e.isConst() -> SINGLE_VALUE
                    else -> SINGLE_CURRENCY_EXPR
                }
            }
            else -> {
                baseCurrency = apiBaseCurrency
                expressionType = if (isCurrencyInDenominator) CURRENCY_DIVISION else MULTI_CURRENCY_EXPR
            }
        }
        return CurrencyMetadata(
            expressionType,
            baseCurrency,
            involvedCurrencies
        )
    }

    private fun createStringRepresentation(rootExpr: Expression, expressionType: ExpressionType): String {

        fun valueFormatWithParsRespect(expr: Expression) =
            if (expr is Negate || expr is Add || expr is Subtract) "(%s)" else "%s"

        fun stringRepr(expr: Expression): String = when (expr) {
            is Const -> expr.number.toStr()
            is ConstWithSuffixes -> expr.evalNumber().toStr()
            is Negate -> "-${stringRepr(expr.e)}"
            is Add -> when (expr.e2) {
                is Negate -> "${stringRepr(expr.e1)} + (${stringRepr(expr.e2)})"
                else -> "${stringRepr(expr.e1)} + ${stringRepr(expr.e2)}"
            }
            is Subtract -> when (expr.e2) {
                is Negate -> "${stringRepr(expr.e1)} - (${stringRepr(expr.e2)})"
                else -> "${stringRepr(expr.e1)} - ${stringRepr(expr.e2)}"
            }
            is Multiply -> when (expr.e1) {
                is CurrenciedExpression -> stringRepr(CurrenciedExpression(Multiply(expr.e1.e, expr.e2), expr.e1.currency))
                else -> "${valueFormatWithParsRespect(expr.e1)}*${valueFormatWithParsRespect(expr.e2)}"
                    .format(stringRepr(expr.e1), stringRepr(expr.e2))
            }
            is Divide -> when (expr.e1) {
                is CurrenciedExpression -> if (expr.e2 is CurrenciedExpression) {
                    "%s/%s".format(stringRepr(expr.e1), stringRepr(expr.e2))
                } else {
                    stringRepr(CurrenciedExpression(Divide(expr.e1.e, expr.e2), expr.e1.currency))
                }
                else -> "${valueFormatWithParsRespect(expr.e1)}/${if (expr.e2 is Const || expr.e2 is CurrenciedExpression) "%s" else "(%s)"}"
                    .format(stringRepr(expr.e1), stringRepr(expr.e2))
            }
            is CurrenciedExpression -> when (expressionType) {
                ONE_UNIT -> "1"
                SINGLE_VALUE, SINGLE_CURRENCY_EXPR -> stringRepr(expr.e)
                MULTI_CURRENCY_EXPR, CURRENCY_DIVISION -> when (expr.e) {
                    is Add, is Subtract -> "(${stringRepr(expr.e)}) ${expr.currency.code}"
                    else -> "${stringRepr(expr.e)} ${expr.currency.code}"
                }
            }
        }

        return stringRepr(rootExpr)
    }

    /**
     * Calculates the given [rootExpr] into [EvaluatedExpression] with
     * [apiBaseCurrency] used as the default one in case no other [Currency] supplied
     */
    fun evaluate(rootExpr: Expression): EvaluatedExpression {
        val (type, base, involved) = captureCurrencyMetadata(rootExpr)

        fun eval(expr: Expression): BigDecimal = when (expr) {
            is Const -> expr.number
            is ConstWithSuffixes -> expr.evalNumber()
            is Negate -> eval(expr.e).negate()
            is Add -> eval(expr.e1) + eval(expr.e2)
            is Subtract -> eval(expr.e1) - eval(expr.e2)
            is Multiply -> eval(expr.e1) * eval(expr.e2)
            is Divide -> eval(expr.e1) / eval(expr.e2)
            is CurrenciedExpression -> when (type) {
                MULTI_CURRENCY_EXPR, CURRENCY_DIVISION -> exchange(eval(expr.e), expr.currency, apiBaseCurrency)
                else -> eval(expr.e)
            }
        }

        return EvaluatedExpression(
            eval(rootExpr).toFixedScaleBigDecimal(),
            type,
            createStringRepresentation(rootExpr, type),
            base,
            involved.toList()
        )
    }
}