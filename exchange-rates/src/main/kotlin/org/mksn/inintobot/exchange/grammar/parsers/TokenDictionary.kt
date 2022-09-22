package org.mksn.inintobot.exchange.grammar.parsers

import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import org.mksn.inintobot.common.currency.Currency
import org.mksn.inintobot.common.rate.RateApi
import org.mksn.inintobot.exchange.grammar.alias.AliasMatcher
import org.mksn.inintobot.exchange.grammar.alias.aliasMatchToken
import org.mksn.inintobot.exchange.grammar.configurableRegexToken


/**
 * Container class for the all available expression tokens
 *
 * @param currencyOrApiRegex regular expression which matches all available currency or api name aliases to
 *                           determine which token is a currency or api name and which is just a malformed input
 */
class TokenDictionary(
    currencyAliasMatcher: AliasMatcher<Currency>,
    apiAliasMatcher: AliasMatcher<RateApi>,
) {

    // greedy whitespace occupation and optional integer part, also with optional thousand separators
    // regex parts : "decimal with thousands" or "decimal with optional integer part" or "regular integer"
    val number = regexToken("number", "(\\d\\s*){1,3}[,.]((\\d\\s*){3}[,.])+(\\s*\\d)+|((\\d\\s*)+)?[.,](\\s*\\d)+|(\\d\\s*)*\\d")
    val currencyAlias = aliasMatchToken("currency alias", currencyAliasMatcher)
    val apiAlias = aliasMatchToken("api alias", apiAliasMatcher)

    // metric suffix must be placed after currency in the regexToken list to support aliases which starts with the one of the suffixes
    val kilo = configurableRegexToken("kilo suffix", "((?<=[\\s0-9кКkK]))[кКkK]", useTransparentBounds = true)
    val mega = configurableRegexToken("mega suffix", "((?<=[\\s0-9мМmM]))[мМmM]", useTransparentBounds = true)

    val exclamation = literalToken("'!'", "!")
    val ampersand = literalToken("'&'", "&")
    val hashtag = literalToken("'#'", "#")

    // currency union can be added with this prefix to allow expressions like '1 dollar into euro on 2022-01-02'
    val inIntoUnion = configurableRegexToken("union 'в'/'in'/'into'/'to'", "(?<=\\s)(?iu)(into|in|to|в)(?-iu)(?=\\s)", useTransparentBounds = true)

    val dateKey = configurableRegexToken("dateKey", "([?]|(?<=\\s)(?iu)(on|at|на|за))\\s*([-<]\\s*\\d+|[12]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])|(0[1-9]|[12]\\d|3[01])/(0[1-9]|1[0-2])/[12]\\d{3})", useTransparentBounds = true)

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
        // union but the cases where this is not expected are extremely rare, so it's OK
        // to live with that
        inIntoUnion,
        currencyAlias,
        apiAlias,
        kilo, mega,
        exclamation, ampersand,
        hashtag,
        whitespace,
        leftPar, rightPar,
        multiply, divide, minus, plus,
        dateKey
    )
}