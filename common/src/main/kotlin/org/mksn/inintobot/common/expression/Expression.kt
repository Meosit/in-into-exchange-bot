package org.mksn.inintobot.common.expression

import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.misc.toStr
import java.math.BigDecimal

enum class SuffixType(val factor: BigDecimal) {
    KILO(1_000.toFixedScaleBigDecimal()),
    MEGA(1_000_000.toFixedScaleBigDecimal())
}

sealed class Expression()

data class CurrenciedExpression(val e: Expression, val currency: Currency) : Expression()

data class ConversionHistoryExpression(val source: Currency, val target: Currency) : Expression()

data class Const(val number: BigDecimal) : Expression()

data class ConstWithSuffixes(val number: BigDecimal, val suffixCount: Int, val suffixType: SuffixType) : Expression()

data class Add(val e1: Expression, val e2: Expression) : Expression()

data class Subtract(val e1: Expression, val e2: Expression) : Expression()

data class Multiply(val e1: Expression, val e2: Expression) : Expression()

data class Divide(val e1: Expression, val e2: Expression) : Expression()

data class Negate(val e: Expression) : Expression()

data class Percent(val e: Expression, val column: Int): Expression()


fun ConstWithSuffixes.evalNumber() = number * suffixType.factor.pow(suffixCount)

fun Expression.isConst() = this is Const || this is ConstWithSuffixes

fun Expression.isOneUnitConst() = this is Const && this.number.toStr() == "1"