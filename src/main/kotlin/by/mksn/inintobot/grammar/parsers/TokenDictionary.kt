package by.mksn.inintobot.grammar.parsers

import com.github.h0tk3y.betterParse.grammar.token
import com.github.h0tk3y.betterParse.lexer.Token


/**
 * Container class for the all available expression tokens
 *
 * @param currencyOrApiRegex regular expression which matches all available currency or api name aliases to
 *                           determine which token is a currency or api name and which is just a malformed input
 */
class TokenDictionary(currencyOrApiRegex: Regex) {

    // greedy whitespace occupation and optional integer part
    val number = token("number", "((\\d\\s*)+)?[.,](\\s*\\d)+|(\\d\\s*)*\\d")
    val currencyOrApi = token("currency or api alias", currencyOrApiRegex)

    // metric suffix must be placed after currency in the token list to support aliases which starts with the one of the suffixes
    val kilo = token("kilo suffix", "[кКkK]")
    val mega = token("mega suffix", "[мМmM]")

    val exclamation = token("'!'", "!")
    val ampersand = token("'&'", "&")
    val decimalDigitsOption = token("decimal digits option '#'", "#")

    // currency can be added with this prefix to allow expressions like '1 dollar into euro'
    val inIntoUnion = token("union 'в'/'на'/'in'/'into'", "(?<=\\s)(?iu)(into|in|в|на)(?-iu)(?=\\s)")

    val whitespace = token("space", "\\s+", ignore = true)

    val leftPar = token("'('", "\\(")
    val rightPar = token("')'", "\\)")

    val multiply = token("'*'", "\\*")
    val divide = token("'/'", "/")
    val minus = token("'-'", "-")
    val plus = token("'+'", "\\+")

    val allTokens: List<Token> = listOf(
        number, currencyOrApi,
        kilo, mega,
        exclamation, ampersand, inIntoUnion,
        decimalDigitsOption,
        whitespace,
        leftPar, rightPar,
        multiply, divide, minus, plus
    )
}