package by.mksn.inintobot.grammar

import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.currency.CurrencyAliasMatcher
import by.mksn.inintobot.expression.*
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalStdlibApi
@ExperimentalUnsignedTypes
class BotInputGrammarTest {

    // @formatter:off
    private val currencies = setOf(
        Currency(
            code = "BYN",
            emoji = "🇧🇾",
            aliases = setOf("BYN", "BYR", "bel", "by", "br", "b", "бун", "бунов", "буны", "буна", "бунах", "бур", "бел", "бр", "б")
        ),
        Currency(
            code = "USD",
            emoji = "🇺🇸",
            aliases = setOf("USD", "us", "dollar", "dollars", "u", "d", "$", "бакс", "баксы", "баксов", "бакса", "баксах", "доллар", "доллары", "долларов", "доллара", "долларах", "долл", "дол", "д", "юсд")
        ),
        Currency(
            code = "EUR",
            emoji = "🇪🇺",
            aliases = setOf("EUR", "euro", "eu", "e", "€", "евро", "евр", "еур", "е")
        ),
        Currency(
            code = "UAH",
            emoji = "🇺🇦",
            aliases = setOf("UAH", "grn", "gr", "ua", "₴", "гривн", "гривна", "гривни", "гривны", "гривня", "гривен", "гривень", "гривнях", "грн", "гр", "г")
        )
    )
    // @formatter:on

    private val currencyAliasMatcher = CurrencyAliasMatcher(currencies)
    private val grammar = BotInputGrammar(TokenNames.DEFAULT, currencyAliasMatcher)

    @Suppress("PrivatePropertyName")
    private val Int.Const
        get() = Const(this.toBigDecimal())

    @Suppress("PrivatePropertyName")
    private val Double.Const
        get() = Const(this.toBigDecimal())

    @Test
    fun single_value() {
        val input = "1"
        val expectedExpr = 1.Const

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun simple_expression_1() {
        val input = "1 + 1"
        val expectedExpr = Add(1.Const, 1.Const)

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun simple_expression_2() {
        val input = "1 * 1"
        val expectedExpr = Multiply(1.Const, 1.Const)

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun simple_expression_3() {
        val input = "1 * 1 + 2"
        val expectedExpr = Add(
            Multiply(
                1.Const,
                1.Const
            ), 2.Const
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun expression_with_brackets_1() {
        val input = "(1 * 1) / (7 - 2)"
        val expectedExpr = Divide(
            Multiply(
                1.Const,
                1.Const
            ), Subtract(7.Const, 2.Const)
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun expression_with_brackets_2() {
        val input = "(1 + (2 - 1)) * ((7 - 2) / 2)"
        val expectedExpr = Multiply(
            Add(
                1.Const,
                Subtract(2.Const, 1.Const)
            ),
            Divide(
                Subtract(
                    7.Const,
                    2.Const
                ), 2.Const
            )
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun value_with_different_currency() {
        val input = "1 гривна"
        val currency = currencyAliasMatcher.matchToCode("UAH")
        val expectedExpr = CurrenciedExpression(1.Const, currency)

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }

    @Test
    fun simple_expression_with_different_currency() {
        val input = "1 + 1 гривна"
        val currency = currencyAliasMatcher.matchToCode("UAH")
        val expectedExpr = CurrenciedExpression(
            Add(
                1.Const,
                1.Const
            ), currency
        )

        val (actualExpr, additionalCurrencies) = grammar.parseToEnd(input)

        assertTrue(additionalCurrencies.isEmpty())
        assertEquals(expectedExpr, actualExpr)
    }
}