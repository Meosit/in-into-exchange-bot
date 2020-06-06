package by.mksn.inintobot.config

/**
 * Currency API Configuration
 */
data class ApiConfig(
    /**
     * Name of the API to reference it within an app
     */
    val name: String,
    /**
     * Currency base 3-letter code
     */
    val baseCode: String,
    /**
     * API url where to fetch the data from
     */
    val url: String
)