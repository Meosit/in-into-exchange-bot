package by.mksn.inintobot.expression

import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.misc.format
import by.mksn.inintobot.misc.toStr
import com.ionspin.kotlin.bignum.decimal.BigDecimal

/**
 * Evaluates the provided [Expression] with multiple currencies support.
 * @param defaultCurrency used as default in case no other currencies supplied
 * @param apiBaseCurrency base [Currency] of the Exchange Rate API used for the evaluation
 * @param exchange function which exchanges the sum from source to target [Currency]
 */
@ExperimentalUnsignedTypes
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

        fun findCurrencies(expr: Expression): Any = when (expr) {
            is Const -> Unit
            is ConstWithSuffixes -> Unit
            is Negate -> findCurrencies(expr.e)
            is Add -> {
                findCurrencies(expr.e1)
                findCurrencies(expr.e2)
            }
            is Subtract -> {
                findCurrencies(expr.e1)
                findCurrencies(expr.e2)
            }
            is Multiply -> {
                findCurrencies(expr.e1)
                findCurrencies(expr.e2)
            }
            is Divide -> {
                findCurrencies(expr.e1)
                findCurrencies(expr.e2)
            }
            is CurrenciedExpression -> involvedCurrencies.add(expr.currency)
        }

        findCurrencies(rootExpr)

        val expressionType: ExpressionType?
        val baseCurrency: Currency?
        when (involvedCurrencies.size) {
            0 -> {
                involvedCurrencies.add(defaultCurrency)
                baseCurrency = defaultCurrency
                expressionType = when {
                    rootExpr.isOneUnitConst() -> ExpressionType.ONE_UNIT
                    rootExpr.isConst() -> ExpressionType.SINGLE_VALUE
                    else -> ExpressionType.SINGLE_CURRENCY_EXPR
                }
            }
            1 -> {
                baseCurrency = involvedCurrencies.first()
                expressionType = when {
                    rootExpr is CurrenciedExpression && rootExpr.e.isOneUnitConst() -> ExpressionType.ONE_UNIT
                    rootExpr is CurrenciedExpression && rootExpr.e.isConst() -> ExpressionType.SINGLE_VALUE
                    else -> ExpressionType.SINGLE_CURRENCY_EXPR
                }
            }
            else -> {
                baseCurrency = apiBaseCurrency
                expressionType = ExpressionType.MULTI_CURRENCY_EXPR
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
                is CurrenciedExpression -> stringRepr(CurrenciedExpression(Divide(expr.e1.e, expr.e2), expr.e1.currency))
                else -> "${valueFormatWithParsRespect(expr.e1)}/${if (expr.e2 is Const) "%s" else "(%s)"}"
                    .format(stringRepr(expr.e1), stringRepr(expr.e2))
            }
            is CurrenciedExpression -> when (expressionType) {
                ExpressionType.ONE_UNIT -> "1"
                ExpressionType.SINGLE_VALUE, ExpressionType.SINGLE_CURRENCY_EXPR -> stringRepr(expr.e)
                ExpressionType.MULTI_CURRENCY_EXPR -> when (expr.e) {
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
                ExpressionType.MULTI_CURRENCY_EXPR -> exchange(eval(expr.e), expr.currency, apiBaseCurrency)
                else -> eval(expr.e)
            }
        }

        return EvaluatedExpression(
            eval(rootExpr),
            type,
            createStringRepresentation(rootExpr, type),
            base,
            involved.toList()
        )
    }
}