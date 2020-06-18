package by.mksn.inintobot.misc

import java.math.BigDecimal
import java.math.RoundingMode


/**
 * Default decimal places for big decimals
 */
const val DEFAULT_DECIMAL_DIGITS: Int = 16

/**
 * Default rounding mode.
 */
val DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP

fun String.toFixedScaleBigDecimal(): BigDecimal = toBigDecimal().setScale(DEFAULT_DECIMAL_DIGITS, DEFAULT_ROUNDING_MODE)

fun Int.toFixedScaleBigDecimal(): BigDecimal = toBigDecimal().setScale(DEFAULT_DECIMAL_DIGITS, DEFAULT_ROUNDING_MODE)

fun Double.toFixedScaleBigDecimal(): BigDecimal = toBigDecimal().setScale(DEFAULT_DECIMAL_DIGITS, DEFAULT_ROUNDING_MODE)

fun BigDecimal.toFixedScaleBigDecimal(): BigDecimal = setScale(DEFAULT_DECIMAL_DIGITS, DEFAULT_ROUNDING_MODE)

/**
 * Shortcut for converting number to string without scientific notation
 */
fun BigDecimal.toStr(): String = stripTrailingZeros().toPlainString()

/**
 * Converts a value to string with rounding to [decimalDigits] after decimal point
 */
fun BigDecimal.toStr(decimalDigits: Int): String =
    setScale(decimalDigits, DEFAULT_ROUNDING_MODE).stripTrailingZeros().toPlainString()
