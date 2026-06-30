package org.mksn.inintobot.common.misc

import kotlin.test.Test
import kotlin.test.assertEquals

class BigDecimalExtsTest {

    @Test
    fun default_decimal_separator_preserves_current_output() {
        assertEquals("1234.56", "1234.56".toFixedScaleBigDecimal().toStr(2))
    }

    @Test
    fun comma_decimal_without_grouping() {
        assertEquals("1234,56", "1234.56".toFixedScaleBigDecimal().toStr(2, decimalSeparator = ','))
    }

    @Test
    fun comma_decimal_with_space_grouping() {
        assertEquals(
            "1 234,56",
            "1234.56".toFixedScaleBigDecimal().toStr(2, thousandSeparator = ' ', decimalSeparator = ',')
        )
    }

    @Test
    fun dot_decimal_with_comma_grouping() {
        assertEquals(
            "1,234.56",
            "1234.56".toFixedScaleBigDecimal().toStr(2, thousandSeparator = ',', decimalSeparator = '.')
        )
    }

    @Test
    fun strip_zeros_false_preserves_decimal_places() {
        assertEquals(
            "1 234,50",
            "1234.5".toFixedScaleBigDecimal().toStr(
                2,
                stripZeros = false,
                precise = false,
                thousandSeparator = ' ',
                decimalSeparator = ','
            )
        )
    }

    @Test
    fun zero_decimal_digits_do_not_render_dangling_separator() {
        assertEquals(
            "1 235",
            "1234.56".toFixedScaleBigDecimal().toStr(0, thousandSeparator = ' ', decimalSeparator = ',')
        )
    }
}
