package org.mksn.inintobot.exchange.grammar.alias

import org.mksn.inintobot.currency.Currencies
import org.mksn.inintobot.currency.Currency

object CurrencyAliases : AutocompleteAliasMatcher<Currency>() {

    override val aliases: Map<String, Currency> = sequence {
        for (c in Currencies) {
            yield(c.code to c)
            c.aliases.forEach { it to c }
        }
    }.toMap()

}