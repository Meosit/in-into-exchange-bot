package by.mksn.inintobot.currency

import com.ionspin.kotlin.bignum.decimal.BigDecimal

/**
 * Container class which represents the result of sum exchange
 */
@ExperimentalUnsignedTypes
data class Exchange(
    val currency: Currency,
    val value: BigDecimal
)