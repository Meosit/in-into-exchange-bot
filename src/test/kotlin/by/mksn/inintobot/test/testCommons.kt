package by.mksn.inintobot.test

import by.mksn.inintobot.api.RateApi
import by.mksn.inintobot.currency.Currency
import by.mksn.inintobot.expression.Const
import by.mksn.inintobot.misc.AliasMatcher
import by.mksn.inintobot.misc.toFixedScaleBigDecimal
import io.ktor.http.*
import java.math.BigDecimal
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
    ),
    Currency(
        code = "PLN",
        emoji = "🇰🇿",
        aliases = setOf("PLN", "PLZ", "zloty", "złoty", "zlot", "złot", "zł", "zl", "z", "pl", "злотый", "злотая", "злотые", "злотых", "злоты", "злот", "зл","з")
    ),
)

val testApis = listOf(
    RateApi(
        name = "NBRB",
        aliases = setOf("NBRB", "нбрб"),
        base = "BYN",
        url = "http://www.nbrb.by/API/ExRates/Rates?Periodicity=0",
        unsupported = setOf(),
        refreshHours = 1
    ),
    RateApi(
        name = "NBU",
        aliases = setOf("NBU", "нбу"),
        base = "UAH",
        url = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json",
        unsupported = setOf(),
        refreshHours = 1
    ),
    RateApi(
        name = "TraderMade",
        aliases = setOf("TraderMade", "tm", "тм"),
        base = "USD",
        url = "blah",
        unsupported = setOf(),
        refreshHours = 1
    )
)
// @formatter:on

/**
 * A matcher based on the [testCurrencies]
 */

val testCurrencyAliasMatcher = AliasMatcher(testCurrencies)

/**
 * A short way of receiving a [Currency] from [testCurrencies] list.
 */
fun String.toCurrency() = testCurrencyAliasMatcher.match(this)

val testApiAliasMatcher = AliasMatcher(testApis)

/**
 * A short way of receiving a [RateApi] from [testApis] list.
 */
fun String.toRateApi() = testApiAliasMatcher.match(this)

/**
 * A short way of defining a [Const] expression from literal
 */
val Int.asConst
    get() = Const(toFixedScaleBigDecimal())

/**
 * A short way of defining a [Const] expression from literal
 */
val Double.asConst
    get() = Const(toFixedScaleBigDecimal())

/**
 * A short way of defining a [Const] expression from literal
 */
val String.asConst
    get() = Const(toFixedScaleBigDecimal())

/**
 * A short way of defining a [BigDecimal] from literal
 */
val Double.bigDecimal: BigDecimal
    get() = toFixedScaleBigDecimal()

/**
 * A short way of defining a [BigDecimal] from literal
 */
val Int.bigDecimal: BigDecimal
    get() = toFixedScaleBigDecimal()

/**
 * A short way of defining a [BigDecimal] from literal
 */
val String.bigDecimal: BigDecimal
    get() = toFixedScaleBigDecimal()

/**
 * Asserts two [Iterable]s in the strict order
 */
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

/**
 * Asserts two [Iterable]s without specific order using [expected] as a base one
 */
fun <E : Any, C : Iterable<E>> assertEqualsUnordered(expected: C, actual: C, keyExtractor: (E) -> Any = { c -> c }) {
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

private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"
val Url.fullUrlWithoutQuery: String get() = "${protocol.name}://$hostWithPortIfRequired$encodedPath"
