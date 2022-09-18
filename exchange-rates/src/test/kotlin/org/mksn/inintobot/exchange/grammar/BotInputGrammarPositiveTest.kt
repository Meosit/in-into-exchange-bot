package org.mksn.inintobot.exchange.grammar

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import org.mksn.inintobot.common.currency.Currencies
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.common.rate.RateApis
import org.mksn.inintobot.exchange.expression.*
import org.mksn.inintobot.exchange.grammar.alias.CurrencyAliasMatcher
import org.mksn.inintobot.exchange.grammar.alias.RateAliasMatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BotInputGrammarPositiveTest {

    private val grammar = BotInputGrammar(CurrencyAliasMatcher, RateAliasMatcher)

    private val Int.bigDecimal get() = this.toFixedScaleBigDecimal()
    private val Int.asConst get() = Const(this.bigDecimal)
    private val Double.asConst get() = Const(this.toFixedScaleBigDecimal())
    private fun String.toCurrency() = Currencies[this]
    private fun String.toRateApi() = RateApis[this]

    /**
     * Asserts two [Iterable]s without specific order using [expected] as a base one
     */
    private fun <E : Any, C : Iterable<E>> assertEqualsUnordered(expected: C, actual: C, keyExtractor: (E) -> Any = { c -> c }) {
        assertEquals(
            expected.count(), actual.count(),
            "Given collections have different size, expected ${expected.count()}, got ${actual.count()}"
        )
        expected.forEachIndexed { index, expectedElem ->
            assertEquals(
                expectedElem, actual.find { keyExtractor(expectedElem) == keyExtractor(it) },
                "Expected element at index $index not found in actual collection!"
            )
        }
    }

    @Test
    fun single_value() {
        val input = "1"
        val expectedExpr = 1.asConst

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun simple_add_expression() {
        val input = "1 + 1"
        val expectedExpr = Add(1.asConst, 1.asConst)

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun simple_multiply_expression() {
        val input = "1 * 1"
        val expectedExpr = Multiply(1.asConst, 1.asConst)

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun simple_expression_with_priority() {
        val input = "1 * 1 + 2"
        val expectedExpr = Add(Multiply(1.asConst, 1.asConst), 2.asConst)

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun simple_multiline_expression() {
        val input = "1\n*\n1   \n+ 2"
        val expectedExpr = Add(Multiply(1.asConst, 1.asConst), 2.asConst)

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun simple_expression_with_zeroless_decimals() {
        val input = "1 * 1.11 + .33 - ,23"
        val expectedExpr = Subtract(Add(Multiply(1.asConst, 1.11.asConst), 0.33.asConst), 0.23.asConst)

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun expression_with_brackets() {
        val input = "(1 * 1) / (7 - 2) #4"
        val expectedExpr = Divide(Multiply(1.asConst, 1.asConst), Subtract(7.asConst, 2.asConst))

        val (actualExpr, additionalCurrencies, _, decimalDigits) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
        assertEquals(4, decimalDigits)
    }

    @Test
    fun expression_with_nested_brackets() {
        val input = "(1 + (2 - 1)) * ((7 - 2) / 2)"
        val expectedExpr = Multiply(
            Add(1.asConst, Subtract(2.asConst, 1.asConst)),
            Divide(Subtract(7.asConst, 2.asConst), 2.asConst)
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun value_with_different_currency() {
        val input = "1 гривна"
        val expectedExpr = CurrenciedExpression(1.asConst, "UAH".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun value_with_different_currency_prefix() {
        val input = "1 mol"
        val expectedExpr = CurrenciedExpression(1.asConst, "MDL".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun value_with_different_currency_prefix_transliterated() {
        val input = "1 tayl"
        val expectedExpr = CurrenciedExpression(1.asConst, "THB".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun value_with_different_currency_prefix_keyboard_switched() {
        val input = "1 tayl"
        val expectedExpr = CurrenciedExpression(1.asConst, "THB".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun value_with_different_currency_and_union_collision() {
        val input = "1в"
        val expectedExpr = CurrenciedExpression(1.asConst, "USD".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun simple_expression_with_different_currency() {
        val input = "1 + 1 гривна"
        val expectedExpr = CurrenciedExpression(Add(1.asConst, 1.asConst), "UAH".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun multiple_currencies_wrong_layout() {
        val input = "1.583 ljkkfh + 1,417 b"
        val expectedExpr = Add(
            CurrenciedExpression(1.583.asConst, "USD".toCurrency()),
            CurrenciedExpression(1.417.asConst, "BYN".toCurrency())
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun multiple_currencies_wrong_layout_union_collision() {
        val input = "1.583в + 1,417 ,tk"
        val expectedExpr = Add(
            CurrenciedExpression(1.583.asConst, "USD".toCurrency()),
            CurrenciedExpression(1.417.asConst, "BYN".toCurrency())
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun multiple_currencies_division() {
        val input = "4.06в / 2, 03 ,ey"
        val expectedExpr = Divide(
            CurrenciedExpression(4.06.asConst, "USD".toCurrency()),
            CurrenciedExpression(2.03.asConst, "BYN".toCurrency())
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun multiple_currencies_division_different_api() {
        val input = "4.06во / 2, 03 ,ey NBU"
        val expectedExpr = Divide(
            CurrenciedExpression(4.06.asConst, "KRW".toCurrency()),
            CurrenciedExpression(2.03.asConst, "BYN".toCurrency())
        )

        val (actualExpr, additionalCurrencies, rateApi) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
        assertEquals("NBU".toRateApi(), rateApi)
    }


    @Test
    fun multiple_currencies_division_with_addition() {
        val input = "4.06в / 2, 03 ,ey + 10"
        val expectedExpr = Add(
            Divide(
                CurrenciedExpression(4.06.asConst, "USD".toCurrency()),
                CurrenciedExpression(2.03.asConst, "BYN".toCurrency())
            ),
            10.asConst
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }


    @Test
    fun multiple_currencies_division_complicated() {
        val input = "((4.06 + 1 - 1)$ + 10uah) / 2, 03 беларуси + 10 - (10в/15,)"
        val expectedExpr = Subtract(
            Add(
                Divide(
                    Add(
                        CurrenciedExpression(Subtract(Add(4.06.asConst, 1.asConst), 1.asConst), "USD".toCurrency()),
                        CurrenciedExpression(10.asConst, "UAH".toCurrency())
                    ),
                    CurrenciedExpression(2.03.asConst, "BYN".toCurrency())
                ),
                10.asConst
            ),
            Divide(CurrenciedExpression(10.asConst, "USD".toCurrency()), CurrenciedExpression(15.asConst, "BYN".toCurrency()))
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun multiple_currencies_division_inside_denominator() {
        val input = "((4.06 + 1 - 1)$ + 10uah)/10uah / (10в/15byn)"
        val expectedExpr = Divide(
            Divide(
                Add(
                    CurrenciedExpression(Subtract(Add(4.06.asConst, 1.asConst), 1.asConst), "USD".toCurrency()),
                    CurrenciedExpression(10.asConst, "UAH".toCurrency())
                ),
                CurrenciedExpression(10.asConst, "UAH".toCurrency())
            ),
            Divide(CurrenciedExpression(10.asConst, "USD".toCurrency()), CurrenciedExpression(15.asConst, "BYN".toCurrency()))
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun additional_currencies_ininto_unions() {
        val input = "10 euro into бр IN dollars"
        val expectedAdditionalCurrencies = setOf(
            "BYN".toCurrency(),
            "USD".toCurrency()
        )
        val expectedExpr = CurrenciedExpression(10.asConst, "EUR".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertEquals(expectedExpr, actualExpr)

        assertEqualsUnordered(expectedAdditionalCurrencies, additionalCurrencies)
    }

    @Test
    fun additional_currencies_russian_unions() {
        val input = "10 бун в доллар на евро"
        val expectedAdditionalCurrencies = setOf(
            "USD".toCurrency(),
            "EUR".toCurrency()
        )
        val expectedExpr = CurrenciedExpression(10.asConst, "BYN".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertEquals(expectedExpr, actualExpr)

        assertEqualsUnordered(expectedAdditionalCurrencies, additionalCurrencies)
    }

    @Test
    fun additional_currencies_symbol_prefixes() {
        val input = "10 euro ! бр !dollars"
        val expectedAdditionalCurrencies = setOf(
            "BYN".toCurrency(),
            "USD".toCurrency()
        )
        val expectedExpr = CurrenciedExpression(10.asConst, "EUR".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertEquals(expectedExpr, actualExpr)

        assertEqualsUnordered(expectedAdditionalCurrencies, additionalCurrencies)
    }

    @Test
    fun different_currency_api() {
        val input = "10 euro nbu"
        val expectedExpr = CurrenciedExpression(10.asConst, "EUR".toCurrency())

        val (actualExpr, additionalCurrencies, apiConfig) = grammar.parseToEnd(input)

        assertEquals(expectedExpr, actualExpr)
        assertEquals(apiConfig, "NBU".toRateApi())
        assertTrue(additionalCurrencies.isEmpty())
    }


    @Test
    fun explicit_currency_api() {
        val input = "10 euro nbp into usd"
        val expectedExpr = CurrenciedExpression(10.asConst, "EUR".toCurrency())

        val expectedAdditionalCurrencies = setOf("USD".toCurrency())

        val (actualExpr, additionalCurrencies, apiConfig) = grammar.parseToEnd(input)

        assertEquals(expectedExpr, actualExpr)
        assertEquals(apiConfig, "NBP".toRateApi())
        assertEqualsUnordered(expectedAdditionalCurrencies, additionalCurrencies)

    }


    @Test
    fun explicit_api_only() {
        val input = "10 fore"
        val expectedExpr = 10.asConst

        val expectedAdditionalCurrencies = setOf<Currency>()

        val (actualExpr, additionalCurrencies, apiConfig) = grammar.parseToEnd(input)

        assertEquals(expectedExpr, actualExpr)
        assertEquals(apiConfig, "Forex".toRateApi())
        assertEqualsUnordered(expectedAdditionalCurrencies, additionalCurrencies)

    }

    @Test
    fun explicit_currency_api_with_name_collision() {
        val input = "10 euro tm into usd"
        val expectedExpr = CurrenciedExpression(10.asConst, "EUR".toCurrency())

        val expectedAdditionalCurrencies = setOf("USD".toCurrency())

        val (actualExpr, additionalCurrencies, apiConfig) = grammar.parseToEnd(input)

        assertEquals(expectedExpr, actualExpr)
        assertEquals(apiConfig, "TraderMade".toRateApi())
        assertEqualsUnordered(expectedAdditionalCurrencies, additionalCurrencies)

    }

    @Test
    fun kilo_suffix() {
        val input = "10k"
        val expectedExpr = ConstWithSuffixes(10.bigDecimal, 1, SuffixType.KILO)

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun kilo_suffixes_with_spaces() {
        val input = "10 kk k"
        val expectedExpr = ConstWithSuffixes(10.bigDecimal, 3, SuffixType.KILO)

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun mega_suffixes() {
        val input = "10Mmm"
        val expectedExpr = ConstWithSuffixes(10.bigDecimal, 3, SuffixType.MEGA)

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }


    @Test
    fun suffixes_with_alias_without_spaces() {
        val input = "10kkbyn"
        val expectedExpr = CurrenciedExpression(
            ConstWithSuffixes(10.bigDecimal, 2, SuffixType.KILO),
            "BYN".toCurrency()
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun suffixes_with_alias_starting_from_same_letter_without_spaces() {
        val input = "10kkkz"
        val expectedExpr = CurrenciedExpression(
            ConstWithSuffixes(10.bigDecimal, 2, SuffixType.KILO),
            "KZT".toCurrency()
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun suffixes_with_alias_starting_from_same_letter_with_spaces() {
        val input = "10k k kz"
        val expectedExpr = CurrenciedExpression(
            ConstWithSuffixes(10.bigDecimal, 2, SuffixType.KILO),
            "KZT".toCurrency()
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun prefix_currency_notation() {
        val input = "$10"
        val expectedExpr = CurrenciedExpression(10.asConst, "USD".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun multi_currency_expression_with_prefix_currency_notation() {
        val input = "$10 + и33"
        val expectedExpr = Add(
            CurrenciedExpression(10.asConst, "USD".toCurrency()),
            CurrenciedExpression(33.asConst, "BYN".toCurrency())
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun multi_currency_expression_with_prefix_and_postfix_currency_notation() {
        val input = "$10 + 33 eur"
        val expectedExpr = Add(
            CurrenciedExpression(10.asConst, "USD".toCurrency()),
            CurrenciedExpression(33.asConst, "EUR".toCurrency())
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun thousands_separator_american() {
        val input = "5,699.20 zł"
        val expectedExpr = CurrenciedExpression(5699.2.asConst, "PLN".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun thousands_separator_american_integer() {
        val input = "5,699,000 zł"
        val expectedExpr = CurrenciedExpression(5699000.asConst, "PLN".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun thousands_separator_german() {
        val input = "5.699.000,20 zł"
        val expectedExpr = CurrenciedExpression(5699000.2.asConst, "PLN".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun thousands_separator_german_integer() {
        val input = "5.699.000 zł"
        val expectedExpr = CurrenciedExpression(5699000.asConst, "PLN".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun thousands_separator_unknown() {
        val input = "5,699.000,20 zł"
        val expectedExpr = CurrenciedExpression(569900020.asConst, "PLN".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }


//    @Test
//    fun kilo_with_alias_collision() {
//        val input = "5kzl"
//        val expectedExpr = CurrenciedExpression(ConstWithSuffixes("5".bigDecimal, 1, SuffixType.KILO), "PLN".toCurrency())
//
//        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)
//
//        assertTrue(additionalCurrencies.isEmpty())
//        assertEquals(expectedExpr, actualExpr)
//    }
}