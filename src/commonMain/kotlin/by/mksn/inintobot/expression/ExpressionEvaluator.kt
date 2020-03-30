package by.mksn.inintobot.expression

import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.util.format
import by.mksn.inintobot.util.toStr
import com.ionspin.kotlin.bignum.decimal.BigDecimal

enum class ExpressionType {
    ONE_UNIT, SINGLE_VALUE, SINGLE_CURRENCY_EXPR, MULTI_CURRENCY_EXPR
}

@ExperimentalUnsignedTypes
data class EvaluatedExpression(
    val result: BigDecimal,
    val type: ExpressionType,
    val stringRepr: String,
    val baseCurrency: Currency,
    val involvedCurrencies: List<Currency>
)

/**
 * Evaluates the provided [Expression] with multiple currencies support.
 * @param apiBaseCurrency base [Currency] of the Exchange Rate API used for the evaluation,
 *                        used as default in case no other currencies supplied
 * @param exchangeToApiBase function which exchanges the sum from provided [Currency] to [apiBaseCurrency]
 */
@ExperimentalUnsignedTypes
class ExpressionEvaluator(
    private val apiBaseCurrency: Currency,
    private val exchangeToApiBase: (value: BigDecimal, currency: Currency) -> BigDecimal
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
                involvedCurrencies.add(apiBaseCurrency)
                baseCurrency = apiBaseCurrency
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
            is Add -> (if (expr.e2 is Negate) "%s + (%s)" else "%s + %s")
                .format(stringRepr(expr.e1), stringRepr(expr.e2))
            is Subtract -> (if (expr.e2 is Negate) "%s - (%s)" else "%s - %s")
                .format(stringRepr(expr.e1), stringRepr(expr.e2))
            is Multiply -> "${valueFormatWithParsRespect(expr.e1)}*${valueFormatWithParsRespect(expr.e2)}"
                .format(stringRepr(expr.e1), stringRepr(expr.e2))
            is Divide -> "${valueFormatWithParsRespect(expr.e1)}/${if (expr.e2 is Const) "%s" else "(%s)"}"
                .format(stringRepr(expr.e1), stringRepr(expr.e2))
            is CurrenciedExpression -> when (expressionType) {
                ExpressionType.ONE_UNIT -> "1 ${expr.currency.code}"
                ExpressionType.SINGLE_VALUE, ExpressionType.SINGLE_CURRENCY_EXPR -> stringRepr(expr.e)
                ExpressionType.MULTI_CURRENCY_EXPR -> if (expr.e is Add || expr.e is Subtract)
                    "(${stringRepr(expr.e)}) ${expr.currency.code}" else "${stringRepr(expr.e)} ${expr.currency.code}"
            }
        }

        return stringRepr(rootExpr)
    }

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
            is CurrenciedExpression -> if (type == ExpressionType.MULTI_CURRENCY_EXPR) {
                exchangeToApiBase(eval(expr.e), expr.currency)
            } else {
                eval(expr.e)
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