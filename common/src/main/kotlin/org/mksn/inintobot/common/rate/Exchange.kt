package org.mksn.inintobot.common.rate

import org.mksn.inintobot.common.currency.Currency
import java.math.BigDecimal

/**
 * Container class which represents the result of sum exchange
 */
data class Exchange(
    val currency: Currency,
    val value: BigDecimal
)