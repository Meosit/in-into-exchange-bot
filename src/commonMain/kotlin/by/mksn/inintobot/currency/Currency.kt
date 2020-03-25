package by.mksn.inintobot.currency

/**
 * Container for the specific currency
 *
 * @property code the ISO format 3-letter currency code
 * @property emoji emoji string used for the Bot output, basically this is a country flag
 * @property aliases a set of official (if any) and commonly-used currency names including the [code] itself, e.g. `$` `dollar`
 */
data class Currency(val code: String, val emoji: String, val aliases: Set<String>) {
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
}