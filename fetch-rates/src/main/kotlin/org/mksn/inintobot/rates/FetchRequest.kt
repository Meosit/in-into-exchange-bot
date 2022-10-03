package org.mksn.inintobot.rates

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class FetchRequest(
    @Serializable(LocalDateSerializer::class)
    val date: LocalDate? = null,
    val backfill: Boolean = false,
    val skipApis: List<String> = listOf(),
)


