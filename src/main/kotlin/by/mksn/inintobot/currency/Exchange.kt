package by.mksn.inintobot.currency

import java.math.BigDecimal

/**
 * Container class which represents the result of sum exchange
 */
data class Exchange(
    val currency: Currency,
    val value: BigDecimal
)