package by.mksn.inintobot.grammar

import by.mksn.inintobot.expression.*
import by.mksn.inintobot.test.*
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BotInputGrammarPositiveTest {

    private val grammar = BotInputGrammar(testCurrencyAliasMatcher, testApiAliasMatcher)

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
    fun simple_expression_with_different_currency() {
        val input = "1 + 1 гривна"
        val expectedExpr = CurrenciedExpression(Add(1.asConst, 1.asConst), "UAH".toCurrency())

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun multiple_currencies_wrong_layout() {
        val input = "1.583 ljkkfh + 1,417 ,ey"
        val expectedExpr = Add(
            CurrenciedExpression(1.583.asConst, "USD".toCurrency()),
            CurrenciedExpression(1.417.asConst, "BYN".toCurrency())
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
        val input = "10 euro nbrb into usd"
        val expectedExpr = CurrenciedExpression(10.asConst, "EUR".toCurrency())

        val expectedAdditionalCurrencies = setOf("USD".toCurrency())

        val (actualExpr, additionalCurrencies, apiConfig) = grammar.parseToEnd(input)

        assertEquals(expectedExpr, actualExpr)
        assertEquals(apiConfig, "NBRB".toRateApi())
        assertEqualsUnordered(expectedAdditionalCurrencies, additionalCurrencies)

    }

    @Test
    fun comma_alias_currency() {
        val input = "2, + 3 euro"
        val expectedExpr = Add(
            CurrenciedExpression(2.asConst, "BYN".toCurrency()),
            CurrenciedExpression(3.asConst, "EUR".toCurrency())
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
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
}