package org.mksn.inintobot.common.misc

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*


/**
 * Default decimal places for big decimals
 */
const val DEFAULT_DECIMAL_DIGITS: Int = 16

/**
 * Default rounding mode.
 */
val DEFAULT_ROUNDING_MODE = RoundingMode.HALF_EVEN

fun String.toFixedScaleBigDecimal(): BigDecimal = toBigDecimal().setScale(DEFAULT_DECIMAL_DIGITS, DEFAULT_ROUNDING_MODE)

fun Int.toFixedScaleBigDecimal(): BigDecimal = toBigDecimal().setScale(DEFAULT_DECIMAL_DIGITS, DEFAULT_ROUNDING_MODE)

fun Double.toFixedScaleBigDecimal(): BigDecimal = toBigDecimal().setScale(DEFAULT_DECIMAL_DIGITS, DEFAULT_ROUNDING_MODE)

fun BigDecimal.toFixedScaleBigDecimal(): BigDecimal = setScale(DEFAULT_DECIMAL_DIGITS, DEFAULT_ROUNDING_MODE)

fun BigDecimal.scaled(decimalDigits: Int): BigDecimal = setScale(decimalDigits, DEFAULT_ROUNDING_MODE)

/**
 * Shortcut for converting number to string without scientific notation
 */
fun BigDecimal.toStr(): String = stripTrailingZeros().toPlainString()

/**
 * Converts a value to string with rounding to [decimalDigits] after decimal point
 */
fun BigDecimal.toStr(
    decimalDigits: Int,
    stripZeros: Boolean = true,
    precise: Boolean = true,
    thousandSeparator: Char? = null
): String {
    val actualScale = if (precise) {
        val precision = stripTrailingZeros().precision()
        val scale = stripTrailingZeros().scale()
        kotlin.math.max(decimalDigits, kotlin.math.min(DEFAULT_DECIMAL_DIGITS, scale - precision + decimalDigits))
    } else decimalDigits

    val scaled = setScale(actualScale, DEFAULT_ROUNDING_MODE)
        .let { if (stripZeros) it.stripTrailingZeros() else it }

    return if (thousandSeparator == null) {
        scaled.toPlainString()
    } else {
        val decimalFormatSymbols = DecimalFormatSymbols(Locale.getDefault()).apply {
            groupingSeparator = thousandSeparator
        }
        val decimalFormat = DecimalFormat("#,##0.${"#".repeat(decimalDigits)}", decimalFormatSymbols).apply {
            isGroupingUsed = true
        }

        decimalFormat.format(scaled)
    }
}
