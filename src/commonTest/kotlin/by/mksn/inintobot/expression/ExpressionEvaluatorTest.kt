package by.mksn.inintobot.expression

import by.mksn.inintobot.test.*
import by.mksn.inintobot.util.toFiniteBigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
class ExpressionEvaluatorTest {

    private val apiBaseCurrency = testCurrencies.first { it.code == "BYN" }
    private val expressionEvaluator = ExpressionEvaluator(
        apiBaseCurrency = apiBaseCurrency,
        exchangeToApiBase = { value, _ -> value }
    )

    @Test
    fun just_const() {
        val expr = Const(1.1111.bigDecimal)

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(1.1111.bigDecimal, value)
        assertEquals("1.1111", stringRepr)
        assertEquals(ExpressionType.SINGLE_VALUE, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(apiBaseCurrency), involvedCurrencies)
    }

    @Test
    fun kilo_suffix() {
        val expr = ConstWithSuffixes(1.42392.bigDecimal, 1, SuffixType.KILO)

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(1423.92.bigDecimal, value)
        assertEquals("1423.92", stringRepr)
        assertEquals(ExpressionType.SINGLE_VALUE, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(apiBaseCurrency), involvedCurrencies)
    }

    @Test
    fun mega_suffixes() {
        val expr = ConstWithSuffixes(10.toFiniteBigDecimal(), 3, SuffixType.MEGA)

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals("10000000000000000000".bigDecimal, value)
        assertEquals("10000000000000000000", stringRepr)
        assertEquals(ExpressionType.SINGLE_VALUE, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(apiBaseCurrency), involvedCurrencies)
    }

    @Test
    fun simple_add_expression() {
        val expr = Add(1.000099.asConst, 123.asConst)

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(124.000099.bigDecimal, value)
        assertEquals("1.000099 + 123", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(apiBaseCurrency), involvedCurrencies)
    }

    @Test
    fun simple_multiply_expression() {
        val expr = Multiply(0.333.asConst, 3.asConst)

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(0.999.bigDecimal, value)
        assertEquals("0.333*3", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(apiBaseCurrency), involvedCurrencies)
    }

    @Test
    fun simple_expression_with_priority() {
        val expr = Add(Multiply(9.asConst, 15.asConst), 2.44.asConst)

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(137.44.bigDecimal, value)
        assertEquals("9*15 + 2.44", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(apiBaseCurrency), involvedCurrencies)
    }

    @Test
    fun expression_with_brackets() {
        val expr = Divide(Multiply(1.asConst, 1.asConst), Subtract(7.asConst, 2.asConst))

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)


