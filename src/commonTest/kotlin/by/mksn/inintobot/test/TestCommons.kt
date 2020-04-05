package by.mksn.inintobot.test

import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.currency.CurrencyAliasMatcher
import by.mksn.inintobot.expression.Const
import by.mksn.inintobot.util.toFiniteBigDecimal
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlin.test.assertEquals


/**
 * A list of [Currency] objects to be used in tests
 */
// @formatter:off
val testCurrencies = listOf(
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
    ),
    Currency(
        code = "KZT",
        emoji = "🇰🇿",
        aliases = setOf("KZT", "kz", "tenge", "тенге", "тенги", "тенг", "тнг")
    )
)
// @formatter:on

/**
 * A matcher based on the [testCurrencies]
 */
@ExperimentalStdlibApi
val testCurrencyAliasMatcher = CurrencyAliasMatcher(testCurrencies)

/**
 * A short way of receiving a [Currency] from [testCurrencies] list.
 */
@ExperimentalStdlibApi
fun String.toCurrency() = testCurrencyAliasMatcher.match(this)

/**
 * A short way of defining a [Const] expression from literal
 */
@ExperimentalUnsignedTypes
val Int.asConst
    get() = Const(toFiniteBigDecimal())

/**
 * A short way of defining a [Const] expression from literal
 */
@ExperimentalUnsignedTypes
val Double.asConst
    get() = Const(toFiniteBigDecimal())

/**
 * A short way of defining a [Const] expression from literal
 */
@ExperimentalUnsignedTypes
val String.asConst
    get() = Const(toFiniteBigDecimal())

/**
 * A short way of defining a [BigDecimal] from literal
 */
@ExperimentalUnsignedTypes
val Double.bigDecimal: BigDecimal
    get() = toFiniteBigDecimal()

/**
 * A short way of defining a [BigDecimal] from literal
 */
@ExperimentalUnsignedTypes
val Int.bigDecimal: BigDecimal
    get() = toFiniteBigDecimal()

/**
 * A short way of defining a [BigDecimal] from literal
 */
@ExperimentalUnsignedTypes
val String.bigDecimal: BigDecimal
    get() = toFiniteBigDecimal()

/**
 * Asserts two [Iterable]s in the strict order
 */
fun <E, C : Iterable<E>> assertEqualsOrdered(expected: C, actual: C) {
    val expectedSeq = expected.asSequence()
    val actualSeq = actual.asSequence()
    assertEquals(expectedSeq.count(), actualSeq.count(), "Given collections have different size!")
    expectedSeq.zip(actualSeq).forEachIndexed { index, (expected, actual) ->
        assertEquals(expected, actual, "Elements at index $index are not same!")
    }
}

/**
 * Asserts two [Iterable]s without specific order using [expected] as a base one
 */
fun <E, C : Iterable<E>> assertEqualsUnordered(expected: C, actual: C) {
    assertEquals(expected.asSequence().count(), actual.asSequence().count(), "Given collections have different size!")
    expected.forEachIndexed { index, expectedElem ->
        assertEquals(
            expectedElem, actual.find { expected == it },
            "Expected element at index $index not found in actual collection!"
        )
    }
}