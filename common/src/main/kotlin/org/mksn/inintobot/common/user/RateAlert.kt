package org.mksn.inintobot.common.user

import kotlinx.serialization.Serializable
import org.mksn.inintobot.common.misc.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class RateAlert(
    val id: String,
    val apiName: String,
    val fromCurrency: String,
    val toCurrency: String,
    val isRelative: Boolean,
    @Serializable(with = BigDecimalSerializer::class)
    val value: BigDecimal,
)