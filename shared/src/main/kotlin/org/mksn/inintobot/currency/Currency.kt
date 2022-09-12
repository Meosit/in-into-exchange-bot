package org.mksn.inintobot.currency

/**
 * Container for the specific currency
 *
 * @property code the ISO format 3-letter currency code
 * @property emoji emoji string used for the Bot output, basically this is a country flag
 */
data class Currency(val code: String, val emoji: String, val aliases: Array<String>) {
    /**
     * Comparison is performed only over the currency 3-letter [code]s.
     * By design the [code] must be unique.
     */
    override fun equals(other: Any?): Boolean = when (other) {
        is Currency -> other.code == code
        else -> false
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }
    companion object {

    }
}