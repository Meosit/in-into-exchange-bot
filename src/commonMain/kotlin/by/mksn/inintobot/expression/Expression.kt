package by.mksn.inintobot.expression

import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.util.toStr
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.toBigDecimal

@ExperimentalUnsignedTypes
enum class SuffixType(val factor: BigDecimal) {
    KILO(1_000.toBigDecimal()),
    MEGA(1_000_000.toBigDecimal())
}

sealed class Expression

data class CurrenciedExpression(val e: Expression, val currency: Currency) : Expression()

@ExperimentalUnsignedTypes
data class Const(val number: BigDecimal) : Expression()

//@ExperimentalUnsignedTypes
//data class MetricSuffix(val e: Expression, val suffixType: SuffixType): Expression()

@ExperimentalUnsignedTypes
data class ConstWithSuffixes(val number: BigDecimal, val suffixCount: Int, val suffixType: SuffixType) : Expression()

data class Add(val e1: Expression, val e2: Expression) : Expression()

data class Subtract(val e1: Expression, val e2: Expression) : Expression()

data class Multiply(val e1: Expression, val e2: Expression) : Expression()

data class Divide(val e1: Expression, val e2: Expression) : Expression()

data class Negate(val e: Expression) : Expression()


@ExperimentalUnsignedTypes
fun ConstWithSuffixes.evalNumber() = number * suffixType.factor.pow(suffixCount)

@ExperimentalUnsignedTypes
fun Expression.isConst() = this is Const || this is ConstWithSuffixes

@ExperimentalUnsignedTypes
fun Expression.isOneUnitConst() = this is Const && this.number.toStr() == "1"