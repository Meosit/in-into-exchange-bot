package org.mksn.inintobot.rates

import kotlinx.serialization.Serializable
import org.mksn.inintobot.currency.Currency
import org.mksn.inintobot.misc.BigDecimalSerializer
import java.math.BigDecimal

/**
 * Container class which represents the result of sum exchange
 */
@Serializable
data class Exchange(
    val currency: Currency,
    @Serializable(BigDecimalSerializer::class)
    val value: BigDecimal
)