        assertEquals(0.2.bigDecimal, value)
        assertEquals("1*1/(7 - 2)", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(apiBaseCurrency), involvedCurrencies)
    }

    @Test
    fun expression_with_nested_brackets() {
        val expr = Multiply(
            Add(1.asConst, Subtract(2.asConst, 1.asConst)),
            Divide(Subtract(7.asConst, 2.asConst), 2.asConst)
        )

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(5.bigDecimal, value)
        assertEquals("(1 + 2 - 1)*(7 - 2)/2", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(apiBaseCurrency), involvedCurrencies)
    }

    @Test
    fun one_unit_with_different_currency() {
        val expr = CurrenciedExpression(1.asConst, "UAH".toCurrency())

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(1.bigDecimal, value)
        assertEquals("1", stringRepr)
        assertEquals(ExpressionType.ONE_UNIT, exprType)
        assertEquals("UAH".toCurrency(), baseCurrency)
        assertEqualsOrdered(listOf("UAH".toCurrency()), involvedCurrencies)
    }

    @Test
    fun one_unit_with_trailing_zeros() {
        val expr = "1.0000000000000000000000000000000000000000000000000000".asConst

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(1.bigDecimal, value)
        assertEquals("1", stringRepr)
        assertEquals(ExpressionType.ONE_UNIT, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(apiBaseCurrency), involvedCurrencies)
    }

    @Test
    fun very_long_number() {
        val expr =
            "488328938372887977341537259497997851352671159292899697236058208809454048246899111241332161343881238402187713643910538138490086922551030374059966793632190643617540775466354136146108018361168082820587948041800957124719210860435589413028616075788651235472".asConst

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals("488328938372887977341537259497997851352671159292899697236058208809454048246899111241332161343881238402187713643910538138490086922551030374059966793632190643617540775466354136146108018361168082820587948041800957124719210860435589413028616075788651235472".bigDecimal, value)
        assertEquals("488328938372887977341537259497997851352671159292899697236058208809454048246899111241332161343881238402187713643910538138490086922551030374059966793632190643617540775466354136146108018361168082820587948041800957124719210860435589413028616075788651235472", stringRepr)
        assertEquals(ExpressionType.SINGLE_VALUE, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(apiBaseCurrency), involvedCurrencies)
    }

    @Test
    fun simple_expression_with_different_currency() {
        val expr =
            CurrenciedExpression(Add("1.2222222003030330000000099999999".asConst, 2.3333333.asConst), "UAH".toCurrency())

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals("3.555555500303033".bigDecimal, value)
        assertEquals("1.222222200303033 + 2.3333333", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals("UAH".toCurrency(), baseCurrency)
        assertEqualsOrdered(listOf("UAH".toCurrency()), involvedCurrencies)
    }

    @Test
    fun multiple_currencies() {
        val expr = Add(
            CurrenciedExpression(1.583.asConst, "USD".toCurrency()),
            CurrenciedExpression(1.417.asConst, "UAH".toCurrency())
        )

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(3.bigDecimal, value)
        assertEquals("1.583 USD + 1.417 UAH", stringRepr)
        assertEquals(ExpressionType.MULTI_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf("USD".toCurrency(), "UAH".toCurrency()), involvedCurrencies)
    }

    @Test
    fun multiple_currencies_including_api_base() {
        val expr = Add(
            Add(
                CurrenciedExpression(1.583.asConst, "USD".toCurrency()),
                CurrenciedExpression(1.417.asConst, apiBaseCurrency)
            ),
            CurrenciedExpression(2.asConst, "UAH".toCurrency())
        )

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(5.bigDecimal, value)
        assertEquals("1.583 USD + 1.417 ${apiBaseCurrency.code} + 2 UAH", stringRepr)
        assertEquals(ExpressionType.MULTI_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf("USD".toCurrency(), apiBaseCurrency, "UAH".toCurrency()), involvedCurrencies)
    }

    @Test
    fun multiple_currencies_with_currencied_expressions() {
        val expr = Add(
            Multiply(
                CurrenciedExpression(Add(1.5899.asConst, 1.3101.asConst), "USD".toCurrency()),
                2.asConst
            ),
            CurrenciedExpression(Divide(4.asConst, 2.asConst), "UAH".toCurrency())
        )

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(7.8.bigDecimal, value)
        assertEquals("(1.5899 + 1.3101)*2 USD + 4/2 UAH", stringRepr)
        assertEquals(ExpressionType.MULTI_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf("USD".toCurrency(), "UAH".toCurrency()), involvedCurrencies)
    }

    @Test
    fun single_currency_expression_with_currency_in_middle() {
        val expr = Multiply(CurrenciedExpression(2.435.asConst, "EUR".toCurrency()), 2.asConst)

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(4.87.bigDecimal, value)
        assertEquals("2.435*2", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals("EUR".toCurrency(), baseCurrency)
        assertEqualsOrdered(listOf("EUR".toCurrency()), involvedCurrencies)
    }

    @Test
    fun single_currency_expression_with_same_currency_multiple_times() {
        val expr = Add(
            Multiply(CurrenciedExpression(2.435.asConst, "EUR".toCurrency()), 2.asConst),
            Divide(CurrenciedExpression(2.asConst, "EUR".toCurrency()), 2.asConst)
        )

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(5.87.bigDecimal, value)
        assertEquals("2.435*2 + 2/2", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals("EUR".toCurrency(), baseCurrency)
        assertEqualsOrdered(listOf("EUR".toCurrency()), involvedCurrencies)
    }

    @Test
    fun multiply_with_division_operand() {
        val expr = Multiply(Divide(8.asConst, 2.asConst), 2.asConst)

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(8.bigDecimal, value)
        assertEquals("8/2*2", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(apiBaseCurrency), involvedCurrencies)
    }

    @Test
    fun divide_with_multiplication_in_denominator() {
        val expr = Divide(8.asConst, Multiply(2.asConst, 2.asConst))

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(2.bigDecimal, value)
        assertEquals("8/(2*2)", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(apiBaseCurrency), involvedCurrencies)
    }

}