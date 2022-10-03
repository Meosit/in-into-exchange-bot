package org.mksn.inintobot.common.rate

import org.mksn.inintobot.common.currency.Currency
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Currency API Configuration
 */
data class RateApi(
    /**
     * Name of the API to reference it within an app
     */
    val name: String,
    /**
     * Currency base used by this API
     */
    val aliases: Array<String>,
    /**
     * Currency base used by this API
     */
    val base: Currency,
    /**
     * API url where to fetch the data from
     */
    val url: String,
    /**
     * API display link to be shown in user documentation
     */
    val displayLink: String,
    /**
     * List of currency codes which are unsupported by this API against the list of all currencies
     */
    val unsupported: Set<String>,
    /**
     * Number of hours after the last refresh to consider the data is outdated and should be refreshed
     */
    val refreshHours: Int,
    /**
     * Number of hours when rates are considered as stale
     */
    val staleTimeoutHours: Int = refreshHours + 1,

    val backFillInfo: RateApiBackFillInfo?,
) {
    /**
     * Comparison is performed only over the API name.
     * By design the [name] must be unique.
     */
    override fun equals(other: Any?): Boolean = when (other) {
        is RateApi -> other.name == name
        else -> false
    }

    override fun toString() = "RateApi[$name]"

    override fun hashCode(): Int {
        return name.hashCode()
    }
}


data class RateApiBackFillInfo(
    val url: String,
    val backFillLimit: LocalDate,
    val backFillDisabled: Boolean = false,
    val dateFormat: DateTimeFormatter = DateTimeFormatter.ISO_DATE,
)