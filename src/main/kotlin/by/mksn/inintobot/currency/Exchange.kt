package by.mksn.inintobot.currency

import by.mksn.inintobot.misc.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Container class which represents the result of sum exchange
 */
@Serializable
data class Exchange(
    val currency: Currency,
    @Serializable(with = BigDecimalSerializer::class)
    val value: BigDecimal
)