package org.mksn.inintobot.rates

import io.ktor.http.*
import org.mksn.inintobot.currency.Currencies
import org.mksn.inintobot.currency.Currency
import kotlin.test.assertEquals


/**
 * A list of [Currency] objects to be used in tests
 */
// @formatter:off
val testCurrencies = listOf(
    Currency(
        code = "BYN",
        emoji = "🇧🇾",
    ),
    Currency(
        code = "USD",
        emoji = "🇺🇸",
    ),
    Currency(
        code = "EUR",
        emoji = "🇪🇺",
    ),
    Currency(
        code = "UAH",
        emoji = "🇺🇦",
    ),
    Currency(
        code = "KZT",
        emoji = "🇰🇿",
    ),
    Currency(
        code = "PLN",
        emoji = "🇰🇿",
    ),
)

val testApis = listOf(
    RateApi(
        name = "NBRB",
        base = Currencies.forCode("BYN"),
        url = "http://www.nbrb.by/API/ExRates/Rates?Periodicity=0",
        displayLink = "blah",
        unsupported = setOf(),
        refreshHours = 1
    ),
    RateApi(
        name = "NBU",
        base = Currencies.forCode("UAH"),
        url = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json",
        displayLink = "blah",
        unsupported = setOf(),
        refreshHours = 1
    ),
    RateApi(
        name = "TraderMade",
        base = Currencies.forCode("USD"),
        url = "blah",
        displayLink = "blah",
        unsupported = setOf(),
        refreshHours = 1
    )
)
// @formatter:on

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
