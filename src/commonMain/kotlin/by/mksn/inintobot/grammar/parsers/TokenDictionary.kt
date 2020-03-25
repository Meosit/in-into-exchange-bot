package by.mksn.inintobot.grammar.parsers

import by.mksn.inintobot.currency.CurrencyAliasMatcher
import by.mksn.inintobot.grammar.TokenNames
import com.github.h0tk3y.betterParse.grammar.token
import com.github.h0tk3y.betterParse.lexer.Token


/**
 * Container class for the all available expression tokens
 *
 * @param tokenNames [TokenNames] class with localized names used in error messages
 * @param allCurrenciesRegex regular expression which matches all available currency aliases to
 *                           determine which token is a currency and which is just a malformed input
 */
@ExperimentalStdlibApi
class TokenDictionary(tokenNames: TokenNames, allCurrenciesRegex: Regex) {

    val number = token(tokenNames.number, "(\\d\\s*)+[.,](\\s*\\d)+|(\\d\\s*)*\\d") // greedy whitespace occupation
    val currency = token(tokenNames.currency, allCurrenciesRegex)

    // metric suffix must be placed after currency in the token list to support aliases which starts with the one of the suffixes
    val kilo = token(tokenNames.kilo, "[кКkK]")
    val mega = token(tokenNames.mega, "[мМmM]")

    val exclamation = token(tokenNames.exclamation, "!")
    val ampersand = token(tokenNames.ampersand, "&")

    /**
     * currency can be added with this prefix to allow expressions like '1 dollar into euro'
     */
    val inIntoUnion = token(tokenNames.nativeConversionUnion, "(?<=\\s)(into|in|в)(?=\\s)")
    val whitespace = token(tokenNames.whitespace, "\\s+", ignore = true)

    val leftPar = token(tokenNames.leftPar, "\\(")
    val rightPar = token(tokenNames.rightPar, "\\)")

    val multiply = token(tokenNames.multiply, "\\*")
    val divide = token(tokenNames.divide, "/")
    val minus = token(tokenNames.minus, "-")
    val plus = token(tokenNames.plus, "\\+")

    /**
     * This token is for proper error handling: it placed last and would be captured only of no other (valid) tokens matched.
     * @see InvalidCurrencyFoundException
     */
    val invalidCurrencyToken = token(tokenNames.currency, CurrencyAliasMatcher.BROAD_ALIAS_REGEX)

    val allTokens: List<Token> = listOf(
        number, currency,
        kilo, mega,
        exclamation, ampersand, inIntoUnion,
        whitespace,
        leftPar, rightPar,
        multiply, divide, minus, plus,
        invalidCurrencyToken
    )
}