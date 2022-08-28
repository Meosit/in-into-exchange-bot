package org.mksn.inintobot.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class LibKtTest {

    @Test
    fun testSharedFunction() {
        assertEquals("", sharedFunction(arrayOf("")))
    }
}