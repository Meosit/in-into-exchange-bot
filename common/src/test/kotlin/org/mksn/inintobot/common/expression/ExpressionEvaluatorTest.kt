package org.mksn.inintobot.common.expression

import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.rate.RateApis
import org.mksn.inintobot.common.rate.UnknownCurrencyException
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExpressionEvaluatorTest {

    private val apiBaseCurrency = Currencies.first { it.code == "BYN" }
    private val expressionEvaluator = ExpressionEvaluator(
        defaultCurrency = apiBaseCurrency,
        apiBaseCurrency = apiBaseCurrency,
        exchange = { value, _, _ -> value }
    )

    private val Double.bigDecimal: BigDecimal get() = toFixedScaleBigDecimal()

    private val Int.bigDecimal: BigDecimal get() = toFixedScaleBigDecimal()

    private val String.bigDecimal: BigDecimal get() = toFixedScaleBigDecimal()

    private val Int.asConst get() = Const(toFixedScaleBigDecimal())

    private val Double.asConst get() = Const(toFixedScaleBigDecimal())

    private val String.asConst get() = Const(toFixedScaleBigDecimal())



    fun <E, C : Iterable<E>> assertEqualsOrdered(expected: C, actual: C) {
        val expectedSeq = expected.asSequence()
        val actualSeq = actual.asSequence()
        assertEquals(
            expectedSeq.count(), actualSeq.count(),
            "Given collections have different size, expected ${expectedSeq.count()}, got ${actualSeq.count()}"
        )
        expectedSeq.zip(actualSeq).forEachIndexed { index, (expected, actual) ->
            assertEquals(expected, actual, "Elements at index $index are not same!")
        }
    }


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
        val expr = ConstWithSuffixes(10.toFixedScaleBigDecimal(), 3, SuffixType.MEGA)

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
    fun simple_percent_add_expression() {
        val expr = Add(200.asConst, Percent(20.asConst, 12))

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(240.bigDecimal, value)
        assertEquals("200 + 20%", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(apiBaseCurrency), involvedCurrencies)
    }

    @Test
    fun simple_percent_subtract_expression() {
        val expr = Subtract(1000.asConst, Percent(115.asConst, 12))

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals((-150).bigDecimal, value)
        assertEquals("1000 - 115%", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(apiBaseCurrency), involvedCurrencies)
    }


    @Test
    fun complex_percent_add_expression() {
        val expr = Add(200.asConst, Percent(Divide(Add(20.asConst, 180.asConst), Multiply(2.asConst, "2.5".asConst)), 12))

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(280.bigDecimal, value)
        assertEquals("200 + ((20 + 180)/(2*2.5))%", stringRepr)
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
        val expr = CurrenciedExpression(1.asConst, Currencies["UAH"])

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(1.bigDecimal, value)
        assertEquals("1", stringRepr)
        assertEquals(ExpressionType.ONE_UNIT, exprType)
        assertEquals(Currencies["UAH"], baseCurrency)
        assertEqualsOrdered(listOf(Currencies["UAH"]), involvedCurrencies)
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
        val expr = CurrenciedExpression(Add("1.2222222003030330000000099999999".asConst, 2.3333333.asConst), Currencies["UAH"])

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals("1.222222200303033 + 2.3333333", stringRepr)
        assertEquals("3.555555500303033".bigDecimal, value)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals(Currencies["UAH"], baseCurrency)
        assertEqualsOrdered(listOf(Currencies["UAH"]), involvedCurrencies)
    }

    @Test
    fun multiple_currencies() {
        val expr = Add(
            CurrenciedExpression(1.583.asConst, Currencies["USD"]),
            CurrenciedExpression(1.417.asConst, Currencies["UAH"])
        )

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(3.bigDecimal, value)
        assertEquals("1.583 USD + 1.417 UAH", stringRepr)
        assertEquals(ExpressionType.MULTI_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(Currencies["USD"], Currencies["UAH"]), involvedCurrencies)
    }

    @Test
    fun currencied_division() {
        val expr = Divide(
            CurrenciedExpression(4.06.asConst, Currencies["USD"]),
            CurrenciedExpression(2.03.asConst, Currencies["UAH"])
        )

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(2.bigDecimal, value)
        assertEquals(ExpressionType.CURRENCY_DIVISION, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(Currencies["USD"], Currencies["UAH"]), involvedCurrencies)
        assertEquals("4.06 USD/2.03 UAH", stringRepr)
    }

    @Test
    fun currencied_division_complicated() {
        val expr = Subtract(
            Add(
                Divide(
                    Add(
                        CurrenciedExpression(Subtract(Add(4.06.asConst, 1.asConst), 1.asConst), Currencies["USD"]),
                        CurrenciedExpression(10.asConst, Currencies["UAH"])
                    ),
                    CurrenciedExpression(2.03.asConst, Currencies["BYN"])
                ),
                10.asConst
            ),
            Divide(CurrenciedExpression(10.asConst, Currencies["USD"]), CurrenciedExpression(15.asConst, Currencies["BYN"]))
        )

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(ExpressionType.CURRENCY_DIVISION, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(Currencies["USD"], Currencies["UAH"], Currencies["BYN"]), involvedCurrencies)
        assertEquals("((4.06 + 1 - 1) USD + 10 UAH)/2.03 BYN + 10 - 10 USD/15 BYN", stringRepr)
        assertEquals("16.2594417077175698".bigDecimal, value)
    }

    @Test
    fun currencied_division_complicated_division_in_division() {
        val expr = Divide(
            Divide(
                Add(
                    CurrenciedExpression(Subtract(Add(4.06.asConst, 1.asConst), 1.asConst), Currencies["USD"]),
                    CurrenciedExpression(10.asConst, Currencies["UAH"])
                ),
                CurrenciedExpression(10.asConst, Currencies["UAH"])
            ),
            Divide(CurrenciedExpression(10.asConst, Currencies["USD"]), CurrenciedExpression(15.asConst, Currencies["BYN"]))
        )

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(ExpressionType.CURRENCY_DIVISION, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(Currencies["USD"], Currencies["UAH"], Currencies["BYN"]), involvedCurrencies)
        assertEquals("((4.06 + 1 - 1) USD + 10 UAH)/10 UAH/(10 USD/15 BYN)", stringRepr)
        assertEquals("2.1089999999999999".bigDecimal, value)
    }

    @Test
    fun multiple_currencies_including_api_base() {
        val expr = Add(
            Add(
                CurrenciedExpression(1.583.asConst, Currencies["USD"]),
                CurrenciedExpression(1.417.asConst, apiBaseCurrency)
            ),
            CurrenciedExpression(2.asConst, Currencies["UAH"])
        )

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(5.bigDecimal, value)
        assertEquals("1.583 USD + 1.417 ${apiBaseCurrency.code} + 2 UAH", stringRepr)
        assertEquals(ExpressionType.MULTI_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(Currencies["USD"], apiBaseCurrency, Currencies["UAH"]), involvedCurrencies)
    }

    @Test
    fun multiple_currencies_with_currencied_expressions() {
        val expr = Add(
            Multiply(
                CurrenciedExpression(Add(1.5899.asConst, 1.3101.asConst), Currencies["USD"]),
                2.asConst
            ),
            CurrenciedExpression(Divide(4.asConst, 2.asConst), Currencies["UAH"])
        )

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(7.8.bigDecimal, value)
        assertEquals("(1.5899 + 1.3101)*2 USD + 4/2 UAH", stringRepr)
        assertEquals(ExpressionType.MULTI_CURRENCY_EXPR, exprType)
        assertEquals(apiBaseCurrency, baseCurrency)
        assertEqualsOrdered(listOf(Currencies["USD"], Currencies["UAH"]), involvedCurrencies)
    }

    @Test
    fun single_currency_expression_with_currency_in_middle() {
        val expr = Multiply(CurrenciedExpression(2.435.asConst, Currencies["EUR"]), 2.asConst)

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(4.87.bigDecimal, value)
        assertEquals("2.435*2", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals(Currencies["EUR"], baseCurrency)
        assertEqualsOrdered(listOf(Currencies["EUR"]), involvedCurrencies)
    }

    @Test
    fun single_currency_expression_with_same_currency_multiple_times() {
        val expr = Add(
            Multiply(CurrenciedExpression(2.435.asConst, Currencies["EUR"]), 2.asConst),
            Divide(CurrenciedExpression(2.asConst, Currencies["EUR"]), 2.asConst)
        )

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(5.87.bigDecimal, value)
        assertEquals("2.435*2 + 2/2", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals(Currencies["EUR"], baseCurrency)
        assertEqualsOrdered(listOf(Currencies["EUR"]), involvedCurrencies)
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

    @Test
    fun simple_currencied_expression() {
        val currency = Currencies["USD"]
        val expr = CurrenciedExpression(10.asConst, currency)

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals(10.bigDecimal, value)
        assertEquals("10", stringRepr)
        assertEquals(ExpressionType.SINGLE_VALUE, exprType)
        assertEquals(currency, baseCurrency)
        assertEqualsOrdered(listOf(currency), involvedCurrencies)
    }

    @Test
    fun custom_default_currency() {
        val defaultCurrency = Currencies["KZT"]
        val apiBaseCurrency = Currencies["BYN"]
        val expressionEvaluator = ExpressionEvaluator(
            defaultCurrency = defaultCurrency,
            apiBaseCurrency = apiBaseCurrency,
            exchange = { value, _, _ -> value }
        )

        val expr = Add("1.2222222003030330000000099999999".asConst, 2.3333333.asConst)

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals("3.555555500303033".bigDecimal, value)
        assertEquals("1.222222200303033 + 2.3333333", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals(defaultCurrency, baseCurrency)
        assertEqualsOrdered(listOf(defaultCurrency), involvedCurrencies)
    }

    @Test
    fun must_not_call_exchange_when_single_currency_expression() {
        val defaultCurrency = Currencies["BYN"]
        val api = RateApis.first()
        val expressionEvaluator = ExpressionEvaluator(
            defaultCurrency = defaultCurrency,
            apiBaseCurrency = api.base,
            exchange = { _, from, _ -> throw UnknownCurrencyException(api, from) }
        )

        val targetCurrency = Currencies["KZT"]
        val expr = CurrenciedExpression(Add(10.asConst, 10.asConst), targetCurrency)

        val (value, exprType, stringRepr, baseCurrency, involvedCurrencies) = expressionEvaluator.evaluate(expr)

        assertEquals("20".bigDecimal, value)
        assertEquals("10 + 10", stringRepr)
        assertEquals(ExpressionType.SINGLE_CURRENCY_EXPR, exprType)
        assertEquals(targetCurrency, baseCurrency)
        assertEqualsOrdered(listOf(targetCurrency), involvedCurrencies)
    }

    @Test
    fun must_throw_currency_inside_percent_placement() {
        val expr = Divide(
            Divide(
                Add(
                    Percent(CurrenciedExpression(Subtract(Add(4.06.asConst, 1.asConst), 1.asConst), Currencies["USD"]), 12),
                    CurrenciedExpression(10.asConst, Currencies["UAH"])
                ),
                CurrenciedExpression(10.asConst, Currencies["UAH"])
            ),
            Divide(CurrenciedExpression(10.asConst, Currencies["USD"]), CurrenciedExpression(15.asConst, Currencies["BYN"]))
        )


        val t = assertFailsWith<PercentCurrencyException> { expressionEvaluator.evaluate(expr) }

        assertEquals(12, t.column)
    }


    @Test
    fun must_throw_invalid_percent_placement() {
        val expr = Divide(
            Divide(
                Add(
                    CurrenciedExpression(Subtract(Add(Percent(4.06.asConst, 42), 1.asConst), 1.asConst), Currencies["USD"]),
                    CurrenciedExpression(10.asConst, Currencies["UAH"])
                ),
                CurrenciedExpression(10.asConst, Currencies["UAH"])
            ),
            Divide(CurrenciedExpression(10.asConst, Currencies["USD"]), CurrenciedExpression(15.asConst, Currencies["BYN"]))
        )


        val t = assertFailsWith<PercentPlacementException> { expressionEvaluator.evaluate(expr) }

        assertEquals(42, t.column)
    }

}