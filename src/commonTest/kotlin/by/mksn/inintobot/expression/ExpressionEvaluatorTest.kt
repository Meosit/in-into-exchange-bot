package by.mksn.inintobot.expression

import by.mksn.inintobot.test.testCurrencies
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
class ExpressionEvaluatorTest {

    private val expressionEvaluator = ExpressionEvaluator(
        apiBaseCurrency = testCurrencies.first { it.code == "BYN" },
        exchangeToApiBase = { value, _ -> value }
    )

    @Test
    fun simple_expression() {
        val expr = Add(Const(1.1111.toBigDecimal()), Const(1.1111.toBigDecimal()))

        val (value, _, stringRepr) = expressionEvaluator.evaluate(expr)

        assertEquals(2.2222.toBigDecimal(), value)
        assertEquals("1.1111 + 1.1111", stringRepr)
    }

    @Test
    fun just_const() {
        val expr = Const(1.1111.toBigDecimal())

        val (value, _, stringRepr) = expressionEvaluator.evaluate(expr)

        assertEquals(2.2222.toBigDecimal(), value)
        assertEquals("1.1111 + 1.1111", stringRepr)
    }

    @Test
    fun const_with_kilo_suffix() {
        val expr = ConstWithSuffixes(1.toBigDecimal(), 1, SuffixType.KILO)

        val (value, _, stringRepr) = expressionEvaluator.evaluate(expr)

        assertEquals(1000.toBigDecimal(), value)
        assertEquals("1000", stringRepr)
    }

}