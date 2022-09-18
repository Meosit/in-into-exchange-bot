package org.mksn.inintobot.rates

import io.ktor.http.*
import kotlin.test.assertEquals

private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"

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
