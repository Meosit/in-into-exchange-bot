package org.mksn.inintobot.exchange.grammar.alias

import org.mksn.inintobot.common.currency.Currencies
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails

class AutocompleteAliasMatcherTest {


    @Test
    fun exact_currency_code() {
        val expectedCurrency = Currencies["USD"]
        val actualCurrency = CurrencyAliasMatcher.match("USD")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun random_case_currency_code() {
        val expectedCurrency = Currencies["USD"]

        val actualCurrency = CurrencyAliasMatcher.match("Usd")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun exact_alias() {
        val expectedCurrency = Currencies["USD"]

        val actualCurrency = CurrencyAliasMatcher.match("$")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun random_case_alias() {
        val expectedCurrency = Currencies["USD"]

        val actualCurrency = CurrencyAliasMatcher.match("dOlLar")

        assertEquals(expectedCurrency, actualCurrency)
    }


    @Test
    fun prefix_alias() {
        val expectedCurrency = Currencies["USD"]

        val actualCurrency = CurrencyAliasMatcher.match("dol")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun russian_to_english_layout_alias() {
        val expectedCurrency = Currencies["USD"]

        val actualCurrency = CurrencyAliasMatcher.match("вщддфк")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun keyboard_layout_alias() {
        val expectedCurrency =
            Currencies["USD"]

        val actualCurrency = CurrencyAliasMatcher.match("ljkkfh")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun transliterated_alias() {
        val expectedCurrency = Currencies["UAH"]

        val actualCurrency = CurrencyAliasMatcher.match("grivna")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun word_ending_alias() {
        val expectedCurrency = Currencies["GBP"]

        val actualCurrency = CurrencyAliasMatcher.match("фунтов")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun word_ending_single_letter() {
        val expectedCurrency = Currencies["USD"]

        val actualCurrency = CurrencyAliasMatcher.match("доллара")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun word_ending_short_candidate() {
        val expectedCurrency = Currencies["RON"]

        val actualCurrency = CurrencyAliasMatcher.match("лей")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun single_letter_alias_collision() {
        val usd = Currencies["USD"]
        val uah = Currencies["UAH"]

        val englishLetterCurrency = CurrencyAliasMatcher.match("u")
        val russianLetterCurrency = CurrencyAliasMatcher.match("г")

        assertEquals(usd, englishLetterCurrency)
        assertEquals(uah, russianLetterCurrency)
    }

    @Test
    fun no_such_alias() {
        val exception = assertFails { CurrencyAliasMatcher.match("no such alias") }
        assertContains(exception.message ?: "??", "no such alias")
    }

}