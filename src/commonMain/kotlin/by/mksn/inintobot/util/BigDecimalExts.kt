package by.mksn.inintobot.util

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode
import com.ionspin.kotlin.bignum.decimal.toBigDecimal


/**
 * Default decimal places for big decimals
 */
const val DEFAULT_DECIMAL_PRECISION: Long = 16

/**
 * Default rounding mode.
 * Need to change this with ROUND_HALF_EVEN when it will be supported
 */
val DEFAULT_ROUNDING_MODE = RoundingMode.ROUND_HALF_AWAY_FROM_ZERO

/**
 * Default mode with some reasonable precision to avoid non-terminating operations
 */
val DEFAULT_DECIMAL_MODE = DecimalMode(DEFAULT_DECIMAL_PRECISION, DEFAULT_ROUNDING_MODE)

@ExperimentalUnsignedTypes
fun String.toFiniteBigDecimal() = toBigDecimal(decimalMode = DEFAULT_DECIMAL_MODE)
    .roundToDigitPositionAfterDecimalPoint(DEFAULT_DECIMAL_PRECISION, DEFAULT_ROUNDING_MODE)

@ExperimentalUnsignedTypes
fun Int.toFiniteBigDecimal() = toBigDecimal(decimalMode = DEFAULT_DECIMAL_MODE)
    .roundToDigitPositionAfterDecimalPoint(DEFAULT_DECIMAL_PRECISION, DEFAULT_ROUNDING_MODE)

@ExperimentalUnsignedTypes
fun Double.toFiniteBigDecimal() = toBigDecimal(decimalMode = DEFAULT_DECIMAL_MODE)
    .roundToDigitPositionAfterDecimalPoint(DEFAULT_DECIMAL_PRECISION, DEFAULT_ROUNDING_MODE)

/**
 * Fixed version of the [BigDecimal.toStringExpanded] function with trailing point removed
 */
@ExperimentalUnsignedTypes
fun BigDecimal.toStr() = toStringExpanded().trimEnd { it == '.' }
