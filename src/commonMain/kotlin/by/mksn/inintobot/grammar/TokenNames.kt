package by.mksn.inintobot.grammar

/**
 * Container class for human-readable token names.
 * It's supposed that this class will be loaded from some kind of localized config
 */
data class TokenNames(
    val number: String,
    val kilo: String,
    val mega: String,
    val leftPar: String,
    val rightPar: String,
    val multiply: String,
    val divide: String,
    val minus: String,
    val plus: String,
    val whitespace: String,
    val currency: String,
    val exclamation: String,
    val ampersand: String,
    val nativeConversionUnion: String
) {
    companion object {
        val DEFAULT = TokenNames(
            number = "number",
            kilo = "kilo suffix",
            mega = "mega suffix",
            leftPar = "'('",
            rightPar = "')'",
            multiply = "'*'",
            divide = "'/'",
            minus = "'-'",
            plus = "'+'",
            whitespace = "space",
            currency = "currency",
            exclamation = "'!'",
            ampersand = "'&'",
            nativeConversionUnion = "union 'в'/'into'/'in'"
        )
    }
}