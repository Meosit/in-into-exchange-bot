package by.mksn.inintobot.grammar.parsers

import by.mksn.inintobot.currency.CurrencyAliasMatcher
import com.github.h0tk3y.betterParse.grammar.token
import com.github.h0tk3y.betterParse.lexer.Token


/**
 * Container class for the all available expression tokens
 *
 * @param allCurrenciesRegex regular expression which matches all available currency aliases to
 *                           determine which token is a currency and which is just a malformed input
 */
@ExperimentalStdlibApi
class TokenDictionary(allCurrenciesRegex: Regex) {

    // greedy whitespace occupation and optional integer part
    val number = token("number", "((\\d\\s*)+)?[.,](\\s*\\d)+|(\\d\\s*)*\\d")
    val currency = token("currency", allCurrenciesRegex)

    // metric suffix must be placed after currency in the token list to support aliases which starts with the one of the suffixes
    val kilo = token("kilo suffix", "[кКkK]")
    val mega = token("mega suffix", "[мМmM]")

    val exclamation = token("'!'", "!")
    val ampersand = token("'&'", "&")

    // currency can be added with this prefix to allow expressions like '1 dollar into euro'
    val inIntoUnion = token("union 'в'/'in'/'into'", "(?<=\\s)(into|in|в)(?=\\s)")

    val whitespace = token("space", "\\s+", ignore = true)

    val leftPar = token("'('", "\\(")
    val rightPar = token("')'", "\\)")

    val multiply = token("'*'", "\\*")
    val divide = token("'/'", "/")
    val minus = token("'-'", "-")
    val plus = token("'+'", "\\+")

    /**
     * This token is for proper error handling: it placed last and would be captured only of no other (valid) tokens matched.
     * @see InvalidCurrencyFoundException
     */
    val invalidCurrencyToken = token("invalid currency", CurrencyAliasMatcher.BROAD_ALIAS_REGEX)

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