package by.mksn.inintobot.grammar

import by.mksn.inintobot.test.testApiAliasMatcher
import by.mksn.inintobot.test.testCurrencyAliasMatcher
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.MismatchedToken
import com.github.h0tk3y.betterParse.parser.NoMatchingToken
import com.github.h0tk3y.betterParse.parser.UnparsedRemainder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BotInputGrammarNegativeTest {

    private val grammar = BotInputGrammar(testCurrencyAliasMatcher, testApiAliasMatcher)

    @Test
    fun kilo_suffix_in_the_middle_of_number() {
        val input = "100k 000 ,h"

        val result = grammar.tryParseToEnd(input)

        assertTrue(result is UnparsedRemainder)
        assertEquals(6, result.startsWith.column)
    }

    @Test
    fun invalid_operator_at_the_end() {
        val input = "123^^"

        val result = grammar.tryParseToEnd(input)

        assertTrue(result is UnparsedRemainder)
        assertEquals(4, result.startsWith.column)
    }

    @Test
    fun invalid_currency_alias_at_start() {
        val input = "asd?/"

        val result = grammar.tryParseToEnd(input)
        println(result)
        assertTrue(result is NoMatchingToken)
        assertEquals(1, result.tokenMismatch.column)
        assertEquals("asd?/", result.tokenMismatch.text)
    }

    @Test
    fun invalid_operator_at_start() {
        val input = "%23"

        val result = grammar.tryParseToEnd(input)

        assertTrue(result is NoMatchingToken)
        assertEquals(1, result.tokenMismatch.column)
        assertEquals("%23", result.tokenMismatch.text)
    }

    @Test
    fun division_operator_at_start() {
        val input = "/*123 &BYN"

        val result = grammar.tryParseToEnd(input)

        assertTrue(result is MismatchedToken)
        assertEquals(1, result.found.column)
        assertEquals("/", result.found.text)
    }

    @Test
    fun multiply_operator_at_start() {
        val input = "*1000"

        val result = grammar.tryParseToEnd(input)

        assertTrue(result is MismatchedToken)
        assertEquals(1, result.found.column)
        assertEquals("*", result.found.text)
    }

    @Test
    fun division_by_currencied_number() {
        val input = "123EUR / 123 USD"

        val result = grammar.tryParseToEnd(input)

        assertTrue(result is UnparsedRemainder)
        assertEquals(14, result.startsWith.column)
        assertEquals("USD", result.startsWith.text)
    }

    @Test
    fun invalid_decimal_part_delimiter() {
        val input = "1.9к\$ * 0ю91"

        val result = grammar.tryParseToEnd(input)

        assertTrue(result is UnparsedRemainder)
        assertEquals(10, result.startsWith.column)
        assertEquals("ю91", result.startsWith.text)
    }

    @Test
    fun two_consequent_currency_definitions() {
        val input = "123 EUR BYN"

        val result = grammar.tryParseToEnd(input)

        assertTrue(result is UnparsedRemainder)
        assertEquals(9, result.startsWith.column)
        assertEquals("BYN", result.startsWith.text)
    }

    @Test
    fun invalid_alias_which_contains_the_valid_one() {
        val input = "1 usdd"

        val result = grammar.tryParseToEnd(input)

        assertTrue(result is UnparsedRemainder)
        assertEquals(6, result.startsWith.column)
    }

    @Test
    fun native_union_concatenated_with_currency() {
        val input = "18 euroinpounds"

        val result = grammar.tryParseToEnd(input)

        assertTrue(result is UnparsedRemainder)
        assertEquals(8, result.startsWith.column)
    }


    @Test
    fun native_union_with_currency_alias_collision() {
        val input = "18 в + 11 euro"

        val result = grammar.tryParseToEnd(input)

        assertTrue(result is UnparsedRemainder)
        assertEquals(4, result.startsWith.column)
    }

    @Test
    fun invalid_alias_in_multi_currency_expression() {
        val input = "10 долларов + 10 asdf"

        val result = grammar.tryParseToEnd(input)
        println(result)
        assertTrue(result is UnparsedRemainder)
        assertEquals(13, result.startsWith.column)
        assertEquals("+", result.startsWith.text)
    }

    @Test
    fun currency_only_query_with_illegal_char_at_the_end() {
        val input = "доллар ?"

        val result = grammar.tryParseToEnd(input)

        assertTrue(result is UnparsedRemainder)
        assertEquals(8, result.startsWith.column)
    }

    @Test
    fun currency_only_query_with_invalid_alias() {
        val input = "adfs"

        val result = grammar.tryParseToEnd(input)
        println(result)
        assertTrue(result is NoMatchingToken)
        assertEquals(1, result.tokenMismatch.column)
        assertEquals("adfs", result.tokenMismatch.text)
    }

    @Test
    fun invalid_api_name() {
        val input = "10 euro IIIII"

        val result = grammar.tryParseToEnd(input)
        println(result)
        assertTrue(result is UnparsedRemainder)
        assertEquals(9, result.startsWith.column)
        assertEquals("IIIII", result.startsWith.text)
    }

    @Test
    fun currency_instead_of_api() {
        val input = "10 euro kzt !usd"

        val result = grammar.tryParseToEnd(input)
        println(result)
        assertTrue(result is UnparsedRemainder)
        assertEquals(9, result.startsWith.column)
        assertEquals("kzt", result.startsWith.text)
    }

    @Test
    fun api_wrong_placement() {
        val input = "10 euro !usd nbrb"

        val result = grammar.tryParseToEnd(input)
        println(result)
        assertTrue(result is UnparsedRemainder)
        assertEquals(14, result.startsWith.column)
        assertEquals("nbrb", result.startsWith.text)
    }

    @Test
    fun output_as_input() {
        val input = """
            🇧🇾BYN  5.68
            🇺🇸USD  2.37
            🇪🇺EUR  2.07
            🇷🇺RUB  170
        """.trimIndent()

        val result = grammar.tryParseToEnd(input)
        println(result)
        assertTrue(result is NoMatchingToken)
        assertEquals(1, result.tokenMismatch.column)
        assertEquals("\uD83C\uDDE7\uD83C\uDDFEBYN  5.68 \uD83C\uDDFA\uD83C\uDDF8USD  2.37 \uD83C\uDDEA\uD83C\uDDFAEUR  2.07 \uD83C\uDDF7\uD83C\uDDFARUB  170", result.tokenMismatch.text)
    }
}