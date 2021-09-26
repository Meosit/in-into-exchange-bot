package by.mksn.inintobot.grammar.parsers

import by.mksn.inintobot.grammar.configurableRegexToken
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken


/**
 * Container class for the all available expression tokens
 *
 * @param currencyOrApiRegex regular expression which matches all available currency or api name aliases to
 *                           determine which token is a currency or api name and which is just a malformed input
 */
class TokenDictionary(currencyOrApiRegex: Regex) {

    // greedy whitespace occupation and optional integer part
    val number = regexToken("number", "((\\d\\s*)+)?[.,](\\s*\\d)+|(\\d\\s*)*\\d")
    val currencyOrApi = regexToken("currency or api alias", currencyOrApiRegex)

    // metric suffix must be placed after currency in the regexToken list to support aliases which starts with the one of the suffixes
    val kilo = regexToken("kilo suffix", "[кКkK]")
    val mega = regexToken("mega suffix", "[мМmM]")

    val exclamation = literalToken("'!'", "!")
    val ampersand = literalToken("'&'", "&")
    val decimalDigitsOption = literalToken("decimal digits option '#'", "#")

    // currency can be added with this prefix to allow expressions like '1 dollar into euro'
    val inIntoUnion = configurableRegexToken("union 'в'/'на'/'in'/'into'", "\\b(?iu)(into|in|to|в|на)(?-iu)(?=\\s)", useTransparentBounds = true)

    val whitespace = regexToken("space", "\\s+", ignore = true)

    val leftPar = literalToken("'('", "(")
    val rightPar = literalToken("')'", ")")

    val multiply = literalToken("'*'", "*")
    val divide = literalToken("'/'", "/")
    val minus = literalToken("'-'", "-")
    val plus = literalToken("'+'", "+")

    val allTokens: List<Token> = listOf(
        number,
        // inIntoUnion must be placed before currency or API regexToken to avoid 'в' letter
        // to be treated as dollar alias rather than additional currency union.
        // There is a drawback that ' в ' will always be treated as additional currency
        // union but the cases where this is not expected are extremely rare so it's OK
        // to live with that
        inIntoUnion,
        currencyOrApi,
        kilo, mega,
        exclamation, ampersand,
        decimalDigitsOption,
        whitespace,
        leftPar, rightPar,
        multiply, divide, minus, plus
    )
}