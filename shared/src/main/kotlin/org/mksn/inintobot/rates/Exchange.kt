package org.mksn.inintobot.rates

import org.mksn.inintobot.currency.Currency
import java.math.BigDecimal

/**
 * Container class which represents the result of sum exchange
 */
data class Exchange(
    val currency: Currency,
    val value: BigDecimal
)