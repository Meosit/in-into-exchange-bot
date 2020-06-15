package by.mksn.inintobot.misc

import by.mksn.inintobot.currency.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class AliasMatcherTest {

    @Test
    fun exact_currency_code() {
        val expectedCurrency = Currency("USD", "", setOf("USD", "dollar", "$"))
        val matcher = AliasMatcher(listOf(expectedCurrency))

        val actualCurrency = matcher.match("USD")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun random_case_currency_code() {
        val expectedCurrency = Currency("USD", "", setOf("USD", "dollar", "$"))
        val matcher = AliasMatcher(listOf(expectedCurrency))

        val actualCurrency = matcher.match("Usd")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun exact_alias() {
        val expectedCurrency = Currency("USD", "", setOf("USD", "dollar", "$"))
        val matcher = AliasMatcher(listOf(expectedCurrency))

        val actualCurrency = matcher.match("$")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun random_case_alias() {
        val expectedCurrency = Currency("USD", "", setOf("USD", "dollar", "$"))
        val matcher = AliasMatcher(listOf(expectedCurrency))

        val actualCurrency = matcher.match("dOlLar")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun russian_to_english_layout_alias() {
        val expectedCurrency = Currency("USD", "", setOf("USD", "dollar", "$"))
        val matcher = AliasMatcher(listOf(expectedCurrency))

        val actualCurrency = matcher.match("вщддфк")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun english_to_russian_layout_alias() {
        val expectedCurrency =
            Currency("USD", "", setOf("USD", "dollar", "доллар", "$"))
        val matcher = AliasMatcher(listOf(expectedCurrency))

        val actualCurrency = matcher.match("ljkkfh")

        assertEquals(expectedCurrency, actualCurrency)
    }

    @Test
    fun single_letter_alias_collision() {
        val usd = Currency("USD", "", setOf("USD", "u"))
        val uah = Currency("UAH", "", setOf("UAH", "г"))
        val matcher = AliasMatcher(listOf(usd, uah))

        val englishLetterCurrency = matcher.match("u")
        val russianLetterCurrency = matcher.match("г")

        assertEquals(usd, englishLetterCurrency)
        assertEquals(uah, russianLetterCurrency)
    }

    @Test
    fun single_non_letter_alias() {
        val expectedCurrency =
            Currency("BYN", "", setOf("BYN", "бр", "б", "бел"))
        val matcher = AliasMatcher(listOf(expectedCurrency))

        val commaCurrency = matcher.match(",")
        val twoLetterAliasCurrency = matcher.match(",h")

        assertEquals(expectedCurrency, commaCurrency)
        assertEquals(expectedCurrency, twoLetterAliasCurrency)
    }

    @Test
    fun no_such_alias() {
        val currency = Currency("BYN", "", setOf("BYN", "бр", "б", "бел"))
        val matcher = AliasMatcher(listOf(currency))

        val exception = assertFails { matcher.match("no such alias") }
        assertEquals("Invalid alias 'no such alias'", exception.message)
    }

}