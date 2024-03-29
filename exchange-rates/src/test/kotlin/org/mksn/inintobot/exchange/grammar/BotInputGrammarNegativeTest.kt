package org.mksn.inintobot.exchange.grammar

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.MismatchedToken
import com.github.h0tk3y.betterParse.parser.NoMatchingToken
import com.github.h0tk3y.betterParse.parser.UnparsedRemainder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BotInputGrammarNegativeTest {

    private val grammar = BotInputGrammar

    @Test
    fun kilo_suffix_in_the_middle_of_number() {
        val input = "100k 000 ,h"

        val result = grammar.tryParseToEnd(input)

        assertIs<UnparsedRemainder>(result)
        assertEquals(6, result.startsWith.column)
    }

    @Test
    fun invalid_operator_at_the_end() {
        val input = "123^^"

        val result = grammar.tryParseToEnd(input)

        assertIs<UnparsedRemainder>(result)
        assertEquals(4, result.startsWith.column)
    }

    @Test
    fun invalid_currency_alias_at_start() {
        val input = "asd?/"

        val result = grammar.tryParseToEnd(input)
        println(result)
        assertIs<NoMatchingToken>(result)
        assertEquals(1, result.tokenMismatch.column)
        assertEquals("asd?/", result.tokenMismatch.text)
    }

    @Test
    fun invalid_operator_at_start() {
        val input = "^23"

        val result = grammar.tryParseToEnd(input)

        assertIs<NoMatchingToken>(result)
        assertEquals(1, result.tokenMismatch.column)
        assertEquals("^23", result.tokenMismatch.text)
    }

    @Test
    fun division_operator_at_start() {
        val input = "/*123 &BYN"

        val result = grammar.tryParseToEnd(input)

        assertIs<MismatchedToken>(result)
        assertEquals(1, result.found.column)
        assertEquals("/", result.found.text)
    }

    @Test
    fun multiply_operator_at_start() {
        val input = "*1000"

        val result = grammar.tryParseToEnd(input)

        assertIs<MismatchedToken>(result)
        assertEquals(1, result.found.column)
        assertEquals("*", result.found.text)
    }

    @Test
    fun multiplication_by_currencied_number_with_sum() {
        val input = "123EUR * 123 USD + 10 EUR"

        val result = grammar.tryParseToEnd(input)

        assertIs<UnparsedRemainder>(result)
        assertEquals(14, result.startsWith.column)
        assertEquals("USD", result.startsWith.text)
    }

    @Test
    fun division_by_currencied_number_with_sum() {
        val input = "123EUR / 123 USD + 10 EUR"

        val result = grammar.tryParseToEnd(input)

        assertIs<UnparsedRemainder>(result)
        assertEquals(14, result.startsWith.column)
        assertEquals("USD", result.startsWith.text)
    }


    @Test
    fun division_by_currencied_number_with_different_default() {
        val input = "123EUR / 123 USD EUR"

        val result = grammar.tryParseToEnd(input)

        assertIs<UnparsedRemainder>(result)
        assertEquals(14, result.startsWith.column)
        assertEquals("USD", result.startsWith.text)
    }

    @Test
    fun invalid_decimal_part_delimiter() {
        val input = "1.9к\$ * 0ю91"

        val result = grammar.tryParseToEnd(input)

        assertIs<UnparsedRemainder>(result)
        assertEquals(10, result.startsWith.column, result.toString())
        assertEquals("ю", result.startsWith.text, result.toString())
    }

    @Test
    fun two_consequent_currency_definitions() {
        val input = "123 EUR BYN"

        val result = grammar.tryParseToEnd(input)

        assertIs<UnparsedRemainder>(result)
        assertEquals(9, result.startsWith.column)
        assertEquals("BYN", result.startsWith.text)
    }

    @Test
    fun invalid_alias_which_contains_the_valid_one() {
        val input = "1 usdd"

        val result = grammar.tryParseToEnd(input)


        assertIs<UnparsedRemainder>(result)
        assertEquals(3, result.startsWith.column, "$result (${grammar.tokenizer.tokenize(input).toList()})")
    }

    @Test
    fun native_union_concatenated_with_currency() {
        val input = "18 euroinpounds"

        val result = grammar.tryParseToEnd(input)

        assertIs<UnparsedRemainder>(result)
        assertEquals(4, result.startsWith.column)
    }


    @Test
    fun native_union_with_currency_alias_collision() {
        val input = "18 в + 11 euro"

        val result = grammar.tryParseToEnd(input)

        assertIs<UnparsedRemainder>(result)
        assertEquals(4, result.startsWith.column)
    }

    @Test
    fun invalid_alias_in_multi_currency_expression() {
        val input = "10 долларов + 10 asdf"

        val result = grammar.tryParseToEnd(input)
        println(result)
        assertIs<UnparsedRemainder>(result)
        assertEquals(13, result.startsWith.column)
        assertEquals("+", result.startsWith.text)
    }

    @Test
    fun currency_only_query_with_illegal_char_at_the_end() {
        val input = "доллар ?"

        val result = grammar.tryParseToEnd(input)

        assertIs<UnparsedRemainder>(result)
        assertEquals(8, result.startsWith.column)
    }

    @Test
    fun currency_only_query_with_invalid_date() {
        val input = "доллар ? 2022-03-35"

        val result = grammar.tryParseToEnd(input)

        assertIs<UnparsedRemainder>(result)
        assertEquals(8, result.startsWith.column)
    }

    @Test
    fun currency_only_query_with_no_spaces() {
        val input = "долларon 2022-03-35"

        val result = grammar.tryParseToEnd(input)

        assertIs<NoMatchingToken>(result)
        assertEquals(1, result.tokenMismatch.column)
    }


    @Test
    fun currency_only_query_with_no_spaces_with_precision() {
        val input = "доллар #14on 2022-03-35"

        val result = grammar.tryParseToEnd(input)

        assertIs<UnparsedRemainder>(result)
        assertEquals(11, result.startsWith.column)
    }

    @Test
    fun currency_only_query_with_invalid_alias() {
        val input = "adfs"

        val result = grammar.tryParseToEnd(input)
        println(result)
        assertIs<NoMatchingToken>(result)
        assertEquals(1, result.tokenMismatch.column)
        assertEquals("adfs", result.tokenMismatch.text)
    }

    @Test
    fun invalid_api_name() {
        val input = "10 euro IIIII"

        val result = grammar.tryParseToEnd(input)
        println(result)
        assertIs<UnparsedRemainder>(result)
        assertEquals(9, result.startsWith.column)
        assertEquals("IIIII", result.startsWith.text)
    }

    @Test
    fun currency_instead_of_api() {
        val input = "10 euro kzt !usd"

        val result = grammar.tryParseToEnd(input)
        println(result)
        assertIs<UnparsedRemainder>(result)
        assertEquals(9, result.startsWith.column)
        assertEquals("kzt", result.startsWith.text)
    }

    @Test
    fun api_wrong_placement() {
        val input = "10 euro nbrb !usd"

        val result = grammar.tryParseToEnd(input)
        println(result)
        assertIs<UnparsedRemainder>(result)
        assertEquals(14, result.startsWith.column)
        assertEquals("!", result.startsWith.text)
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
        assertIs<NoMatchingToken>(result)
        assertEquals(1, result.tokenMismatch.column)
        assertEquals("\uD83C\uDDE7\uD83C\uDDFEBYN  5.68 \uD83C\uDDFA\uD83C\uDDF8USD  2.37 \uD83C\uDDEA\uD83C\uDDFAEUR  2.07 \uD83C\uDDF7\uD83C\uDDFARUB  170", result.tokenMismatch.text)
    }
}